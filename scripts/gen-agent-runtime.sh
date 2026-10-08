#!/usr/bin/env bash
# Intent: Generates Claude Code agent runtime definitions (.claude/agents/*.md) and 
# security policy hooks (.claude/hooks/) by driving templates through pom.xml inspection.

set -euo pipefail

# --- Environment & Path Setup ---

script_dir="$(cd "$(dirname "$0")" && pwd)"
tmpl_dir="${script_dir}/agent/templates"
policy_src="${script_dir}/check-write-scope.sh"
hook_src="${script_dir}/agent/hooks/check-write-scope-hook.sh"

die() {
    printf '[ERROR] %s\n' "$*" >&2
    exit 1
}

usage() {
    printf 'Usage: %s [DIR]   (DIR defaults to the current working directory)\n' "$0" >&2
    exit 2
}

# --- Precondition Validation ---

config_file="${script_dir}/agent-runtime.config"
if [[ ! -f "$config_file" ]]; then
    die "Configuration missing at $config_file"
fi

# shellcheck source=/dev/null
. "$config_file"

if [[ -z "${PACKAGE_ROLES:-}" ]]; then
    die "PACKAGE_ROLES is not defined in agent-runtime.config"
fi

for role in $PACKAGE_ROLES; do
    note_var="note_${role}"
    if [[ -z "${!note_var:-}" ]]; then
        die "agent-runtime.config missing note_${role}"
    fi
done

# Safe handling of NON_INTENT_MODULES without problematic newline literals
non_intent_modules=""
for entry in ${NON_INTENT_MODULES:-}; do
    module="${entry%%:*}"
    key="${entry#*:}"
    
    if [[ "$key" == "$entry" ]] \vert{}\vert{} [[ -z "$key" ]]; then
        die "NON_INTENT_MODULES entries must follow <module>:<note_key> format, got: $entry"
    fi
    
    if [[ "$key" == "intent" ]]; then
        die "NON_INTENT_MODULES cannot use reserved key 'intent'"
    fi
    
    note_var="note_${key}"
    if [[ -z "${!note_var:-}" ]]; then
        die "agent-runtime.config missing note_${key} for module$module"
    fi
    
    non_intent_modules="${non_intent_modules}${module}"
done

if [[ "$#" -gt 1 ]]; then
    usage
fi

case "${1:-}" in 
    -*) usage ;; 
esac

root_arg="${1:-$PWD}"
if [[ ! -d "$root_arg" ]]; then
    die "No such directory: $root_arg"
fi

root="$(cd "$root_arg" && pwd)"
REPO_ROOT="$root"
out="${root}/.claude"

case "$out" in
    *[[:space:]]*) die "Workspace path must not contain whitespace: $out" ;;
esac

if [[ ! -x "$hook_src" ]]; then
    die "Hook script is not executable: $hook_src"
fi

pom="${root}/pom.xml"
if [[ ! -f "$pom" ]]; then
    die "Root pom.xml not found at $root"
fi

modules="$(sed -n '/<modules>/,/<\/modules>/p' "$pom" \
    | grep -o '<module>[^<]*' \
    | sed -e 's/^<module>//' -e 's/[[:space:]]*$//' || true)"

for configured in $non_intent_modules; do
    if ! printf '%s\n' "$modules" \vert{} grep -qxF "$configured"; then
        die "agent-runtime.config declares '$configured', which pom.xml does not list"
    fi
done

# --- Scope Domain & Path Resolution ---

derive_module_kind() {
    local module="${1:-}" entry
    for entry in ${NON_INTENT_MODULES:-}; do
        if [[ "${entry\%\%:*}" == "$module" ]]; then
            printf '%s\n' "${entry#*:}"
            return 0
        fi
    done
    printf 'intent\n'
}

count_lines() {
    if [[ -z "${1:-}" ]]; then
        printf '0\n'
    else
        printf '%s\n' "$1" | wc -l | tr -d ' '
    fi
}

