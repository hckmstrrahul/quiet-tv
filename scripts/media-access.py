#!/usr/bin/env python3
"""Enable/revoke Quiet TV's Android media-session access on the verified OnePlus TV."""
import argparse
from tv import adb, verify, log

COMPONENT='dev.rahul.quiettv/dev.rahul.quiettv.MediaAccessService'
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('action',choices=['enable','disable','status'])
a=p.parse_args()
verify()
before=adb('shell','settings','get','secure','enabled_notification_listeners')
if a.action!='status':
    log({'action':'media-access-'+a.action+'-intent','component':COMPONENT,'previous_listeners':before,
         'purpose':'Observe and control active playback for the Home music widget; notification contents are not processed',
         'restore':'python3 scripts/media-access.py '+('enable' if COMPONENT in before else 'disable')})
    adb('shell','cmd','notification','allow_listener' if a.action=='enable' else 'disallow_listener',COMPONENT,'0')
after=adb('shell','settings','get','secure','enabled_notification_listeners')
enabled=COMPONENT in after.split(':')
if a.action!='status':
    assert enabled==(a.action=='enable'), 'Android did not apply notification-listener access'
    log({'action':'media-access-'+a.action+'-result','component':COMPONENT,'verified':True,'enabled':enabled})
print('Quiet TV media access:', 'enabled' if enabled else 'disabled')
