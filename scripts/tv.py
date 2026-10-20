#!/usr/bin/env python3
"""Explicit-target TV operations with identity checks and an append-only audit log."""
import argparse, datetime, json, os, pathlib, re, subprocess, sys, time
ROOT = pathlib.Path(__file__).resolve().parents[1]
# Private connection details never belong in source control. The firmware guard
# deliberately remains narrow: these operations were tested only on this model.
_CONFIG_PATH = ROOT / '.quiet-tv-device.json'
_CONFIG = json.loads(_CONFIG_PATH.read_text()) if _CONFIG_PATH.exists() else {}
TARGET = os.environ.get('QUIET_TV_ADB_TARGET', _CONFIG.get('target', ''))
SERIAL = os.environ.get('QUIET_TV_SERIAL', _CONFIG.get('serial', ''))
def require_target():
 if not TARGET or not SERIAL or '<' in TARGET or '<' in SERIAL:
  raise RuntimeError('Set QUIET_TV_ADB_TARGET and QUIET_TV_SERIAL, or copy .quiet-tv-device.example.json to .quiet-tv-device.json and fill in your TV details.')
APP = 'dev.rahul.quiettv'
OPTIONAL = {
 'com.google.android.videos': 'Google Play Movies / Google TV: purchased and rented movie library',
 'com.google.android.play.games': 'Google Play Games: game profile, achievements and game discovery',
 'com.amazon.amazonvideo.livingroom': 'Prime Video: Amazon streaming',
 'in.startv.hotstar': 'Hotstar: subscription video and sports',
 'com.sonyliv': 'SonyLIV: Sony video and sports',
 'com.graymatrix.did': 'ZEE5: video streaming',
 'com.jio.media.stb.ondemand': 'JioCinema: video streaming',
 'com.lionsgateplay.videoapp': 'Lionsgate Play: video streaming',
 'com.hungama.movies.tv': 'Hungama: video entertainment',
 'com.viewlift.hoichoi': 'Hoichoi: Bengali video streaming',
 'com.spotify.tv.android': 'Spotify: music streaming',
 'com.jiostb.jiogames': 'JioGames: games',
 'com.jio.web.androidtv': 'Jio browser: web browsing',
 'com.coloros.relax.tv': 'O Relax: ambient relaxation content',
}
def adb(*args):
 require_target()
 r = subprocess.run(['adb','-s',TARGET,*args], capture_output=True, text=True, timeout=90)
 if r.returncode: raise RuntimeError(r.stderr or r.stdout)
 return r.stdout.strip()
def verify():
 expected={'ro.boot.op.model':'55U1S','ro.product.manufacturer':'OnePlus','ro.serialno':SERIAL,'ro.build.display.id':'RTM8.240401.135'}
 for key,value in expected.items():
  got=adb('shell','getprop',key)
  if got!=value: raise RuntimeError(f'Identity mismatch {key}: {got!r}; expected {value!r}. Stopping.')
 return expected
def log(record):
 (ROOT/'logs').mkdir(exist_ok=True)
 with (ROOT/'logs/changes.jsonl').open('a') as f:
  f.write(json.dumps({'time':datetime.datetime.now().astimezone().isoformat(),'target':TARGET,**record})+'\n')
def disable(package):
 if package not in OPTIONAL: raise RuntimeError('Not an approved optional package')
 info=adb('shell','dumpsys','package',package)
 user=re.search(r'User 0:.*',info)
 if not user: raise RuntimeError('No user 0 package state')
 state=int(re.search(r'enabled=(\d+)',user.group()).group(1))
 if state not in (0,1): raise RuntimeError(f'{package} already disabled or unusual state: {state}')
 restore='default-state' if state==0 else 'enable'
 rollback=f'adb -s {TARGET} shell pm {restore} --user 0 {package}'
 log({'action':'disable-intent','package':package,'purpose':OPTIONAL[package],'previous_enabled':state,'restore':rollback})
 result=adb('shell','pm','disable-user','--user','0',package)
 disabled=adb('shell','pm','list','packages','-d','--user','0').splitlines()
 ok='package:'+package in disabled
 log({'action':'disable-result','package':package,'result':result,'verified':ok,'restore':rollback})
 if not ok: raise RuntimeError(result)
 with (ROOT/'logs/restore-debloat.sh').open('a') as f: f.write(rollback+'\n')
 print(package, '→ disabled; rollback:',rollback)
