"""Check proof atlas fidelity, bindings and exposure/marker contracts.

This verifies export integrity and declares coverage; it never certifies art quality.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "assets"
PROOF = ASSETS / "art/proof"

def load(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def checked(path):
    item = (ASSETS / path).resolve()
    assert item.is_relative_to(ASSETS.resolve()) and item.is_file(), path
    return item

def validate():
    catalog = load(PROOF / "catalog.json")
    report = {"status": "technical-only-owner-artistic-selection-open", "packs": []}
    for entry in catalog["packs"]:
        manifest = checked(entry["manifest"])
        assert sha(manifest) == entry["sha256"]
        data = load(manifest)
        assert sha(checked(data["sourceManifest"]["path"])) == data["sourceManifest"]["sha256"]
        pages = {}
        for page in data["atlasPages"]:
            image = checked(page["path"])
            assert sha(image) == page["sha256"]
            pages[page["path"]] = Image.open(image).convert("RGBA")
        frames = list(data.get("regions", []))
        ids = set()
        for clip in data.get("clips", []):
            assert clip["id"] not in ids
            ids.add(clip["id"])
            frames.extend(f for f in clip["frames"] if "path" in f)
            durations = [f.get("seconds", f.get("durationSeconds", clip.get("frameSeconds", 1/24))) * f.get("hold", 1) for f in clip["frames"]]
            assert durations and all(t > 0 for t in durations)
            for marker in clip.get("markers", []):
                frame = marker.get("frame", 0)
                assert 0 <= frame <= len(durations)
                at = marker.get("seconds", sum(durations[:frame]))
                assert 0 <= at <= sum(durations) + 1e-5
        verified = set()
        for frame in frames:
            source = checked(frame["sourcePath"])
            assert sha(source) == frame["sourceSha256"]
            assert frame["widthCells"] > 0 and frame["heightCells"] > 0
            assert 0 <= frame.get("pivotX", .5) <= 1 and 0 <= frame.get("pivotY", 0) <= 1
            original = Image.open(source).convert("RGBA")
            x, y, w, h = (frame[k] for k in ("x", "y", "pixelWidth", "pixelHeight"))
            crop = pages[frame["path"]].crop((x, y, x+w, y+h))
            assert crop.size == original.size and crop.tobytes() == original.tobytes(), source
            verified.add(frame["sourcePath"])
        keys = set()
        for binding in data.get("bindings", []):
            assert binding["clip"] in ids
            key = tuple(binding.get(k, d) for k, d in (("actor", "officer"), ("weapon", "pistol"), ("armor", "suit"), ("action", ""), ("facing", "")))
            assert key not in keys, key
            keys.add(key)
        report["packs"].append({"name": entry["name"], "atlasPages": len(pages), "uniquePixelExactSources": len(verified), "clips": len(ids), "explicitBindings": len(keys)})
    print(json.dumps(report, indent=2))
    return report

if __name__ == "__main__":
    validate()
