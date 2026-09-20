#!/usr/bin/env python3
"""Device-local A/B test. Frame latency excludes final physical display presentation/scanout."""
import argparse
import os
import json
from pathlib import Path
import re
import statistics
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
ADB = Path(os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Android/Sdk'))) / 'platform-tools/adb'


def summary(values):
    values = sorted(values)
    if not values:
        return {'count': 0}
    return {'count': len(values), 'median': statistics.median(values),
            'p95': values[min(len(values)-1, int(len(values)*.95))],
            'min': values[0], 'max': values[-1]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('device')
    parser.add_argument('variant', choices=['baseline', 'fast'])
    parser.add_argument('--label', default='run')
    args = parser.parse_args()
    base = [str(ADB), '-s', args.device]
    out = ROOT / 'artifacts' / 'latency' / (args.label + '-' + args.variant)
    out.mkdir(parents=True, exist_ok=True)

    def shell(*command):
        return subprocess.run(base+['shell', *map(str, command)], capture_output=True,
                              text=True, check=True, timeout=20).stdout

    shell('am', 'force-stop', 'dev.reachpad')
    shell('am', 'start', '--display', '0', '-n', 'dev.reachpad/.MainActivity',
          '--ez', 'benchmark', 'true', '--ez', 'low_latency', str(args.variant == 'fast').lower())
    with (out/'bridge.log').open('w') as bridge_log, (out/'events.log').open('w') as event_log:
        bridge = subprocess.Popen(base+['shell', 'CLASSPATH=/data/local/tmp/reachpad-probe.apk app_process / dev.reachpad.ShellBridge'],
                                  stdout=bridge_log, stderr=subprocess.STDOUT)
        capture = None
        try:
            deadline = time.monotonic()+12
            while 'REACHPAD_DISPLAY=' not in (out/'bridge.log').read_text():
                if bridge.poll() is not None or time.monotonic()>deadline:
                    raise RuntimeError((out/'bridge.log').read_text())
                time.sleep(.1)
            time.sleep(1.2)
            capture = subprocess.Popen(base+['logcat', '-v', 'brief', '-T', '1', 'ReachPadPerf:I', '*:S'],
                                       stdout=event_log, stderr=subprocess.STDOUT)
            output=shell('CLASSPATH=/data/local/tmp/reachpad-probe.apk app_process / dev.reachpad.BenchmarkDriver 360 8')
            (out/'driver.txt').write_text(output)
            print(output.splitlines()[0], flush=True)
            (out/'display.txt').write_text(shell('dumpsys', 'display'))
            time.sleep(.5)
        finally:
            if capture:
                capture.terminate(); capture.wait(timeout=5)
            shell('am', 'force-stop', 'dev.reachpad')
            try:
                bridge.wait(timeout=5)
            except subprocess.TimeoutExpired:
                bridge.terminate(); bridge.wait(timeout=5)

    events=(out/'events.log').read_text()
    source_times={int(x) for x in re.search(r'BENCHMARK_TIMES=([\d,]+)',(out/'driver.txt').read_text()).group(1).split(',')}
    frame_times=[int(v) for v in re.findall(r'frame marker=(\d+)',events)]
    if not frame_times or any(value not in source_times for value in frame_times):
        raise RuntimeError('Rendered markers must match original generated inputs exactly')
    metrics={}
    for phase, key in [('host','ageMs'),('target','ageMs'),('bridge','callUs'),('frame','ageMs')]:
        values=[int(v) for v in re.findall(rf'{phase} (?:input|marker)=\d+ {key}=(\d+)', events)]
        # Exclude process warm-up from both variants equally.
        metrics[phase+'_'+key]=summary(values[10:])
    display=(out/'display.txt').read_text()
    rates=re.findall(r'mBaseDisplayInfo=DisplayInfo\{"ReachPad".*?renderFrameRate ([\d.]+)',display)
    metrics['virtual_display_hz']=rates
    metrics['validated_frame_markers']=len(frame_times)
    metrics['scope']='Original synthetic input timestamp to host GL readback; physical scanout excluded. Same diagnostic overhead in both variants.'
    (out/'summary.json').write_text(json.dumps(metrics,indent=2))
    print(json.dumps(metrics,indent=2),flush=True)
    if metrics['frame_ageMs']['count'] < 50:
        raise RuntimeError('Insufficient measured frames; do not interpret this run')


if __name__=='__main__':
    main()
