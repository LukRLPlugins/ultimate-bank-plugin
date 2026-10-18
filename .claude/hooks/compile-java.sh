#!/usr/bin/env bash
# PostToolUse hook: after Claude edits a .java file, compile the project and
# report errors back to Claude (exit 2 = feedback shown to the model).
set -u

file_path=$(jq -r '.tool_input.file_path // empty')
case "$file_path" in
	*.java) ;;
	*) exit 0 ;;
esac

# Fall back to the Temurin 11 install if this shell predates the JDK install.
if ! command -v java >/dev/null 2>&1 && [ -z "${JAVA_HOME:-}" ]; then
	for jdk in "/c/Program Files/Eclipse Adoptium"/jdk-11*; do
		[ -d "$jdk" ] && export JAVA_HOME="$jdk"
	done
fi

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
if ! output=$(./gradlew compileJava compileTestJava -q --console=plain 2>&1); then
	echo "Compilation failed after editing $file_path:" >&2
	echo "$output" | grep -E 'error:|\.java:[0-9]+' | head -40 >&2
	exit 2
fi
exit 0
