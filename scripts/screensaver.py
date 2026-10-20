#!/usr/bin/env python3
"""Verified-device screensaver selection, firmware integration and exact settings restoration."""
import argparse, datetime, json, pathlib, sys
from tv import ROOT, TARGET, APP, adb, verify, log, quiet_home_integrations
COMPONENT = APP + '/dev.rahul.quiettv.screensaver.ConsoleDreamService'
BASELINE = ROOT / 'logs/screensaver/baseline.json'
KEYS = [('secure', 'screensaver_components'), ('secure', 'screensaver_enabled'), ('system', 'screen_off_timeout')]

def read(namespace, key):
    return adb('shell', 'settings', 'get', namespace, key)

def capture():
    identity = verify()
    if not BASELINE.exists():
        BASELINE.parent.mkdir(parents=True, exist_ok=True)
        record = {'captured':datetime.datetime.now().astimezone().isoformat(), 'identity':identity, 'target':TARGET, 'secure':{}, 'system':{}}
        for ns,key in KEYS: record[ns][key]=read(ns,key)
        BASELINE.write_text(json.dumps(record,indent=2)+'\n')
    record=json.loads(BASELINE.read_text())
    if record['identity'] != identity or record['target'] != TARGET: raise RuntimeError('Screensaver baseline belongs to another TV')
    return record

def write(ns, key, value):
    old=read(ns,key)
    log({'action':'screensaver-setting-intent','namespace':ns,'key':key,'previous':old,'new':value,'restore':'python3 scripts/screensaver.py restore'})
    operation=['delete',ns,key] if value=='null' else ['put',ns,key,str(value)]
    result=adb('shell','settings',*operation)
    actual=read(ns,key)
    log({'action':'screensaver-setting-result','namespace':ns,'key':key,'new':actual,'verified':actual==str(value),'result':result})
    if actual!=str(value):raise RuntimeError('Setting did not apply: '+key)

def restore(record):
    for ns,key in KEYS: write(ns,key,record[ns][key])
    print('Original screensaver and idle timeout restored. No package state changed.')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('action',choices=['status','enable','preview','restore']);parser.add_argument('--minutes',type=int,choices=[5,10,15,30,60],default=5)
    args=parser.parse_args();baseline=capture()
    if args.action=='status':
        print(json.dumps({ns+'.'+key:read(ns,key) for ns,key in KEYS},indent=2));print(adb('shell','dumpsys','dreams'))
    elif args.action=='restore':restore(baseline)
    elif args.action=='enable':
        services=adb('shell','cmd','package','query-services','--brief','-a','android.service.dreams.DreamService','-p',APP)
        if 'ConsoleDreamService' not in services:raise RuntimeError('Install and test the MK-IV APK before selecting it')
        quiet_home_integrations()
        try:
            write('secure','screensaver_components',COMPONENT)
            write('secure','screensaver_enabled','1')
            write('system','screen_off_timeout',str(args.minutes*60000))
            print('MK-IV selected, idle timeout '+str(args.minutes)+' minutes. Original screensaver remains enabled.')
        except Exception:
            restore(baseline);raise
    elif args.action=='preview':
        if read('secure','screensaver_components')!=COMPONENT:raise RuntimeError('Select MK-IV first, or use its normal-app Preview')
        log({'action':'screensaver-system-preview','component':COMPONENT})
        print(adb('shell','am','start','-W','-n','com.android.systemui/.Somnambulator'))
if __name__=='__main__':
    try:main()
    except Exception as e:sys.exit(str(e))
