"""Pack retained proof exports without changing their pixels, bounds or foot origins.

Python 3.11 + Pillow. Run after both specialist source exporters. Source drawings
remain untouched. Runtime receives padded atlas pages and authored clip metadata.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "assets"
PROOF = ASSETS / "art/proof"
PAGE = 2048
PAD = 3

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def asset(path):
    result = (ASSETS / path).resolve()
    if not result.is_relative_to(ASSETS.resolve()) or not result.is_file():
        raise ValueError(f"Invalid source asset: {path}")
    return result

def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8", newline="\n")

def pack(name):
    folder = PROOF / name
    source = folder / ("regions.json" if name == "environment" else "clips.json")
    document = json.loads(source.read_text(encoding="utf-8-sig"))
    entries = list(document.get("regions", []))
    for clip in document.get("clips", []):
        entries.extend(frame for frame in clip["frames"] if "path" in frame)
    paths = sorted({entry["path"] for entry in entries})
    images = {path: Image.open(asset(path)).convert("RGBA") for path in paths}
    records, pages = {}, []
    page = Image.new("RGBA", (PAGE, PAGE))
    x = y = PAD
    row = 0
    for path in paths:
        image = images[path]
        w, h = image.size
        if max(w, h) + PAD * 2 > PAGE:
            raise ValueError(f"Frame exceeds atlas page: {path}")
        if x + w + PAD > PAGE:
            x, y, row = PAD, y + row + PAD * 2, 0
        if y + h + PAD > PAGE:
            pages.append(page)
            page = Image.new("RGBA", (PAGE, PAGE))
            x = y = PAD
            row = 0
        page.paste(image, (x, y))
        # Edge extrusion prevents linear filtering from sampling transparent gutters.
        for border in range(1, PAD):
            page.paste(image.crop((0, 0, w, 1)), (x, y-border))
            page.paste(image.crop((0, h-1, w, h)), (x, y+h-1+border))
            page.paste(image.crop((0, 0, 1, h)), (x-border, y))
            page.paste(image.crop((w-1, 0, w, h)), (x+w-1+border, y))
        for dx in range(1, PAD):
            for dy in range(1, PAD):
                for sx, sy, tx, ty in ((0,0,x-dx,y-dy),(w-1,0,x+w-1+dx,y-dy),(0,h-1,x-dx,y+h-1+dy),(w-1,h-1,x+w-1+dx,y+h-1+dy)):
                    page.putpixel((tx,ty),image.getpixel((sx,sy)))
        records[path] = (len(pages), x, y, w, h)
        x += w + PAD * 2
        row = max(row, h)
    if paths:
        pages.append(page)
    output = folder / "atlases"
    output.mkdir(exist_ok=True)
    page_records = []
    for index, image in enumerate(pages):
        path = output / f"page-{index:02d}.png"
        image.save(path, compress_level=9)
        page_records.append({"path": path.relative_to(ASSETS).as_posix(), "sha256": sha(path), "width": PAGE, "height": PAGE})
    for entry in entries:
        original = entry["path"]
        original_hash = sha(asset(original))
        if entry.get("sha256") and entry["sha256"] != original_hash:
            raise ValueError(f"Specialist source hash changed: {original}")
        index, x, y, w, h = records[original]
        entry.update(sourcePath=original, sourceSha256=original_hash, path=page_records[index]["path"], sha256=page_records[index]["sha256"], x=x, y=y, pixelWidth=w, pixelHeight=h)
    document["atlasPages"] = page_records
    document["sourceManifest"] = {"path": source.relative_to(ASSETS).as_posix(), "sha256": sha(source)}
    write_json(folder / "atlas.json", document)
    return {"name": name, "manifest": (folder / "atlas.json").relative_to(ASSETS).as_posix(), "sha256": sha(folder / "atlas.json"), "pages": len(pages), "uniqueSourceImages": len(paths), "clips": len(document.get("clips", []))}

def main():
    packs = [pack(name) for name in ("environment", "painted", "rendered")]
    write_json(PROOF / "catalog.json", {"version": 1, "status": "comparison-candidates-owner-selection-open", "packs": packs})
    coverage = []
    for style in ("painted", "rendered"):
        document = json.loads((PROOF/style/"clips.json").read_text(encoding="utf-8-sig"))
        bound = {(b.get("weapon", "pistol"), b.get("armor", "suit"), b["action"], b["facing"]): b["clip"] for b in document.get("bindings", [])}
        for weapon in ("unarmed", "pistol", "rifle", "scattergun"):
            for armor in ("suit", "light", "heavy"):
                for action in ("idle", "walk", "ready", "aim", "fire", "hit", "work", "death"):
                    for facing in ("north", "east", "south", "west"):
                        clip = bound.get((weapon, armor, action, facing), "")
                        coverage.append(dict(style=style, weapon=weapon, armor=armor, action=action, facing=facing, status="candidate" if clip else "missing", clip=clip))
    write_json(PROOF / "coverage.json", {"status": "coverage-only-not-artistic-acceptance", "bindings": coverage})
    print(json.dumps(packs, indent=2))

if __name__ == "__main__":
    main()
