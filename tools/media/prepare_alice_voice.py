"""Fetch pinned, tool-only Piper and a from-scratch public-domain-data voice.
Large tool/model bytes stay in ignored .cache and are never runtime assets.
Recorded lock hashes are enforced on every later fetch; archive extraction is bounded.
"""
from pathlib import Path
import hashlib,json,urllib.request,zipfile

ROOT=Path(__file__).resolve().parents[2]
CACHE=ROOT/'tools/media/.cache/atmosphere-sound/voice-tool'
SOURCE=ROOT/'assets/audio/voice/source'
VOICE_REV='5041a7a49ae5b3a0f30e6837769eb03fdbb231bd'
BASE='https://huggingface.co/rhasspy/piper-voices/resolve/'+VOICE_REV+'/'
SPECS={
 'piper_windows_amd64.zip':{'url':'https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_windows_amd64.zip',
  'sha256':'f3c58906402b24f3a96d92145f58acba6d86c9b5db896d207f78dc80811efcea'},
 'en_US-ljspeech-high.onnx':{'url':BASE+'en/en_US/ljspeech/high/en_US-ljspeech-high.onnx',
  'sha256':'5d4f08ba6a2a48c44592eed3ce56bf85e9de3dd4e20df90541ae68a8310c029a'},
 'en_US-ljspeech-high.onnx.json':{'url':BASE+'en/en_US/ljspeech/high/en_US-ljspeech-high.onnx.json',
  'sha256':'7e1f4634af596d83cca997fb7a931ba80b70f8a316a2655ee69c55365e0ace14'}}
EVIDENCE={
 'MODEL_CARD.md':BASE+'en/en_US/ljspeech/high/MODEL_CARD',
 'VOICE_REPOSITORY_README.md':BASE+'README.md',
 'PIPER_MIT_LICENSE.txt':'https://raw.githubusercontent.com/rhasspy/piper/2023.11.14-2/LICENSE.md'}
RIGHTS={
 'AUTHOR_MODEL_LICENSE.txt':('https://brycebeattie.com/files/tts/',
     'LJSpeech (high) / License: public domain'),
 'LJ_SPEECH_DATASET_LICENSE.txt':('https://keithito.com/LJ-Speech-Dataset/',
     'All text, audio, and annotations are in the public domain.')}

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    CACHE.mkdir(parents=True,exist_ok=True);SOURCE.mkdir(parents=True,exist_ok=True)
    lockpath=SOURCE/'toolchain.json'
    old=json.loads(lockpath.read_text()) if lockpath.exists() else {}
    entries={}
    for name,spec in SPECS.items():
        path=CACHE/name
        if not path.exists():
            with urllib.request.urlopen(spec['url']) as response,path.open('wb') as output:
                while chunk:=response.read(1024*1024):output.write(chunk)
        actual=digest(path);expected=old.get('downloads',{}).get(name,{}).get('sha256',spec.get('sha256'))
        if expected and expected!=actual:raise RuntimeError('Hash mismatch: '+name)
        entries[name]={**spec,'sha256':actual,'bytes':path.stat().st_size}
    archive=CACHE/'piper_windows_amd64.zip'
    with zipfile.ZipFile(archive) as z:
        for item in z.infolist():
            target=(CACHE/item.filename).resolve()
            if not target.is_relative_to(CACHE.resolve()):raise RuntimeError('Unsafe tool archive path')
            if not target.exists():z.extract(item,CACHE)
    evidence={}
    for name,url in EVIDENCE.items():
        path=SOURCE/name
        if not path.exists():path.write_bytes(urllib.request.urlopen(url).read())
        evidence[name]={'url':url,'sha256':digest(path),'bytes':path.stat().st_size}
    for name,(url,excerpt) in RIGHTS.items():
        path=SOURCE/name
        path.write_text('Primary source: '+url+'\nVerified 2026-10-21 UTC.\nShort license excerpt: '+excerpt+'\n',encoding='utf-8',newline='\n')
        evidence[name]={'url':url,'sha256':digest(path),'bytes':path.stat().st_size}
    lock={'piper_release':'2023.11.14-2','piper_license':'MIT; tool only, not redistributed in runtime',
          'voice_repository_revision':VOICE_REV,'voice':'en_US-ljspeech-high',
          'model_license':'Public domain per author; repository MIT metadata',
          'dataset_license':'Public domain LJ Speech (Keith Ito / Linda Johnson)',
          'output_use':'Original game dialogue synthesized with a published general voice; no targeted actor/character impersonation. Source terms impose no output restriction.',
          'downloads':entries,'evidence':evidence}
    lockpath.write_text(json.dumps(lock,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(lock,indent=2))

if __name__=='__main__':main()
