"""Extract authored motion masters; no generated/interpolated/warped production poses.

Python 3.11+, Pillow, numpy, scipy. Run from any directory.
All frame coordinates and scalings derive from retained original painted sheets.
"""
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
MOTION = ROOT / "assets/art/survival/motion"
SOURCE, REGIONS, REVIEW = [MOTION / s for s in ("source", "regions", "review")]
CELL = 256
DIRECTIONS = ("north", "east", "south", "west")
FONT = "C:/Windows/Fonts/arial.ttf"


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def components(path, expected, columns=None):
    image = Image.open(path).convert("RGBA")
    data = np.asarray(image)
    labels, count = ndimage.label(data[:, :, 3] >= 32)
    counts = np.bincount(labels.ravel())
    ids = [i for i in range(1, count + 1) if counts[i] > 1000]
    if len(ids) != expected:
        raise ValueError(f"{path}: {len(ids)} substantial components, expected {expected}")
    result = []
    for component in ids:
        main = labels == component
        fringe = ndimage.binary_dilation(main, iterations=3)
        ys, xs = np.where(fringe & (data[:, :, 3] > 0))
        box = (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)
        sprite = np.array(image)
        sprite[:, :, 3] = np.where(fringe, sprite[:, :, 3], 0)
        result.append({"sprite": Image.fromarray(sprite).crop(box), "box": box})
    # Source layouts use equal rows; order each row by its horizontal center.
    columns = columns or (2 if expected == 4 else 4)
    rows = expected // columns
    row_height = image.height / rows
    result.sort(key=lambda x: (round(((x["box"][1] + x["box"][3]) / 2 - row_height / 2) / row_height),
                               (x["box"][0] + x["box"][2]) / 2))
    return result


def helmet_center(sprite):
    # Horizontal anchor from the helmet, avoiding the changing arm/leg silhouette.
    alpha = np.asarray(sprite)[:, :, 3]
    ys, xs = np.where(alpha[: max(1, int(sprite.height * 0.17))] >= 96)
    return float(xs.mean()) if len(xs) else sprite.width / 2


