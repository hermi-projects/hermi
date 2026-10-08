#!/usr/bin/env bash
# Intent: Evaluates whether a candidate file path is permitted by any allowed path scope.
#
# Usage: ./check-write-scope.sh <candidate_path> [allowed_scope_1 allowed_scope_2 ...]
#
# Exit Codes:
#   0 - Access Granted
#   1 - Access Denied
#   2 - Invalid Usage / Shell Error

set -euo pipefail

is_directory_scope() {
    local path_pattern="$1"
    [[ "$path_pattern" == */ ]]
}

is_exact_file_match() {
    local candidate_path="$1"
    local allowed_path="$2"
    [ "$candidate_path" = "$allowed_path" ]
}

is_within_directory_scope() {
    local candidate_path="$1"
    local scope_directory="$2"

    # WHY: Trailing slash prevents prefix false-positives (e.g., '/app/src_backup' matching '/app/src')
    [[ "$scope_directory" == */ ]] || scope_directory="${scope_directory}/"
    [[ "$candidate_path" == "$scope_directory"* ]]
}

canonicalize_path() {
    local raw_path="$1"
    local retains_dir_suffix=0

    is_directory_scope "$raw_path" && retains_dir_suffix=1

    local IFS='/'
    read -ra raw_segments <<< "$raw_path"
    local clean_segments=()

    for segment in "${raw_segments[@]}"; do
        case "$segment" in
            ""|".") continue ;;
            "..")
                if [ ${#clean_segments[@]} -gt 0 ]; then
                    unset 'clean_segments[${#clean_segments[@]}-1]'
                fi
                ;;
            *) clean_segments+=("$segment") ;;
        esac
    done

    local canonicalized
    canonicalized=$(IFS='/'; echo "${clean_segments[*]}")

    [[ "$raw_path" == /* ]] && canonicalized="/${canonicalized}"

    if [ "$retains_dir_suffix" -eq 1 ] && [ -n "$canonicalized" ] && [ "$canonicalized" != "/" ]; then
        canonicalized="${canonicalized}/"
    fi

    echo "$canonicalized"
}

is_candidate_permitted_by_scope() {
    local candidate_path="$1"
    local allowed_scope="$2"

    local canonical_candidate
    canonical_candidate=$(canonicalize_path "$candidate_path")

    local canonical_scope
    canonical_scope=$(canonicalize_path "$allowed_scope")

    if is_directory_scope "$allowed_scope"; then
        is_within_directory_scope "$canonical_candidate" "$canonical_scope"
    else
        is_exact_file_match "$canonical_candidate" "$canonical_scope"
    fi
}

evaluate_access_permission() {
    local candidate_path="$1"
    shift
    local allowed_scopes=("$@")

    for scope in "${allowed_scopes[@]}"; do
        [ -n "$scope" ] || continue
        if is_candidate_permitted_by_scope "$candidate_path" "$scope"; then
            return 0
        fi
    done

    return 1
}

validate_cli_arguments() {
    if [ "$#" -lt 1 ]; then
        echo "Usage: $0 <path_to_check> [allowed_path_1 allowed_path_2 ...]" >&2
        exit 2
    fi
}

main() {
    validate_cli_arguments "$@"

    local target_file_path="$1"
    shift
    local allowed_path_scopes=("$@")

    if [ ${#allowed_path_scopes[@]} -eq 0 ]; then
        exit 1
    fi

    if evaluate_access_permission "$target_file_path" "${allowed_path_scopes[@]}"; then
        exit 0
    else
        exit 1
    fi
}

main "$@"