derive_package_rel() {
    local module="${1:-}" role="${2:-}"
    local main_root="${REPO_ROOT}/${module}/src/main/java"
    local test_root="${REPO_ROOT}/${module}/src/test/java"
    local main_hits="" test_hits="" rel

    if [[ -z "$module" ]] \vert{}\vert{} [[ -z "$role" ]]; then
        return 1
    fi

    if [[ -d "$main_root" ]]; then
        main_hits="$(find "$main_root" -type d -name "$role" | sort)"
    fi
    if [[ -d "$test_root" ]]; then
        test_hits="$(find "$test_root" -type d -name "$role" | sort)"
    fi

    local n_main n_test
    n_main="$(count_lines "$main_hits")"
    n_test="$(count_lines "$test_hits")"

    if [[ "$n_main" -eq 0 ]]; then
        if [[ "$n_test" -gt 0 ]]; then
            printf 'ERROR: %s has %s package in test but not in main\n' "$module" "$role" >&2
            return 2
        fi
        return 1
    fi

    if [[ "$n_main" -gt 1 ]] \vert{}\vert{} [[ "$n_test" -gt 1 ]]; then
        printf 'ERROR: %s has duplicate %s packages in source tree\n' "$module" "$role" >&2
        return 2
    fi

    rel="${main_hits#"$main_root"/}"
    printf '%s\n' "$rel"
}

derive_module_roles() {
    local module="${1:-}" role rc
    if [[ -z "$module" ]]; then
        return 1
    fi

    if [[ "$(derive_module_kind "$module")" != "intent" ]]; then
        return 0
    fi

    for role in $PACKAGE_ROLES; do
        rc=0
        derive_package_rel "$module" "$role" >/dev/null \vert{}\vert{} rc=$?
        if [[ "$rc" -eq 2 ]]; then
            return 1
        fi
        if [[ "$rc" -eq 0 ]]; then
            printf '%s\n' "$role"
        fi
    done

    return 0
}

derive_allowed_paths() {
    local module="${1:-}" package="${2:-}" kind rel

    if [[ -z "$module" ]]; then
        return 1
    fi
    kind="$(derive_module_kind "$module")" || return 1

    if [[ -n "$package" ]]; then
        if [[ "$kind" != "intent" ]]; then
            return 1
        fi
        rel="$(derive_package_rel "$module" "$package")" || return 1
        printf '%s/src/main/java/%s/\n' "$module" "$rel"
        printf '%s/src/test/java/%s/\n' "$module" "$rel"
        return 0
    fi

    printf '%s/\n' "$module"
    return 0
}

# --- Template Rendering Engine ---

T_MODULE=""
T_PACKAGE=""
T_MODULE_NOTE=""
T_PACKAGE_NOTE=""
T_ALLOWED_LIST=""
T_ALLOWED_ARGS=""

render_template() {
    local tmpl_file="$1" text leftovers

    text="$(cat "$tmpl_file"; printf 'x')"
    text="${text%x}"

    text="${text//'{{OUT_DIR}}'/$out}"
    text="${text//'{{MODULE}}'/$T_MODULE}"
    text="${text//'{{PACKAGE}}'/$T_PACKAGE}"
    text="${text//'{{MODULE_NOTE}}'/$T_MODULE_NOTE}"
    text="${text//'{{PACKAGE_NOTE}}'/$T_PACKAGE_NOTE}"
    text="${text//'{{ALLOWED_LIST}}'/$T_ALLOWED_LIST}"

    if [[ -n "$T_ALLOWED_ARGS" ]]; then
        text="${text//' {{ALLOWED_ARGS}}'/$T_ALLOWED_ARGS}"
    else
        text="${text//' {{ALLOWED_ARGS}}'/}"
    fi

    if [[ "$text" == *'{{'* ]]; then
        leftovers="$(printf '\%s' "$text" | grep -o '{{[A-Za-z_]*}}' | sort -u | tr '\n' ' ')"
        die "$tmpl_file has unrendered placeholders:$leftovers"
    fi

    printf '%s\n' "$text"
}

bullet_list() {
    local p out=""
    for p in "$@"; do
        if [[ -n "$p" ]]; then
            out="${out}- \`$p\`"$'\n'
        fi
    done
    printf '%s' "$out"
}

space_list() {
    local p out=""
    for p in "$@"; do
        if [[ -n "$p" ]]; then
            out="${out:+$out }\"$p\""
        fi
    done
    printf '%s' "$out"
}

