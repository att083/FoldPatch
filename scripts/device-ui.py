#!/usr/bin/env python3
"""Local test helper; operates on the default display using visible labels."""
import os,shutil,argparse,subprocess,xml.etree.ElementTree as E,re
p=argparse.ArgumentParser();p.add_argument('device');p.add_argument('action',choices=['dump','tap']);p.add_argument('text',nargs='?');p.add_argument('--native',action='store_true');p.add_argument('--left',type=float,default=.45);p.add_argument('--right',type=float,default=.55);p.add_argument('--width',type=int,default=1768);p.add_argument('--scale',type=float,default=1.0,help='Uniform SystemUI fit scale; normal reflow is 1');a=p.parse_args()
adb=[os.environ.get('REACHPAD_ADB') or shutil.which('adb') or os.path.expanduser('~/Android/Sdk/platform-tools/adb'),'-s',a.device]
subprocess.run(adb+['shell','uiautomator','dump','/data/local/tmp/reachpad-ui.xml'],check=True,stdout=subprocess.DEVNULL)
x=subprocess.check_output(adb+['shell','cat','/data/local/tmp/reachpad-ui.xml']);nodes=list(E.fromstring(x).iter('node'))
if a.action=='dump':
 for n in nodes:
  if n.get('text'):print(n.get('text'),n.get('bounds'))
else:
 for n in sorted(nodes,key=lambda n:(n.get("clickable")!="true",n.get("text")!=a.text)):
  if n.get('text')==a.text or n.get('content-desc')==a.text:
   l,t,r,b=map(int,re.findall(r'\d+',n.get('bounds')))
   x=(l+r)/2
   if a.native:
    edge=round(a.width*a.left);start=round(a.width*a.right);cut=edge/a.scale
    x=x*a.scale if x<cut else start+(x-cut)*a.scale
   subprocess.run(adb+['shell','input','-d','0','tap',str(round(x)),str(round((t+b)/2*(a.scale if a.native else 1)))],check=True);break
 else:raise SystemExit('Label not visible: '+str(a.text))
