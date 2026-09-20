#!/usr/bin/env python3
"""Generate the variant manifest; production has no test or ADB bootstrap entry points."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
A = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', A[1:-1])
variant, output = sys.argv[1:]
if variant not in ('debug', 'release'):
    raise SystemExit('Unknown variant')
tree = ET.parse(ROOT / 'AndroidManifest.xml')
app = tree.getroot().find('application')
app.set(A + 'debuggable', str(variant == 'debug').lower())
if variant == 'debug':
    ET.SubElement(app, 'activity', {A+'name': '.KeyboardEditProbeActivity', A+'exported': 'true'})
    ET.SubElement(app, 'activity', {A+'name': '.LocalizationProbeActivity', A+'exported': 'true'})
    ET.SubElement(app, 'activity', {A+'name': '.TouchSideProbeActivity', A+'exported': 'true', A+'configChanges': 'orientation|screenSize|smallestScreenSize|screenLayout|density'})
    ET.SubElement(app, 'activity', {A+'name': '.InputIntegrityProbeActivity', A+'exported': 'true', A+'configChanges': 'orientation|screenSize|smallestScreenSize|screenLayout|density'})
    ET.SubElement(app, 'receiver', {A+'name': '.PointerDriverReceiver', A+'exported': 'true', A+'permission': 'android.permission.DUMP'})
if variant == 'release':
    for element in list(app):
        name = element.get(A + 'name', '')
        if name in ('.ProbeActivity', '.InputProbeActivity', '.LatencyProbeActivity', '.BridgeProvider'):
            app.remove(element)
        elif name == '.MainActivity':
            element.set(A + 'exported', 'false')
tree.write(output, encoding='utf-8', xml_declaration=True)
