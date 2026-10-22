"""ATM-SOUND: original helmet synthesis and organ scores. No external samples.
Editable note/envelope/spectral tables. Run compose_audio.py --repeatability.
Requires numpy and ffmpeg. All cache/intermediates stay under ignored .cache.
"""
from __future__ import annotations
import argparse, hashlib, json, math, shutil, subprocess, wave
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'assets/audio'
CACHE=ROOT/'tools/media/.cache/atmosphere-sound'
SR=44100
TAU=2*np.pi
SEED=2115

def sha(path): return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def hz(note): return 440*2**((note-69)/12)
def envelope(n,attack=.02,release=.1):
    e=np.ones(n,np.float32)
    a,r=min(n,round(attack*SR)),min(n,round(release*SR))
    if a: e[:a]*=np.sin(np.linspace(0,np.pi/2,a))**2
    if r: e[-r:]*=np.cos(np.linspace(0,np.pi/2,r))**2
    return e

def band_noise(seconds,low,high,seed):
    n=round(seconds*SR);x=np.random.default_rng(seed).normal(0,1,n)
    f=np.fft.rfftfreq(n,1/SR)
    shape=(1-np.exp(-(f/max(low,1))**4))/np.sqrt(1+(f/high)**8);shape[0]=0
    x=np.fft.irfft(np.fft.rfft(x)*shape,n)
    return (x/max(float(np.std(x)),1e-9)).astype(np.float32)

def normalize(x,db):
    x=np.asarray(x,np.float32);x-=np.mean(x,axis=0,keepdims=True)
    peak=float(np.max(np.abs(x)))
    if peak: x*=10**(db/20)/peak
    return x

def add(dst,mono,start,gain=1,pan=0):
    """Circular placement retains releases at the exact loop period."""
    at=round(start*SR)%len(dst);x=np.asarray(mono,np.float32)*gain
    if dst.ndim==2:
        x=x[:,None]*np.array([math.cos((pan+1)*math.pi/4),math.sin((pan+1)*math.pi/4)],np.float32)
    while len(x):
        count=min(len(x),len(dst)-at);dst[at:at+count]+=x[:count];x,at=x[count:],0

def organ(note,seconds,stop='wood',seed=0):
    """Authored additive pipe stop; no sampled instrument."""
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR;f=hz(note)
    partials={'wood':[(1,1),(2,.19),(3,.065),(4,.025)],
              'reed':[(1,.85),(2,.09),(3,.19),(5,.055)],
              'glass':[(1,1),(2,.06),(4,.015)]}[stop]
    x=np.zeros(len(t),np.float32)
    for multiple,gain in partials:
        x+=gain*np.sin(TAU*f*multiple*t+.007*np.sin(TAU*.23*t))
    x+=.008*band_noise(seconds,250,1100,SEED+seed)*np.exp(-t*5)
    return x*envelope(len(x),min(.65,seconds/5),min(2.4,seconds/3))

# Original, widely spaced notes. D-A-C-E is this project's prior signal identity.
# No reference score, MIDI, sample or borrowed soundtrack melody was imported.
SCORES={
 'menu':{'title':'Behind the Visor','seconds':72,'peak':-15,
  'form':'Three suspended organ breaths with unscored listening space.',
  'chords':[(5,10,[38,57,64]),(28,11,[41,57,60,67]),(52,12,[38,55,62,69])],
  'notes':[(14,74,4),(22,81,3),(40,84,5),(61,88,5)]},
 'exploration':{'title':'A Body in Vacuum','seconds':96,'peak':-16,
  'form':'Twelve-second unscored opening; incomplete phrases, twenty-second gaps in new note attacks; no drum grid.',
  'chords':[(14,12,[38,57,64]),(43,13,[36,55,62,69]),(77,13,[38,57,60,64])],
  'notes':[(18,74,5),(25,81,4),(48,84,6),(79,88,5)]},
 'combat':{'title':'Weight Through the Hull','seconds':96,'peak':-14,
  'form':'Four irregular low-organ groups and tactical gaps; no real-time fuse rhythm.',
  'chords':[(2,8,[38,50,57]),(22,8,[36,53,62]),(47,9,[34,53,60]),(73,12,[38,57,64])],
  'notes':[(6,62,2),(11,69,3),(26,65,2),(32,72,3),(51,64,3),(59,70,2),(77,74,4),(85,81,4)]},
 'danger':{'title':'The Glass Between','seconds':72,'peak':-18,
  'form':'Sparse suspended seconds and narrowing register. No score heartbeat/siren; body is separate.',
  'chords':[(8,12,[38,57,63]),(31,12,[37,56,62]),(54,12,[38,57,64])],
  'notes':[(18,75,5),(40,74,5),(62,76,4)]},
 'alice':{'title':'Permission to Choose','seconds':100,'peak':-15,
  'form':'Single reed question, incomplete signal, wider humane organ answer; unresolved final breath.',
  'chords':[(5,11,[38,57,64]),(31,12,[41,57,60,67]),(56,13,[43,59,62,69]),(79,13,[38,57,60,64])],
  'notes':[(11,74,5),(20,81,4),(37,84,5),(45,88,5),(60,74,4),(66,81,4),(72,84,4),(84,88,6)]},
 'ending':{'title':'One Light Behind Mars','seconds':80,'peak':-16,
  'form':'Eight seconds for physical aftermath, grief before warmth, suspended departure rather than fanfare.',
  'chords':[(10,13,[38,57,60]),(34,13,[41,57,60,67]),(58,14,[38,57,64,69])],
  'notes':[(17,74,6),(28,81,4),(43,84,6),(65,88,6)]}}

