"""Collect versioned upstream notices. Inputs are read-only archives/JDK; no downloaded code is executed."""
from pathlib import Path
import hashlib,json,re,sys,urllib.request,zipfile

sources=Path(sys.argv[1]);jdk=Path(sys.argv[2]);out=Path('licenses');out.mkdir(exist_ok=True)
prior=json.loads((out/'MANIFEST.json').read_text()) if (out/'MANIFEST.json').exists() else {}
records={}
def write(name,data,source):
    digest=hashlib.sha256(data).hexdigest()
    if name in prior and prior[name]['sha256']!=digest:raise RuntimeError('Changed pinned notice; inspect before updating: '+name)
    path=out/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(data)
    records[name]={'source':source,'sha256':digest}
def remote(name,url):
    req=urllib.request.Request(url,headers={'User-Agent':'OriginWar-notice-packager'})
    try:
        with urllib.request.urlopen(req,timeout=60) as response:data=response.read()
    except Exception as error:raise RuntimeError('Could not read verified notice URL: '+url) from error
    write(name,data,url)
def archive(archive_name,relative,target):
    with zipfile.ZipFile(sources/archive_name) as z:
        names=[n for n in z.namelist() if n.split('/',1)[-1]==relative]
        if len(names)!=1:raise RuntimeError('Ambiguous/missing upstream notice: '+relative)
        write(target,z.read(names[0]),archive_name+':'+names[0])

for filename in ('LICENSE','AUTHORS','CC-LICENSE'):archive('libgdx-1.14.2-source.zip',filename,'libgdx/'+filename)
archive('lwjgl-3.4.3-source.zip','LICENSE.md','lwjgl/LICENSE.md')
for item in ('core/libffi_license.txt','core/liburing_license.txt','glfw/glfw_license.txt','jemalloc/jemalloc_license.txt','openal/openal_soft_license.txt','opengl/khronos_license.txt'):
    archive('lwjgl-3.4.3-source.zip','modules/lwjgl/'+item,'lwjgl/'+item.replace('/','-'))
for filename in ('COPYING','BSD-3Clause','LICENSE-pffft','fmt-11.2.0/LICENSE','gsl/LICENSE'):
    archive('openal-soft-lwjgl-b874cc37.zip',filename,'openal/'+filename.replace('/','-'))
# Preserve every distinct original source-header attribution, including BitmapFont's BSD grant.
with zipfile.ZipFile(sources/'libgdx-1.14.2-source.zip') as z:
    headers={}
    for name in z.namelist():
        if Path(name).suffix not in ('.java','.c','.h','.cpp','.kt'):continue
        text=z.read(name).decode('utf-8',errors='replace');start=text.find('/*');end=text.find('*/',start)
        if start>=0 and end>start and start<400:
            header=text[start:end+2]
            if re.search(r'copyright|licensed|permission|public domain',header,re.I):headers.setdefault(header,[]).append(name)
    payload='\n\n'.join('SOURCE: '+names[0]+'\n'+header for header,names in sorted(headers.items()))
    write('libgdx/SOURCE_HEADERS.txt',payload.encode(),'libgdx-1.14.2-source.zip: original source headers, deduplicated')
with zipfile.ZipFile(sources/'lwjgl-3.4.3-source.zip') as z:
    blocks=[]
    for name in z.namelist():
        if '/stb/src/main/c/' in name and name.endswith('.h'):
            text=z.read(name).decode(errors='replace');index=text.rfind('ALTERNATIVE A')
            if index>=0:blocks.append('SOURCE: '+name+'\n'+text[index:])
    write('lwjgl/STB_NOTICES.txt','\n\n'.join(blocks).encode(),'lwjgl-3.4.3-source.zip: bundled STB header ending license blocks')

raw='https://raw.githubusercontent.com/'
for repo,ref in [('yellowstonegames/SquidSquad','883a335218c8beffc41c95b0263881910928150b'),('tommyettinger/digital','22cbccede42d2a61216bedb3c03b8d5cbc97e274'),('tommyettinger/jdkgdxds','82c3fe77756ba1cbe951c77db5615236f46e92b2'),('tommyettinger/juniper','aa469ee349e3480dbbb69caa33ac4bd18d3098b2')]:
    for file in (('LICENSE',) if repo.endswith('/SquidSquad') else ('LICENSE','AUTHORS')):remote(repo.split('/')[-1]+'/'+file,raw+repo+'/'+ref+'/'+file)
for repo in ('crux','regexodus','funderby'):
    remote(repo+'/LICENSE',raw+'tommyettinger/'+repo+'/master/LICENSE')
remote('font/LICENSE-OFL-1.1',raw+'liberationfonts/liberation-fonts/4b0192046158094654e865245832c66d2104219e/LICENSE')
for name in ('LICENSE','AUTHORS'):remote('gdx-jnigen/'+name,raw+'libgdx/gdx-jnigen/be21532209a3735c01c2a804a0e48d1a20dbb0de/'+name)
remote('LGPL-2.1.txt','https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt')
write('temurin/NOTICE',(jdk/'NOTICE').read_bytes(),'Temurin21.0.12.1+1 original distribution NOTICE')
write('temurin/RELEASE_IDENTITY.txt',(jdk/'release').read_bytes(),'Temurin21.0.12.1+1 original release metadata')
voice=Path('assets/audio/voice/source')
if (voice/'toolchain.json').exists():
    locked=json.loads((voice/'toolchain.json').read_text(encoding='utf-8'))
    for name,entry in locked['evidence'].items():
        payload=(voice/name).read_bytes()
        if hashlib.sha256(payload).hexdigest()!=entry['sha256']:raise RuntimeError('Voice evidence hash mismatch: '+name)
        write('alice/'+name,payload,entry['url'])
(out/'MANIFEST.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
print(f'Collected {len(records)} original notice payloads; archive source materials remain separate.')
