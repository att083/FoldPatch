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
    app = manifest.find('application')
    assert app.get(A+'debuggable') == 'false'
    assert app.get(A+'allowBackup') == 'false'
    assert manifest.find('uses-sdk').get(A+'minSdkVersion') == '34'
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