def score(role):
    spec=SCORES[role];x=np.zeros((round(spec['seconds']*SR),2),np.float32)
    for i,(start,duration,notes) in enumerate(spec['chords']):
        for j,note in enumerate(notes):
            add(x,organ(note,duration,seed=i*7+j),start+j*.09,.15/max(1,len(notes)/3),[-.3,.22,-.12,.33][j%4])
    for i,(start,note,duration) in enumerate(spec['notes']):
        add(x,organ(note,duration,'reed' if role=='alice' else 'glass',i+90),start,.095,-.16 if i%2 else .16)
    dry=x.copy()
    for seconds,gain in ((.17,.14),(.37,.10),(.71,.065),(1.13,.035)):
        x+=np.roll(dry[:,::-1],round(seconds*SR),axis=0)*gain
    return normalize(x,spec['peak'])

def pulse(seconds=.36,note=42,decay=20):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR
    return (np.sin(TAU*hz(note)*t)*.8+np.sin(TAU*hz(note)*2*t)*.2)*np.exp(-t*decay)*envelope(len(t),.006,.035)

def contact(seconds,seed,weight=1,edge=650):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR
    x=band_noise(seconds,80,edge,seed)*np.exp(-t*24)*.20
    x+=(np.sin(TAU*82*t)*.6+np.sin(TAU*171*t)*.13)*np.exp(-t*15)
    return x*envelope(len(x),.004,.05)*weight

def mechanism(seconds,seed,heavy=False):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR
    x=.12*band_noise(seconds,100,800,seed)*np.sin(np.pi*np.minimum(t/seconds,1))**1.5
    x+=(.16*np.sin(TAU*(83*t+8*t*t))+.05*np.sin(TAU*231*t))*envelope(len(t),.12,.18)
    add(x,contact(.14,seed+1),.02,.5)
    add(x,contact(.23,seed+2),seconds-.24,.9 if heavy else .55)
    return x

def body(kind,level):
    x=np.zeros(12*SR,np.float32)
    if kind=='breath':
        period=[6,4,3][level]
        for i,start in enumerate(np.arange(0,12,period)):
            for offset,duration,gain,cutoff in ((.12,period*.34,.65,800),(period*.43,period*.43,.8,650)):
                n=round(duration*SR);t=np.arange(n,dtype=np.float32)/SR
                air=band_noise(duration,110,cutoff,SEED+level*100+i*7+round(offset*100))
                chest=.10*np.sin(TAU*89*t)*np.sin(np.pi*t/duration)
                air=(air*.23+chest)*np.sin(np.pi*t/duration)**1.6
                add(x,air,start+offset,gain)
        return normalize(x,-10)
    period=[1,.8,.6][level]
    for start in np.arange(0,12,period):
        add(x,pulse(.20,38,24),start+.12,.8)
        add(x,pulse(.16,42,27),start+.12+period*.22,.43)
    return normalize(x,-13)

