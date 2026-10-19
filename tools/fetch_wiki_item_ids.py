"""Download the OSRS Wiki's item name -> item ID table (bucket 'infobox_item') to a JSON file.

usage: python -I fetch_wiki_item_ids.py <out.json>

Output: [{"name": item_name, "page": page_name, "sub": page_name_sub, "ids": [int, ...]}, ...]
Used by convert_wiki_storage_sets.py to turn wiki piece names into item IDs. Polite: one request per 500 rows.
"""
import json
import sys
import time
import urllib.parse
import urllib.request

API = 'https://oldschool.runescape.wiki/api.php'
UA = 'ultimate-bank-plugin data tool (https://github.com/LukRLPlugins/ultimate-bank-plugin)'
PAGE = 500

out = sys.argv[1]
rows, offset = [], 0
while True:
	query = ("bucket('infobox_item').select('item_name','item_id','page_name','page_name_sub')"
		f".limit({PAGE}).offset({offset}).run()")
	url = API + '?' + urllib.parse.urlencode({'action': 'bucket', 'format': 'json', 'query': query})
	with urllib.request.urlopen(urllib.request.Request(url, headers={'User-Agent': UA}), timeout=60) as r:
		data = json.load(r)
	if 'error' in data:
		raise SystemExit(data['error'])
	batch = data.get('bucket', [])
	for b in batch:
		ids = [int(i) for i in (b.get('item_id') or []) if str(i).isdigit()]
		if ids:
			rows.append({'name': b.get('item_name', ''), 'page': b.get('page_name', ''), 'sub': b.get('page_name_sub', ''), 'ids': ids})
	print(f'offset {offset}: {len(batch)} rows', file=sys.stderr)
	if len(batch) < PAGE:
		break
	offset += PAGE
	time.sleep(0.5)

with open(out, 'w', encoding='utf-8', newline='\n') as f:
	json.dump(rows, f, ensure_ascii=False)
print(f'{len(rows)} items with IDs', file=sys.stderr)
