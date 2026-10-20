"""Original Origin War synthesis. No samples or external instrument recordings.
Reproducible source for Signal Through Steel and the M1 survival sound library.
Requires numpy and ffmpeg. Outputs live under assets/audio; intermediates ignored.
"""
from __future__ import annotations
import argparse, hashlib, json, math, shutil, subprocess, wave
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
# Additional score tables are deliberately editable: MIDI note numbers, beat
# positions, harmonic voicings and section orchestration remain visible here.
# The original exploration() function above is retained byte-for-byte.
SCORE_ORDER=("menu","combat","danger","alice","ending")
SCORES={
 "menu":{"title":"The Signal Before Departure","bpm":80,"bars":24,"peak":-8,
    "chords":[(50,57,60,64),(48,55,60,64),(46,53,57,62),(46,53,57,65),
              (43,50,57,62),(48,55,60,64),(45,52,57,62),(45,52,55,61)],
    "melody":[[(0,74),(1,81),(2.5,84)],[(.5,88),(2,86)],[(0,86),(2,81)],
              [(0,77),(2,81)],[(0,79),(1.5,81),(3,86)],[(0,84),(2.5,79)],
              [(0,76),(1.5,81),(3,86)],[(.5,85),(2,81)]],
    "form":"Three eight-bar statements: beacon theme, interlocking signal, lower-register departure. Bell melody, warm strings and sparse clock."},
 "combat":{"title":"Steel Against the Breach","bpm":120,"bars":48,"peak":-7,
    "chords":[(50,53,57,64),(50,53,57,60),(46,53,57,62),(39,55,58,62),
              (43,50,57,62),(46,53,57,60),(45,52,55,61),(45,52,58,61)],
    "melody":[[(.5,62),(1.25,69),(2.5,72),(3.25,76)],[(0,74),(2,72),(3,69)],
              [(0,65),(1.5,69),(2.75,74)],[(.5,70),(2,67),(3,62)],
              [(0,67),(1,69),(2.5,74)],[(.5,72),(1.5,69),(3,65)],
              [(0,64),(1.25,69),(2.75,73)],[(.5,76),(2,73),(3.25,69)]],
    "form":"Six eight-bar sections: pulse, theme, pressure, percussion-light bridge, upper theme, return. Syncopated bass, metallic percussion and short brass-like lead."},
 "danger":{"title":"Air Between Heartbeats","bpm":80,"bars":24,"peak":-10,
    "chords":[(38,57,60,64),(38,57,60,63),(34,53,57,62),(34,53,56,62),
              (31,50,57,62),(31,50,56,62),(33,52,57,62),(33,52,55,61)],
    "melody":[[(2,76)],[],[(1,74)],[(2.5,72)],[(0,69)],[],[(2,70)],[(2.5,69)]],
    "form":"Three eight-bar warning bands: exposed paired pulse, narrowing upper response, restrained critical return. Breath-like noise swells and dissonant low strings leave actual alerts distinct."},
 "alice":{"title":"A Choice Freely Made","bpm":96,"bars":40,"peak":-8,
    "chords":[(50,57,60,64),(48,55,60,65),(46,53,57,62),(53,57,60,65),
              (43,50,57,62),(48,55,60,64),(45,52,57,62),(45,52,55,61)],
    "melody":[[(0,74),(1.5,81),(3,84)],[(0,88),(2,86)],[(.5,86),(2,84),(3,81)],
              [(0,89),(2,88)],[(0,86),(1.5,81),(3,79)],[(.5,84),(2,88)],
              [(0,86),(1.5,85),(3,81)],[(0,85),(2,81)]],
    "form":"Five eight-bar paragraphs: ethical question, gentle answer, lower-register reflection, expanded finale development, quiet chosen return. Expressive synthesized reed/glass over strings; no voice samples."},
 "ending":{"title":"Toward the Red Dawn","bpm":96,"bars":32,"peak":-8,
    "chords":[(41,53,57,60),(48,55,60,64),(46,53,57,62),(53,57,60,65),
              (43,50,55,62),(48,55,60,64),(50,57,60,65),(48,55,60,64)],
    "melody":[[(0,77),(1.5,81),(3,84)],[(0,88),(2,86)],[(0,86),(2,84)],
              [(0,89),(2.5,88)],[(0,86),(1.5,84),(3,79)],[(.5,84),(2,88)],
              [(0,86),(1.5,84),(3,81)],[(0,79),(2,76)]],
    "form":"Four eight-bar epilogue sections: open relative-major horizon, answering counterline, fuller resolution, quiet credits return. Related interval identity resolves without a generic victory fanfare."}
}
def instrument(note,seconds,kind="glass"):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR;f=hz(note);p=TAU*f*t
    if kind=="strings":
        x=(.60*np.sin(p+.05*np.sin(TAU*4.7*t))+.20*np.sin(p*1.002)+
           .10*np.sin(p*2)+.05*np.sin(p*3))*env(len(t),.30,min(.8,seconds*.4))
    elif kind=="reed":
        x=(.72*np.sin(p)+.16*np.sin(p*3)+.06*np.sin(p*5))*np.exp(-t*.6)*env(len(t),.055,min(.4,seconds*.35))
    elif kind=="pulse":
        x=sum(np.sin(p*k)/k for k in (1,3,5,7))*.52*np.exp(-t*3)*env(len(t),.006,min(.08,seconds*.3))
    else:
        x=(.62*np.sin(p)+.21*np.sin(p*2.006)+.07*np.sin(p*3.97))*np.exp(-t*1.3)*env(len(t),.015,min(.55,seconds*.4))
    return x.astype(np.float32)
