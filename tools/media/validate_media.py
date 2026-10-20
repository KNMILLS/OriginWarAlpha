"""Validate exported media bytes/headers/decoding. Artistic acceptance is separate."""
from __future__ import annotations
import hashlib,json,math,subprocess,wave
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    a=ROOT/"assets"/"audio";m=json.loads((a/"manifest.json").read_text())
    report={"scope":"file/header/decoded-level/loop-seam checks; no audible acceptance","sfx":{}}
    for event,entry in m["sfx"].items():
        p=ROOT/entry["path"]
        with wave.open(str(p),"rb") as w:
            assert w.getsampwidth()==2 and w.getframerate()==44100 and w.getnchannels()==1
            x=np.frombuffer(w.readframes(w.getnframes()),dtype="<i2").astype(np.float32)/32768
        assert np.isfinite(x).all() and np.max(np.abs(x))<.99
        assert abs(len(x)/44100-entry["duration_seconds"])<.0001
        report["sfx"][event]={"sha256":sha(p),"samples":len(x),"peak_dbfs":round(20*math.log10(max(1e-12,float(np.max(np.abs(x))))),2)}
    p=a/"music"/"exploration.ogg"
    r=subprocess.run(["ffmpeg","-hide_banner","-loglevel","error","-i",str(p),"-f","f32le","-acodec","pcm_f32le","-"],capture_output=True,check=True)
    x=np.frombuffer(r.stdout,dtype="<f4").reshape(-1,2)
    assert len(x)==96*44100 and np.isfinite(x).all() and np.max(np.abs(x))<.99
    report["music"]={"sha256":sha(p),"decoded_seconds":len(x)/44100,"decoded_peak_dbfs":round(20*math.log10(float(np.max(np.abs(x)))),2),
        "decoded_rms_dbfs":round(20*math.log10(float(np.sqrt(np.mean(x*x)))),2),"boundary_delta":round(float(np.max(np.abs(x[0]-x[-1]))),7)}
    atlas=ROOT/"assets"/"art"/"station.png"
    if atlas.exists():
        im=Image.open(atlas);assert im.size==(1024,1024) and im.mode=="RGBA"
        data=np.asarray(im)
        assert data[896:1024,896:1024,3].max()==0
        occupied=[bool(data[(i//8)*128:(i//8+1)*128,(i%8)*128:(i%8+1)*128,3].max()) for i in range(64)]
        assert all(occupied[:63]) and not occupied[63]
        for i in list(range(5,11))+list(range(14,56)):
            a=data[(i//8)*128:(i//8+1)*128,(i%8)*128:(i%8+1)*128,3]
            assert not np.any((a>0)&(a<32))
        native=np.asarray(Image.open(ROOT/"assets"/"art"/"source"/"station-native.png"))
        assert np.array_equal(data,np.repeat(np.repeat(native,4,axis=0),4,axis=1))
        for first in (32,36,40,44,48):
            frames=[native[(i//8)*32:(i//8+1)*32,(i%8)*32:(i%8+1)*32] for i in range(first,first+4)]
            assert all(not np.array_equal(frames[j],frames[(j+1)%4]) for j in range(4))
        report["atlas"]={"sha256":sha(atlas),"size":im.size,"slots":64,"occupied_slots":sum(occupied),"transparent_blank_slot":True,
            "isolated_alpha_cutoff":32,"native_nearest_neighbor_scale":4,"distinct_walk_frames_per_group":4}
    (Path(__file__).parent/"validation.json").write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps({k:v for k,v in report.items() if k!="sfx"},indent=2))
    print("Validated SFX:",len(report["sfx"]))
if __name__=="__main__": main()
