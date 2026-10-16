"""Compile Java and run local probes without Gradle or the native build."""
from pathlib import Path
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
out = root / 'build/control-check'
out.mkdir(parents=True, exist_ok=True)
jdk = Path('C:/Program Files/Eclipse Adoptium/jdk-17.0.20.101-hotspot/bin')
jars = list((Path.home() / '.gradle/caches/modules-2/files-2.1').glob('*/*/*/*/*.jar'))
classpath = ';'.join(str(p) for p in jars)
sources = []
for base in (root / 'src/main/java', root / 'src/test/java/vegas/sony/remote'):
    sources.extend(p for p in base.rglob('*.java') if 'native' not in [part.lower() for part in p.parts])
sources.append(root / 'src/test/java/vegas/sony/module/combat/OpenExpoCombatModulesProbe.java')
sources.append(root / 'src/test/java/vegas/sony/module/ModuleBindProbe.java')
sources.append(root / 'src/test/java/vegas/sony/protocol/ProtocolOutboundSafetyProbe.java')
args = ['--release', '8', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(out)] + [str(p) for p in sources]
argfile = out / 'javac.args'
argfile.write_text('\n'.join('"' + a.replace('\\', '/') + '"' for a in args), encoding='utf-8')
subprocess.run([str(jdk / 'javac.exe'), '@' + str(argfile)], cwd=root, check=True)
for probe in ('ControlPanelProbe', 'PartySyncProbe', 'LocalControlSessionTokenProbe'):
    subprocess.run([str(jdk / 'java.exe'), '-cp', str(out) + ';' + str(root / 'src/main/resources') + ';' + classpath,
                'vegas.sony.remote.' + probe], cwd=root, check=True)
subprocess.run([str(jdk / 'java.exe'), '-cp', str(out) + ';' + str(root / 'src/main/resources') + ';' + classpath,
                'vegas.sony.module.ModuleBindProbe'], cwd=root, check=True)
subprocess.run([str(jdk / 'java.exe'), '-cp', str(out) + ';' + str(root / 'src/main/resources') + ';' + classpath,
                'vegas.sony.module.combat.OpenExpoCombatModulesProbe'], cwd=root, check=True)
subprocess.run([str(jdk / 'java.exe'), '-cp', str(out) + ';' + str(root / 'src/main/resources') + ';' + classpath,
                'vegas.sony.protocol.ProtocolOutboundSafetyProbe'], cwd=root, check=True)
node = shutil.which('node')
if not node:
    raise RuntimeError('Node.js is required to validate the control-panel JavaScript')
subprocess.run([node, '--check', str(root / 'build/control-probe/app.js')], cwd=root, check=True)
print('Control panel JavaScript syntax check passed.')
