"""Convert Wasted Bank Space location enums (legacy ItemID) into gameval-based int arrays.

usage: python -I convert.py <conv dir> <locations src dir> <out dir>
"""
import os
import re
import sys

conv, src, out = sys.argv[1:4]


def load(name):
	m = {}
	for line in open(os.path.join(conv, name)):
		k, v = line.strip().split(' = ')
		m.setdefault(k, int(v))
	return m


legacy = load('legacy.txt')
gameval_by_value = {}
for k, v in load('gameval.txt').items():
	gameval_by_value.setdefault(v, []).append(k)

ENTRY = re.compile(r'^\s*\w+\(\s*(?:ItemID\.(\w+)|(\d+))\s*(?:,\s*(true|false))?\s*\)[,;]?\s*(?:/\*.*\*/)?\s*(?://.*)?$')
COMMENT = re.compile(r'^\s*//\s?(.*)$')

HEADER = open(os.path.join(conv, 'header.txt')).read()
problems = []

for fname in sorted(os.listdir(src)):
	cls = fname[:-5]
	body, bis, seen = [], [], set()
	for line in open(os.path.join(src, fname), encoding='utf-8'):
		m = ENTRY.match(line)
		if m:
			legacy_name, raw, is_bis = m.groups()
			value = legacy.get(legacy_name) if legacy_name else int(raw)
			if value is None:
				problems.append(f'{cls}: unknown legacy constant {legacy_name}')
				continue
			names = gameval_by_value.get(value)
			if not names:
				problems.append(f'{cls}: no gameval name for {legacy_name or raw} ({value})')
				continue
			ref = 'ItemID.' + names[0]
			if value in seen:
				problems.append(f'{cls}: duplicate {ref}')
				continue
			seen.add(value)
			body.append('\t\t' + ref + ',')
			if is_bis == 'true':
				bis.append('\t\t' + ref + ',')
			continue
		c = COMMENT.match(line)
		if c and body is not None and seen:
			body.append('\t\t// ' + c.group(1).strip())
		elif c and not seen and c.group(1).strip():
			body.append('\t\t// ' + c.group(1).strip())
	with open(os.path.join(out, cls + 'Items.java'), 'w', encoding='utf-8', newline='\n') as f:
		f.write(HEADER)
		f.write('package com.ultimatebank.feature.storagesavings.data;\n\n')
		f.write('import net.runelite.api.gameval.ItemID;\n\n')
		f.write(f'final class {cls}Items\n{{\n')
		f.write('\tstatic final int[] ITEMS = {\n' + '\n'.join(body) + '\n\t};\n')
		if bis:
			f.write('\n\tstatic final int[] BIS = {\n' + '\n'.join(bis) + '\n\t};\n')
		f.write('\n\tprivate ' + cls + 'Items()\n\t{\n\t}\n}\n')
	print(f'{cls}: {len(seen)} items, {len(bis)} bis')

print('\n'.join(problems) or 'no problems')
