#!/usr/bin/env python3
"""Check translation coverage, format contracts, and production UI resource use."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
folders = ('values', 'values-en', 'values-ko', 'values-ja', 'values-b+zh+Hans', 'values-b+zh+Hant')
def read(folder):
    nodes = ET.parse(ROOT/'res'/folder/'strings.xml').getroot().findall('string')
    strings = {n.attrib['name']: n for n in nodes}
    assert len(strings) == len(nodes), f'Duplicate keys in {folder}'
    assert all(n.text and n.text.strip() for n in nodes), f'Blank translation in {folder}'
    return strings
base = read('values')
assert (ROOT/'res/values/strings.xml').read_bytes() == (ROOT/'res/values-en/strings.xml').read_bytes(), 'English must match the default fallback'
pattern = re.compile(r'%(\d+)\$([.\d]*[sdf])')
for folder in folders:
    strings = read(folder)
    assert strings.keys() == base.keys(), (folder, strings.keys() ^ base.keys())
    for key, node in strings.items():
        assert sorted(pattern.findall(node.text)) == sorted(pattern.findall(base[key].text)), (folder, key, 'format mismatch')
        if pattern.search(node.text):
            rest = pattern.sub('', node.text).replace('%%', '')
            assert '%' not in rest, (folder, key, 'unescaped percent')
        else:
            assert node.get('formatted') == 'false', (folder, key, 'literal format')
        if folder != 'values-ko' and not key.startswith('key_'):
            assert not re.search('[가-힣]', node.text), (folder, key, 'Korean text leaked')
# These are the current native product surfaces. Legacy virtual-display experiments are excluded.
for name in ('NativeActivity', 'NativeRangeActivity', 'NativeService', 'FloatingControls', 'PointerPractice', 'TouchSide', 'ReachUi', 'ReachTile'):
    source = (ROOT/'src/dev/foldpatch'/f'{name}.java').read_text()
    assert not re.search(r'"[^"\n]*[가-힣][^"\n]*"', source), (name, 'hard-coded UI text')
    for key in re.findall(r'R\.string\.(\w+)', source):
        assert key in base, (name, key, 'missing resource')
config = ET.parse(ROOT/'res/xml/locales_config.xml').getroot()
assert {n.get('{http://schemas.android.com/apk/res/android}name') for n in config} == {'en','ko','ja','zh-Hans','zh-Hant'}
print(f'Locale checks passed: {len(base)} keys × {len(folders)} locales, matching placeholders, no missing native UI resources')
