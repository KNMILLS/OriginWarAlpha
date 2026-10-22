"""Repeatable extraction of retained illustrated equipment sources; no production painting."""
from __future__ import annotations

import hashlib
import io
import json
import math
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / "assets/art/survival/equipment-motion"
SOURCE, REGIONS, REVIEW = [ART / name for name in ("source", "regions", "review")]
CELL = 256
DIRECTIONS = ("north", "east", "south", "west")


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def components(path):
    image = Image.open(path).convert("RGBA")
    data = np.asarray(image)
    labels, count = ndimage.label(data[:, :, 3] >= 32)
    counts = np.bincount(labels.ravel())
    result = []
    for component in range(1, count + 1):
        if counts[component] < 1000:
            continue
        main = labels == component
        fringe = ndimage.binary_dilation(main, iterations=3) & ((labels == 0) | main)
        ys, xs = np.where(fringe & (data[:, :, 3] > 0))
        box = [int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1]
        rgba = np.array(data)
        rgba[:, :, 3] = np.where(fringe, rgba[:, :, 3], 0)
        result.append({"sprite": Image.fromarray(rgba).crop(box), "box": box})
    assert len(result) == 4, (path, len(result))
    return sorted(result, key=lambda item: (item["box"][0] + item["box"][2]) / 2)


