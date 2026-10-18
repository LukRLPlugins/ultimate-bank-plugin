#!/usr/bin/env bash
# Helper for the LukRLPlugins project board (org project #1), used by the /idea, /prototype, /approve and
# /status skills. Columns and fields are looked up by name, so renaming IDs on GitHub doesn't break it.
#
#   board.sh whoami                         exit 1 unless the active gh account is Luk-Plugins
#   board.sh set <issue-or-pr-url> <Status> [Plugin]
#                                           add the item to the board (if needed) and set its Status / Plugin
#   board.sh list                           every board item as: status | plugin | #num | title | url
set -euo pipefail

OWNER=LukRLPlugins
PROJECT=1
LUK=Luk-Plugins

whoami_check()
{
	local login
	login=$(gh api user --jq .login)
	if [ "$login" != "$LUK" ]; then
		echo "Active gh account is '$login', not $LUK. Run: gh auth switch -u $LUK" >&2
		exit 1
	fi
}

project_id()
{
	gh project view "$PROJECT" --owner "$OWNER" --format json --jq .id
}

# field_ids <field name> <option name> -> "<field id> <option id>"
field_ids()
{
	gh project field-list "$PROJECT" --owner "$OWNER" --format json \
		--jq ".fields[] | select(.name==\"$1\") | \"\(.id) \(.options[] | select(.name==\"$2\") | .id)\""
}

item_id()
{
	local url=$1 id
	id=$(gh project item-list "$PROJECT" --owner "$OWNER" --format json --limit 1000 \
		--jq ".items[] | select(.content.url==\"$url\") | .id")
	if [ -z "$id" ]; then
		id=$(gh project item-add "$PROJECT" --owner "$OWNER" --url "$url" --format json --jq .id)
	fi
	echo "$id"
}

set_field()
{
	local pid=$1 item=$2 name=$3 value=$4 ids
	ids=$(field_ids "$name" "$value")
	if [ -z "$ids" ]; then
		echo "No $name option '$value' on the board" >&2
		exit 1
	fi
	gh project item-edit --project-id "$pid" --id "$item" \
		--field-id "${ids% *}" --single-select-option-id "${ids#* }" >/dev/null
}

case "${1:-}" in
	whoami)
		whoami_check
		;;
	set)
		whoami_check
		url=$2 status=$3 plugin=${4:-}
		pid=$(project_id)
		item=$(item_id "$url")
		set_field "$pid" "$item" Status "$status"
		if [ -n "$plugin" ]; then
			set_field "$pid" "$item" Plugin "$plugin"
		fi
		echo "$url -> $status${plugin:+ ($plugin)}"
		;;
	list)
		gh project item-list "$PROJECT" --owner "$OWNER" --format json --limit 1000 \
			--jq '.items[] | "\(.status // "-") | \(.plugin // "-") | #\(.content.number // "-") | \(.content.title) | \(.content.url // "")"'
		;;
	*)
		sed -n '2,9p' "$0" | sed 's/^# \{0,1\}//'
		exit 2
		;;
esac
