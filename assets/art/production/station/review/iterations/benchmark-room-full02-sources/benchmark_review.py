"""Compose existing exported pixels at gameplay scale; does not author assets."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json,sys

ROOT=Path(__file__).resolve().parents[3]
STATION=ROOT/'assets/art/production/station'
OUT=STATION/('review/iterations/benchmark-room-pilot03' if '--pilot03' in sys.argv else 'review/iterations/benchmark-room-pilot02' if '--pilot02' in sys.argv else 'review/iterations/benchmark-room-pilot01') if '--pilot' in sys.argv else STATION
REVIEW=OUT/'review';REVIEW.mkdir(parents=True,exist_ok=True)
FONT=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',17)
SMALL=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',13)
manifest=json.loads((OUT/'regions.json').read_text())
regions={r['name']:r for r in manifest['regions']}
baseline=json.loads((STATION/'review/iterations/benchmark-room-baseline/regions.json').read_text())
old={r['name']:r for r in baseline['regions']}

def sprite(board,name,anchor,cell,source=regions):
    r=source[name];path=ROOT/'assets'/r['path']
    if source is old:path=STATION/'review/iterations/benchmark-room-baseline/regions'/path.name
    im=Image.open(path).convert('RGBA')
    im=im.resize((round(r['widthCells']*cell),round(r['heightCells']*cell)),Image.Resampling.LANCZOS)
    board.alpha_composite(im,(round(anchor[0]-r['pivotX']*im.width),round(anchor[1]-(1-r['pivotY'])*im.height)))

board=Image.new('RGBA',(1280,910),'#1d2525');d=ImageDraw.Draw(board)
d.text((20,15),'BM-ROOM — CANDIDATE PIXELS AT GAMEPLAY CELL SIZES',font=FONT,fill='#dbd7c5')
d.text((20,42),'Exported source board only. Live lighting, native motion and owner finish acceptance remain separate.',font=SMALL,fill='#a9b5b2')
states=['closed','open','locked','braced','damaged']
for row,cell in enumerate([77,47]):
    base=310+row*250
    d.text((20,base-(220 if cell==77 else 155)),f'{cell}px cell — '+('1080p' if cell==77 else '720p')+' approximate ordinary scale',font=SMALL,fill='#e0dcca')
    for col,state in enumerate(states):
        x=100+col*145
        for step in [-1,0,1]:d.rectangle((x-cell/2,base+step*cell-cell/2,x+cell/2,base+step*cell+cell/2),outline='#384443')
        sprite(board,'door.'+state+'.east',(x,base),cell)
        d.text((x-32,base+65),state,font=SMALL,fill='#b6c0bd')
    for col,name in enumerate(['machine.sirs.idle.south','prop.inspection-terminal','prop.cargo-cradle']):
        x=850+col*145;sprite(board,name,(x,base),cell)
        d.text((x-50,base+65),name.replace('prop.','').replace('machine.',''),font=SMALL,fill='#b6c0bd')

d.text((20,685),'Material / shape diagnosis — 80px cell',font=FONT,fill='#dbd7c5')
for col,name in enumerate(['prop.receiving-bulkhead','prop.inspection-desk','prop.maintenance-bench','prop.inspection-terminal','prop.cargo-cradle','control.switch.powered']):
    x=110+col*210;sprite(board,name,(x,860),80)
    d.text((x-75,875),name.replace('prop.',''),font=SMALL,fill='#b6c0bd')
board.convert('RGB').save(REVIEW/'benchmark-room-scale.jpg',quality=96)
print(REVIEW/'benchmark-room-scale.jpg')

comparison=Image.new('RGBA',(1250,660),'#1d2525');d=ImageDraw.Draw(comparison)
d.text((20,15),'SIDE-ON DOOR — MATCHED BASELINE / CANDIDATE AT 77PX CELL',font=FONT,fill='#dbd7c5')
d.text((20,42),'Same orthographic camera, passage axis and gameplay footprint. Candidate uses a deeper pressure-bulkhead construction.',font=SMALL,fill='#a9b5b2')
for row,source in enumerate([old,regions]):
    baseline=295+row*295
    d.text((20,baseline-205),'BASELINE' if row==0 else 'CANDIDATE',font=SMALL,fill='#dbd7c5')
    for col,state in enumerate(states):
        x=140+col*240;sprite(comparison,'door.'+state+'.east',(x,baseline),77,source)
        d.text((x-35,baseline+30),state,font=SMALL,fill='#b6c0bd')
comparison.convert('RGB').save(REVIEW/'east-door-before-after.jpg',quality=96)

if 'effect.metal-impact.00' in regions:
    effects=Image.new('RGBA',(1100,490),'#1d2525');d=ImageDraw.Draw(effects)
    d.text((20,15),'AUTHORED CONTACT EFFECTS — 77PX CELL / SEVEN POSES / .245S',font=FONT,fill='#dbd7c5')
    for row,kind in enumerate(['metal-impact','suit-impact','sparks']):
        d.text((15,80+row*140),kind,font=SMALL,fill='#dbd7c5')
        for index in range(7):sprite(effects,f'effect.{kind}.{index:02}',(180+index*140,160+row*140),77)
    effects.convert('RGB').save(REVIEW/'material-effects-poses.jpg',quality=96)
