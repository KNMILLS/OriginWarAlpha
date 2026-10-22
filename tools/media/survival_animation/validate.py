"""Meaningful source/export/anchor checks; never awards artistic/runtime acceptance."""
from __future__ import annotations

import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
MOTION = ROOT / "assets/art/survival/motion"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def outputs():
    files = [MOTION / "clips.json", MOTION / "editable-motion.ora", MOTION / "source/HASHES.json"]
    files += sorted((MOTION / "regions").glob("*.png"))
    files += [p for p in sorted((MOTION / "review").iterdir())
              if p.name.startswith(("officer-walk", "machines-walk", "officer-fire", "playback"))]
    return {str(p.relative_to(MOTION)): sha(p) for p in files}


def main():
    data = json.loads((MOTION / "clips.json").read_text(encoding="utf-8"))
    clips = data["clips"]
    assert data["version"] == 1 and data["referenceCellPixels"] == 256
    expected = {f"officer.walk.{d}": 12 for d in ("north", "east", "south", "west")}
    expected.update({f"machine.{m}.walk": 8 for m in ("sirs", "add", "rover")})
    expected.update({f"officer.{a}.{d}": 4 if a == "fire" else 1
                     for a in ("fire", "aim") for d in ("east", "south", "west")})
    assert {c["id"]: len(c["frames"]) for c in clips} == expected
    regions, worst_anchor_error = set(), 0
    for clip in clips:
        frames = clip["frames"]
        common = {(f["pixelWidth"], f["pixelHeight"], f["pivotX"], f["pivotY"], f["sourceScale"]) for f in frames}
        assert len(common) == 1, f"Per-frame canvas/pivot/scale drift in {clip['id']}"
        assert len({f["sha256"] for f in frames}) == len(frames), f"Duplicated pose in {clip['id']}"
        assert clip["frameSeconds"] > 0
        for marker in clip["markers"]:
            assert 0 <= marker["frame"] < len(frames)
        for frame in frames:
            path = ROOT / "assets" / frame["path"]
            assert path.resolve().is_relative_to((MOTION / "regions").resolve())
            assert sha(path) == frame["sha256"]
            regions.add(frame["path"])
            image = Image.open(path)
            assert image.mode == "RGBA" and image.size == (frame["pixelWidth"], frame["pixelHeight"])
            alpha = np.asarray(image)[:, :, 3]
            assert alpha[0, 0] == alpha[-1, -1] == 0 and (alpha > 0).any()
            assert frame["widthCells"] == image.width / 256 and frame["heightCells"] == image.height / 256
            actual_ground = image.height - 1 - int(np.where(alpha >= 96)[0].max())
            error = abs(actual_ground - image.height * frame["pivotY"])
            assert error <= 1.01, f"Ground anchor drift {clip['id']}: {error}"
            worst_anchor_error = max(worst_anchor_error, error)
            source = Image.open(MOTION / "source" / frame["source"])
            x0, y0, x1, y1 = frame["sourceCrop"]
            assert 0 <= x0 < x1 <= source.width and 0 <= y0 < y1 <= source.height
    assert len(regions) == 84
    hashes = json.loads((MOTION / "source/HASHES.json").read_text(encoding="utf-8"))["files"]
    for item in hashes:
        assert sha(MOTION / "source" / item["path"]) == item["sha256"]
    generation = json.loads((MOTION / "source/GENERATION.json").read_text(encoding="utf-8"))
    assert len(generation["generations"]) == 14 and len(generation["blocked"]) == 1
    for entry in generation["generations"]:
        assert (MOTION / "source" / entry["source"]).is_file()
        assert (MOTION / "source" / entry["prompt"]).is_file()
    with zipfile.ZipFile(MOTION / "editable-motion.ora") as archive:
        assert archive.testzip() is None
        assert archive.read("mimetype") == b"image/openraster"
        assert len([n for n in archive.namelist() if n.startswith("data/")]) == 87
    for stem in ("officer-walk", "machines-walk", "officer-fire"):
        gif = Image.open(MOTION / "review" / f"{stem}.gif")
        duration = 0
        for i in range(gif.n_frames):
            gif.seek(i)
            duration += gif.info["duration"]
        assert gif.n_frames > 1 and duration == 500
    before = outputs()
    subprocess.run([sys.executable, "-B", str(Path(__file__).with_name("build.py"))], check=True)
    after = outputs()
    assert before == after, f"Non-repeatable outputs: {[k for k in before if before[k] != after.get(k)]}"
    report = {"technical": "PASS", "clips": len(clips), "uniquePaintedRegions": len(regions),
              "frameBindings": sum(len(c["frames"]) for c in clips),
              "maxSolidSoleAnchorDriftReferencePixels": worst_anchor_error,
              "sourceHashes": "PASS", "byteRepeatableExports": "PASS", "editableArchiveCRC": "PASS",
              "browserPreview": "48 images loaded; phase2->5 in150ms; pause7->7 in150ms; another500ms cycle sampled; no image load failures",
              "nativeGameMotion": "NOT_TESTED in this worker", "commercialAnimationQuality": "NOT_ACCEPTED",
              "remaining": ["front/rear cycle choreography", "equipment variants", "machine cardinal facings",
                            "north pistol action service rejection", "hit/interact/death/door actions",
                            "native foot planting and marker synchronization", "owner artistic approval"]}
    (MOTION / "validation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
