#!/usr/bin/env python3
"""Import the small native icon subset from Hugeicons' MIT free Stroke Rounded set.
Google Sans is bundled unmodified from Google Fonts under the OFL.
"""
import base64, hashlib, io, json, pathlib, re, tarfile, urllib.request
import xml.etree.ElementTree as ET
ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSION = '4.3.5'
ICONS = {'palette':'PaintBoard', 'source':'HeartPulse', 'speed':'DashboardSpeed01',
         'mode':'PlayCircle', 'colors':'Colors', 'evolution':'Activity01', 'density':'Grid',
         'timer':'Timer01', 'shuffle':'Shuffle', 'brightness':'Sun03', 'frames':'MonitorPlay',
         'scanlines':'ScanLine', 'forever':'Infinity01', 'reset':'RotateCcw', 'settings':'Settings01',
         'preview':'View', 'save':'Save', 'cancel':'Cancel01',
         'play':'Play','pause':'Pause','previous':'Previous','next':'Next','music':'MusicNote03'}
def fetch(url): return urllib.request.urlopen(url, timeout=60).read()
def main():
    licenses = ROOT/'app/src/main/assets/licenses'; licenses.mkdir(parents=True, exist_ok=True)
    font = ROOT/'app/src/main/res/font/google_sans.ttf'; font.parent.mkdir(parents=True, exist_ok=True)
    # Pin the font to the last commit affecting its Google Fonts directory.
    commit = 'b7c8331cf620346575487ef8db0e2c729f014675'
    base = f'https://raw.githubusercontent.com/google/fonts/{commit}/ofl/googlesans/'
    font.write_bytes(fetch(base+'GoogleSans%5BGRAD%2Copsz%2Cwght%5D.ttf'))
    (licenses/'GoogleSans-OFL.txt').write_bytes(fetch(base+'OFL.txt'))
    metadata = json.loads(fetch(f'https://registry.npmjs.org/@hugeicons/core-free-icons/{VERSION}'))
    package = fetch(metadata['dist']['tarball'])
    assert 'sha512-'+base64.b64encode(hashlib.sha512(package).digest()).decode() == metadata['dist']['integrity']
    ns = 'http://schemas.android.com/apk/res/android'; ET.register_namespace('android', ns)
    def attrs(**values): return {f'{{{ns}}}{k}': str(v) for k,v in values.items()}
    with tarfile.open(fileobj=io.BytesIO(package)) as archive:
        (licenses/'Hugeicons-MIT.txt').write_bytes(archive.extractfile('package/LICENSE.md').read())
        for resource, name in ICONS.items():
            raw = archive.extractfile(f'package/dist/esm/{name}Icon.js').read().decode()
            array = raw.split(' = ',1)[1].split(';',1)[0]
            nodes = json.loads(re.sub(r'([{,]\s*)(\w+):', r'\1"\2":', array))
            vector = ET.Element('vector', attrs(width='24dp',height='24dp',viewportWidth=24,viewportHeight=24))
            for tag, a in nodes:
                assert not (set(a) - {'d','cx','cy','r','stroke','strokeWidth','strokeLinecap','strokeLinejoin','key'}), a
                if tag == 'path': path = a['d']
                elif tag == 'circle':
                    x,y,r = [float(a[k]) for k in ('cx','cy','r')]
                    path = f'M{x-r},{y} a{r},{r} 0 1,0 {2*r},0 a{r},{r} 0 1,0 {-2*r},0'
                else: raise ValueError(tag)
                # Subpixel-equivalent at TV icon sizes; keep VectorDrawable paths compact.
                path = re.sub(r'-?\d+\.\d+', lambda m: f'{float(m[0]):.3f}'.rstrip('0').rstrip('.'), path)
                ET.SubElement(vector,'path',attrs(pathData=path,fillColor='@android:color/transparent',
                    strokeColor='#E9EEF2',strokeWidth=a.get('strokeWidth','1.5'),
                    strokeLineCap=a.get('strokeLinecap','round'),strokeLineJoin=a.get('strokeLinejoin','round')))
            ET.indent(vector)
            dest=ROOT/f'app/src/main/res/drawable/huge_{resource}.xml'
            dest.write_text('<!-- Hugeicons free Stroke Rounded: '+name+'Icon; MIT. See assets/licenses. -->\n'+ET.tostring(vector,encoding='unicode')+'\n')
    provenance = {'googleSans':{'commit':commit,'url':base,'sha256':hashlib.sha256(font.read_bytes()).hexdigest()},
                  'hugeicons':{'package':'@hugeicons/core-free-icons','version':VERSION,'integrity':metadata['dist']['integrity'],'icons':ICONS}}
    (ROOT/'docs/ui-assets.json').write_text(json.dumps(provenance,indent=2)+'\n')
    print(f'Imported Google Sans and {len(ICONS)} native vector icons.')
if __name__ == '__main__': main()
