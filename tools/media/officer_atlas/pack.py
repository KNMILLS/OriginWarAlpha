"""Lossless, foot-anchor preserving production atlases. Does not repaint pixels."""
from pathlib import Path
import hashlib
import json
import math
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "assets"
FOLDER = ASSETS / "art/production/officer"
PAGE = 2048
PAD = 3
MAX_PAGES = 8


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")


def source(path):
    result = (ASSETS / path).resolve()
    if not result.is_relative_to(ASSETS.resolve()) or not result.is_file():
        raise ValueError(f"Invalid production source {path}")
    return result


def trim(image, entry):
    """Keep alpha bounds plus the authored origin; record exact source coordinates."""
    width, height = image.size
    bounds = image.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("Empty actor frame")
    anchor_x = entry.get("pivotX", .5) * width
    anchor_y = (1 - entry.get("pivotY", 0)) * height
    left = max(0, min(bounds[0] - 2, math.floor(anchor_x)))
    top = max(0, min(bounds[1] - 2, math.floor(anchor_y)))
    right = min(width, max(bounds[2] + 2, math.ceil(anchor_x)))
    bottom = min(height, max(bounds[3] + 2, math.ceil(anchor_y)))
    w, h = right - left, bottom - top
    metadata = {
        "sourceCanvas": [width, height], "trim": [left, top, right, bottom],
        "sourceWidthCells": entry["widthCells"], "sourceHeightCells": entry["heightCells"],
        "sourcePivotX": entry.get("pivotX", .5), "sourcePivotY": entry.get("pivotY", 0),
        "widthCells": entry["widthCells"] * w / width,
        "heightCells": entry["heightCells"] * h / height,
        "pivotX": (anchor_x - left) / w,
        "pivotY": (bottom - anchor_y) / h,
    }
    return image.crop((left, top, right, bottom)), metadata


def extrude(page, image, x, y):
    w, h = image.size
    page.paste(image, (x, y))
    for n in range(1, PAD):
        page.paste(image.crop((0, 0, w, 1)), (x, y-n))
        page.paste(image.crop((0, h-1, w, h)), (x, y+h-1+n))
        page.paste(image.crop((0, 0, 1, h)), (x-n, y))
        page.paste(image.crop((w-1, 0, w, h)), (x+w-1+n, y))
    for dx in range(1, PAD):
        for dy in range(1, PAD):
            for sx, sy, tx, ty in ((0,0,x-dx,y-dy),(w-1,0,x+w-1+dx,y-dy),(0,h-1,x-dx,y+h-1+dy),(w-1,h-1,x+w-1+dx,y+h-1+dy)):
                page.putpixel((tx,ty), image.getpixel((sx,sy)))


def pack():
    manifest = FOLDER / "clips.json"
    data = json.loads(manifest.read_text(encoding="utf-8-sig"))
    frames = [frame for clip in data["clips"] for frame in clip["frames"]]
    images, placements, aliases, metadata = {}, {}, {}, {}
    for frame in frames:
        original = source(frame["path"])
        if frame.get("sha256") and sha(original) != frame["sha256"]:
            raise ValueError(f"Changed source {original}")
        with Image.open(original) as opened:
            cropped, meta = trim(opened.convert("RGBA"), frame)
        key = (cropped.size, hashlib.sha256(cropped.tobytes()).hexdigest())
        images.setdefault(key, cropped)
        aliases[frame["path"]] = key
        if frame["path"] in metadata and metadata[frame["path"]] != meta:
            raise ValueError(f"Inconsistent source bounds/anchor for {frame['path']}")
        metadata[frame["path"]] = meta
    # Larger first, deterministic shelf packing; identical pixels share a placement.
    pages, page = [], Image.new("RGBA", (PAGE, PAGE))
    x = y = PAD
    row = 0
    for key in sorted(images, key=lambda k: (-k[0][1], -k[0][0], k[1])):
        image = images[key]
        w, h = image.size
        if max(w,h) + PAD*2 > PAGE:
            raise ValueError("Production frame exceeds page")
        if x + w + PAD > PAGE:
            x, y, row = PAD, y + row + PAD*2, 0
        if y + h + PAD > PAGE:
            pages.append(page)
            page, x, y, row = Image.new("RGBA", (PAGE,PAGE)), PAD, PAD, 0
        extrude(page, image, x, y)
        placements[key] = (len(pages), x, y, w, h)
        x += w + PAD*2
        row = max(row, h)
    pages.append(page)
    if len(pages) > MAX_PAGES:
        raise ValueError(f"Officer texture budget exceeded: {len(pages)} > {MAX_PAGES} pages; revise packing/residency before expanding")
    output = FOLDER / "atlases"
    output.mkdir(exist_ok=True)
    records = []
    for i, page in enumerate(pages):
        path = output / f"page-{i:02d}.png"
        page.save(path, compress_level=9)
        records.append(dict(path=path.relative_to(ASSETS).as_posix(), sha256=sha(path), width=PAGE, height=PAGE))
    for frame in frames:
        path = frame["path"]
        index, x, y, w, h = placements[aliases[path]]
        frame.update(metadata[path], sourcePath=path, sourceSha256=sha(source(path)), path=records[index]["path"], sha256=records[index]["sha256"], x=x, y=y, pixelWidth=w, pixelHeight=h)
    data.update(atlasPages=records, sourceManifest=dict(path=manifest.relative_to(ASSETS).as_posix(), sha256=sha(manifest)), memoryBudget=dict(maxPages=MAX_PAGES, maxRgbaBytes=MAX_PAGES*PAGE*PAGE*4, actualRgbaBytes=len(pages)*PAGE*PAGE*4, residency="selected officer candidate only"))
    write(FOLDER / "atlas.json", data)
    bound = {(b["weapon"], b["armor"], b["action"], b["facing"]): b["clip"] for b in data["bindings"]}
    coverage=[]
    for weapon in ("unarmed","pistol","rifle","scattergun"):
        for armor in ("suit","light","heavy"):
            for action in ("idle","start","walk","stop","ready","aim","fire","melee","work","hit","death"):
                for facing in ("north","east","south","west"):
                    clip=bound.get((weapon,armor,action,facing),"")
                    opening=armor=="suit" and weapon in ("unarmed","pistol")
                    coverage.append(dict(weapon=weapon,armor=armor,action=action,facing=facing,openingReachable=opening,status="candidate" if clip else "missing",clip=clip))
    write(FOLDER / "coverage.json",dict(status="candidate-not-artistic-acceptance",bindings=coverage))
    summary=dict(clips=len(data["clips"]),sourceFrames=len(frames),uniqueCrops=len(images),pages=len(pages),rgbaBytes=len(pages)*PAGE*PAGE*4)
    print(json.dumps(summary,indent=2))
    return summary


if __name__ == "__main__":
    pack()
