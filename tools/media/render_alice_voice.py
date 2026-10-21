"""Render authoritative ALICE quoted speech with pinned offline Piper.
No invented words, actor imitation, model redistribution, or fake voice noise.
Source WAVs and exact text spans are retained; radio processing is reproducible.
"""
from pathlib import Path
import argparse,hashlib,json,re,shutil,subprocess,wave
import numpy as np
from compose_helmet_audio import SR,CACHE,OUT,ROOT,band_noise,envelope,write_wav,sha

TOOL=CACHE/'voice-tool'
SOURCE=OUT/'voice/source'
JAVA=ROOT/'core/src/main/java/io/zipcoder/originwar/campaign/Campaign.java'
PLAN=SOURCE/'sardonic-delivery.json'

def word_tokens(text):
    return re.findall(r"[A-Za-z0-9]+(?:['\u2019][A-Za-z0-9]+)*",text.casefold())

def delivery(entry,plan):
    preset=plan['clips'][entry['key']]
    phrases=preset['phrases']
    assert word_tokens(' '.join(p['text'] for p in phrases))==word_tokens(' '.join(entry['spoken_spans'])),entry['key']
    assert 0<=preset['anxiety']<=.6 and 1<=len(phrases)<=12
    for phrase in phrases:
        assert .90<=phrase['length_scale']<=1.15
        assert 0<=phrase['pause_before_seconds']<=.65
        assert -1<=phrase['tail_semitones']<=.3
        assert .9<=phrase['level']<=1.1
    return preset

def read_source(path):
    with wave.open(str(path),'rb') as w:
        assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==22050
        return np.frombuffer(w.readframes(w.getnframes()),'<i2').astype(np.float32)/32768

def write_source(path,x):
    path.parent.mkdir(parents=True,exist_ok=True)
    with wave.open(str(path),'wb') as w:
        w.setnchannels(1);w.setsampwidth(2);w.setframerate(22050)
        w.writeframes(np.rint(np.clip(x,-1,1)*32767).astype('<i2').tobytes())