def effects():
    fx={}
    def put(name,x,peak=-13): fx[name]=normalize(x,peak)
    put('step',contact(.32,10));put('step_lowgrav',contact(.23,11,.65,430),-17)
    put('door',mechanism(.95,12),-16);put('hatch',mechanism(1.35,13,True),-14)
    put('lock',np.concatenate([contact(.09,14),np.zeros(round(.05*SR)),contact(.12,15)]),-15)
    put('pickup',contact(.19,16,edge=950),-18)
    t=np.arange(round(1.3*SR),dtype=np.float32)/SR
    valve=.12*band_noise(1.3,180,1600,17)*envelope(len(t),.08,.30)
    add(valve,contact(.13,18),0,.9);add(valve,contact(.17,19),1.08,.6);put('oxygen_refill',valve,-11)
    put('switch',contact(.16,20,edge=1100),-16)
    for name,note in (('warning',72),('alarm',62),('error',53),('confirm',65),('ui',67)):
        length=.42 if name in ('warning','alarm') else .13;t=np.arange(round(length*SR),dtype=np.float32)/SR
        y=(np.sin(TAU*hz(note)*t)*.12+np.sin(TAU*hz(note+7)*t)*.035)*envelope(len(t),.012,.06)
        if name=='warning': y*=.65+.35*np.sin(TAU*7*t)**2
        put(name,y,-13 if name=='warning' else -19)
    put('gun',contact(.31,23,edge=520),-11);put('impact',contact(.42,24,edge=700),-13)
    put('melee',contact(.37,25,edge=460),-12);put('drop',contact(.31,26,edge=900),-18)
    put('terminal',mechanism(.28,27),-20);put('robot_step',mechanism(.25,28),-19)
    put('sirs_step',mechanism(.22,29),-18)
    y=np.zeros(round(.52*SR),np.float32);add(y,contact(.22,30,edge=460),0,.8);add(y,contact(.25,31,edge=520),.22,.65);put('add_step',y,-16)
    y=np.zeros(round(.30*SR),np.float32);add(y,contact(.09,32,edge=900),0,.5);add(y,contact(.10,33,edge=1000),.16,.6);put('rover_step',y,-19)
    put('sirs_gun',np.concatenate([contact(.06,34,edge=950),contact(.24,35,edge=600)]),-13)
    y=np.zeros(round(.65*SR),np.float32);add(y,mechanism(.18,36),0,.3);add(y,contact(.40,37,edge=430),.18,1);put('add_gun',y,-11)
    # One finite armed signature, no accelerating wall-clock countdown.
    y=np.zeros(round(.45*SR),np.float32)
    add(y,contact(.11,38,edge=1000),0,.45);add(y,pulse(.18,65,23),.10,.35);add(y,pulse(.18,58,23),.26,.45);put('rover',y,-11)
    put('rover_tick',pulse(.17,58,28),-13)
    t=np.arange(round(1.7*SR),dtype=np.float32)/SR
    y=(band_noise(1.7,35,480,39)*.22+np.sin(TAU*(47*t-6*t*t))*.3)*np.exp(-t*3.1)*envelope(len(t),.008,.4);put('explosion',y,-9)
    put('medicine',mechanism(.33,40),-18)
    put('hack',np.concatenate([mechanism(.16,41),np.zeros(round(.12*SR)),contact(.14,42)]),-18)
    put('death',organ(38,2.6,'wood',43),-20);put('victory',organ(62,2.7,'glass',44),-22)
    # Compatibility cues. Continuous body loops replace old app scheduling.
    put('breath',body('breath',1)[:4*SR],-13);put('pulse',body('heartbeat',1)[:round(.8*SR)],-16)
    t=np.arange(round(1.6*SR),dtype=np.float32)/SR
    y=(.03*band_noise(1.6,130,750,45)+.04*np.sin(TAU*173*t))*np.sin(np.pi*t/1.6)**2
    put('hallucination',y,-27)  # Pressure/visor resonance, never speech/ghosts.
    return fx

def write_wav(path,x):
    path.parent.mkdir(parents=True,exist_ok=True)
    if x.ndim==1: x=x[:,None]
    with wave.open(str(path),'wb') as f:
        f.setnchannels(x.shape[1]);f.setsampwidth(2);f.setframerate(SR)
        f.writeframes(np.rint(np.clip(x,-1,1)*32767).astype('<i2').tobytes())

def metrics(x):
    return {'duration_seconds':round(len(x)/SR,6),'channels':1 if x.ndim==1 else x.shape[1],
      'sample_rate':SR,'peak_dbfs':round(20*math.log10(max(1e-12,float(np.max(np.abs(x))))),3),
      'rms_dbfs':round(20*math.log10(max(1e-12,float(np.sqrt(np.mean(x*x))))),3),
      'source_float_sha256':hashlib.sha256(x.tobytes()).hexdigest(),
      'boundary_sample_delta':round(float(np.max(np.abs(x[0]-x[-1]))),8)}