def make_frames(direction):
    base_name = f"officer-walk-{direction}-v1.png"
    base = components(SOURCE / base_name, 12)
    # A constant reference calibration for this direction, NOT per-frame scaling.
    reference_height = base[0]["sprite"].height
    scale = round(1.3 * CELL) / reference_height
    sequence = [(base_name, index, base[index], scale) for index in range(12)]
    scaled = []
    for source_name, component, info, fixed_scale in sequence:
        sprite = info["sprite"]
        width, height = round(sprite.width * fixed_scale), round(sprite.height * fixed_scale)
        sprite = sprite.resize((width, height), Image.Resampling.LANCZOS)
        scaled.append((sprite, round(helmet_center(info["sprite"]) * fixed_scale),
                       source_name, component, info["box"], fixed_scale))
    # Identical per-direction canvases include true alpha padding above the tallest pose.
    # Ground anchor is calibrated from the first solid painted sole once per clip.
    half_width = max(max(center, sprite.width - center) for sprite, center, *_ in scaled) + 8
    canvas_width = int(math.ceil(half_width * 2))
    canvas_height = max(sprite.height for sprite, *_ in scaled) + 16
    frames, ground_pivot = [], None
    for index, (sprite, center, source_name, component, crop, fixed_scale) in enumerate(scaled):
        canvas = Image.new("RGBA", (canvas_width, canvas_height), (0, 0, 0, 0))
        x, y = canvas_width // 2 - center, canvas_height - 8 - sprite.height
        canvas.alpha_composite(sprite, (x, y))
        if ground_pivot is None:
            ground_pivot = (canvas_height - 1 - int(np.where(np.asarray(canvas)[:, :, 3] >= 96)[0].max())) / canvas_height
        path = REGIONS / f"officer.walk.{direction}.{index:02d}.png"
        canvas.save(path)
        frames.append({
            "path": f"art/survival/motion/regions/{path.name}",
            "widthCells": canvas_width / CELL, "heightCells": canvas_height / CELL,
            "pivotX": (canvas_width // 2) / canvas_width, "pivotY": ground_pivot,
            "pixelWidth": canvas_width, "pixelHeight": canvas_height,
            "source": source_name, "sourceComponent": component + 1,
            "sourceCrop": crop, "sourceScale": fixed_scale, "sha256": sha(path),
        })
    return {
        "id": f"officer.walk.{direction}", "frameSeconds": 1 / 24,
        "loop": True, "cycleDistanceCells": 2, "referencePoseHeightCells": 1.3, "equipment": "pistol",
        "frames": frames, "markers": [{"frame": 0, "kind": "contact", "foot": "left"},
                                      {"frame": 6, "kind": "contact", "foot": "right"}],
        "artisticAcceptance": "candidate only: phase choreography and native foot planting require review",
    }


def make_machine(name, reference_axis, reference_units):
    source_name = f"{name}-walk-v1.png"
    poses = components(SOURCE / source_name, 8)
    reference = poses[0]["sprite"]
    scale = reference_units * CELL / (reference.height if reference_axis == "h" else reference.width)
    resized = [(info["sprite"].resize((round(info["sprite"].width * scale),
                 round(info["sprite"].height * scale)), Image.Resampling.LANCZOS), info) for info in poses]
    width, height = max(p.width for p, _ in resized) + 16, max(p.height for p, _ in resized) + 16
    frames, ground_pivot = [], None
    for index, (sprite, info) in enumerate(resized):
        canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        canvas.alpha_composite(sprite, ((width - sprite.width) // 2, height - 8 - sprite.height))
        if ground_pivot is None:
            ground_pivot = (height - 1 - int(np.where(np.asarray(canvas)[:, :, 3] >= 96)[0].max())) / height
        path = REGIONS / f"machine.{name}.walk.{index:02d}.png"
        canvas.save(path)
        frames.append({"path": f"art/survival/motion/regions/{path.name}",
            "widthCells": width / CELL, "heightCells": height / CELL,
            "pivotX": 0.5, "pivotY": ground_pivot, "pixelWidth": width, "pixelHeight": height,
            "source": source_name, "sourceComponent": index + 1, "sourceCrop": info["box"],
            "sourceScale": scale, "sha256": sha(path)})
    return {"id": f"machine.{name}.walk", "frameSeconds": 1 / 16,
        "loop": True, "cycleDistanceCells": 2, "facingCoverage": "original three-quarter view only",
        "frames": frames, "markers": [{"frame": 0, "kind": "contact"}, {"frame": 4, "kind": "contact"}],
        "artisticAcceptance": "candidate: native sequence, machine facings and owner review required"}


def make_fire(direction):
    source_name = f"officer-fire-{direction}-v1.png"
    poses = components(SOURCE / source_name, 4, columns=4)
    scale = 1.3 * CELL / poses[0]["sprite"].height
    sprites = [(p["sprite"].resize((round(p["sprite"].width * scale), round(p["sprite"].height * scale)),
                   Image.Resampling.LANCZOS), round(helmet_center(p["sprite"]) * scale), p) for p in poses]
    width = math.ceil(max(max(c, s.width - c) for s, c, _ in sprites) * 2 + 16)
    height = max(s.height for s, _, _ in sprites) + 16
    frames, ground_pivot = [], None
    for index, (sprite, center, pose) in enumerate(sprites):
        canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        canvas.alpha_composite(sprite, (width // 2 - center, height - 8 - sprite.height))
        if ground_pivot is None:
            ground_pivot = (height - 1 - int(np.where(np.asarray(canvas)[:, :, 3] >= 96)[0].max())) / height
        path = REGIONS / f"officer.fire.{direction}.{index:02d}.png"
        canvas.save(path)
        frames.append({"path": f"art/survival/motion/regions/{path.name}", "widthCells": width / CELL,
            "heightCells": height / CELL, "pivotX": (width // 2) / width, "pivotY": ground_pivot,
            "pixelWidth": width, "pixelHeight": height, "source": source_name,
            "sourceComponent": index + 1, "sourceCrop": pose["box"], "sourceScale": scale,
            "sha256": sha(path)})
    fire = {"id": f"officer.fire.{direction}", "frameSeconds": .07, "loop": False,
        "equipment": "pistol", "frames": frames,
        "markers": [{"frame": 2, "kind": "release"}, {"frame": 3, "kind": "recovery"}],
        "artisticAcceptance": "candidate: native release/recovery and owner review required"}
    aim = {"id": f"officer.aim.{direction}", "frameSeconds": 1 / 24, "loop": False,
        "equipment": "pistol", "frames": [frames[1]], "markers": [],
        "artisticAcceptance": "candidate: static authored aim pose"}
    return fire, aim


def frame_image(frame):
    return Image.open(ROOT / "assets" / frame["path"]).convert("RGBA")


def contacts(clips, stem="officer-walk"):
    cell_w, cell_h = 194, 248
    columns = max(len(c["frames"]) for c in clips)
    sheet = Image.new("RGBA", (cell_w * columns, cell_h * len(clips)), (22, 28, 30, 255))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.truetype(FONT, 15)
    for row, clip in enumerate(clips):
        for index, frame in enumerate(clip["frames"]):
            sprite = frame_image(frame)
            ratio = min(172 / sprite.width, 206 / sprite.height)
            sprite = sprite.resize((round(sprite.width * ratio), round(sprite.height * ratio)),
                                   Image.Resampling.LANCZOS)
            x, y = index * cell_w, row * cell_h
            sheet.alpha_composite(sprite, (x + (cell_w - sprite.width) // 2, y + 28))
            draw.text((x + 6, y + 6), f"{clip['id'].replace('machine.', '').replace('officer.walk.', '')} {index:02d}",
                      font=font, fill=(213, 223, 220))
            draw.line((x + 4, y + 234, x + cell_w - 4, y + 234), fill=(77, 93, 94), width=1)
    sheet.convert("RGB").save(REVIEW / f"{stem}-contact.jpg", quality=94)

    # Twelve original frames at native cadence; no tweening is performed.
    previews = []
    for i in range(12):
        canvas = Image.new("RGBA", (240 * len(clips), 440), (20, 25, 27, 255))
        draw = ImageDraw.Draw(canvas)
        draw.text((12, 9), f"SH-ANIM authored gait candidates - phase {i:02d} / 24fps",
                  fill=(216, 225, 221), font=font)
        for col, clip in enumerate(clips):
            # Preview 24Hz samples of a 16fps machine clip or 24fps officer clip.
            raw_frame = int(i / 24 / clip["frameSeconds"])
            frame_index = raw_frame % len(clip["frames"]) if clip["loop"] else min(raw_frame, len(clip["frames"]) - 1)
            sprite = frame_image(clip["frames"][frame_index])
            x = col * 240 + 120 - sprite.width // 2
            frame = clip["frames"][frame_index]
            y = round(404 - sprite.height * (1 - frame["pivotY"]))
            canvas.alpha_composite(sprite, (x, y))
            draw.text((col * 240 + 16, 420), clip["id"].split(".")[-1],
                      fill=(216, 225, 221), font=font)
        previews.append(canvas)
    previews[0].save(REVIEW / f"{stem}.webp", save_all=True, append_images=previews[1:],
                     duration=[42, 41, 42] * 4, loop=0, lossless=True, method=6)
    # GIF quantizes durations to 10ms; this schedule preserves a 500ms full cycle.
    rgb = [f.convert("RGB") for f in previews]
    rgb[0].save(REVIEW / f"{stem}.gif", save_all=True, append_images=rgb[1:],
                duration=[40, 40, 40, 40, 40, 50] * 2, loop=0, disposal=2)


def editable(clips):
    root = ET.Element("image", {"version": "0.0.1", "w": "4096",
                                "h": str(len(clips) * 512), "name": "Original motion poses"})
    stack = ET.SubElement(root, "stack")
    merged = Image.new("RGBA", (4096, len(clips) * 512))
    with zipfile.ZipFile(MOTION / "editable-motion.ora", "w", compression=zipfile.ZIP_DEFLATED) as archive:
        def write(path, data, compressed=True):
            item = zipfile.ZipInfo(path, date_time=(2026, 10, 21, 0, 0, 0))
            item.compress_type = zipfile.ZIP_DEFLATED if compressed else zipfile.ZIP_STORED
            archive.writestr(item, data)
        write("mimetype", b"image/openraster", False)
        for row, clip in enumerate(clips):
            for index, frame in enumerate(clip["frames"]):
                sprite = frame_image(frame)
                x, y = index * 336 + 12, row * 512 + 16
                layer_path = f"data/{row:02d}-{index:02d}.png"
                write(layer_path, (ROOT / "assets" / frame["path"]).read_bytes())
                ET.SubElement(stack, "layer", {"name": f"{clip['id']}.{index:02d}",
                    "src": layer_path, "x": str(x), "y": str(y), "opacity": "1",
                    "visibility": "visible", "composite-op": "svg:src-over"})
                merged.alpha_composite(sprite, (x, y))
        write("stack.xml", ET.tostring(root, encoding="utf-8", xml_declaration=True))
        data = io.BytesIO()
        merged.save(data, format="PNG")
        write("mergedimage.png", data.getvalue())


def html(clips):
    payload = json.dumps(clips)
    (REVIEW / "playback.html").write_text("""<!doctype html><meta charset="utf-8">
<title>SH-ANIM motion review</title><style>
body{background:#14191b;color:#d5dfdc;font:16px system-ui;margin:24px}
canvas{background:#171e20;max-width:100%;border:1px solid #465556}
button{padding:8px 16px;margin:8px}p{max-width:850px}
</style><h1>Officer motion candidates</h1>
<p>Original painted frames at 24 frames per second, half-second cycle candidates.
Technical playback, not native game or commercial-quality acceptance.</p>
<button id="pause">Pause</button><button id="step">Next phase</button>
<span id="phase"></span><br><canvas id="canvas" width="960" height="440"></canvas>
<script>
const clips=PAYLOAD, ctx=document.querySelector('canvas').getContext('2d');
const images=clips.map(c=>c.frames.map(f=>{const i=new Image;i.src='../../../../'+f.path;return i}));
let paused=false,phase=0,last=performance.now(),elapsed=0;
document.querySelector('#pause').onclick=()=>{paused=!paused;document.querySelector('#pause').textContent=paused?'Play':'Pause'};
document.querySelector('#step').onclick=()=>{paused=true;phase=(phase+1)%12;document.querySelector('#pause').textContent='Play'};
function draw(now){const dt=now-last;last=now;if(!paused){elapsed+=Math.min(dt,100);while(elapsed>=1000/24){phase=(phase+1)%12;elapsed-=1000/24;}}
ctx.clearRect(0,0,960,440);ctx.fillStyle='#d5dfdc';ctx.font='16px system-ui';
clips.forEach((c,col)=>{const f=c.frames[phase],img=images[col][phase];if(img.complete)ctx.drawImage(img,col*240+120-f.pixelWidth*f.pivotX,404-f.pixelHeight*(1-f.pivotY));ctx.fillText(c.id.split('.').pop(),col*240+16,430)});
document.querySelector('#phase').textContent='Phase '+phase+' / 12';requestAnimationFrame(draw)}requestAnimationFrame(draw);
</script>""".replace("PAYLOAD", payload), encoding="utf-8", newline="\n")


def main():
    REGIONS.mkdir(parents=True, exist_ok=True)
    REVIEW.mkdir(parents=True, exist_ok=True)
    officer_clips = [make_frames(direction) for direction in DIRECTIONS]
    machine_clips = [make_machine("sirs", "h", 1.4), make_machine("add", "w", 1.25),
                     make_machine("rover", "w", 1.0)]
    action_clips = [clip for direction in ("east", "south", "west") for clip in make_fire(direction)]
    clips = officer_clips + machine_clips + action_clips
    data = {"version": 1, "referenceCellPixels": CELL, "pivotOrigin": "bottom-left", "clips": clips}
    (MOTION / "clips.json").write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")
    contacts(officer_clips)
    contacts(machine_clips, "machines-walk")
    contacts([c for c in action_clips if ".fire." in c["id"]], "officer-fire")
    editable(clips)
    html(officer_clips)
    sources = [{"path": p.name, "sha256": sha(p)} for p in sorted(SOURCE.iterdir()) if p.is_file()]
    (SOURCE / "HASHES.json").write_text(json.dumps({"files": [s for s in sources if s["path"] != "HASHES.json"]}, indent=2) + "\n", encoding="utf-8", newline="\n")
    bindings = sum(len(c['frames']) for c in clips)
    unique = len({f['path'] for c in clips for f in c['frames']})
    print(f"Exported {len(clips)} clips / {bindings} frame bindings / {unique} painted regions")


if __name__ == "__main__":
    main()
