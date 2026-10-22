"""Reproducible native32 human idle export from retained original source rasters."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / "assets/art/animation/humans"
SOURCE = BASE / "source/humans-generated.png"
STATION = ROOT / "assets/art/source/station-native.png"
ACTORS = ROOT / "assets/art/animation/actors-native.png"
FAMILIES = ("west", "carmack", "survivor", "operator")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def tile(image, slot):
    return image.crop(((slot % 8) * 32, (slot // 8) * 32,
                       (slot % 8 + 1) * 32, (slot // 8 + 1) * 32))


def anchor(image):
    alpha = np.asarray(image.getchannel("A"))
    ys, xs = np.nonzero(alpha >= 128)
    assert len(xs), "Missing solid human silhouette"
    box = (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)
    band = max(box[1], round(box[3] - (box[3] - box[1]) * .25))
    _, feet_x = np.nonzero(alpha[band:box[3]] >= 128)
    feet_center = (float(feet_x.min()) + float(feet_x.max()) + 1) / 2
    return box, (feet_center, float(box[3]))


def export_pair(frames):
    geometry = [anchor(frame) for frame in frames]
    left = max(foot[0] - box[0] for box, foot in geometry)
    right = max(box[2] - foot[0] for box, foot in geometry)
    height = max(foot[1] - box[1] for box, foot in geometry)
    scale = min(15 / max(1, left), 15 / max(1, right), 28 / max(1, height))
    result, details = [], []
    for image, (box, foot) in zip(frames, geometry):
        crop = image.crop(box)
        size = (max(1, round(crop.width * scale)),
                max(1, round(crop.height * scale)))
        glyph = crop.resize(size, Image.Resampling.LANCZOS)
        position = (round(16 - (foot[0] - box[0]) * scale),
                    round(30 - (foot[1] - box[1]) * scale))
        assert position[0] >= 0 and position[1] >= 0
        assert position[0] + size[0] <= 32 and position[1] + size[1] <= 32
        canvas = Image.new("RGBA", (32, 32))
        canvas.alpha_composite(glyph, position)
        pixels = np.asarray(canvas).copy()
        pixels[pixels[:, :, 3] < 32] = 0
        result.append(Image.fromarray(pixels, "RGBA"))
        details.append({"source_solid_bbox": box, "source_foot_anchor": foot,
                        "pair_scale": round(scale, 9), "native_anchor": [16, 30],
                        "native_position": position, "native_size": size,
                        "alpha_cutoff": 32})
    return result, details


def pack():
    source = Image.open(SOURCE).convert("RGBA")
    native = Image.new("RGBA", (256, 32))
    slots = []
    for family_index, family in enumerate(FAMILIES[:3]):
        frames, boxes = [], []
        for phase in range(2):
            box = (round(family_index * source.width / 3),
                   round(phase * source.height / 2),
                   round((family_index + 1) * source.width / 3),
                   round((phase + 1) * source.height / 2))
            frames.append(source.crop(box))
            boxes.append(box)
        exports, details = export_pair(frames)
        for phase in range(2):
            slot = family_index * 2 + phase
            native.alpha_composite(exports[phase], (slot * 32, 0))
            slots.append({"id": slot, "family": family, "phase": phase,
                          "source": "source/humans-generated.png",
                          "source_cell_box": boxes[phase], **details[phase]})
    previous = Image.open(ACTORS).convert("RGBA")
    for phase in range(2):
        native.alpha_composite(tile(previous, 60 + phase), ((6 + phase) * 32, 0))
        slots.append({"id": 6 + phase, "family": "operator", "phase": phase,
                      "source": "assets/art/animation/actors-native.png",
                      "source_slot": 60 + phase, "reuse": "exact native RGBA pixels"})
    native.save(BASE / "humans-native.png", optimize=True)
    native.resize((1024, 128), Image.Resampling.NEAREST).save(BASE / "humans.png", optimize=True)
    review = BASE / "review"
    review.mkdir(exist_ok=True)
    contact = Image.new("RGBA", (1024, 128), (20, 30, 36, 255))
    contact.alpha_composite(native.resize((1024, 128), Image.Resampling.NEAREST))
    contact.convert("RGB").save(review / "humans-4x-on-dark.png", optimize=True)
    phases = []
    for phase in range(2):
        frame = Image.new("RGBA", (512, 128), (20, 30, 36, 255))
        for family_index in range(4):
            piece = native.crop(((family_index * 2 + phase) * 32, 0,
                                 (family_index * 2 + phase + 1) * 32, 32))
            frame.alpha_composite(piece.resize((128, 128), Image.Resampling.NEAREST),
                                  (family_index * 128, 0))
        phases.append(frame.convert("RGB"))
    phases[0].save(review / "humans.gif", save_all=True, append_images=phases[1:],
                   duration=500, loop=0)
    manifest = {
        "task": "M5-HUMAN-IDLES",
        "baseline": "874e205cc79152a833cc7b0c8b308538ae42e907",
        "status": "Original idle export candidate; static visual review and runtime integration are separate",
        "atlas": {"path": "assets/art/animation/humans/humans.png", "size": [1024, 128],
                  "native": "assets/art/animation/humans/humans-native.png",
                  "native_size": [256, 32], "columns": 8, "rows": 1,
                  "native_cell_pixels": 32, "nearest_scale": 4, "mode": "RGBA"},
        "layout": "WEST2, CARMACK2, SURVIVOR2, OPERATOR2; columns row-major",
        "palette": "Existing project navy/teal/steel/amber; Carmack lab-coat white, brown hair and human skin tones. No palette recolor of previous atlases.",
        "provenance": "Original built-in OpenAI image_gen human idle art from the existing original station identities; no external pack, legacy raster or stock artwork. Operator native pair reused from existing original project actor atlas. Owner controls project release licensing.",
        "source_prompt": "source/prompt.txt",
        "generation_attempts": 1,
        "first_failures": [],
        "selection": "First generated six-cell sheet selected; source raster remains unchanged.",
        "export_source": "tools/media/pack_humans.py",
        "reference_station_sha256": digest(STATION),
        "reference_actors_sha256": digest(ACTORS),
        "source_files": [{"path": str(path.relative_to(ROOT)).replace("\\", "/"),
                          "sha256": digest(path)}
                         for path in sorted((BASE / "source").rglob("*")) if path.is_file()],
        "cleanup": "Shared scale within each pair; foot/leg anchor at16,30; new frame alpha below32 cleared. Operator copied without raster processing. Native32 source reduction followed by nearest4 export.",
        "manual_review": ["West keeps navy officer cap/coat with amber insignia; Carmack keeps white coat/teal shirt/brown hair; survivor keeps amber safety helmet and steel/amber workwear.",
                          "Two arms/two legs and subtle body/head/hand differences inspected; no walk, gun or invented spawn.",
                          "Source and native/4x contact sheets checked for silhouette, alignment, alpha and cell separation."],
        "limitations": ["Quiet front/down idle pairs only; no new walking or directional dialogue animation.",
                        "Animated radio/dialogue portrait and world consumption/timing are lead-owned; West remains usable offstage without an invented NPC spawn.",
                        "Static review and export metrics do not prove actual game readability, timing or human acceptance."],
        "slots": slots,
    }
    (BASE / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")


def validate(repeat):
    products = [BASE / name for name in ("humans.png", "humans-native.png", "manifest.json",
                                         "review/humans-4x-on-dark.png", "review/humans.gif")]
    before = {str(path.relative_to(BASE)): digest(path) for path in products}
    if repeat:
        pack()
        assert before == {str(path.relative_to(BASE)): digest(path) for path in products}
    runtime = Image.open(BASE / "humans.png")
    native = Image.open(BASE / "humans-native.png")
    assert runtime.size == (1024, 128) and native.size == (256, 32)
    assert runtime.mode == native.mode == "RGBA"
    pixels = np.asarray(native)
    assert np.array_equal(np.asarray(runtime),
                          np.repeat(np.repeat(pixels, 4, axis=0), 4, axis=1))
    previous = Image.open(ACTORS).convert("RGBA")
    pairs = []
    for family_index, family in enumerate(FAMILIES):
        a = pixels[:, family_index * 64:family_index * 64 + 32]
        b = pixels[:, family_index * 64 + 32:family_index * 64 + 64]
        changed = int(np.count_nonzero(np.any(a != b, axis=2)))
        mask_changed = int(np.count_nonzero((a[:, :, 3] >= 128) != (b[:, :, 3] >= 128)))
        assert changed > 0 and mask_changed > 0, family
        for phase, piece in enumerate((a, b)):
            alpha = piece[:, :, 3]
            ys, xs = np.nonzero(alpha >= 128)
            assert len(xs) and np.any(alpha == 0)
            assert not np.any((alpha > 0) & (alpha < 32))
            if family_index < 3:
                assert 28 <= int(ys.max()) <= 30
                assert not np.any(alpha[:, 0]) and not np.any(alpha[:, -1])
                assert not np.any(alpha[0, :]) and not np.any(alpha[-1, :])
            else:
                assert np.array_equal(piece, np.asarray(tile(previous, 60 + phase)))
        pairs.append({"family": family, "changed_native_pixels": changed,
                      "changed_solid_mask_pixels": mask_changed})
    manifest = json.loads((BASE / "manifest.json").read_text())
    assert digest(STATION) == manifest["reference_station_sha256"]
    assert digest(ACTORS) == manifest["reference_actors_sha256"]
    for source in manifest["source_files"]:
        assert digest(ROOT / source["path"]) == source["sha256"]
    report = {"result": "PASS", "scope": "Static export/layout/alpha/pair/anchor/source-hash/operator-identity and repeatability only; not runtime acceptance",
              "size": [1024, 128], "native_size": [256, 32], "occupied_slots": 8,
              "operator_exact_native_frames": 2, "pairs": pairs,
              "packing_repeatability": "PASS" if repeat else "NOT_RUN",
              "source_hashes_verified": True, "reference_atlases_unchanged": True,
              "atlas_sha256": digest(BASE / "humans.png"),
              "native_sha256": digest(BASE / "humans-native.png")}
    (BASE / "validation.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    pack()
    if args.verify:
        print(json.dumps(validate(True), indent=2))


if __name__ == "__main__":
    main()
