"""Build storage-sets.json (POH costume room sets and their pieces) from OSRS wiki "<storage> space" pages.

usage: python -I convert_wiki_storage_sets.py <raw wiki dir> <out.json>

<raw wiki dir> holds the raw wikitext (https://oldschool.runescape.wiki/w/<Page>?action=raw) of:
  Fancy_dress_box_space, Armour_case_space, Magic_wardrobe_space, Cape_rack_space, Treasure_chest_space

Output: {"<STORAGE>": [{"set": name, "items": [item names], "tier": clue tier or null, "accomplishment": bool}]}
Item names are resolved to IDs at runtime (only among that storage's item list).
"""
import json
import os
import re
import sys

src, out = sys.argv[1:3]

PAGES = {
	'FANCY_DRESS_BOX': 'Fancy_dress_box_space',
	'ARMOUR_CASE': 'Armour_case_space',
	'MAGIC_WARDROBE': 'Magic_wardrobe_space',
	'CAPE_RACK': 'Cape_rack_space',
	'TREASURE_CHEST': 'Treasure_chest_space',
}
TIERS = ['beginner', 'easy', 'medium', 'hard', 'elite', 'master']
SKILLS = ['Agility', 'Attack', 'Construct.', 'Cooking', 'Crafting', 'Defence', 'Farming', 'Firemaking', 'Fishing',
	'Fletching', 'Herblore', 'Hitpoints', 'Hunter', 'Magic', 'Mining', 'Prayer', 'Ranging', 'Runecraft', 'Sailing',
	'Slayer', 'Smithing', 'Strength', 'Thieving', 'Woodcutting', 'Woodcut.']
ACCOMPLISHMENT = {s + ' cape' for s in SKILLS} | {'Quest point cape', 'Achievement diary cape', 'Music cape'}

SET_ROW = re.compile(r'^\|\s*\[\[([^\]|]+)(?:\|([^\]]+))?\]\]')
ITEM = re.compile(r'\{\{[Pp]link[pt]?\|([^|}]+)')

result = {}
for storage, page in PAGES.items():
	text = open(os.path.join(src, page + '.txt'), encoding='utf-8').read()
	# Only the item tables, after the build/creation tables
	markers = ('==Armour sets==', '==Magical armour==', '==Capes==', '==Storable rewards==', '[[Beekeeper')
	start = min((text.find(h) for h in markers if text.find(h) >= 0), default=0)
	start = text.rfind('\n', 0, start) + 1
	lines = text[start:].splitlines()
	sets, tier, current = [], None, None
	for line in lines:
		heading = re.match(r'^=+\s*([^=]+?)\s*=+\s*$', line)
		if heading:
			name = heading.group(1).lower()
			if name.startswith('changes') or name.startswith('trivia'):
				break
			tier = next((t for t in TIERS if name.startswith(t)), None) if storage == 'TREASURE_CHEST' else None
			if storage == 'TREASURE_CHEST' and name.startswith('not stor'):
				tier = 'skip'
			continue
		row = SET_ROW.match(line)
		if row:
			current = {'set': (row.group(2) or row.group(1)).strip(), 'items': [], 'tier': tier,
				'accomplishment': storage == 'CAPE_RACK' and row.group(1).strip() in ACCOMPLISHMENT}
			if tier != 'skip':
				sets.append(current)
			continue
		if current is not None and ITEM.search(line):
			for name in ITEM.findall(line):
				name = name.strip()
				if name not in current['items']:
					current['items'].append(name)
			current = None
	sets = [s for s in sets if s['items']]
	result[storage] = sets
	print(f"{storage}: {len(sets)} sets, {sum(len(s['items']) for s in sets)} pieces"
		+ (f", accomplishment {sum(s['accomplishment'] for s in sets)}" if storage == 'CAPE_RACK' else '')
		+ (f", tiers {sorted({s['tier'] for s in sets}, key=str)}" if storage == 'TREASURE_CHEST' else ''))

with open(out, 'w', encoding='utf-8', newline='\n') as f:
	json.dump(result, f, ensure_ascii=False, separators=(',', ':'))
