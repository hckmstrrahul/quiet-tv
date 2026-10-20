#!/usr/bin/env python3
import csv, datetime, pathlib, re
from tv import ROOT, adb, verify
verify()
folder=ROOT/'inventory'/datetime.datetime.now().strftime('%Y%m%d-%H%M%S'); folder.mkdir(parents=True)
commands={'properties':['getprop'],'packages':['pm','list','packages','-f','-U','--user','0'],'enabled':['pm','list','packages','-e','--user','0'],'disabled':['pm','list','packages','-d','--user','0'],'system':['pm','list','packages','-s','--user','0'],'third-party':['pm','list','packages','-3','--user','0'],'home':['cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME'],'tv-apps':['cmd','package','query-activities','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.LEANBACK_LAUNCHER'],'inputs':['dumpsys','tv_input'],'hdmi':['dumpsys','hdmi_control'],'package-details':['dumpsys','package']}
results={}
for key,args in commands.items():
 results[key]=adb('shell',*args); (folder/(key+'.txt')).write_text(results[key]+'\n')
disabled=set(results['disabled'].splitlines()); system=set(results['system'].splitlines())
with (folder/'packages.csv').open('w') as f:
 writer=csv.writer(f); writer.writerow(['package','state','type','uid','apk'])
 for line in results['packages'].splitlines():
  match=re.match(r'package:(.*)=([^ ]+) uid:(\d+)',line)
  if match:
   path,pkg,uid=match.groups(); writer.writerow([pkg,'disabled' if 'package:'+pkg in disabled else 'enabled','system' if 'package:'+pkg in system else 'user',uid,path])
(ROOT/'inventory/LATEST').write_text(str(folder.relative_to(ROOT))+'\n')
print(folder)
