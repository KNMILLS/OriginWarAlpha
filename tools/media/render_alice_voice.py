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
    x*=1+(.018+.026*fear)*np.sin(2*np.pi*4.8*t)
    # Quiet narrow mechanical sideband, subtle dry robotic edge.
    x*=.985+.015*np.sin(2*np.pi*71*t)
    x*=envelope(n,.018,.045)
    peak=max(float(np.max(np.abs(x))),1e-8)
    x=np.tanh(x/peak*3)/np.tanh(3)  # Gentle radio saturation reduces plosive crest factor.
    x*=10**(-9/20)
    static=band_noise(n/SR,550,3500,seed)*.0014
    # Carrier opens/closes around words. Static stays over 40 dB below speech peak.
    static*=envelope(n,.03,.05)
    x+=static
    return np.concatenate([np.zeros(round(.09*SR),np.float32),x,np.zeros(round(.15*SR),np.float32)])

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--repeatability',action='store_true')
    parser.add_argument('--reprocess-only',action='store_true');args=parser.parse_args()
    lock=json.loads((SOURCE/'toolchain.json').read_text())
    if not args.reprocess_only:
        for name,spec in lock['downloads'].items():assert sha(TOOL/name)==spec['sha256'],name
    piper=TOOL/'piper/piper.exe';model=TOOL/'en_US-ljspeech-high.onnx';ffmpeg=shutil.which('ffmpeg')
    prior={e['key']:e for e in json.loads((SOURCE/'dialogue-source.json').read_text())['clips']} if args.reprocess_only else {}
    source=[];runtime=[]
    for i,entry in enumerate(extract()):
        # Punctuation/text remains authoritative. Separate existing spoken spans
        # permit a deliberate hesitation while narratorial paragraphs stay silent.
        words='\n'.join(entry['spoken_spans'])
        rawpath=SOURCE/(entry['key']+'-dry.wav')
        cmd=[str(piper),'--model',str(model),'--output_file',str(rawpath),
             '--length_scale',str(1.03+entry['fear']*.07),'--noise_scale','0',
             '--noise_w','0','--sentence_silence','.32']
        if args.reprocess_only:assert sha(rawpath)==prior[entry['key']]['source_wav_sha256']
        else:subprocess.run(cmd,input=words+'\n',text=True,encoding='utf-8',capture_output=True,check=True)
        with wave.open(str(rawpath),'rb') as w:
            assert w.getnchannels()==1 and w.getsampwidth()==2
            rate=w.getframerate();raw=np.frombuffer(w.readframes(w.getnframes()),'<i2').astype(np.float32)/32768
        processed=radio(raw,rate,entry['fear'],2115+i)
        wav=CACHE/(entry['key']+'-radio.wav');write_wav(wav,processed)
        target=OUT/'voice'/(entry['key']+'.ogg')
        encode=[ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(wav),'-map_metadata','-1',
                '-c:a','libvorbis','-q:a','4','-fflags','+bitexact','-flags:a','+bitexact',str(target)]
        subprocess.run(encode,check=True)
        repeat=prior.get(entry['key'],{}).get('repeatability','not requested')
        if args.repeatability and not args.reprocess_only:
            second=CACHE/(entry['key']+'-dry-repeat.wav');repeatcmd=cmd.copy();repeatcmd[repeatcmd.index('--output_file')+1]=str(second)
            subprocess.run(repeatcmd,input=words+'\n',text=True,encoding='utf-8',capture_output=True,check=True)
            assert second.read_bytes()==rawpath.read_bytes(),entry['key']
            assert np.array_equal(processed,radio(raw,rate,entry['fear'],2115+i)),entry['key']
            repeat='independent Piper WAV and processing float bytes identical';second.unlink()
        source.append({**entry,'source_wav_sha256':sha(rawpath),'source_rate':rate,
                       'piper_arguments':cmd[5:],'repeatability':repeat})
        runtime.append({'dialogue_id':entry['dialogue_id'],'text_sha256':entry['text_sha256'],
           'path':'audio/voice/'+target.name,'sha256':sha(target),'duration_seconds':round(len(processed)/SR,6)})
        wav.unlink()
    campaign_sha=hashlib.sha256(JAVA.read_text(encoding='utf-8').encode('utf-8')).hexdigest()
    (SOURCE/'dialogue-source.json').write_text(json.dumps({'campaign_source_sha256':campaign_sha,'source_line_endings':'UTF-8 canonical LF',
       'description':'Exact full text and ALICE quoted spans, no narrator or rewritten campaign copy.',
       'clips':source},indent=2)+'\n',encoding='utf-8',newline='\n')
    (OUT/'voice/index.json').write_text(json.dumps({'version':1,'clips':runtime},indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'clips':len(runtime),'seconds':sum(x['duration_seconds'] for x in runtime),
                      'repeatability':args.repeatability},indent=2))

if __name__=='__main__':main()
