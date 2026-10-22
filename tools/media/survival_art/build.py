"""Repeatable extraction of original generated art; never paints production pixels.

Requires Pillow, numpy, scipy. Run from any directory with Python 3.11+.
Components isolate neighboring sprites; the retained antialias fringe is copied from
the original source, and original masters are never modified.
"""
from __future__ import annotations

import hashlib
import io
import json
import zipfile
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[3]
ART = ROOT / "assets/art/survival"
SOURCE = ART / "source"
REGIONS = ART / "regions"
REVIEW = ART / "review"
REFERENCE_CELL = 256

# Component numbers refer to alpha>=24, four-connected, scanline-label ordering.
# Each tuple: name, source component, sizing axis, grid-unit size, display layer.
RECIPES = {
    "officer-turnaround.png": [
        ("officer.north", 1, "h", 1.3, "world"),
        ("officer.east", 2, "h", 1.3, "world"),
        ("officer.south", 3, "h", 1.3, "world"),
        ("officer.west", 4, "h", 1.3, "world"),
    ],
    "structure-master.png": [
        ("wall.bulkhead", 2, "w", 1.0, "world"),
        ("wall.corner", 1, "w", 1.0, "world"),
        ("wall.glass", 3, "w", 1.0, "world"),
        ("wall.panel", 4, "w", 1.0, "world"),
        ("door.closed", 5, "w", 1.0, "world"),
        ("door.open", 6, "w", 1.0, "world"),
        ("door.locked", 7, "w", 1.0, "world"),
        ("hatch.closed", 8, "w", 1.0, "ground"),
        ("terminal.console", 9, "h", 0.95, "world"),
        ("control.switch", 10, "h", 0.8, "world"),
        ("prop.cargo", 12, "w", 0.95, "world"),
        ("prop.workbench", 11, "w", 1.8, "world"),
    ],
    "machine-initial.png": [
        ("machine.sirs", 1, "h", 1.4, "world"),
        ("machine.add", 2, "w", 1.25, "world"),
        ("machine.rover", 3, "w", 1.0, "world"),
        ("machine.camera", 9, "w", 0.6, "world"),
        ("machine.turret", 8, "w", 1.0, "world"),
        ("machine.constructor", 7, "w", 2.0, "world"),
    ],
    "item-master.png": [
        ("item.oxygen", 1, "h", 0.5, "world"),
        ("item.cells", 3, "w", 0.45, "world"),
        ("item.medicine", 2, "h", 0.4, "world"),
        ("item.pistol", 4, "w", 0.5, "world"),
        ("item.rifle", 7, "w", 0.9, "world"),
        ("item.scattergun", 8, "w", 0.8, "world"),
        ("item.armor-light", 6, "h", 0.7, "world"),
        ("item.armor-heavy", 5, "h", 0.75, "world"),
        ("item.probe", 11, "w", 0.55, "world"),
        ("item.brace", 9, "w", 0.65, "world"),
        ("prop.tool-roll", 10, "w", 0.55, "ground"),
        ("prop.records", 12, "w", 0.5, "ground"),
    ],
    "alice-master.png": [
        ("alice.avatar", 1, "h", 2.0, "avatar"),
        ("alice.concerned", 2, "h", 2.0, "avatar"),
    ],
}
FLOORS = ["security", "habitation", "medical", "industrial", "command", "low-gravity"]


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def extract(image: Image.Image, labels: np.ndarray, component: int):
    target = labels == component
    if int(target.sum()) < 500:
        raise ValueError(f"Component {component} is missing or implausibly small")
    # Retain original 3px antialias fringe, excluding any other sprite component.
    fringe = ndimage.binary_dilation(target, iterations=3)
    data = np.array(image)
    data[:, :, 3] = np.where(fringe, data[:, :, 3], 0)
    ys, xs = np.where(fringe & (data[:, :, 3] > 0))
    box = (int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1)
    return Image.fromarray(data).crop(box), box, int(target.sum())


def save_region(name, image, axis, units, layer, source, extra=None):
    if axis == "floor":
        width = height = REFERENCE_CELL
        pivot_x = pivot_y = 0.0
    else:
        ratio = units * REFERENCE_CELL / (image.width if axis == "w" else image.height)
        width = max(1, round(image.width * ratio))
        height = max(1, round(image.height * ratio))
        pivot_x, pivot_y = 0.5, 0.0
    output = REGIONS / f"{name}.png"
    image.convert("RGBA").resize((width, height), Image.Resampling.LANCZOS).save(output)
    data = dict(name=name, path=f"art/survival/regions/{name}.png",
                widthCells=width / REFERENCE_CELL, heightCells=height / REFERENCE_CELL,
                pivotX=pivot_x, pivotY=pivot_y, layer=layer,
                pixelWidth=width, pixelHeight=height, source=source,
                sha256=sha(output))
    if extra:
        data.update(extra)
    return data


