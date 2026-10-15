#!/usr/bin/env python3
"""Create a private, persistent APK signing key outside the source tree."""
import os
from pathlib import Path
import secrets
import subprocess

folder = Path.home() / '.local/share/tsc-android-print/signing'
folder.mkdir(parents=True, exist_ok=True, mode=0o700)
folder.chmod(0o700)
store, props = folder / 'release.p12', folder / 'release.properties'
if store.exists() or props.exists():
    if not (store.exists() and props.exists()):
        raise SystemExit('Incomplete signing files; recover them instead of replacing the existing key.')
    print('Existing signing configuration: ' + str(props))
else:
    password = secrets.token_urlsafe(36)
    env = dict(os.environ, TSC_STORE_PASS=password)
    java = Path(os.environ['JAVA_HOME']) / 'bin/keytool'
    subprocess.run([str(java), '-genkeypair', '-keystore', str(store), '-storetype', 'PKCS12',
        '-storepass:env', 'TSC_STORE_PASS', '-keypass:env', 'TSC_STORE_PASS',
        '-alias', 'tsc-direct-print', '-keyalg', 'RSA', '-keysize', '3072', '-validity', '10000',
        '-dname', 'CN=TSC Direct Print Local, OU=Private Application'], env=env, check=True)
    store.chmod(0o600)
    fd = os.open(props, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w') as out:
        out.write(f'storeFile={store}\nstorePassword={password}\nkeyAlias=tsc-direct-print\nkeyPassword={password}\n')
    print('Created private signing configuration: ' + str(props))