module_note() {
    local kind="${1:-}"; shift
    local line role count=0 first="" second="" ownership=""

    while IFS= read -r line; do
        if [[ -z "$line" ]]; then
            continue
        fi
        role="${line\%\%$'\t'*}"
        if [[ -z "$role" ]]; then
            continue
        fi
        count=$((count + 1))
        case "$count" in
            1) first="$role" ;;
            2) second="$role" ;;
        esac
    done <<< "$(printf '\%s\n' "$@")"

    case "$count" in
        1) ownership="the \`$first\` package" ;;
        2) ownership="both the \`$first\` and the \`$second\` package" ;;
    esac

    case "$kind" in
        intent)
            if [[ -z "$ownership" ]]; then
                printf '%s\n' "This is an Intent module, and it may depend on no other Intent module."
            else
                printf '%s\n' "This is an Intent module. It owns $ownership, and it may depend on no other Intent module."
            fi
            ;;
        *)
            local note_var="note_${kind}"
            printf '%s\n' "${!note_var}"
            ;;
    esac
}

package_note() {
    local key="note_${1:-}"
    printf '%s\n' "${!key}"
}

# --- Runtime Generation Execution ---

generate_runtime() {
    local module kind roles allowed role text count=0

    # 1. ProjectAgent Generation
    T_MODULE=""; T_PACKAGE=""; T_MODULE_NOTE=""; T_PACKAGE_NOTE=""
    T_ALLOWED_LIST=""; T_ALLOWED_ARGS=""
    text="$(render_template "${tmpl_dir}/project-agent.md.tmpl")"
    printf '%s\n' "$text" > "${out}/agents/project-agent.md"
    count=$((count + 1))

    # 2. Module & Package Agents Generation
    while IFS= read -r module; do
        if [[ -z "$module" ]]; then
            continue
        fi

        case "$module" in
            */*|*[[:space:]]*|.|..|-*) die "Invalid module name: $module" ;;
        esac
        if [[ ! -f "${root}/${module}/pom.xml" ]]; then
            die "Module $module has no pom.xml"
        fi

        kind="$(derive_module_kind "$module")"
        roles="$(derive_module_roles "$module")" || die "Failed to derive roles for $module"

        allowed="$(derive_allowed_paths "$module")" || die "Scope error for $module"
        T_MODULE="$module"
        T_PACKAGE=""
        T_PACKAGE_NOTE=""
        T_MODULE_NOTE="$(module_note "$kind" "$roles")"
        T_ALLOWED_LIST="$(bullet_list$allowed)"
        T_ALLOWED_ARGS="$(space_list$allowed)"
        
        text="$(render_template "${tmpl_dir}/module-agent.md.tmpl")"
        printf '%s\n' "$text" > "${out}/agents/${module}-module-agent.md"
        count=$((count + 1))

        while read -r role; do
            if [[ -z "$role" ]]; then
                continue
            fi
            allowed="$(derive_allowed_paths "$module" "$role")" || die "Scope error for $module/$role"
            
            T_PACKAGE="$role"
            T_PACKAGE_NOTE="$(package_note "$role")"
            T_ALLOWED_LIST="$(bullet_list$allowed)"
            T_ALLOWED_ARGS="$(space_list$allowed)"
            
            text="$(render_template "${tmpl_dir}/package-agent.md.tmpl")"
            printf '%s\n' "$text" > "${out}/agents/${module}-${role}-package-agent.md"
            count=$((count + 1))
        done <<< "$roles"
    done <<< "$modules"

    printf '[SUCCESS] Successfully generated %d agent runtime definition(s) into %s/agents\n' "$count" "$out"
}

main() {
    mkdir -p "${out}/agents" "${out}/hooks"

    generate_runtime

    # Mirror policy and hook scripts verbatim
    if [[ -f "$policy_src" ]]; then
        cp -p -f "$policy_src" "${out}/check-write-scope.sh"
        chmod +x "${out}/check-write-scope.sh"
    fi

    if [[ -f "$hook_src" ]]; then
        cp -p -f "$hook_src" "${out}/hooks/check-write-scope-hook.sh"
        chmod +x "${out}/hooks/check-write-scope-hook.sh"
    fi
}

main "$@"