"""Fetch pinned dependency source materials for the offline distribution; executes no downloaded code."""
from pathlib import Path
import hashlib, json, sys, urllib.request

DEST=Path(sys.argv[1] if len(sys.argv)>1 else 'build/dependency-sources')
DEST.mkdir(parents=True,exist_ok=True)
ARTIFACTS=[
 ('OpenJDK21U-jdk-sources_21.0.12.1_1.tar.gz','https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.12.1%2B1/OpenJDK21U-jdk-sources_21.0.12.1_1.tar.gz','sha256','573057d03584ae793fb7ec9a14c76d826d9187a53efeefd99da47403a5308234'),
 ('jlayer-1.0.1-gdx-sources.jar','https://repo.maven.apache.org/maven2/com/badlogicgames/jlayer/jlayer/1.0.1-gdx/jlayer-1.0.1-gdx-sources.jar','sha1','f1c4ffebc5e4f68be914404faab580c20de8ea1f'),
 ('jorbis-0.0.17-sources.jar','https://repo.maven.apache.org/maven2/org/jcraft/jorbis/0.0.17/jorbis-0.0.17-sources.jar','sha1','5446be5d62e4c6b347c8e6cd28dc00a936417ba2'),
 ('openal-soft-lwjgl-b874cc37.zip','https://github.com/LWJGL-CI/openal-soft/archive/b874cc37a1ea0c094e7d369d892d37d2cda29239.zip','sha256','53729d800ec5347e2ebe2b8fc075db7c2617ea57b05205c7d9a48b8850e237b5'),
 ('lwjgl-3.4.3-source.zip','https://github.com/LWJGL/lwjgl3/archive/refs/tags/3.4.3.zip','sha256','16bdfd598b63c195995f3c1dc3aac8603c1b69afe628f3ba2e1fb2d013b20d07'),
 ('libgdx-1.14.2-source.zip','https://github.com/libgdx/libgdx/archive/refs/tags/1.14.2.zip','sha256','1c13f8ffa6d668663f6081be95b09c4c9b23e7de3c8982f246f62a498fd9fac4'),
]
manifest=[]
for name,url,algorithm,expected in ARTIFACTS:
    path=DEST/name
    if not path.exists():
        request=urllib.request.Request(url,headers={'User-Agent':'OriginWar-source-packager'})
        with urllib.request.urlopen(request,timeout=120) as incoming,path.open('xb') as output:
            while chunk:=incoming.read(1024*1024):output.write(chunk)
    data=path.read_bytes()
    if algorithm and hashlib.new(algorithm,data).hexdigest()!=expected:
        raise RuntimeError(f'Published source checksum mismatch: {name}; retained for inspection')
    record={'file':name,'url':url,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
    manifest.append(record);print(json.dumps(record))
(DEST/'SOURCE_MANIFEST.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
