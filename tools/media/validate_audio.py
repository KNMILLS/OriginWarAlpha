"""Audio-only export validation; waveform metrics never imply audible review."""
from __future__ import annotations
import hashlib,json,math,re,subprocess,wave
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2]
AUDIO=ROOT/"assets"/"audio"
REQUIRED=("menu","exploration","combat","danger","alice","ending")
NEW_SFX=("melee","rover","explosion","death","victory","hack","breath","pulse","hallucination","error","confirm","drop")
EXPLORATION_SHA="b232b9d46ec7b98dd43333812068416dfab54cb8376f5eda3ad9655379b1c80e"
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def db(value):return round(20*math.log10(max(1e-12,float(value))),3)
def loudness(path):
    p=subprocess.run(["ffmpeg","-nostdin","-hide_banner","-i",str(path),
        "-af","loudnorm=I=-20:TP=-2:LRA=11:print_format=json","-f","null","-"],
        capture_output=True,text=True,check=True)
    blocks=re.findall(r"\{\s*\"input_i\".*?\}",p.stderr,re.S)
    assert blocks,"Missing loudness analysis"
    data=json.loads(blocks[-1])
    return {"integrated_lufs":float(data["input_i"]),"true_peak_dbtp":float(data["input_tp"]),
            "loudness_range_lu":float(data["input_lra"])}
def decode(path):
    p=subprocess.run(["ffmpeg","-nostdin","-hide_banner","-loglevel","error",
        "-i",str(path),"-f","f32le","-acodec","pcm_f32le","-"],
        capture_output=True,check=True)
    return np.frombuffer(p.stdout,dtype="<f4").reshape(-1,2)
def main():
    m=json.loads((AUDIO/"manifest.json").read_text())
    assert set(m["music"])==set(REQUIRED)
    assert len(m["sfx"])>=28 and set(NEW_SFX)<=set(m["sfx"])
    assert m["repeatability"]["performed"] and m["repeatability"]["result"]=="PASS"
    report={"scope":"headers, full decoding, duration, channel count, sample/true peaks, loop boundary metrics and source/export repeatability; no audible acceptance",
        "source_sha256":sha(ROOT/"tools"/"media"/"compose_audio.py"),
        "repeatability":m["repeatability"],"music":{},"sfx":{}}
    for name in REQUIRED:
        entry=m["music"][name];path=ROOT/entry["path"]
        probe=subprocess.run(["ffprobe","-v","error","-select_streams","a:0",
            "-show_entries","stream=codec_name,sample_rate,channels","-of","json",str(path)],
            capture_output=True,text=True,check=True)
        stream=json.loads(probe.stdout)["streams"][0]
        assert stream["codec_name"]=="vorbis" and stream["sample_rate"]=="44100" and stream["channels"]==2
        x=decode(path);seconds=len(x)/44100
        assert seconds>= (90 if name in ("exploration","combat","alice") else 60)
        assert abs(seconds-entry["duration_seconds"])<.0001
        assert np.isfinite(x).all() and np.max(np.abs(x))<.99
        assert sha(path)==entry["sha256"]
        steps=np.abs(np.diff(x,axis=0))
        p99=float(np.quantile(steps,.99));delta=float(np.max(np.abs(x[0]-x[-1])))
        edge=882
        first=db(np.sqrt(np.mean(x[:edge]*x[:edge])))
        last=db(np.sqrt(np.mean(x[-edge:]*x[-edge:])))
        level=loudness(path)
        assert level["true_peak_dbtp"]<0
        ratio=delta/max(p99,1e-12)
        report["music"][name]={"sha256":sha(path),"decoded_seconds":seconds,
            "sample_rate":44100,"channels":2,"decoded_peak_dbfs":db(np.max(np.abs(x))),
            "decoded_rms_dbfs":db(np.sqrt(np.mean(x*x))),**level,
            "dc_offset_max":round(float(np.max(np.abs(np.mean(x,axis=0)))),8),
            "loop_metrics":{"first_to_last_sample_delta":round(delta,8),
                "adjacent_sample_delta_p99":round(p99,8),"boundary_to_p99_step_ratio":round(ratio,3),
                "first_20ms_rms_dbfs":first,"last_20ms_rms_dbfs":last,
                "edge_rms_difference_db":round(abs(first-last),3),
                "flag_for_focused_listening":bool(ratio>3 or abs(first-last)>8),
                "interpretation":"Diagnostics for listening priority; neither a seamless-loop claim nor an audible pass."}}
    previous=json.loads((ROOT/"tools"/"media"/"validation.json").read_text())["sfx"]
    for name,entry in m["sfx"].items():
        path=ROOT/entry["path"]
        with wave.open(str(path),"rb") as w:
            assert w.getnchannels()==1 and w.getsampwidth()==2 and w.getframerate()==44100
            x=np.frombuffer(w.readframes(w.getnframes()),dtype="<i2").astype(np.float32)/32768
        assert len(x)>0 and np.isfinite(x).all() and np.max(np.abs(x))<.99
        assert abs(len(x)/44100-entry["duration_seconds"])<.0001
        h=sha(path);assert h==entry["sha256"]
        if name in previous:assert h==previous[name]["sha256"],"M1 byte preservation failed: "+name
        report["sfx"][name]={"sha256":h,"duration_seconds":len(x)/44100,
            "sample_rate":44100,"channels":1,"bits":16,"peak_dbfs":db(np.max(np.abs(x))),
            "rms_dbfs":db(np.sqrt(np.mean(x*x))),"preserved_m1_bytes":name in previous}
    assert report["music"]["exploration"]["sha256"]==EXPLORATION_SHA
    assert len({e["sha256"] for e in report["music"].values()})==6
    assert len({e["sha256"] for e in report["sfx"].values()})==len(m["sfx"])
    report["result"]="PASS export validation; actual listening/runtime acceptance remains open"
    (AUDIO/"validation.json").write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps({"music":{name:{"seconds":e["decoded_seconds"],"lufs":e["integrated_lufs"],
        "true_peak_dbtp":e["true_peak_dbtp"],"loop_review_flag":e["loop_metrics"]["flag_for_focused_listening"]}
        for name,e in report["music"].items()},"sfx_count":len(report["sfx"]),
        "preserved_m1_effects":sum(e["preserved_m1_bytes"] for e in report["sfx"].values()),
        "exploration_hash_preserved":True,"repeatability":report["repeatability"],"result":report["result"]},indent=2))
if __name__=="__main__":
    from validate_helmet_audio import main as helmet_validation
    helmet_validation()