def colored_noise(seconds,rng,width=9):
    n=round(seconds*SR)
    return np.convolve(rng.normal(0,1,n),np.ones(width)/width,mode="same").astype(np.float32)
def percussion(kind,rng):
    seconds={"kick":.32,"snare":.24,"hat":.11,"tick":.16}[kind]
    n=round(seconds*SR);t=np.arange(n,dtype=np.float32)/SR
    if kind=="kick":x=np.sin(TAU*(40*t+4*(1-np.exp(-t*18))))*np.exp(-t*14)
    elif kind=="snare":x=(colored_noise(seconds,rng,5)*.8+np.sin(TAU*168*t)*.2)*np.exp(-t*23)
    elif kind=="hat":
        noise=rng.normal(0,1,n);x=(noise-np.convolve(noise,np.ones(9)/9,mode="same"))*.4*np.exp(-t*70)
    else:x=(np.sin(TAU*830*t)*.6+np.sin(TAU*1360*t)*.3)*np.exp(-t*38)
    return (x*env(n,.002,.035)).astype(np.float32)
def scored_cue(name):
    score=SCORES[name];beat=60/score["bpm"];bar=4*beat
    music=np.zeros((round(score["bars"]*bar*SR),2),np.float32)
    rng=np.random.default_rng(211500+SCORE_ORDER.index(name))
    for b in range(score["bars"]):
        section,k=b//8,b%8;start=b*bar;chord=score["chords"][k]
        pad_gain={"menu":.073,"combat":.045,"danger":.046,"alice":.077,"ending":.080}[name]
        if name=="alice" and section==3:pad_gain*=1.25
        if name=="combat" and section==3:pad_gain*=.72
        for j,note in enumerate(chord):
            add(music,instrument(note,bar+.9,"strings"),start,pad_gain,(-.65,-.2,.2,.65)[j])
        if name!="danger":
            add(music,tone(chord[0]-12,beat*1.8,"bass"),start,.11)
        if name=="combat":
            density=4 if section==3 else 8
            for j in range(density):
                offset=j*(4/density)
                note=chord[0]-12+(12 if j%3==2 else 0)
                add(music,instrument(note,beat*.43,"pulse"),start+offset*beat,.12)
            for offset in ((0,2) if section==3 else (0,1.5,2.75)):
                add(music,percussion("kick",rng),start+offset*beat,.075)
            if section!=3:
                for offset in (1,3):add(music,percussion("snare",rng),start+offset*beat,.045,.1)
                for offset in (.5,1.5,2.5,3.5):add(music,percussion("hat",rng),start+offset*beat,.022,-.3 if b%2 else .3)
        elif name=="danger":
            for offset,gain in ((0,.11),(.38,.072),(2,.09),(2.38,.06)):
                add(music,percussion("kick",rng),start+offset*beat,gain)
            if b%2==0:
                air=colored_noise(bar*.9,rng,29)*env(round(bar*.9*SR),.65,.8)
                add(music,air,start,.055,-.25 if b%4 else .25)
        elif name=="menu" and section==1:
            for offset,j in ((.75,1),(2.25,2),(3.5,3)):
                add(music,instrument(chord[j]+12,.7),start+offset*beat,.05,-.5 if j%2 else .5)
        elif name=="alice" and section in (1,3):
            for offset,j in ((1,1),(2.5,2)):
                add(music,instrument(chord[j]+12,beat*1.2,"reed"),start+offset*beat,.055,-.35)
            if section==3 and b%2==0:add(music,percussion("kick",rng),start,.035)
        elif name=="ending" and section in (1,2):
            for j,offset in enumerate((.5,1.5,2.5,3.5)):
                add(music,instrument(chord[j]+12,.9),start+offset*beat,.05,-.4 if j%2 else .4)
        play=(name!="combat" or section!=3) and (name!="danger" or section!=0 or k%2==0)
        if play:
            for offset,note in score["melody"][k]:
                trans=0
                if name=="menu" and section==2:trans=-12
                if name=="combat" and section==4:trans=12
                if name=="alice" and section in (2,4):trans=-12
                if name=="ending" and section==3:trans=-12
                kind="reed" if name in ("combat","alice") else "glass"
                dur=beat*(.75 if name=="combat" else 1.8)
                gain={"menu":.11,"combat":.105,"danger":.055,"alice":.12,"ending":.115}[name]
                add(music,instrument(note+trans,dur,kind),start+offset*beat,gain,-.25 if k%2 else .25)
                if name=="alice" and section==3:
                    add(music,instrument(note+12,dur,"glass"),start+offset*beat,.035,.4)
        # Finale/epilogue counterline develops the shared D-A-C-E interval family.
        if name in ("alice","ending") and section==2 and k%2==1:
            for offset,note in ((.75,69),(2.5,72)):
                add(music,instrument(note,beat*2,"reed"),start+offset*beat,.055,-.45)
    dry=music.copy()
    for delay,gain in ((beat*.5,.18),(beat,.10),(beat*1.5,.055),(beat*3,.025)):
        music+=np.roll(dry[:,::-1],round(delay*SR),axis=0)*gain
    return normalized(np.tanh(music*1.08),score["peak"])
