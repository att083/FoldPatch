#!/usr/bin/env python3
"""Check generated release surface, permissions and pinned dependency integrity."""
import hashlib
from pathlib import Path
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
A = '{http://schemas.android.com/apk/res/android}'
with tempfile.TemporaryDirectory() as tmp:
    output = Path(tmp) / 'AndroidManifest.xml'
    subprocess.run([sys.executable, str(ROOT/'scripts/prepare-manifest.py'), 'release', str(output)], check=True)
    manifest = ET.parse(output).getroot()
    assert manifest.get('package') == 'dev.foldpatch'
    app = manifest.find('application')
    assert {p.get(A+'authorities') for p in app.findall('provider')} == {'dev.foldpatch.shizuku'}
    for component in ('pointer_accessibility.xml', 'keyboard.xml'):
        settings = ET.parse(ROOT/'res/xml'/component).getroot().get(A+'settingsActivity')
        assert settings == 'dev.foldpatch.NativeActivity', (component, settings)
    for source in (ROOT/'src').rglob('*.java'):
        text = source.read_text()
        assert 'dev.reachpad' not in text, source
        if source.is_relative_to(ROOT/'src/dev/foldpatch'):
            assert text.startswith('package dev.foldpatch;'), source
    assert app.get(A+'debuggable') == 'false'
    assert app.get(A+'allowBackup') == 'false'
    assert manifest.find('uses-sdk').get(A+'minSdkVersion') == '35'
    assert manifest.find('uses-sdk').get(A+'targetSdkVersion') == '37'
    for name in ('.NativeActivity', '.NativeRangeActivity'):
        activity = next(e for e in app.findall('activity') if e.get(A+'name') == name)
        # Android 15 needs explicit opt-in; Android 16's forced behavior can hide
        # this omission and make a subpage Back unexpectedly finish the activity.
        assert activity.get(A+'enableOnBackInvokedCallback') == 'true', name
    assert int(manifest.get(A+'versionCode')) > 0 and manifest.get(A+'versionName')
    names = {e.get(A+'name') for e in app}
    assert not names.intersection({'.ProbeActivity','.InputProbeActivity','.LatencyProbeActivity','.BridgeProvider','.TouchSideProbeActivity','.LocalizationProbeActivity','.KeyboardEditProbeActivity','.InputIntegrityProbeActivity','.PointerDriverReceiver'})
    public_activities = {e.get(A+'name') for e in app.findall('activity') if e.get(A+'exported') == 'true'}
    assert public_activities == {'.NativeActivity'}, public_activities
    permissions = {e.get(A+'name') for e in manifest.findall('uses-permission')}
    assert 'android.permission.INTERNET' not in permissions
    assert 'android.permission.READ_EXTERNAL_STORAGE' not in permissions
    assert 'android.permission.QUERY_ALL_PACKAGES' not in permissions
for line in (ROOT/'libs/SHA256SUMS').read_text().splitlines():
    expected, filename = line.split()
    assert hashlib.sha256((ROOT/'libs'/filename).read_bytes()).hexdigest() == expected, filename
print('Release checks passed: SDK, version, non-debuggable, entry points, permissions, dependency hashes')