def shape_phrase(raw,phrase):
    # Trim numeric silence only, retaining 40ms around every nonzero speech sample.
    # No consonant/word is truncated to obtain a clipped style.
    nz=np.flatnonzero(np.abs(raw)>1/32768)
    assert len(nz)>0
    x=raw[max(0,nz[0]-882):min(len(raw),nz[-1]+883)]
    n=len(x);extra=round(n*1.12)+4
    t=np.arange(extra,dtype=np.float64)/max(n-1,1)
    tail=np.clip((t-.68)/.32,0,1)
    ratio=2**(phrase['tail_semitones']*tail/12)
    positions=np.concatenate(([0.],np.cumsum(ratio[:-1])))
    positions=positions[positions<n-1]
    shaped=np.interp(positions,np.arange(n),x).astype(np.float32)
    # Phrase-level emphasis is authored, not an unverified guessed word alignment.
    rms=max(float(np.sqrt(np.mean(shaped*shaped))),1e-8)
    shaped*=min(.065/rms,.80/max(float(np.max(np.abs(shaped))),1e-8))*phrase['level']
    edge=min(110,len(shaped)//2)
    if edge:
        shaped[:edge]*=np.linspace(0,1,edge);shaped[-edge:]*=np.linspace(1,0,edge)
    return shaped

def assemble(phrase_records,preset):
    chunks=[]
    for record,phrase in zip(phrase_records,preset['phrases']):
        chunks.append(np.zeros(round(phrase['pause_before_seconds']*22050),np.float32))
        chunks.append(shape_phrase(read_source(SOURCE/record['file']),phrase))
    chunks.append(np.zeros(round(.10*22050),np.float32))
    x=np.concatenate(chunks);peak=float(np.max(np.abs(x)))
    if peak>.90:x*=.90/peak
    return x

def extract():
    code=JAVA.read_text(encoding='utf-8');entries=[]
    for match in re.finditer(r'case "([^"\n]+)"->d\("A\.L\.I\.C\.E\."',code):
        block=code[match.start():].split('\n            case ',1)[0]
        strings=[json.loads(s) for s in re.findall(r'"(?:\\.|[^"\\])*"',block)]
        texts=strings[3:6] if match[1]=='alice-talk' else strings[3:4]
        for variant,full in enumerate(texts):
            spoken=re.findall(r'"([^"]+)"',full,re.S)
            assert spoken,(match[1],variant)
            key=match[1]+('-'+str(variant) if len(texts)>1 else '')
            entries.append({'key':key,'dialogue_id':match[1],'full_text':full,
              'text_sha256':hashlib.sha256(full.encode()).hexdigest(),'spoken_spans':spoken,
              'fear':.6 if match[1] in ('refusal','sacrifice-plan','pod-ready') else .18})
    assert len(entries)==9,len(entries)
    return entries

def radio(raw,rate,fear,seed):
    # Fixed resampling then restrained high/low filtering: a close helmet speaker,
    # not a cathedral reverb, monster pitch shift, or whole-sentence static mask.
    n=round(len(raw)*SR/rate)
    x=np.interp(np.arange(n)*rate/SR,np.arange(len(raw)),raw).astype(np.float32)
    f=np.fft.rfftfreq(n,1/SR)
    filt=(1-np.exp(-(f/190)**4))/np.sqrt(1+(f/3200)**8)
    x=np.fft.irfft(np.fft.rfft(x)*filt,n).astype(np.float32)
    t=np.arange(n,dtype=np.float32)/SR
    x*=1+(.004+.026*fear)*np.sin(2*np.pi*4.8*t)
    # Quiet narrow mechanical sideband, subtle dry robotic edge.
    x*=.985+.015*np.sin(2*np.pi*71*t)
    x*=envelope(n,.018,.045)
    peak=max(float(np.max(np.abs(x))),1e-8)
    x=np.tanh(x/peak*3)/np.tanh(3)  # Gentle radio saturation reduces plosive crest factor.
    x*=10**(-10/20)
    static=band_noise(n/SR,550,3500,seed)*.0014
    # Carrier opens/closes around words. Static stays over 40 dB below speech peak.
    static*=envelope(n,.03,.05)
    x+=static
    return np.concatenate([np.zeros(round(.09*SR),np.float32),x,np.zeros(round(.15*SR),np.float32)])

def main():
    if (SOURCE/'expressive/manifest.json').exists():
        raise SystemExit('The active ALICE set uses expressive masters. Use export_expressive_alice.py with a fresh output directory; historical Piper sources remain preserved.')
    parser=argparse.ArgumentParser();parser.add_argument('--repeatability',action='store_true')
    parser.add_argument('--reprocess-only',action='store_true');parser.add_argument('--only')
    args=parser.parse_args()
    plan=json.loads(PLAN.read_text(encoding='utf-8'));assert plan['version']==1
    lock=json.loads((SOURCE/'toolchain.json').read_text())
    if not args.reprocess_only:
        for name,spec in lock['downloads'].items():assert sha(TOOL/name)==spec['sha256'],name
    piper=TOOL/'piper/piper.exe';model=TOOL/'en_US-ljspeech-high.onnx';ffmpeg=shutil.which('ffmpeg')
    prior={e['key']:e for e in json.loads((SOURCE/'dialogue-source.json').read_text())['clips']}
    runtime={(e['dialogue_id'],e['text_sha256']):e for e in json.loads((OUT/'voice/index.json').read_text())['clips']}
    entries=extract();assert set(plan['clips'])=={e['key'] for e in entries}
    if args.only:assert args.only in plan['clips']
    processed_keys=[]
    for i,entry in enumerate(entries):
        preset=delivery(entry,plan)
        if args.only and args.only!=entry['key']:continue
        entry['fear']=preset['anxiety']
        rawpath=SOURCE/(entry['key']+'-dry.wav')
        if args.reprocess_only:
            assert sha(rawpath)==prior[entry['key']]['source_wav_sha256']
            assert prior[entry['key']]['delivery_plan_sha256']==sha(PLAN)
            records=prior[entry['key']]['phrases']
            for record in records:assert sha(SOURCE/record['file'])==record['sha256']
            repeat=prior[entry['key']]['repeatability']
        else:
            records=[]
            for j,phrase in enumerate(preset['phrases']):
                name='phrases/'+entry['key']+'-'+str(j).zfill(2)+'.wav'
                path=SOURCE/name;path.parent.mkdir(parents=True,exist_ok=True)
                cmd=[str(piper),'--model',str(model),'--output_file',str(path),
                     '--length_scale',str(phrase['length_scale']),'--noise_scale','0',
                     '--noise_w','0','--sentence_silence','0']
                subprocess.run(cmd,input=phrase['text']+'\n',text=True,encoding='utf-8',capture_output=True,check=True)
                if args.repeatability:
                    second=CACHE/(entry['key']+'-'+str(j)+'-repeat.wav')
                    again=cmd.copy();again[4]=str(second)
                    subprocess.run(again,input=phrase['text']+'\n',text=True,encoding='utf-8',capture_output=True,check=True)
                    assert second.read_bytes()==path.read_bytes(),(entry['key'],j)
                    second.unlink()
                records.append({'file':name,'sha256':sha(path),'synthesis_text':phrase['text'],
                                'piper_arguments':cmd[5:]})
            combined=assemble(records,preset)
            if args.repeatability:assert np.array_equal(combined,assemble(records,preset))
            write_source(rawpath,combined)
            repeat='independent phrase Piper WAV bytes and assembled float bytes identical' if args.repeatability else 'not requested'
        raw=read_source(rawpath)
        processed=radio(raw,22050,entry['fear'],2115+i)
        wav=CACHE/(entry['key']+'-radio.wav');write_wav(wav,processed)
        target=OUT/'voice'/(entry['key']+'.ogg')
        encode=[ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(wav),'-map_metadata','-1',
                '-c:a','libvorbis','-q:a','4','-fflags','+bitexact','-flags:a','+bitexact',str(target)]
        subprocess.run(encode,check=True)
        prior[entry['key']]={**entry,'source_wav_sha256':sha(rawpath),'source_rate':22050,
                'delivery_plan_sha256':sha(PLAN),'phrases':records,'repeatability':repeat}
        runtime[(entry['dialogue_id'],entry['text_sha256'])]={'dialogue_id':entry['dialogue_id'],'text_sha256':entry['text_sha256'],
           'path':'audio/voice/'+target.name,'sha256':sha(target),'duration_seconds':round(len(processed)/SR,6)}
        wav.unlink()
        processed_keys.append(entry['key'])
    campaign_sha=hashlib.sha256(JAVA.read_text(encoding='utf-8').encode('utf-8')).hexdigest()
    (SOURCE/'dialogue-source.json').write_text(json.dumps({'campaign_source_sha256':campaign_sha,'source_line_endings':'UTF-8 canonical LF',
       'description':'Exact Campaign full text/quoted words; authored phrasing changes punctuation and performance only.',
       'clips':[prior[e['key']] for e in entries]},indent=2)+'\n',encoding='utf-8',newline='\n')
    ordered=[runtime[(e['dialogue_id'],e['text_sha256'])] for e in entries]
    (OUT/'voice/index.json').write_text(json.dumps({'version':1,'clips':ordered},indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'processed':processed_keys,'clips':len(ordered),'seconds':{e['path']:e['duration_seconds'] for e in ordered},
                      'repeatability':args.repeatability},indent=2))

if __name__=='__main__':main()
