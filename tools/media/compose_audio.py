"""Original Origin War synthesis. No samples or external instrument recordings.
Reproducible source for Signal Through Steel and the M1 survival sound library.
Requires numpy and ffmpeg. Outputs live under assets/audio; intermediates ignored.
"""
from __future__ import annotations
import json, math, shutil, subprocess, wave
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/"assets"/"audio"
SCRATCH=Path(__file__).resolve().parent/".render"
SR=44100
TAU=2*np.pi
RNG=np.random.default_rng(2115)

def hz(n): return 440*2**((n-69)/12)
def env(n,a=.01,r=.1):
    x=np.ones(n,np.float32); na=min(n,round(a*SR)); nr=min(n,round(r*SR))
    if na: x[:na]*=np.sin(np.linspace(0,np.pi/2,na))**2
    if nr: x[-nr:]*=np.cos(np.linspace(0,np.pi/2,nr))**2
    return x
def tone(note,seconds,color="bell"):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR; f=hz(note)
    if color=="pad":
        x=(.60*np.sin(TAU*f*t+.16*np.sin(TAU*.13*t))+.20*np.sin(TAU*f*1.003*t)+
           .12*np.sin(TAU*f*2*t)+.04*np.sin(TAU*f*3*t))*env(len(t),.55,.95)
    elif color=="bass":
        x=(.72*np.sin(TAU*f*t)+.18*np.sin(TAU*f*2*t)+.07*np.sin(TAU*f*3*t))*np.exp(-t*.55)*env(len(t),.015,.18)
    elif color=="pluck":
        x=(.65*np.sin(TAU*f*t)+.22*np.sin(TAU*f*2.005*t)+.09*np.sin(TAU*f*3*t))*np.exp(-t*3.2)*env(len(t),.008,.20)
    else:
        x=(.66*np.sin(TAU*f*t)+.19*np.sin(TAU*f*2.008*t)+.07*np.sin(TAU*f*4.03*t))*np.exp(-t*2.1)*env(len(t),.012,.25)
    return x.astype(np.float32)
def add(loop,x,start,gain=1,pan=0):
    idx=(round(start*SR)+np.arange(len(x)))%len(loop)
    np.add.at(loop[:,0],idx,x*gain*math.cos((pan+1)*math.pi/4))
    np.add.at(loop[:,1],idx,x*gain*math.sin((pan+1)*math.pi/4))
def normalized(x,peak_db):
    x=np.asarray(x,np.float32); x=x-np.mean(x,axis=0,keepdims=True)
    peak=float(np.max(np.abs(x)))
    if peak: x*=10**(peak_db/20)/peak
    return x
def write_wav(path,x):
    path.parent.mkdir(parents=True,exist_ok=True)
    if x.ndim==1: x=x[:,None]
    with wave.open(str(path),"wb") as w:
        w.setnchannels(x.shape[1]); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x,-1,1)*32767).astype("<i2").tobytes())
def metrics(x):
    return {"duration_seconds":round(len(x)/SR,6),
        "peak_dbfs":round(20*math.log10(max(1e-12,float(np.max(np.abs(x))))),2),
        "rms_dbfs":round(20*math.log10(max(1e-12,float(np.sqrt(np.mean(x*x))))),2)}