def export_ogg(path,x,ffmpeg):
    wav=CACHE/(path.stem+'-render.wav');write_wav(wav,x);path.parent.mkdir(parents=True,exist_ok=True)
    subprocess.run([ffmpeg,'-hide_banner','-loglevel','error','-y','-i',str(wav),'-map_metadata','-1',
      '-c:a','libvorbis','-q:a','4','-fflags','+bitexact','-flags:a','+bitexact',str(path)],check=True)
    wav.unlink()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--repeatability',action='store_true');args=parser.parse_args()
    ffmpeg=shutil.which('ffmpeg')
    if not ffmpeg: raise SystemExit('ffmpeg is required')
    CACHE.mkdir(parents=True,exist_ok=True)
    manifest={'task':'ATM-SOUND','status':'produced/export-validated; listening/runtime acceptance separate',
      'source':'tools/media/compose_helmet_audio.py','seed':SEED,
      'provenance':'Original additive pipe organ and deterministic body/contact/noise synthesis. No external samples, soundfont or copied melody.',
      'format':{'music':'OGG/Vorbis, 44100Hz stereo','sfx':'PCM16 WAV, 44100Hz mono','ambience':'OGG/Vorbis, 44100Hz mono'},
      'identity':'D-A-C-E signal, slow suspended organ, close suit body and filtered structure-borne contact.',
      'music':{},'sfx':{},'ambience':{},'repeatability':{},
      'mix':{'headroom':'All groups share .70 output reserve; six effects share .65/sqrt(N). Master/music/SFX sliders retained.',
             'body':'Breath .70 / heart .36 of SFX healthy; strain follows true oxygenation. Centered and close.',
             'score':'.68 role trim, -8dB under voice; no score pulse masquerades as a fuse.',
             'outside':'Quiet distance-attenuated structure contact, modest pan, no invented events.',
             'voice':'Dry filtered ALICE, .86 SFX, static below words; interrupt on dialogue change.'},
      'limitations':['Metrics prove files and levels, not hearing/emotion/pronunciation/backend seams.',
                      'Synthetic fear is a processing/prosody target requiring owner listening.']}
    for role,spec in SCORES.items():
        x=score(role);path=OUT/'music'/(role+'.ogg');export_ogg(path,x,ffmpeg)
        manifest['music'][role]={'title':spec['title'],'path':path.relative_to(ROOT).as_posix(),
          'arrangement':spec['form'],'loop':True,'loop_start_seconds':0,'loop_end_seconds':spec['seconds'],**metrics(x),'sha256':sha(path)}
        if args.repeatability:
            y=score(role);assert x.tobytes()==y.tobytes(),role
            second=CACHE/(role+'-repeat.ogg');export_ogg(second,y,ffmpeg);assert sha(second)==sha(path),role
            manifest['repeatability'][role]='float and exported OGG bytes identical';second.unlink()
    for name,x in effects().items():
        path=OUT/'sfx'/(name+'.wav');write_wav(path,x)
        manifest['sfx'][name]={'path':path.relative_to(ROOT).as_posix(),**metrics(x),'sha256':sha(path)}
    if args.repeatability:
        for name,x in effects().items():
            assert hashlib.sha256(x.tobytes()).hexdigest()==manifest['sfx'][name]['source_float_sha256'],name
        manifest['repeatability']['sfx']='second-render float hashes identical; deterministic PCM16 export'
    for kind in ('breath','heartbeat'):
        for level,label in enumerate(('healthy','warning','critical')):
            name=kind+'_'+label;x=body(kind,level);path=OUT/'ambience'/(name+'.ogg');export_ogg(path,x,ffmpeg)
            manifest['ambience'][name]={'path':path.relative_to(ROOT).as_posix(),'loop':True,**metrics(x),'sha256':sha(path)}
            if args.repeatability:
                y=body(kind,level);assert x.tobytes()==y.tobytes(),name
                second=CACHE/(name+'-repeat.ogg');export_ogg(second,y,ffmpeg);assert sha(second)==sha(path),name
                second.unlink()
    if args.repeatability: manifest['repeatability']['ambience']='all second-render float and exported OGG bytes identical'
    (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8',newline='\n')
    (OUT/'runtime.json').write_text(json.dumps({'version':1,'sfx':{k:v['duration_seconds'] for k,v in manifest['sfx'].items()}},indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'music':6,'sfx':len(manifest['sfx']),'ambience':6,'repeatability':manifest['repeatability']},indent=2))

if __name__=='__main__': main()
