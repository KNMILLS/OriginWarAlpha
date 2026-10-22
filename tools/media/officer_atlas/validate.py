"""Reconstruct trimmed source canvases and verify pixels, anchors and budget."""
from pathlib import Path
import hashlib
import json
from PIL import Image
from pack import ASSETS, FOLDER, MAX_PAGES, PAGE, source, sha


def validate(folder=FOLDER, kind="officer"):
    data=json.loads((folder/"atlas.json").read_text(encoding="utf-8"))
    assert sha(source(data["sourceManifest"]["path"])) == data["sourceManifest"]["sha256"]
    if "sourceRegionsManifest" in data:
        assert sha(source(data["sourceRegionsManifest"]["path"])) == data["sourceRegionsManifest"]["sha256"]
    assert len(data["atlasPages"]) <= MAX_PAGES
    pages={}
    for entry in data["atlasPages"]:
        path=source(entry["path"])
        assert sha(path)==entry["sha256"]
        pages[entry["path"]]=Image.open(path).convert("RGBA")
    ids=set()
    frames=list(data.get("regions", []))
    for clip in data["clips"]:
        assert clip["id"] not in ids
        ids.add(clip["id"])
        span=sum(f.get("seconds",clip.get("frameSeconds",1/24))*f.get("hold",1) for f in clip["frames"])
        assert span>0
        for marker in clip.get("markers",[]):
            at=marker.get("seconds",sum(f.get("seconds",clip.get("frameSeconds",1/24))*f.get("hold",1) for f in clip["frames"][:marker.get("frame",0)]))
            assert 0<=at<=span+1e-5
        frames.extend(clip["frames"])
    for frame in frames:
            path=source(frame["sourcePath"])
            assert sha(path)==frame["sourceSha256"]
            original=Image.open(path).convert("RGBA")
            left,top,right,bottom=frame["trim"]
            x,y,w,h=(frame[k] for k in ("x","y","pixelWidth","pixelHeight"))
            crop=pages[frame["path"]].crop((x,y,x+w,y+h))
            assert crop.tobytes()==original.crop((left,top,right,bottom)).tobytes(),path
            # Preserve visible RGBA exactly; hidden RGB outside alpha is immaterial.
            reconstructed=Image.new("RGBA",original.size)
            reconstructed.paste(crop,(left,top))
            assert reconstructed.getchannel("A").tobytes()==original.getchannel("A").tobytes(),path
            assert abs((frame["pivotX"]*w+left)/original.width-frame["sourcePivotX"])<1e-8
            assert abs((frame["pivotY"]*h+original.height-bottom)/original.height-frame["sourcePivotY"])<1e-8
            assert 0<=frame["pivotX"]<=1 and 0<=frame["pivotY"]<=1
    keys=set()
    for binding in data["bindings"]:
        assert binding["clip"] in ids
        key=tuple(binding[k] for k in (("weapon","armor","action","facing") if kind=="officer" else ("actor","action","facing")))
        assert key not in keys,key
        keys.add(key)
    assert data["memoryBudget"]["actualRgbaBytes"]==len(pages)*PAGE*PAGE*4
    print(json.dumps(dict(status="source-pixels-anchors-and-budget-verified-artistic-acceptance-open",clips=len(ids),bindings=len(keys),pages=len(pages)),indent=2))


if __name__=="__main__":
    validate()