def mono_add(out,voice,start,gain=1):
    i=round(start*SR);n=min(len(voice),len(out)-i)
    if n>0:out[i:i+n]+=voice[:n]*gain
def additional_effects():
    rng=np.random.default_rng(21152026);e={}
    def blank(seconds):return np.zeros(round(seconds*SR),np.float32)
    x=blank(.48)
    rush=colored_noise(.28,rng,7)*env(round(.28*SR),.015,.09)
    mono_add(x,rush,0,.7);mono_add(x,percussion("snare",rng),.14,.7)
    e["melee"]=x
    x=blank(2.2)
    mono_add(x,percussion("kick",rng),0,.35)
    for when,note in ((0,69),(.65,72),(1.15,74),(1.55,76),(1.85,81)):
        mono_add(x,instrument(note,.18,"pulse"),when,.60)
        mono_add(x,percussion("tick",rng),when,.18)
    # A real armed ROVER: mechanical transients + accelerating pitched countdown.
    e["rover"]=x
    n=round(1.8*SR);t=np.arange(n,dtype=np.float32)/SR
    e["explosion"]=(colored_noise(1.8,rng,17)*np.exp(-t*3)*1.3+
                   sweep(80,23,1.8)*np.exp(-t*4)*.7)*env(n,.002,.42)
    x=blank(2.8)
    for when,note in ((0,74),(.45,72),(.90,69),(1.35,62)):
        mono_add(x,instrument(note,1.4,"reed"),when,.4)
    e["death"]=x*env(len(x),.01,.5)
    x=blank(3.0)
    for when,note in ((0,65),(.3,69),(.6,72),(.9,74),(1.3,77)):
        mono_add(x,instrument(note,1.6,"glass"),when,.3)
    e["victory"]=x
    x=blank(1.05)
    for when,note in ((0,76),(.12,81),(.24,84),(.48,88),(.64,93)):
        mono_add(x,instrument(note,.35,"pulse"),when,.35)
    e["hack"]=x
    n=round(2.4*SR);t=np.arange(n,dtype=np.float32)/SR
    shape=(np.sin(np.pi*np.minimum(t/1.0,1))**2)*(t<1)
    shape+=.75*(np.sin(np.pi*np.clip((t-1.15)/1.25,0,1))**2)
    e["breath"]=colored_noise(2.4,rng,35)*shape*env(n,.02,.25)
    x=blank(.90)
    for when,gain in ((0,.75),(.21,.43)):
        n=round(.32*SR);t=np.arange(n,dtype=np.float32)/SR
        mono_add(x,np.sin(TAU*64*t)*np.exp(-t*18)*env(n,.008,.08),when,gain)
    e["pulse"]=x
    n=round(1.9*SR);t=np.arange(n,dtype=np.float32)/SR
    # Airy detuned glass wash, without the true ROVER's ticks/countdown cadence.
    e["hallucination"]=(sweep(1180,510,1.9)*.28+np.sin(TAU*(1213*t+5*t*t))*.14+
        colored_noise(1.9,rng,19)*.35)*env(n,.45,.65)
    x=blank(.30)
    mono_add(x,instrument(70,.16,"pulse"),0,.5)
    mono_add(x,instrument(64,.18,"pulse"),.10,.5)
    e["error"]=x
    x=blank(.42)
    mono_add(x,instrument(74,.28,"glass"),0,.5)
    mono_add(x,instrument(81,.30,"glass"),.10,.4)
    e["confirm"]=x
    x=blank(.58)
    mono_add(x,instrument(88,.30,"glass"),0,.32)
    mono_add(x,instrument(79,.36,"glass"),.14,.45)
    mono_add(x,percussion("tick",rng),.29,.20)
    e["drop"]=x
    return e