def exploration():
    beat=.75; bar=3
    music=np.zeros((round(96*SR),2),np.float32)
    # Eight-bar harmony, four orchestrated paragraphs. Return resolves suspended A.
    chords=[(50,57,60,64),(50,57,60,64),(46,53,57,62),(46,53,57,62),
            (43,50,57,62),(43,50,57,62),(45,52,57,62),(45,52,55,62)]
    # D-A-C-E call, D-C-A response, G-A-D extension, E-C#-A cadence.
    melody=[[(0,74),(1.5,81),(2.5,84)],[(0,88),(2,86)],
        [(0,86),(1,84),(2.5,81)],[(1,79),(3,77)],
        [(0,79),(1.5,81),(2.5,86)],[(0,84),(2,81)],
        [(0,76),(1.5,81),(3,86)],[(0,85),(2.5,81)]]
    for b in range(32):
        phrase,k=b//8,b%8; start=b*bar; chord=chords[k]
        for j,note in enumerate(chord):
            add(music,tone(note,bar+1.1,"pad"),start,[.085,.095,.085,.070][phrase],[-.6,-.2,.25,.65][j])
        add(music,tone(chord[0]-12,1.65,"bass"),start,.16)
        if phrase in (1,2): add(music,tone(chord[0]-12,.65,"bass"),start+2.5*beat,.075)
        if phrase!=0 or k%2==0:
            for offset,note in melody[k]:
                add(music,tone(note-(12 if phrase==3 else 0),1.65),start+offset*beat,
                    [.095,.12,.10,.085][phrase],-.25 if k%2 else .3)
        if phrase in (1,2):
            for offset,j in ((.75,1),(2.25,2),(3.5,3)):
                add(music,tone(chord[j]+12,.8,"pluck"),start+offset*beat,.038,-.5 if j%2 else .5)
        if phrase==2:
            for offset in (0,1.5,2,3.5):
                n=round(.14*SR);t=np.arange(n,dtype=np.float32)/SR
                pulse=(np.sin(TAU*(96*t-80*t*t))*.62+RNG.normal(0,.08,n))*np.exp(-t*36)*env(n,.003,.035)
                add(music,pulse,start+offset*beat,.07,-.1)
    # Circular stereo reflections retain all release tails across the loop seam.
    dry=music.copy()
    for delay,gain in ((.375,.22),(.75,.13),(1.125,.07),(2.25,.035)):
        music+=np.roll(dry[:,::-1],round(delay*SR),axis=0)*gain
    return normalized(np.tanh(music*1.05),-7)
def sweep(f0,f1,seconds):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR
    return np.sin(TAU*(f0*t+(f1-f0)*t*t/(2*seconds)))
def hiss(seconds,decay=8):
    n=round(seconds*SR);t=np.arange(n,dtype=np.float32)/SR
    return np.convolve(RNG.normal(0,1,n),np.ones(5)/5,mode="same")*np.exp(-t*decay)
def sound_library():
    e={}
    def motion(name,seconds,lo,hi,gain,noise,decay,attack,release):
        n=round(seconds*SR)
        e[name]=(sweep(lo,hi,seconds)*gain+hiss(seconds,decay)*noise)*env(n,attack,release)
    motion("step",.18,130,45,.65,.6,34,.002,.035)
    motion("step_lowgrav",.32,200,65,.55,.2,15,.008,.12)
    motion("door",.85,85,180,.35,.5,1.2,.10,.18)
    motion("lock",.28,450,160,.35,.45,30,.003,.08)
    e["pickup"]=tone(81,.55)*.6+tone(88,.55)*.35
    motion("oxygen_refill",1.55,150,430,.08,.65,.9,.1,.38)
    motion("switch",.20,510,750,.28,.65,50,.002,.055)
    n=round(SR);t=np.arange(n,dtype=np.float32)/SR
    e["hatch"]=(sweep(60,95,1)*.5+hiss(1,3)*.32+np.sin(TAU*40*t)*np.exp(-t*5)*.55)*env(n,.012,.23)
    n=round(.8*SR);t=np.arange(n,dtype=np.float32)/SR
    e["warning"]=np.sin(TAU*620*t)*(.55+.45*np.sin(TAU*4*t))*env(n,.035,.16)
    n=round(.4*SR);t=np.arange(n,dtype=np.float32)/SR
    e["gun"]=(hiss(.4,22)*.9+sweep(190,30,.4)*np.exp(-t*17)*.8)*env(n,.001,.12)
    n=round(.35*SR);t=np.arange(n,dtype=np.float32)/SR
    e["impact"]=(hiss(.35,22)*.6+np.sin(TAU*760*t)*np.exp(-t*25)*.3+np.sin(TAU*1120*t)*np.exp(-t*30)*.15)*env(n,.001,.09)
    e["ui"]=tone(86,.14,"pluck")
    n=round(.64*SR);t=np.arange(n,dtype=np.float32)/SR
    e["terminal"]=(np.sin(TAU*810*t)*.35+np.sin(TAU*1080*t)*.22)*(.5+.5*np.sign(np.sin(TAU*7*t)))*env(n,.015,.14)
    n=round(.33*SR);t=np.arange(n,dtype=np.float32)/SR
    e["robot_step"]=(np.sin(TAU*68*t)*np.exp(-t*15)*.7+hiss(.33,28)*.45+np.sin(TAU*340*t)*np.exp(-t*22)*.15)*env(n,.002,.08)
    n=round(1.4*SR);t=np.arange(n,dtype=np.float32)/SR
    phase=TAU*(460*t+70/(TAU*.8)*(1-np.cos(TAU*.8*t)))
    e["alarm"]=(np.sin(phase)*.55+np.sin(phase*2)*.12)*env(n,.08,.22)
    e["medicine"]=tone(76,.72)*.45+tone(83,.72)*.35+tone(88,.72)*.2
    purposes={
        "step":"Human boot on metal deck; one per accepted footfall.",
        "step_lowgrav":"Extended magnetic-boot release in low gravity.",
        "door":"Bulkhead slide motor; one cue per state transition.",
        "lock":"Latch refusal, distinct from successful switch.",
        "pickup":"Two-note acquisition confirmation.",
        "oxygen_refill":"Valve hiss and pressure rise on actual refill.",
        "switch":"Relay click and successful power-up chirp.",
        "hatch":"Heavy docking latch and powered hatch movement.",
        "warning":"Oxygen warning; rate-limit playback.",
        "gun":"Battery-gun impulse with low recoil body.",
        "impact":"Metal strike and short ringing tail.",
        "ui":"Quiet navigation or confirmation blip.",
        "terminal":"Successful data handshake.",
        "robot_step":"Heavy servomotor footfall distinct from a human.",
        "alarm":"Bounded security alarm rise/fall.",
        "medicine":"Medicine identification confirmation."}
    manifest={}
    peaks={"gun":-9,"impact":-12,"hatch":-12,"warning":-17,"alarm":-17,"ui":-22,"pickup":-18,"medicine":-18,"terminal":-19}
    for name,x in e.items():
        x=normalized(x,peaks.get(name,-16));write_wav(OUT/"sfx"/(name+".wav"),x)
        manifest[name]={**metrics(x),"channels":1,"sample_rate":SR,
            "path":"assets/audio/sfx/"+name+".wav","purpose":purposes[name]}
    return manifest