def restore_stock_state():
 path=ROOT/'logs/home-stock-state.json'
 record=json.loads(path.read_text()) if path.exists() else {'restore_operation':'default-state'}
 operation=record['restore_operation']
 if operation not in ('default-state','enable'): raise RuntimeError('Invalid saved restoration operation')
 result=adb('shell','pm',operation,'--user','0','com.google.android.tvlauncher')
 log({'action':'restore-stock-state','operation':operation,'result':result})
 related=ROOT/'logs/home-recommendations-state.json'
 if related.exists():
  recommendation_operation=json.loads(related.read_text())['restore_operation']
  if recommendation_operation not in ('default-state','enable'): raise RuntimeError('Invalid recommendations restoration operation')
  result=adb('shell','pm',recommendation_operation,'--user','0','com.google.android.tvrecommendations')
  log({'action':'restore-stock-recommendations','operation':recommendation_operation,'result':result})
def quiet_home_integrations():
 # Verified on this firmware: Recommendations launches a private Google Home
 # helper on DREAMING_STARTED and crashes if Google Home is disabled.
 disabled=adb('shell','pm','list','packages','-d','--user','0').splitlines()
 package='com.google.android.tvrecommendations'
 if 'package:com.google.android.tvlauncher' not in disabled or 'package:'+package in disabled: return
 info=adb('shell','dumpsys','package',package)
 state=int(re.search(r'User 0:.*enabled=(\d+)',info).group(1))
 if state not in (0,1): raise RuntimeError('Unexpected recommendations state')
 path=ROOT/'logs/home-recommendations-state.json'
 if not path.exists(): path.write_text(json.dumps({'package':package,'original_enabled_state':state,'restore_operation':'default-state' if state==0 else 'enable','reason':'DREAMING_STARTED crashes on missing disabled Google Home helper'},indent=2)+'\n')
 log({'action':'disable-home-recommendations-intent','package':package,'previous_enabled':state,'restore':'python3 scripts/tv.py restore-home','reason':'Observed DREAMING_STARTED crash caused by disabled stock Home helper'})
 result=adb('shell','pm','disable-user','--user','0',package)
 ok='package:'+package in adb('shell','pm','list','packages','-d','--user','0').splitlines()
 log({'action':'disable-home-recommendations-result','package':package,'result':result,'verified':ok})
 if not ok: raise RuntimeError('Recommendations package state did not apply')
