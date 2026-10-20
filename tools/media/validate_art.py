"""Static art-format/packing checks; does not claim running-game acceptance."""
from __future__ import annotations
import argparse,hashlib,json,subprocess,sys
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/"assets"/"art"
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def cell(x,i,columns):
    return x[(i//columns)*32:(i//columns+1)*32,(i%columns)*32:(i%columns+1)*32]
def inspect(base,name,rows):
    native=base/(name+"-native.png");atlas=base/(name+".png")
    n=Image.open(native);a=Image.open(atlas)
    assert n.mode==a.mode=="RGBA" and n.size==(256,rows*32) and a.size==(1024,rows*128)
    x=np.asarray(n);y=np.asarray(a)
    assert np.array_equal(y,np.repeat(np.repeat(x,4,axis=0),4,axis=1))
    assert all(cell(x,i,8)[:,:,3].max()>0 for i in range(rows*8))
    return x,{"native_sha256":sha(native),"atlas_sha256":sha(atlas),"columns":8,"rows":rows,
              "native_cell_pixels":32,"nearest_scale":4,"occupied_slots":rows*8}
def main():
    p=argparse.ArgumentParser();p.add_argument("--verify-repeatability",action="store_true");args=p.parse_args()
    keep=[ART/"animation"/"actors.png",ART/"animation"/"actors-native.png",
          ART/"animation"/"manifest.json",ART/"sectors"/"sectors.png",ART/"sectors"/"sectors-native.png",ART/"sectors"/"manifest.json",
          ART/"finale"/"finale.png",ART/"finale"/"finale-native.png",ART/"finale"/"manifest.json"]
    before={str(q.relative_to(ROOT)):sha(q) for q in keep}
    if args.verify_repeatability:
        subprocess.run([sys.executable,str(ROOT/"tools"/"media"/"pack_animation.py")],check=True,capture_output=True)
        assert before=={str(q.relative_to(ROOT)):sha(q) for q in keep},"Packing bytes changed on an independent rerun"
    x,actors=inspect(ART/"animation","actors",8)
    actors["frame_pairs"]=[]
    for first in range(0,64,2):
        a,b=cell(x,first,8),cell(x,first+1,8)
        changed=int(np.count_nonzero(np.any(a!=b,axis=2)))
        assert changed>0,"Identical animation pair at"+str(first)
        for i,z in ((first,a),(first+1,b)):
            alpha=z[:,:,3]
            assert np.any(alpha==0),"Actor cell lost transparency"
            if i not in (54,55):assert not np.any((alpha>0)&(alpha<32))
        actors["frame_pairs"].append({"slots":[first,first+1],"changed_native_pixels":changed,
                                      "manual_review_priority":changed<10})
    s,sectors=inspect(ART/"sectors","sectors",4)
    sectors["floor_edge_metrics"]=[]
    for i in range(5):
        z=cell(s,i,8);assert np.all(z[:,:,3]==255)
        sectors["floor_edge_metrics"].append({"slot":i,
            "left_right_rgb_mean_absolute_difference":round(float(np.abs(z[:,0,:3].astype(float)-z[:,-1,:3]).mean()),3),
            "top_bottom_rgb_mean_absolute_difference":round(float(np.abs(z[0,:,:3].astype(float)-z[-1,:,:3]).mean()),3),
            "interpretation":"Texture-repetition diagnostic, not an in-game seam acceptance."})
    assert len({sha_bytes(cell(s,i,8)) for i in range(5)})==5
    for i in range(10,32):
        alpha=cell(s,i,8)[:,:,3]
        assert np.any(alpha==0) and not np.any((alpha>0)&(alpha<32))
    for base,report in ((ART/"animation",actors),(ART/"sectors",sectors)):
        manifest=json.loads((base/"manifest.json").read_text())
        for source in manifest["source_rasters"]:assert sha(ROOT/source["path"])==source["sha256"]
        assert sha(ART/"station.png")==manifest["reference_station_sha256"]
        report.update({"source_hashes_verified":True,"reference_station_unchanged":True,
                       "packing_repeatability":"PASS" if args.verify_repeatability else "NOT_RUN",
                       "scope":"Static source/export/cell/alpha/frame-difference verification only; not anatomy proof, actual animation timing or gameplay acceptance."})
        (base/"validation.json").write_text(json.dumps(report,indent=2)+"\n")
    final=ART/"finale";f=Image.open(final/"finale.png");n=Image.open(final/"finale-native.png")
    assert f.mode==n.mode=="RGB" and f.size==(1024,512) and n.size==(512,256)
    assert np.array_equal(np.asarray(f),np.repeat(np.repeat(np.asarray(n),2,axis=0),2,axis=1))
    fm=json.loads((final/"manifest.json").read_text());assert sha(ROOT/fm["source"]["path"])==fm["source"]["sha256"]
    finale={"size":f.size,"aspect":"2:1","opaque":True,"nearest_scale":2,"atlas_sha256":sha(final/"finale.png"),
            "packing_repeatability":"PASS" if args.verify_repeatability else "NOT_RUN","scope":"Static source/export only; no in-game finale acceptance."}
    (final/"validation.json").write_text(json.dumps(finale,indent=2)+"\n")
    print(json.dumps({"finale":finale,"actors":{k:v for k,v in actors.items() if k!="frame_pairs"},
        "frame_pairs":len(actors["frame_pairs"]),"minimum_pair_changed_pixels":min(e["changed_native_pixels"] for e in actors["frame_pairs"]),
        "sectors":sectors},indent=2))
def sha_bytes(x):return hashlib.sha256(x.tobytes()).hexdigest()
if __name__=="__main__":main()