def main():
    SCRATCH.mkdir(parents=True,exist_ok=True);(OUT/"music").mkdir(parents=True,exist_ok=True)
    music=exploration();wav=SCRATCH/"exploration-render.wav";write_wav(wav,music)
    ffmpeg=shutil.which("ffmpeg")
    if not ffmpeg: raise RuntimeError("ffmpeg required for OGG/Vorbis export")
    subprocess.run([ffmpeg,"-hide_banner","-loglevel","error","-y","-i",str(wav),
        "-c:a","libvorbis","-q:a","5","-fflags","+bitexact","-flags:a","+bitexact","-metadata","title=Signal Through Steel",
        "-metadata","artist=Origin War original synthesis","-metadata",
        "comment=32 bars; 80 BPM; cyclic tails; no external samples",str(OUT/"music"/"exploration.ogg")],check=True)
    fx=sound_library()
    manifest={"task":"M1-MEDIA","status":"authored exports; integration/listening acceptance pending",
        "source":"tools/media/compose_audio.py","seed":2115,
        "provenance":"Original waveform/noise synthesis and authored notes/arrangement. No legacy or external audio samples.",
        "music":{"exploration":{"title":"Signal Through Steel","path":"assets/audio/music/exploration.ogg",
            "bpm":80,"bars":32,"beats_per_bar":4,"key_center":"D minor / suspended A",
            "channels":2,"sample_rate":SR,"loop":True,"loop_start_seconds":0,"loop_end_seconds":96,
            "arrangement":"Four eight-bar orchestrations: sparse beacon, harmonic clock, restrained industrial pulse, lower-register return. Circular reflections retain boundary tails.",
            **metrics(music),"boundary_sample_delta":round(float(np.max(np.abs(music[0]-music[-1]))),7),
            "acceptance":"Export checks only; actual game decoding, looping, mix and artistic listening require integration review."}},
        "sfx":fx,"limitations":["One exploration cue of the eventual six soundtrack roles.",
            "16 effects of the eventual 25+ library; no recorded voices.",
            "No integrated runtime or audible-quality acceptance is claimed."]}
    (OUT/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"music_seconds":len(music)/SR,"sfx_count":len(fx),"music_metrics":metrics(music)},indent=2))
if __name__=="__main__": main()
