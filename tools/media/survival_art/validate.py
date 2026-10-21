"""Independent integrity checks for SH-ART; no runtime/artistic acceptance claim."""
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
ART = ROOT / "assets/art/survival"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate():
    generation = json.loads((ART / "source/GENERATION.json").read_text(encoding="utf-8"))
    for source in generation["sources"]:
        assert sha(ART / "source" / source["path"]) == source["sha256"], source["path"]
        if "prompt" in source:
            assert sha(ART / "source" / source["prompt"]) == source["promptSha256"]
    manifest = json.loads((ART / "regions.json").read_text(encoding="utf-8"))
    assert manifest["referenceCellPixels"] == 256
    assert manifest["pivotOrigin"] == "bottom-left"
    regions = manifest["regions"]
    names = {r["name"] for r in regions}
    assert len(names) == len(regions) == 42
    expected = {"officer." + d for d in ("north", "east", "south", "west")}
    expected |= {"door.closed", "door.open", "door.locked", "alice.avatar", "alice.concerned"}
    expected |= {"machine." + k for k in ("sirs", "add", "rover", "camera", "turret", "constructor")}
    expected |= {"item." + k for k in ("oxygen", "cells", "medicine", "pistol", "rifle", "scattergun", "armor-light", "armor-heavy", "probe", "brace")}
    assert expected <= names
    cutouts = 0
    for r in regions:
        path = (ROOT / "assets" / r["path"]).resolve()
        assert path.is_relative_to(ART.resolve())
        assert path.is_file() and sha(path) == r["sha256"]
        image = Image.open(path)
        assert image.mode == "RGBA"
        assert image.size == (r["pixelWidth"], r["pixelHeight"])
        assert abs(image.width / 256 - r["widthCells"]) < 1e-9
        assert abs(image.height / 256 - r["heightCells"]) < 1e-9
        assert 0 <= r["pivotX"] <= 1 and 0 <= r["pivotY"] <= 1
        assert r["layer"] in {"ground", "world", "avatar"}
        a = np.array(image)[:, :, 3]
        assert int((a > 128).sum()) > 100
        if r["name"].startswith("floor."):
            assert image.size == (256, 256) and int(a.min()) == 255
        else:
            assert int(a.min()) == 0 and int((a == 0).sum()) > a.size * .03
            cutouts += 1
    assert cutouts == 36
    lookup = {r["name"]: r for r in regions}
    assert len({lookup["officer." + d]["sha256"] for d in ("north", "east", "south", "west")}) == 4
    for d in ("north", "east", "south", "west"):
        assert abs(lookup["officer." + d]["heightCells"] - 1.3) < .004
    aperture = Image.open(ART / "regions/door.open.png").getchannel("A")
    assert aperture.getpixel((aperture.width // 2, aperture.height // 2)) == 0
    with zipfile.ZipFile(ART / "editable-kit.ora") as archive:
        assert archive.read("mimetype") == b"image/openraster"
        assert len([n for n in archive.namelist() if n.startswith("data/layer")]) == 42
    return {"regions": len(regions), "opaqueFloors": 6, "alphaCutouts": cutouts,
            "distinctOfficerFacings": 4, "doorApertureTransparent": True,
            "sourceAndPromptHashes": "PASS", "manifestAndBounds": "PASS",
            "editableAssetLayers": 42}


if __name__ == "__main__":
    result = validate()
    before = {p.relative_to(ART).as_posix(): sha(p) for p in ART.rglob("*")
              if p.is_file() and (p.parent.name == "regions" or p.name in {"regions.json", "editable-kit.ora"})}
    subprocess.run([sys.executable, str(Path(__file__).with_name("build.py"))], check=True, cwd=ROOT)
    after = {p.relative_to(ART).as_posix(): sha(p) for p in ART.rglob("*")
             if p.is_file() and (p.parent.name == "regions" or p.name in {"regions.json", "editable-kit.ora"})}
    assert before == after, "Non-repeatable production output"
    result["byteRepeatability"] = "PASS"
    validate()
    result["runtimeMotionLightingAndOwnerAcceptance"] = "NOT_TESTED"
    (ART / "validation.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(result, indent=2))