def select_home(quiet):
 target=APP+'/.Home' if quiet else 'com.google.android.tvlauncher/.MainActivity'
 resolver=('cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
 previous=adb('shell',*resolver)
 log({'action':'select-home-intent','previous':previous,'new':target,'restore':f'python3 scripts/tv.py {"restore-home" if quiet else "home"}'})
 try:
  if quiet:
   result=adb('shell','am','start','-W','-n',APP+'/.MainActivity')
   log({'action':'pre-home-launch-check','result':result})
   pre_resumed=''
   for _ in range(12):
    pre_resumed='\n'.join(x for x in adb('shell','dumpsys','activity','activities').splitlines() if 'mResumedActivity' in x)
    if 'dev.rahul.quiettv/' in pre_resumed: break
    time.sleep(.5)
   if 'dev.rahul.quiettv/' not in pre_resumed:
    raise RuntimeError('Replacement launcher failed to resume; stock state unchanged')
  else: restore_stock_state()
  result=adb('shell','cmd','package','set-home-activity','--user','0',target)
  log({'action':'select-home-result','target_component':target,'result':result})
  resolved=adb('shell',*resolver)
  expected=target.split('/')[0]+'/'
  if quiet and expected not in resolved:
   # This firmware's privileged Google HOME filter has priority 2. Quiet TV is
   # capped to priority 0, so the preferred Home setting alone cannot win.
   info=adb('shell','dumpsys','package','com.google.android.tvlauncher')
   state=int(re.search(r'User 0:.*enabled=(\d+)',info).group(1))
   if state not in (0,1): raise RuntimeError('Unexpected stock state; refusing to overwrite recovery baseline')
   path=ROOT/'logs/home-stock-state.json'
   if not path.exists():
    path.write_text(json.dumps({'package':'com.google.android.tvlauncher','original_enabled_state':state,'restore_operation':'default-state' if state==0 else 'enable'},indent=2)+'\n')
   log({'action':'disable-stock-home-intent','previous_enabled':state,'reason':'Higher-priority stock HOME overrides preferred launcher','restore':'python3 scripts/tv.py restore-home'})
   result=adb('shell','pm','disable-user','--user','0','com.google.android.tvlauncher')
   log({'action':'disable-stock-home-result','result':result})
   result=adb('shell','cmd','package','set-home-activity','--user','0',target)
   log({'action':'select-home-after-stock-disable','result':result})
   resolved=adb('shell',*resolver)
  if expected not in resolved: raise RuntimeError('Default Home did not resolve to '+target)
  if quiet and (ROOT/'logs/home-recommendations-state.json').exists(): quiet_home_integrations()
  adb('shell','input','keyevent','KEYCODE_HOME')
  resumed=''
  for _ in range(12):
   time.sleep(.5)
   resumed='\n'.join(line for line in adb('shell','dumpsys','activity','activities').splitlines() if 'mResumedActivity' in line)
   if expected in resumed: break
  if expected not in resumed: raise RuntimeError('Home key did not resume '+target)
  enabled=adb('shell','pm','list','packages','-e','--user','0').splitlines()
  if 'package:com.oneplus.tv.launcher' not in enabled: raise RuntimeError('OnePlus launcher must stay enabled')
  log({'action':'select-home-verified','resolved':resolved,'resumed':resumed,'google_launcher_enabled':'package:com.google.android.tvlauncher' in enabled,'oneplus_launcher_enabled':True})
  print(resolved); print(resumed)
 except Exception as error:
  log({'action':'select-home-failed','error':str(error)})
  try: restore_stock_state()
  except Exception as recovery_error: log({'action':'stock-state-recovery-failed','error':str(recovery_error)})
  # Keep recovery independent of component-state changes, which this firmware blocks.
  for args in [('cmd','package','set-home-activity','--user','0','com.google.android.tvlauncher/.MainActivity'),('am','start','-W','-n','com.google.android.tvlauncher/.MainActivity')]:
   try: log({'action':'home-recovery','command':list(args),'result':adb('shell',*args)})
   except Exception as recovery_error: log({'action':'home-recovery-failed','error':str(recovery_error)})
  raise
def main():
 p=argparse.ArgumentParser(); p.add_argument('action',choices=['verify','disable','install','launch','stock','home','restore-home']); p.add_argument('value',nargs='?'); a=p.parse_args()
 identity=verify()
 if a.action=='verify': print(json.dumps(identity,indent=2))
 elif a.action=='disable': disable(a.value)
 elif a.action=='install':
  path=pathlib.Path(a.value).resolve()
  log({'action':'install-intent','apk':str(path)})
  result=adb('install','-r',str(path)); log({'action':'install-result','result':result}); print(result)
 elif a.action=='launch':
  result=adb('shell','am','start','-W','-n',APP+'/.MainActivity'); log({'action':'launch','result':result}); print(result)
 elif a.action in ('home','restore-home','stock'): select_home(a.action=='home')
if __name__=='__main__':
 try: main()
 except Exception as e: sys.exit(str(e))
