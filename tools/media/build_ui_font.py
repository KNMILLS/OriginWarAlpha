"""Build the UI bitmap font from retained OFL Liberation Sans2.1.5 using Pillow.
The 60px source is filtered down to a20px logical UI size, independently of pixel art.
"""
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from PIL import __version__ as pillow_version, features

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'assets/ui'
SOURCE = OUT / 'source/LiberationSans-Regular.ttf'
EXPECTED = '76d04c18ea243f426b7de1f3ad208e927008f961dc5945e5aad352d0dfde8ee8'
assert hashlib.sha256(SOURCE.read_bytes()).hexdigest() == EXPECTED
size = 60
font = ImageFont.truetype(str(SOURCE), size)
ascent, descent = font.getmetrics()
chars = list(range(32, 127)) + list(range(160, 256)) + [0x2013, 0x2014, 0x2018, 0x2019, 0x201C, 0x201D, 0x2026, 0x2192]
atlas = Image.new('RGBA', (1024, 1024), (255, 255, 255, 0))
draw = ImageDraw.Draw(atlas)
lines = [f'info face="OriginWarUI" size={size} bold=0 italic=0 charset="" unicode=1 stretchH=100 smooth=1 aa=1 padding=0,0,0,0 spacing=0,0',
         f'common lineHeight={ascent+descent+5} base={ascent} scaleW=1024 scaleH=1024 pages=1 packed=0',
         'page id=0 file="ui.png"', f'chars count={len(chars)}']
x = y = 4
row = 0
for code in chars:
    value = chr(code)
    left, top, right, bottom = font.getbbox(value, anchor='ls')
    width, height = right-left, bottom-top
    if x + width + 4 > atlas.width:
        x = 4
        y += row + 8
        row = 0
    assert y+height+4 < atlas.height
    if width and height:
        draw.text((x-left, y-top), value, font=font, fill='white', anchor='ls')
    lines.append(f'char id={code} x={x} y={y} width={width} height={height} xoffset={left} yoffset={ascent+top} xadvance={round(font.getlength(value))} page=0 chnl=15')
    x += max(1, width) + 8
    row = max(row, height)
OUT.mkdir(exist_ok=True, parents=True)
atlas.save(OUT/'ui.png', optimize=True)
(OUT/'ui.fnt').write_text('\n'.join(lines)+'\n', encoding='utf-8', newline='\n')
manifest = {'font': 'OriginWarUI bitmap derivative of Liberation Sans2.1.5 Regular',
    'license': 'SIL OFL1.1; licenses/font/LICENSE-OFL-1.1', 'source_sha256': EXPECTED,
    'upstream_archive': 'https://github.com/liberationfonts/liberation-fonts/files/7261482/liberation-fonts-ttf-2.1.5.tar.gz',
    'upstream_archive_sha256': '7191c669bf38899f73a2094ed00f7b800553364f90e2637010a69c0e268f25d0',
    'source_size_px': size, 'logical_default_px': 20, 'characters': chars,
    'export_runtime': {'Pillow': pillow_version, 'FreeType': features.version_module('freetype2')},
    'runtime_filter': 'Linear; world artwork remains Nearest',
    'outputs': {p: hashlib.sha256((OUT/p).read_bytes()).hexdigest() for p in ['ui.png', 'ui.fnt']}}
(OUT/'manifest.json').write_text(json.dumps(manifest, indent=2)+'\n', encoding='utf-8', newline='\n')
print('Built high-resolution UI font:', len(chars), 'glyphs; source SHA verified')
