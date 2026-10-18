"""Convert Bank Skill Sorting's SkillBankData.java tab lists + icons to default-tabs.json.

usage: python -I bss_tabs.py <SkillBankData.java> <out.json>
"""
import json
import re
import sys

src, out = sys.argv[1:3]
text = open(src, encoding='utf-8').read()
no_comments = re.sub(r'//[^\n]*', '', text)

consts = dict(re.findall(r'public static final String (TAG_\w+) = "([^"]+)";', text))
icons = {consts[k]: int(v) for k, v in re.findall(r'i\.put\((TAG_\w+), (\d+)\);', text.split('LEGACY_TAG_ICONS')[0])}

# Tab order = order of addX(m) calls in the static initializer
methods = re.findall(r'\n\t\tadd(\w+)\(m\);', text)
bodies = {}
for name, const, body in re.findall(r'private static void add(\w+)\(Map<String, List<Integer>> m\)\s*\{.*?m\.put\((TAG_\w+), Arrays\.asList\((.*?)\)\);', no_comments, re.S):
	bodies[name] = (consts[const], [int(n) for n in re.findall(r'-?\d+', body)])

tabs = []
for name in methods:
	tab_id, items = bodies[name]
	tabs.append({'id': tab_id, 'icon': icons.get(tab_id, 0), 'items': items})
	print(f'{tab_id}: {len(items)} items, icon {icons.get(tab_id)}')

missing = set(bodies) - set(methods)
print('unreferenced methods:', missing or 'none')
with open(out, 'w', encoding='utf-8', newline='\n') as f:
	json.dump({'tabs': tabs}, f, separators=(',', ':'))