def sweep(f0,f1,seconds):
    t=np.arange(round(seconds*SR),dtype=np.float32)/SR
    return np.sin(TAU*(f0*t+(f1-f0)*t*t/(2*seconds)))
def hiss(seconds,decay=8):
    n=round(seconds*SR);t=np.arange(n,dtype=np.float32)/SR
    return np.convolve(RNG.normal(0,1,n),np.ones(5)/5,mode="same")*np.exp(-t*decay)
def sound_library(destination=None):
    destination=destination or OUT/"sfx"
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
    e.update(additional_effects())
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
        "medicine":"Medicine identification confirmation.",
        "melee":"Melee swing followed by a short contact body.",
        "rover":"Real armed ROVER: mechanical movement and accelerating pitched countdown; play on armed threat, not every render or hallucination.",
        "explosion":"Broadband robotic explosion with low pressure tail.",
        "death":"Descending broken signal on actual player death.",
        "victory":"Authored warm resolution sting for a legitimately completed objective/ending.",
        "hack":"Ascending data-lock sequence for an accepted successful hack.",
        "breath":"Synthesized inhale/exhale air texture for oxygen strain; no voice samples.",
        "pulse":"Paired subdued heartbeat for physiological strain.",
        "hallucination":"Airy detuned phantom glass wash; deliberately no mechanical countdown or impact ticks.",
        "error":"Short descending refusal/error, distinct from successful confirmation.",
        "confirm":"Two-note affirmative menu/dialogue action.",
        "drop":"Descending two-clink object release."}
    manifest={}
    peaks={"gun":-9,"impact":-12,"hatch":-12,"warning":-17,"alarm":-17,"ui":-22,"pickup":-18,"medicine":-18,"terminal":-19,
        "melee":-12,"rover":-14,"explosion":-8,"death":-17,"victory":-14,"hack":-18,"breath":-23,"pulse":-22,"hallucination":-23,"error":-20,"confirm":-21,"drop":-17}
    for name,x in e.items():
        x=normalized(x,peaks.get(name,-16));write_wav(destination/(name+".wav"),x)
        manifest[name]={**metrics(x),"channels":1,"sample_rate":SR,
            "path":"assets/audio/sfx/"+name+".wav","purpose":purposes[name]}
    return manifest
