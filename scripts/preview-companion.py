#!/usr/bin/env python3
"""Local-only companion UI sandbox; never connects to a TV.
Run: python3 scripts/preview-companion.py [--port 8766]
Open / for the sample dashboard, or /?pair=1 for pairing (code 123456).
Changes stay in process memory. Restart to reset the sample layout.
"""
import argparse
import copy
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from threading import Lock
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
CATALOG = [{'id': p['id'], 'title': p['title']} for p in json.loads((ROOT / 'docs/wallpapers.json').read_text())]
APPS = [
    ('com.heytap.tv.btspeaker', 'Bluetooth Stereo'), ('com.ionitech.airscreen', 'AirScreen'),
    ('com.heytap.tv.camera', 'Camera'), ('com.oneplus.tv.userguide', 'E-manual'),
    ('com.heytap.tv.filebrowser', 'File Manager'), ('com.heytap.tv.livetv', 'Live TV'),
    ('com.netflix.ninja', 'Netflix'), ('com.oneplus.tv.screensaver', 'OnePlus Pictorial'),
    ('com.oneplus.tv.launcher', 'OxygenPlay'), ('com.android.vending', 'Play Store'),
    ('com.heytap.tv.assistant.oneplus', 'Smart Manager'), ('net.oneplus.weather', 'Weather'),
    ('com.google.android.youtube.tv', 'YouTube'), ('com.google.android.youtube.tvmusic', 'YouTube Music'),
]
STATE = dict(format='quiet-tv-layout', version=4, accent='#AADBCB', background='#0C1018', columns=6,
             wallpaperId='wallpaper_03', wallpaperShuffle=False, photo=False, connectedDevices=1,
             appVersion='0.11.0', wallpapers=CATALOG,
             apps=[dict(package=p, label=n) for p, n in APPS], order=[p for p, _ in APPS])
LOCK = Lock()

def backup():
    return {k: copy.deepcopy(STATE[k]) for k in ('format', 'version', 'order', 'accent', 'background', 'columns', 'wallpaperId', 'wallpaperShuffle')}

def apply(action):
    if action['action'] == 'move':
        index = STATE['order'].index(action['package']); direction = action['direction']
        assert direction in (-1, 1)
        target = index + direction
        if 0 <= target < len(STATE['order']):
            STATE['order'][index], STATE['order'][target] = STATE['order'][target], STATE['order'][index]
            STATE['apps'].sort(key=lambda a: STATE['order'].index(a['package']))
    elif action['action'] == 'appearance':
        if 'accent' in action:
            value = action['accent']; assert len(value) == 7 and value.startswith('#'); int(value[1:], 16)
            STATE['accent'] = value.upper()
        if 'columns' in action:
            assert action['columns'] in (4, 5, 6); STATE['columns'] = action['columns']
    elif action['action'] == 'backdrop':
        value = action['id']; assert value == 'shuffle' or value in {p['id'] for p in CATALOG}
        STATE['wallpaperShuffle'] = value == 'shuffle'; STATE['photo'] = False
        if value != 'shuffle': STATE['wallpaperId'] = value
    elif action['action'] == 'clear-wallpaper': STATE['photo'] = False
    else: raise ValueError('Unknown action')

class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_): pass
    def reply(self, data, mime='application/json', status=200):
        if mime == 'application/json': data = json.dumps(data).encode()
        self.send_response(status)
        self.send_header('Content-Type', mime)
        self.send_header('Content-Length', str(len(data)))
        self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Content-Security-Policy', "default-src 'none'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data: blob:; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'")
        self.end_headers(); self.wfile.write(data)
    def allowed(self):
        hosts = {f'localhost:{self.server.server_port}', f'127.0.0.1:{self.server.server_port}'}
        if self.headers.get('Host') not in hosts: return False
        return not self.headers.get('Origin') or self.headers['Origin'] in {'http://' + h for h in hosts}
    def handle_request(self):
        if not self.allowed(): return self.reply({'error': 'Local preview only'}, status=403)
        path = urlsplit(self.path).path
        if self.command == 'GET':
            static = {'/': ('companion/index.html', 'text/html; charset=utf-8'),
                      '/app.css': ('companion/app.css', 'text/css'), '/app.js': ('companion/app.js', 'text/javascript')}
            if path in static:
                file, mime = static[path]; data = (ASSETS / file).read_bytes()
                if path == '/': data = data.replace(b'<head>', b'<head><meta name="quiet-tv-preview" content="true">')
                return self.reply(data, mime)
            if path == '/google-sans.ttf': return self.reply((ROOT/'app/src/main/res/font/google_sans.ttf').read_bytes(), 'font/ttf')
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if not 0 <= length <= (4*1024*1024 if path == '/api/wallpaper' else 65536):
                return self.reply({'error': 'File too large'}, status=413)
            body = self.rfile.read(length) if self.command == 'POST' else b''
            if path == '/pair' and self.command == 'POST':
                return self.reply({'token': 'local-preview'} if body == b'123456' else {'error': 'For this local preview, use 123456.'}, status=200 if body == b'123456' else 401)
            if self.headers.get('Authorization') != 'Bearer local-preview': return self.reply({'error': 'Enter the preview code 123456.'}, status=401)
            with LOCK:
                if self.command == 'GET':
                    if path == '/api/state': return self.reply(STATE)
                    if path == '/api/backup': return self.reply(backup())
                    if path == '/api/preview': return self.reply(json.loads((ROOT/'scripts/fixtures/companion-home.json').read_text()))
                    if path.startswith('/api/backgrounds/'):
                        ident = path.rsplit('/', 1)[-1]
                        if ident in {p['id'] for p in CATALOG}: return self.reply((ASSETS/'wallpapers/thumbs'/f'{ident}.jpg').read_bytes(), 'image/jpeg')
                if self.command == 'POST':
                    if path == '/api/action': apply(json.loads(body)); return self.reply(STATE)
                    if path == '/api/wallpaper':
                        assert self.headers.get('Content-Type') in ('image/jpeg','image/png','image/webp') and body
                        STATE['photo'] = True; return self.reply(STATE)
                    if path == '/api/restore':
                        data = json.loads(body); assert data['format'] == 'quiet-tv-layout' and data['version'] in (1,2,3,4)
                        assert data['columns'] in (4,5,6) and data['wallpaperId'] in {p['id'] for p in CATALOG}
                        for key in ('accent','background'):
                            assert len(data[key]) == 7 and data[key].startswith('#'); int(data[key][1:],16)
                        assert isinstance(data['order'],list) and len(data['order']) <= 200 and len(set(data['order'])) == len(data['order'])
                        known = {p for p,_ in APPS}; order = [p for p in data['order'] if p in known]
                        order += [p for p in STATE['order'] if p not in order]
                        STATE.update({k:data[k] for k in ('columns','accent','background','wallpaperId','wallpaperShuffle')})
                        STATE['order']=order;STATE['apps'].sort(key=lambda a:order.index(a['package']));STATE['photo']=False
                        return self.reply(STATE)
            return self.reply({'error': 'Not found'}, status=404)
        except (ValueError, KeyError, TypeError, AssertionError): return self.reply({'error': 'Invalid preview request or layout file.'}, status=400)
    do_GET = handle_request
    do_POST = handle_request

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--port', type=int, default=8766)
    args = parser.parse_args(); server = ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    print(f'Quiet TV preview: http://localhost:{args.port} (no TV connection)', flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: server.server_close()
