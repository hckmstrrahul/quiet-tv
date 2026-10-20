#!/usr/bin/env python3
"""Live view/control of the identity-verified OnePlus TV on this Mac."""
import argparse
import os
import shutil
import subprocess
import sys

from tv import TARGET, log, verify, require_target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--view-only', action='store_true', help='Disable keyboard and mouse control')
    args = parser.parse_args()
    binary = shutil.which('scrcpy')
    if not binary or not shutil.which('adb'):
        parser.exit(1, 'Install scrcpy and adb first: brew install scrcpy android-platform-tools\n')
    require_target()
    subprocess.run(['adb', 'connect', TARGET], check=True, timeout=20)
    identity = verify()
    command = [binary, '--serial=' + TARGET, '--no-audio', '--no-power-on',
               '--no-clipboard-autosync', '--max-size=1280', '--max-fps=30',
               '--video-bit-rate=4M', '--window-title=Quiet TV — Live',
               '--shortcut-mod=lctrl,rctrl,lalt,ralt']
    if args.view_only:
        command.append('--no-control')
    log({'action': 'mirror-start', 'identity': identity, 'view_only': args.view_only,
         'audio_forwarding': False, 'command': command,
         'stop': 'Close the Quiet TV — Live window or press Control+Q in it.'})
    print('Opening live TV. Arrows: navigate; Return: select; Control+B: Back; '
          'Control+H: Home; Control+Q: close mirror.', flush=True)
    os.execv(binary, command)


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, subprocess.SubprocessError, OSError) as error:
        sys.exit(str(error))