M1_SFX=("alarm","door","gun","hatch","impact","lock","medicine","oxygen_refill",
        "pickup","robot_step","step","step_lowgrav","switch","terminal","ui","warning")
EXPLORATION_SHA256="b232b9d46ec7b98dd43333812068416dfab54cb8376f5eda3ad9655379b1c80e"
MUSIC_ORDER=("exploration",)+SCORE_ORDER
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def float_digest(x):return hashlib.sha256(x.astype("<f4").tobytes()).hexdigest()
def encode_music(name,music,path):
    wav=SCRATCH/"audio-render.wav";write_wav(wav,music)
    ffmpeg=shutil.which("ffmpeg")
    if not ffmpeg:raise RuntimeError("ffmpeg required for OGG/Vorbis export")
    title="Signal Through Steel" if name=="exploration" else SCORES[name]["title"]
    comment=("32 bars; 80 BPM; cyclic tails; no external samples" if name=="exploration"
             else f'{SCORES[name]["bars"]} bars; {SCORES[name]["bpm"]} BPM; cyclic tails; no external samples')
    subprocess.run([ffmpeg,"-hide_banner","-loglevel","error","-y","-i",str(wav),
        "-c:a","libvorbis","-q:a","5","-fflags","+bitexact","-flags:a","+bitexact",
        "-metadata","title="+title,"-metadata","artist=Origin War original synthesis",
        "-metadata","comment="+comment,str(path)],check=True)