def region(name, sprite, scale, anchor_x, source, component, crop, canvas_size=None):
    resized = sprite.resize((round(sprite.width * scale), round(sprite.height * scale)), Image.Resampling.LANCZOS)
    anchor = round(anchor_x * scale)
    width = math.ceil(max(anchor, resized.width - anchor) * 2 + 16)
    height = resized.height + 16
    if canvas_size:
        width, height = canvas_size
    output = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    output.alpha_composite(resized, (width // 2 - anchor, height - 8 - resized.height))
    alpha = np.asarray(output)[:, :, 3]
    ground = height - 1 - int(np.where(alpha >= 96)[0].max())
    path = REGIONS / f"{name}.png"
    output.save(path)
    return {"name": name, "path": f"art/survival/equipment-motion/regions/{name}.png",
            "widthCells": width / CELL, "heightCells": height / CELL,
            "pivotX": (width // 2) / width, "pivotY": ground / height, "layer": "world",
            "pixelWidth": width, "pixelHeight": height, "source": source,
            "sourceComponent": component, "sourceCrop": crop, "sourceScale": scale,
            "sourceAnchorX": anchor_x, "referenceBodyHeightCells": 1.3, "sha256": sha(path)}


def static_equipment():
    result = []
    for equipment in ("unarmed", "rifle", "scattergun"):
        source = f"officer-{equipment}-turnaround-v1.png"
        for number, pose in enumerate(components(SOURCE / source)):
            sprite = pose["sprite"]
            alpha = np.asarray(sprite)[:, :, 3]
            ys, xs = np.where(alpha >= 96)
            # Static full-body height excludes transparent fringe, not equipment width.
            scale = 1.3 * CELL / (int(ys.max()) - int(ys.min()) + 1)
            head = alpha[:max(1, round(sprite.height * .17))] >= 96
            anchor_x = float(np.where(head)[1].mean())
            result.append(region(f"officer.{equipment}.{DIRECTIONS[number]}", sprite, scale,
                                 anchor_x, source, number + 1, pose["box"]))
    return result


def north_fire():
    source = "officer-fire-north-v1.png"
    poses = components(SOURCE / source)
    # One body calibration for all frames. A muzzle flash must not shrink the officer.
    # Source body reference runs from north helmet crown y=100 to solid sole y=840.
    scale = 1.3 * CELL / 740
    anchors = (254, 677, 1101, 1538)  # source X, manually inspected helmet/torso axes
    fitted = []
    for anchor, pose in zip(anchors, poses):
        local = anchor - pose["box"][0]
        rw = round(pose["sprite"].width * scale)
        rx = round(local * scale)
        fitted.append((math.ceil(max(rx, rw - rx) * 2 + 16), round(pose["sprite"].height * scale) + 16))
    canvas = (max(item[0] for item in fitted), max(item[1] for item in fitted))
    frames = [region(f"officer.fire.north.{number:02d}", pose["sprite"], scale,
                     anchors[number] - pose["box"][0], source, number + 1, pose["box"], canvas)
              for number, pose in enumerate(poses)]
    # Require the shared bottom registration to survive source/extraction, never conceal drift.
    ground = frames[0]["pivotY"]
    assert all(abs((frame["pivotY"] - ground) * frame["pixelHeight"]) <= 1 for frame in frames)
    for frame in frames:
        frame["pivotY"] = ground
    clip = {"id": "officer.fire.north", "equipment": "pistol", "facing": "north",
            "loop": False, "frameSeconds": .07, "frames": frames,
            "markers": [{"frame": 2, "kind": "release"}, {"frame": 3, "kind": "recovery"}],
            "artisticAcceptance": "candidate: no native synchronization or owner acceptance"}
    aim = {"id": "officer.aim.north", "equipment": "pistol", "facing": "north",
           "loop": False, "frameSeconds": 1, "frames": [frames[1]], "markers": [],
           "artisticAcceptance": "authored aim drawing; not a second animation"}
    return [clip, aim]


def reviews(regions, clips):
    font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 16)
    sheet = Image.new("RGBA", (960, 1200), (21, 27, 29, 255))
    draw = ImageDraw.Draw(sheet)
    for index, entry in enumerate(regions):
        image = Image.open(ROOT / "assets" / entry["path"]).convert("RGBA")
        image.thumbnail((220, 320), Image.Resampling.LANCZOS)
        x, y = (index % 4) * 240, (index // 4) * 400
        sheet.alpha_composite(image, (x + (240 - image.width) // 2, y + 60 + (320 - image.height)))
        draw.text((x + 10, y + 12), entry["name"].replace("officer.", ""), font=font, fill=(220, 227, 222))
    sheet.convert("RGB").save(REVIEW / "equipment-contact.jpg", quality=94)
    frames = []
    for entry in clips[0]["frames"]:
        canvas = Image.new("RGBA", (320, 430), (21, 27, 29, 255))
        sprite = Image.open(ROOT / "assets" / entry["path"]).convert("RGBA")
        canvas.alpha_composite(sprite, (round(160 - sprite.width * entry["pivotX"]),
                                      round(408 - sprite.height * (1 - entry["pivotY"]))))
        ImageDraw.Draw(canvas).text((12, 12), "NORTH PISTOL - candidate", font=font, fill=(220, 227, 222))
        frames.append(canvas.convert("RGB"))
    frames[0].save(REVIEW / "north-fire.gif", save_all=True, append_images=frames[1:], duration=70, loop=0)
    contact = Image.new("RGB", (320 * 4, 430), (21, 27, 29))
    for index, frame in enumerate(frames):
        contact.paste(frame, (index * 320, 0))
    contact.save(REVIEW / "north-fire-contact.jpg", quality=94)


def editable(regions):
    width = max(entry["pixelWidth"] for entry in regions)
    height = max(entry["pixelHeight"] for entry in regions)
    root = ET.Element("image", {"w": str(width), "h": str(height), "name": "Equipment candidates"})
    stack = ET.SubElement(root, "stack")
    with zipfile.ZipFile(ART / "editable-equipment.ora", "w", zipfile.ZIP_DEFLATED) as archive:
        def write(name, data):
            info = zipfile.ZipInfo(name, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data)
        write("mimetype", b"image/openraster")
        for index, entry in enumerate(regions):
            path = ROOT / "assets" / entry["path"]
            location = f"data/pose{index:02d}.png"
            write(location, path.read_bytes())
            ET.SubElement(stack, "layer", {"name": entry["name"], "src": location,
                "x": str((width - entry["pixelWidth"]) // 2), "y": str(height - entry["pixelHeight"]),
                "opacity": "1.0", "visibility": "visible" if index == 0 else "hidden",
                "composite-op": "svg:src-over"})
        write("stack.xml", ET.tostring(root, encoding="utf-8", xml_declaration=True))


def main():
    REGIONS.mkdir(parents=True, exist_ok=True)
    REVIEW.mkdir(parents=True, exist_ok=True)
    regions, clips = static_equipment(), north_fire()
    (ART / "regions.json").write_text(json.dumps({"version": 1, "referenceCellPixels": CELL,
        "pivotOrigin": "bottom-left", "coverage": "12 static equipment facings only; no walk/fire variants",
        "regions": regions}, indent=2) + "\n", encoding="utf-8", newline="\n")
    (ART / "clips.json").write_text(json.dumps({"version": 1, "referenceCellPixels": CELL,
        "pivotOrigin": "bottom-left", "clips": clips}, indent=2) + "\n", encoding="utf-8", newline="\n")
    reviews(regions, clips)
    editable(regions + clips[0]["frames"])
    files = [{"path": str(path.relative_to(SOURCE)).replace("\\", "/"), "sha256": sha(path)}
             for path in sorted(SOURCE.rglob("*")) if path.is_file() and path.name != "HASHES.json"]
    (SOURCE / "HASHES.json").write_text(json.dumps({"files": files}, indent=2) + "\n", encoding="utf-8", newline="\n")
    print("Exported 12 static equipment facings, 4 north-fire drawings and 1 aim alias")


if __name__ == "__main__":
    main()
