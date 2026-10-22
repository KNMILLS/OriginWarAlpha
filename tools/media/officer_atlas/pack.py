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


def place_images(images, order):
    """Deterministic MaxRects placement without rotation or resampling."""
    free_pages, placements = [], {}
    for key in order:
        width, height = images[key].size
        w, h = width + PAD*2, height + PAD*2
        if max(w, h) > PAGE:
            raise ValueError("Production frame exceeds page")
        best = None
        for index, free in enumerate(free_pages):
            for x,y,fw,fh in free:
                if w <= fw and h <= fh:
                    score=(min(fw-w,fh-h),max(fw-w,fh-h),index,y,x)
                    if best is None or score < best[0]:
                        best=(score,index,x,y)
        if best is None:
            index=len(free_pages);free_pages.append([(0,0,PAGE,PAGE)]);x=y=0
        else:
            _,index,x,y=best
        split=[]
        for fx,fy,fw,fh in free_pages[index]:
            if x>=fx+fw or x+w<=fx or y>=fy+fh or y+h<=fy:
                split.append((fx,fy,fw,fh));continue
            if x>fx:split.append((fx,fy,x-fx,fh))
            if x+w<fx+fw:split.append((x+w,fy,fx+fw-x-w,fh))
            if y>fy:split.append((fx,fy,fw,y-fy))
            if y+h<fy+fh:split.append((fx,y+h,fw,fy+fh-y-h))
        split=sorted(set(split))
        free_pages[index]=[a for a in split if not any(a!=b and a[0]>=b[0] and a[1]>=b[1] and a[0]+a[2]<=b[0]+b[2] and a[1]+a[3]<=b[1]+b[3] for b in split)]
        placements[key]=(index,x+PAD,y+PAD,width,height)
    return len(free_pages),placements


def pack(folder=FOLDER, kind="officer"):
    manifest = folder / "clips.json"
    data = json.loads(manifest.read_text(encoding="utf-8-sig"))
    regions_manifest = folder / "regions.json"
    if kind == "station" and regions_manifest.is_file():
        animated_paths = {f["path"] for c in data["clips"] for f in c["frames"]}
        data["regions"] = list(data.get("regions", [])) + [r for r in json.loads(regions_manifest.read_text(encoding="utf-8-sig"))["regions"] if r["path"] not in animated_paths]
        data["sourceRegionsManifest"] = dict(path=regions_manifest.relative_to(ASSETS).as_posix(), sha256=sha(regions_manifest))
    frames = list(data.get("regions", [])) + [frame for clip in data["clips"] for frame in clip["frames"]]
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
    # Compare three fixed orders; all preserve pixels, origins and transparent padding.
    orders=[sorted(images,key=lambda k:(-k[0][0]*k[0][1],-max(k[0]),k[1])),sorted(images,key=lambda k:(-k[0][1],-k[0][0],k[1])),sorted(images,key=lambda k:(-max(k[0]),-min(k[0]),k[1]))]
    count,placements=min((place_images(images,order) for order in orders),key=lambda result:result[0])
    pages=[Image.new("RGBA",(PAGE,PAGE)) for _ in range(count)]
    for key,(index,x,y,w,h) in placements.items():extrude(pages[index],images[key],x,y)
    if len(pages) > MAX_PAGES:
        raise ValueError(f"{kind} texture budget exceeded: {len(pages)} > {MAX_PAGES} pages; revise packing/residency before expanding")
    output = folder / "atlases"
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
    data.update(atlasPages=records, sourceManifest=dict(path=manifest.relative_to(ASSETS).as_posix(), sha256=sha(manifest)), memoryBudget=dict(maxPages=MAX_PAGES, maxRgbaBytes=MAX_PAGES*PAGE*PAGE*4, actualRgbaBytes=len(pages)*PAGE*PAGE*4, residency="selected production candidate only"))
    write(folder / "atlas.json", data)
    if kind == "officer":
        coverage = officer_coverage(data)
    else:
        coverage = [dict(b, status="candidate") for b in data["bindings"]]
    write(folder / "coverage.json",dict(status="candidate-not-artistic-acceptance",bindings=coverage))
    summary=dict(clips=len(data["clips"]),sourceFrames=len(frames),uniqueCrops=len(images),pages=len(pages),rgbaBytes=len(pages)*PAGE*PAGE*4)
    print(json.dumps(summary,indent=2))
    return summary


def officer_coverage(data):
    bound = {(b["weapon"], b["armor"], b["action"], b["facing"]): b["clip"] for b in data["bindings"]}
    coverage=[]
    for weapon in ("unarmed","pistol","rifle","scattergun"):
        for armor in ("suit","light","heavy"):
            for action in ("idle","start","walk","stop","ready","aim","fire","melee","work","hit","death","turn_from_north","turn_from_east","turn_from_south","turn_from_west"):
                for facing in ("north","east","south","west"):
                    if action == "turn_from_" + facing:
                        continue
                    clip=bound.get((weapon,armor,action,facing),"")
                    applicable=not(weapon=="unarmed" and action in ("aim","fire"))
                    opening=armor=="suit" and weapon in ("unarmed","pistol") and applicable
                    coverage.append(dict(weapon=weapon,armor=armor,action=action,facing=facing,openingReachable=opening,status="not-applicable" if not applicable else "candidate" if clip else "missing",clip=clip))
    return coverage


if __name__ == "__main__":
    pack()
