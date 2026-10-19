#!/usr/bin/env bash
# Picks which account the dev client logs in with, by copying ~/.runelite/credentials-<name>.properties over
# ~/.runelite/credentials.properties (the only file RuneLite's dev client reads). Run before ./gradlew run.
# Never prints the files' contents: they hold login tokens.
#
#   use-credentials.sh <name>     e.g. 1, 2, main, iron
#   use-credentials.sh --list     list saved accounts
set -euo pipefail

dir="$HOME/.runelite"

list()
{
	local found=0
	for f in "$dir"/credentials-*.properties; do
		[ -e "$f" ] || continue
		found=1
		f=${f##*/credentials-}
		echo "  ${f%.properties}"
	done
	[ $found = 1 ] || echo "  (none saved yet: copy credentials.properties to credentials-<name>.properties)"
}

name=${1:-}
if [ -z "$name" ] || [ "$name" = "--list" ]; then
	echo "Saved accounts:"
	list
	exit 0
fi

if [[ ! "$name" =~ ^[A-Za-z0-9_-]+$ ]]; then
	echo "Account name may only contain letters, digits, - and _" >&2
	exit 2
fi

src="$dir/credentials-$name.properties"
if [ ! -f "$src" ]; then
	echo "No saved account '$name'. Saved accounts:" >&2
	list >&2
	exit 1
fi

cp "$src" "$dir/credentials.properties"
echo "Dev client will log in as account '$name'"
