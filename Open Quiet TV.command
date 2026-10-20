#!/bin/zsh
# Double-click in Finder to reopen the verified OnePlus TV live mirror.
set -eu
export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
cd -- "${0:A:h}"
exec python3 scripts/mirror-tv.py "$@"
