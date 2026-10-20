#!/usr/bin/env python3
"""Download the pinned Gradle distribution with macOS curl's TLS trust store.
Use when Java cannot validate the network's TLS chain. No TLS checks are disabled.
"""
import hashlib, pathlib, subprocess
URL='https://downloads.gradle.org/distributions/gradle-8.11.1-bin.zip'
SHA='f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6'
number=int.from_bytes(hashlib.md5(URL.encode()).digest(),'big'); key=''
while number:
 number, digit=divmod(number,36); key='0123456789abcdefghijklmnopqrstuvwxyz'[digit]+key
folder=pathlib.Path.home()/'.gradle/wrapper/dists/gradle-8.11.1-bin'/key
folder.mkdir(parents=True,exist_ok=True)
archive=folder/'gradle-8.11.1-bin.zip'
if (folder/'gradle-8.11.1-bin.zip.ok').exists():
 print('Gradle wrapper distribution already installed'); raise SystemExit
cached=pathlib.Path('/tmp/quiettv-gradle.zip')
if cached.exists() and hashlib.sha256(cached.read_bytes()).hexdigest()==SHA:
 archive.write_bytes(cached.read_bytes())
else:
 subprocess.run(['curl','--fail','--location','--retry','2',URL,'--output',str(archive)],check=True)
if hashlib.sha256(archive.read_bytes()).hexdigest()!=SHA:
 archive.unlink(); raise SystemExit('SHA-256 mismatch; archive removed')
print('Verified Gradle archive placed in wrapper cache:',folder)