def technical_contacts(regions):
    # Technical review annotations/backgrounds only; artwork is composited unchanged.
    font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 15)
    columns, cell_w, cell_h = 6, 220, 210
    rows = (len(regions) + columns - 1) // columns
    sheet = Image.new("RGBA", (columns * cell_w, rows * cell_h), (24, 29, 30, 255))
    draw = ImageDraw.Draw(sheet)
    for index, region in enumerate(regions):
        image = Image.open(REGIONS / (region["name"] + ".png"))
        image.thumbnail((192, 166), Image.Resampling.LANCZOS)
        x, y = (index % columns) * cell_w, (index // columns) * cell_h
        sheet.alpha_composite(image, (x + (cell_w - image.width) // 2, y + 26 + (166 - image.height) // 2))
        draw.text((x + 8, y + 5), region["name"], font=font, fill=(212, 221, 215))
    sheet.convert("RGB").save(REVIEW / "all-regions-contact.jpg", quality=94)

    canvas = Image.new("RGBA", (1024, 704), (15, 19, 21, 255))
    draw = ImageDraw.Draw(canvas)
    draw.text((18, 12), "SH-ART TECHNICAL COMPOSITE - not runtime / lighting / collision evidence", font=font, fill=(214, 224, 217))
    lookup = {r["name"]: r for r in regions}
    cell, ox, oy = 88, 72, 105

    def place(name, gx, gy):
        r = lookup[name]
        image = Image.open(REGIONS / (name + ".png"))
        w, h = round(r["widthCells"] * cell), round(r["heightCells"] * cell)
        image = image.resize((w, h), Image.Resampling.LANCZOS)
        # World/ground objects here use a visible ground contact at bottom center.
        x, y = round(ox + gx * cell - w * r["pivotX"]), round(oy + gy * cell - h * (1 - r["pivotY"]))
        canvas.alpha_composite(image, (x, y))

    floor = Image.open(REGIONS / "floor.security.png").resize((cell, cell), Image.Resampling.LANCZOS)
    for x in range(10):
        for y in range(6):
            canvas.alpha_composite(floor, (ox + x * cell, oy + y * cell))
    for x in range(10):
        place("wall.panel" if x == 2 else "door.open" if x == 6 else "wall.bulkhead", x + 0.5, 1)
    objects = [("terminal.console", 2, 2.8), ("prop.workbench", 8, 2.8),
               ("control.switch", 5.4, 3.0), ("officer.south", 4.1, 4.2),
               ("machine.sirs", 7.4, 4.3), ("prop.cargo", 1.4, 4.5),
               ("item.oxygen", 3, 5.0), ("item.cells", 3.7, 5.0)]
    for name, gx, gy in sorted(objects, key=lambda t: t[2]):
        place(name, gx, gy)
    canvas.convert("RGB").save(REVIEW / "technical-room-composite.jpg", quality=95)


def editable_catalog(regions):
    """One asset per editable OpenRaster layer, not invented brushstroke layers."""
    columns, pitch = 6, 560
    rows = (len(regions) + columns - 1) // columns
    root = ET.Element("image", {"version": "0.0.1", "w": str(columns * pitch),
                                "h": str(rows * pitch), "name": "SH-ART reference-cell kit"})
    stack = ET.SubElement(root, "stack")
    merged = Image.new("RGBA", (columns * pitch, rows * pitch))
    with zipfile.ZipFile(ART / "editable-kit.ora", "w", compression=zipfile.ZIP_DEFLATED) as archive:
        # Fixed timestamps make the editable source archive byte repeatable too.
        def write(path, data, compression=zipfile.ZIP_DEFLATED):
            item = zipfile.ZipInfo(path, date_time=(2026, 10, 21, 0, 0, 0))
            item.compress_type = compression
            archive.writestr(item, data)
        write("mimetype", b"image/openraster", zipfile.ZIP_STORED)
        for index, region in enumerate(regions):
            image = Image.open(REGIONS / (region["name"] + ".png"))
            x, y = (index % columns) * pitch + 10, (index // columns) * pitch + 10
            path = f"data/layer{index:03d}.png"
            write(path, (REGIONS / (region["name"] + ".png")).read_bytes())
            ET.SubElement(stack, "layer", {"name": region["name"], "src": path,
                          "x": str(x), "y": str(y), "opacity": "1.0",
                          "visibility": "visible", "composite-op": "svg:src-over"})
            merged.alpha_composite(image, (x, y))
        write("stack.xml", ET.tostring(root, encoding="utf-8", xml_declaration=True))
        data = io.BytesIO()
        merged.save(data, format="PNG")
        write("mergedimage.png", data.getvalue())


def main():
    REGIONS.mkdir(parents=True, exist_ok=True)
    REVIEW.mkdir(parents=True, exist_ok=True)
    regions = []
    floor = Image.open(SOURCE / "floor-master.png").convert("RGBA")
    if floor.size != (1536, 1024):
        raise ValueError("Unexpected floor master size")
    for index, name in enumerate(FLOORS):
        x, y = (index % 3) * 512, (index // 3) * 512
        box = (x, y, x + 512, y + 512)
        regions.append(save_region("floor." + name, floor.crop(box), "floor", 1, "ground",
                                   "floor-master.png", {"crop": box}))
    for source, specs in RECIPES.items():
        image = Image.open(SOURCE / source).convert("RGBA")
        labels, _ = ndimage.label(np.array(image)[:, :, 3] >= 24)
        for name, component, axis, units, layer in specs:
            selected, box, pixels = extract(image, labels, component)
            regions.append(save_region(name, selected, axis, units, layer, source,
                                       dict(component=component, crop=box, sourceBodyPixels=pixels)))
    manifest = dict(version=1, referenceCellPixels=REFERENCE_CELL, pivotOrigin="bottom-left",
                    pathRoot="assets", artistMode="original built-in image_gen; deterministic extraction only",
                    regions=regions)
    (ART / "regions.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8", newline="\n")
    technical_contacts(regions)
    editable_catalog(regions)
    print(f"Exported {len(regions)} named regions; review contact and technical composite saved.")


if __name__ == "__main__":
    main()
