#!/usr/bin/env bash
# Intent: Claude Code PreToolUse Hook interceptor enforcing write-scope authorization.
#
# Usage (in .claude.json):
#   "hooks": {
#     "PreToolUse": "bash .claude/hooks/check-write-scope-hook.sh \"allowed_path_1/\" \"allowed_path_2\""
#   }
#
# Payload Expectation:
#   Receives JSON payload on stdin containing tool execution details.

set -euo pipefail

respond_with_deny() {
    local refusal_reason="$1"
    jq -n --arg reason "$refusal_reason" '{
        hookSpecificOutput: {
            hookState: "DENY",
            message: $reason
        }
    }'
    exit 0
}

respond_with_allow() {
    exit 0
}

assert_required_dependencies() {
    if ! command -v jq &> /dev/null; then
        respond_with_deny "PreToolUse Hook Environment Error: 'jq' tool is missing."
    fi
}

assert_policy_engine_exists() {
    local engine_script="$1"
    if [ ! -f "$engine_script" ]; then
        respond_with_deny "PreToolUse Hook Environment Error: Policy script missing at $engine_script"
    fi
}

extract_target_file_from_payload() {
    local payload_json="$1"
    echo "$payload_json" | jq -r '.tool_input.file_path // .tool_input.path // empty'
}

locate_policy_engine_script() {
    local hook_dir
    hook_dir="$(cd "$(dirname "$0")" && pwd)"
    echo "$(cd "$hook_dir/.." && pwd)/check-write-scope.sh"
}

main() {
    assert_required_dependencies

    local stdin_payload
    stdin_payload=$(cat)
    if [ -z "$stdin_payload" ]; then
        respond_with_deny "PreToolUse Hook Protocol Error: Received empty JSON payload."
    fi

    local target_file_path
    target_file_path=$(extract_target_file_from_payload "$stdin_payload")

    # WHY: Read-only tool calls or commands without path arguments carry no write risk.
    if [ -z "$target_file_path" ]; then
        respond_with_allow
    fi

    local policy_engine
    policy_engine=$(locate_policy_engine_script)
    assert_policy_engine_exists "$policy_engine"

    local configured_allowed_scopes=("$@")

    set +e
    bash "$policy_engine" "$target_file_path" "${configured_allowed_scopes[@]}"
    local evaluation_result=$?
    set -e

    if [ "$evaluation_result" -eq 0 ]; then
        respond_with_allow
    else
        respond_with_deny "SCOPE VIOLATION: Unauthorized write access to '$target_file_path'. Permitted scopes: $*"
    fi
}

main "$@"