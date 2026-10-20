#!/usr/bin/env python3
"""Check Quiet TV navigation and dismissible menus without changing preferences.
Run with the TV/mirror controls idle. Never sends keys to HDMI or another app.
"""
import re
import xml.etree.ElementTree as ET
from tv import adb, verify, APP, log


def guard():
    lines = [line for line in adb('shell', 'dumpsys', 'activity', 'activities').splitlines() if 'mResumedActivity' in line]
    if not lines or not all(APP+'/.MainActivity' in line or APP+'/.Home' in line for line in lines):
        raise RuntimeError('Quiet TV Home is not foreground; stopping input: '+str(lines))


def tree():
    guard()
    adb('shell', 'uiautomator', 'dump', '/sdcard/quiettv-ui.xml')
    raw = adb('shell', 'cat', '/sdcard/quiettv-ui.xml')
    return ET.fromstring(raw[raw.index('<?xml'):])


def name(node):
    return node.get('content-desc') or node.get('text')


def key(code):
    guard()
    adb('shell', 'input', 'keyevent', 'KEYCODE_'+code)


def tap(title):
    node = next(n for n in tree().iter('node') if name(n) == title)
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    guard()
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))


def labels():
    return [name(n) for n in tree().iter('node') if name(n)]


def focus():
    return next((name(n) for n in tree().iter('node') if n.get('focused') == 'true'), None)


def main():
    verify()
    guard()
    before = adb('shell', 'run-as', APP, 'cat', 'shared_prefs/home.xml')
    titles = labels()
    assert all(title in titles for title in ('Appearance', 'TV settings', 'Connect device'))
    assert not any(title in titles for title in ('All apps', 'Favorites', 'Inputs', 'Hold OK to organize'))
    for button, title in [('Appearance', 'Home settings')]:
        tap(button)
        assert title in labels(), title
        guard()
        adb('shell', 'input', 'tap', '30', '500')
        assert title not in labels(), 'Outside click failed: '+title
    # Leave touch mode, then walk left across the header without activating anything.
    key('DPAD_UP')
    for _ in range(2): key('DPAD_LEFT')
    assert focus() == 'Appearance', focus()
    key('DPAD_RIGHT'); assert focus() == 'TV settings', focus()
    inputs=[t for t in titles if t.startswith('HDMI') and t.endswith(('connected','disconnected'))]
    assert inputs, 'No HDMI input pills'
    connected=[t for t in inputs if t.endswith(', connected')]
    assert inputs[:len(connected)] == connected, 'Connected inputs must come first'
    for title in inputs:
        key('DPAD_RIGHT'); assert focus() == title, focus()
    key('DPAD_RIGHT'); assert focus() and ('playing' in focus().lower() or 'playback' in focus().lower()), focus()
    key('DPAD_RIGHT'); assert focus().startswith('Bengaluru weather'), focus()
    key('DPAD_LEFT')
    for _ in range(len(inputs)+2): key('DPAD_LEFT')
    key('DPAD_DOWN'); assert focus() == 'Connect device', focus()
    key('DPAD_UP'); assert focus() == 'Appearance', focus()
    after = adb('shell', 'run-as', APP, 'cat', 'shared_prefs/home.xml')
    def preferences(raw):
        return {n.get('name'): n.text if n.tag == 'string' else n.get('value') for n in ET.fromstring(raw)}
    initial, final = preferences(before), preferences(after)
    # Current wallpaper advances during shuffle; other preferences must not change.
    if initial.get('wallpaperShuffle') == 'true':
        initial.pop('wallpaperCurrent', None); final.pop('wallpaperCurrent', None)
    assert initial == final, 'Preferences changed unexpectedly'
    assert 'favorites' not in final
    log({'action': 'smoke-test', 'version': '0.10.1', 'result': 'passed',
         'tests': ['header pills', 'no favorite tabs', 'outside dismiss Appearance', 'connected HDMI first', 'weather navigation',
                   'D-pad header/grid routes', 'preferences preserved']})
    print('PASS: header, grid routes, outside dismissal and unchanged preferences.')


if __name__ == '__main__':
    main()
