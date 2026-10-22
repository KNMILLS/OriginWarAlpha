"""Validate reproducibility and honest export coverage, not native/artistic acceptance."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import zipfile

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / "assets/art/survival/equipment-motion"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def outputs():
    return {str(path.relative_to(ART)): sha(path) for path in ART.rglob("*")
            if path.is_file() and "source" not in path.relative_to(ART).parts and path.name != "validation.json"}


def main():
    data = json.loads((ART / "regions.json").read_text(encoding="utf-8"))
    clips = json.loads((ART / "clips.json").read_text(encoding="utf-8"))["clips"]
    expected = {f"officer.{equipment}.{facing}" for equipment in ("unarmed", "rifle", "scattergun")
                for facing in ("north", "east", "south", "west")}
    assert {entry["name"] for entry in data["regions"]} == expected
    assert {entry["id"]: len(entry["frames"]) for entry in clips} == {"officer.fire.north": 4, "officer.aim.north": 1}
    assert len({frame["sha256"] for frame in clips[0]["frames"]}) == 4
    assert clips[0]["markers"] == [{"frame": 2, "kind": "release"}, {"frame": 3, "kind": "recovery"}]
    entries = data["regions"] + clips[0]["frames"]
    for entry in entries:
        path = ROOT / "assets" / entry["path"]
        assert path.resolve().is_relative_to((ART / "regions").resolve())
        assert sha(path) == entry["sha256"]
        image = Image.open(path)
        assert image.mode == "RGBA" and image.size == (entry["pixelWidth"], entry["pixelHeight"])
        alpha = np.asarray(image)[:, :, 3]
        assert (alpha == 0).any() and (alpha >= 96).any() and (alpha[0] == 0).all()
        assert (alpha[-1] == 0).all() and (alpha[:, 0] == 0).all() and (alpha[:, -1] == 0).all()
        solid_y = np.where(alpha >= 96)[0]
        ground = image.height - 1 - int(solid_y.max())
        assert abs(ground - image.height * entry["pivotY"]) <= 1.01
        assert entry["widthCells"] == image.width / 256 and entry["heightCells"] == image.height / 256
        if entry["name"] in expected:
            assert abs((int(solid_y.max()) - int(solid_y.min()) + 1) / 256 - 1.3) < .025
    common = {(frame["pixelWidth"], frame["pixelHeight"], frame["pivotX"], frame["pivotY"], frame["sourceScale"])
              for frame in clips[0]["frames"]}
    assert len(common) == 1
    for item in json.loads((ART / "source/HASHES.json").read_text(encoding="utf-8"))["files"]:
        assert sha(ART / "source" / item["path"]) == item["sha256"]
    generation = json.loads((ART / "source/GENERATION.json").read_text(encoding="utf-8"))
    assert len(generation["generations"]) == 4
    for entry in generation["generations"]:
        assert (ART / "source" / entry["source"]).is_file()
        assert (ART / "source" / entry["prompt"]).read_text(encoding="utf-8").strip() == entry["requestPrompt"]
    with zipfile.ZipFile(ART / "editable-equipment.ora") as archive:
        assert archive.testzip() is None and archive.read("mimetype") == b"image/openraster"
        assert len([name for name in archive.namelist() if name.startswith("data/")]) == 16
    gif = Image.open(ART / "review/north-fire.gif")
    assert gif.n_frames == 4
    total = 0
    for number in range(4):
        gif.seek(number)
        total += gif.info["duration"]
    assert total == 280
    before = outputs()
    subprocess.run([sys.executable, "-B", str(Path(__file__).with_name("build.py"))], check=True)
    assert before == outputs(), "Non-repeatable asset export"
    report = {"technical": "PASS", "staticEquipmentFacings": 12, "northPistolDrawings": 4,
              "aimAliases": 1, "sourceHashes": "PASS", "groundAnchors": "PASS",
              "trueTransparentAlpha": "PASS", "byteRepeatableExport": "PASS", "editableArchiveCRC": "PASS",
              "nativeGame": "NOT_TESTED by this worker", "ownerArtisticAcceptance": "OPEN",
              "limitations": ["unarmed/rifle/scattergun remain static during movement/actions",
                 "no rifle/scattergun aim/fire clips", "north pistol preparation begins already raised",
                 "machine cardinal facing coverage unchanged", "normal-speed synchronization awaits native review"]}
    (ART / "validation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
