"""Export clean performances and a separate deterministic speech-free radio layer."""
import argparse,hashlib,json,subprocess
from pathlib import Path
import numpy as np
from compose_helmet_audio import export_ogg,band_noise

TONE={'pitch_semitones':1.25,'high_shelf_db':2.,'high_shelf_mid_hz':2200.,'high_shelf_width_hz':600.,'peak_ceiling_dbfs':-10.,'voice_layer':'clean; no radio filtering, saturation, modulation or static'}

def decode_native(path,pitch=False):
    cmd=['ffmpeg','-nostdin','-v','error','-i',str(path),'-ac','1']
    if pitch:cmd+=['-af','rubberband=tempo=1:pitch='+str(2**(TONE['pitch_semitones']/12))+':formant=shifted']
    data=subprocess.run(cmd+['-f','f32le','-acodec','pcm_f32le','-'],capture_output=True,check=True).stdout
    return np.frombuffer(data,'<f4')

def clean_voice(path):
    pitched=decode_native(path,True)
    n=round(len(pitched)*44100/24000)
    x=np.interp(np.arange(n)*24000/44100,np.arange(len(pitched)),pitched).astype(np.float32)
    frequencies=np.fft.rfftfreq(len(x),1/44100)
    transition=1/(1+np.exp(-(frequencies-TONE['high_shelf_mid_hz'])/TONE['high_shelf_width_hz']))
    shelf=10**(TONE['high_shelf_db']/20)
    x=np.fft.irfft(np.fft.rfft(x)*(1+(shelf-1)*transition),len(x)).astype(np.float32)
    x*=10**(TONE['peak_ceiling_dbfs']/20)/max(float(np.abs(x).max()),1e-8)
    edge=min(220,len(x)//2);x[:edge]*=np.linspace(0,1,edge);x[-edge:]*=np.linspace(1,0,edge)
    return np.concatenate([np.zeros(2646,np.float32),x,np.zeros(5292,np.float32)])

def static_layer():
    # Periodic, speech-free original texture: independent of every voice master.
    n=12*44100;t=np.arange(n,dtype=np.float64)/44100
    x=band_noise(12,350,5000,211503)*(.7+.2*np.sin(2*np.pi*.5*t))
    x+=.04*np.sin(2*np.pi*91*t)
    x=x.astype(np.float32);x*=.02/max(float(np.abs(x).max()),1e-8)
    return x

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    p=argparse.ArgumentParser();p.add_argument('--sources',type=Path,required=True);p.add_argument('--output-dir',type=Path,required=True);p.add_argument('--keys',nargs='+');args=p.parse_args()
    args.output_dir.mkdir(parents=True,exist_ok=True)
    records={p.stem:json.loads(p.read_text(encoding='utf-8')) for p in args.sources.glob('*.json') if p.stem not in ('receipt','export-receipt')}
    keys=args.keys or list(records);clips=[]
    for key in keys:
        entry=records[key];path=args.sources/entry['source_wav'];assert sha(path)==entry['source_wav_sha256']
        assert entry['source_rate']==24000 and entry['finite'] and 2<entry['seconds']<55
        raw=decode_native(path);assert np.isfinite(raw).all() and len(raw)==round(entry['seconds']*24000)
        x=clean_voice(path);target=args.output_dir/(key+'.ogg');assert not target.exists()
        export_ogg(target,x,'ffmpeg')
        clips.append({'key':key,'dialogue_id':entry['dialogue_id'],'text_sha256':entry['full_text_sha256'],
            'path':'audio/voice/'+target.name,'sha256':sha(target),'duration_seconds':round(len(x)/44100,6),
            'source_wav_sha256':sha(path),'source_rate':24000,'tone':TONE,'layer':'clean_voice'})
    static=args.output_dir/'radio-static.ogg'
    if not static.exists():export_ogg(static,static_layer(),'ffmpeg')
    receipt=args.output_dir/('export-receipt-'+keys[0]+'.json')
    with receipt.open('x',encoding='utf-8') as f:json.dump({'scope':'Clean voice and separate speech-free static exports; heard performance is separate.','clips':clips,'radio_static':{'path':'audio/voice/radio-static.ogg','sha256':sha(static),'duration_seconds':12,'speech_free_by_construction':True}},f,indent=2)
    print(json.dumps({'clips':len(clips),'output':str(args.output_dir)},indent=2))

if __name__=='__main__':main()