def main():
    global RNG
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verify-repeatability",action="store_true",
        help="Recompose and encode every cue/effect twice; compare source and final hashes.")
    args=parser.parse_args()
    SCRATCH.mkdir(parents=True,exist_ok=True);(OUT/"music").mkdir(parents=True,exist_ok=True)
    (OUT/"sfx").mkdir(parents=True,exist_ok=True)
    RNG=np.random.default_rng(2115)
    music_manifest={};candidate=SCRATCH/"candidate.ogg"
    for name in MUSIC_ORDER:
        music=exploration() if name=="exploration" else scored_cue(name)
        encode_music(name,music,candidate);target=OUT/"music"/(name+".ogg")
        rendered_hash=digest(candidate)
        if name=="exploration":
            if rendered_hash!=EXPLORATION_SHA256:
                raise RuntimeError("Exploration source/encoder reproduction changed; preserved runtime file is untouched.")
            if target.exists() and digest(target)!=EXPLORATION_SHA256:
                raise RuntimeError("Existing exploration differs from the preserved M1 hash.")
        if not target.exists() or (name!="exploration" and digest(target)!=rendered_hash):
            shutil.copyfile(candidate,target)
        score=SCORES.get(name,{})
        music_manifest[name]={"title":score.get("title","Signal Through Steel"),
            "path":"assets/audio/music/"+name+".ogg","bpm":score.get("bpm",80),
            "bars":score.get("bars",32),"beats_per_bar":4,"channels":2,"sample_rate":SR,
            "loop":True,"loop_start_seconds":0,"loop_end_seconds":len(music)/SR,
            "arrangement":score.get("form","Four eight-bar orchestrations: sparse beacon, harmonic clock, restrained industrial pulse, lower-register return."),
            **metrics(music),"boundary_sample_delta":round(float(np.max(np.abs(music[0]-music[-1]))),7),
            "source_float_sha256":float_digest(music),"sha256":rendered_hash,
            "acceptance":"Authored/export-validated only; actual playback/loop/mix and artistic listening remain root acceptance."}
        print("Rendered",name,len(music)/SR,rendered_hash[:12],flush=True)
    candidate_sfx=SCRATCH/"sfx-candidates";fx=sound_library(candidate_sfx)
    preserved={}
    for name,entry in fx.items():
        source=candidate_sfx/(name+".wav");target=OUT/"sfx"/(name+".wav");h=digest(source)
        if name in M1_SFX:
            if target.exists() and digest(target)!=h:
                raise RuntimeError("Original M1 effect changed: "+name+"; runtime file was not overwritten.")
            preserved[name]=h
        if not target.exists() or (name not in M1_SFX and digest(target)!=h):shutil.copyfile(source,target)
        entry["sha256"]=h
    repeat={"performed":False,"scope":"Run with --verify-repeatability to recompose/encode all six scores and 28 effects."}
    if args.verify_repeatability:
        RNG=np.random.default_rng(2115)
        for name in MUSIC_ORDER:
            music=exploration() if name=="exploration" else scored_cue(name)
            assert float_digest(music)==music_manifest[name]["source_float_sha256"],name+" source samples differ"
            encode_music(name,music,candidate)
            assert digest(candidate)==music_manifest[name]["sha256"],name+" OGG bytes differ"
        repeated=sound_library(candidate_sfx)
        for name in repeated:assert digest(candidate_sfx/(name+".wav"))==fx[name]["sha256"],name+" WAV bytes differ"
        repeat={"performed":True,"result":"PASS","music_sources":6,"ogg_exports":6,"wav_exports":len(fx),
                "comparison":"Independent second synthesis/bitexact encoding in the same pinned environment; every source and final-file SHA256 matched."}
        print("Independent repeatability PASS: 6 compositions/OGG exports and",len(fx),"WAV exports",flush=True)
    manifest={"task":"M5-AUDIO","status":"six authored score roles and 28 effects; audible/runtime acceptance open",
        "source":"tools/media/compose_audio.py","seed":2115,
        "provenance":"Original oscillator/noise synthesis, editable note/beat/harmony/arrangement tables. No legacy/external recordings, samples, soundfonts or generative-audio service.",
        "format":{"music":"OGG/Vorbis quality5, 44.1kHz stereo","effects":"PCM16 WAV, 44.1kHz mono"},
        "identity":"D-A-C-E signal interval family; relative-major epilogue. Each role has its own harmony/form/orchestration and independent source RNG.",
        "music":music_manifest,"sfx":fx,
        "preservation":{"exploration_sha256":EXPLORATION_SHA256,"existing_m1_sfx_sha256":preserved},
        "repeatability":repeat,
        "recommended_mix":{"units":"proposed linear relative gains within the corresponding user-controlled bus; not applied by this task",
            "music_role_gains":{"menu":.65,"exploration":.48,"combat":.42,"danger":.50,"alice":.58,"ending":.60},
            "sfx_groups":{"human_steps":.55,"mechanisms":.70,"gun_melee_impacts":.80,"rover_countdown":.80,"oxygen_alerts":.72,"ui":.55,"breath_pulse_phantom":.45},
            "contrast":"Crossfade music roles rather than stack complete scores. Danger supplies low paired pulse/air tension; true ROVER uses mechanical accelerating ticks, phantom uses airy glass with no countdown. Dialogue/critical cues should remain intelligible.",
            "mix_acceptance":"Starting proposals only; root must check user sliders, simultaneous cues, backend voices, loop transitions and actual listening."},
        "limitations":["Six soundtrack roles and 28 purposeful effects are authored exports, not audible or final-game acceptance.",
            "ROVER and hallucination are deliberately distinguishable source cues; logical event mapping and truth/perception scheduling remain lead-owned.",
            "A.L.I.C.E. cue includes both character/ethical-question and fuller finale development; scene timing/crossfades require actual campaign integration.",
            "Directional/attack/death art and sector dressing are outside this audio task."]}
    (OUT/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"music_seconds":{n:e["duration_seconds"] for n,e in music_manifest.items()},
                      "sfx_count":len(fx),"repeatability":repeat},indent=2))
if __name__=="__main__":main()
