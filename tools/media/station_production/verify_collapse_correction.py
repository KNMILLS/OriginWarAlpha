"""Verify the bounded corpse-sensor correction and retain matched pixel evidence."""
from pathlib import Path
import hashlib,json
from PIL import Image,ImageDraw,ImageFont

ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/station'
BEFORE=OUT/'review/iterations/benchmark-room-full02-rejected-sensor'
old=json.loads((BEFORE/'regions.json').read_text())
current=json.loads((OUT/'regions.json').read_text())
masters={r['name']:r for r in json.loads((BEFORE/'MASTER_HASHES.json').read_text())['masters']}
by_name={r['name']:r for r in current['regions']}
assert {r['name'] for r in old['regions']}==set(by_name)
unchanged=[];collapse=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
for region in old['regions']:
    name=region['name'];path=ROOT/'assets'/region['path']
    if name.startswith('machine.sirs.destroyed.'):
        before=Image.open(BEFORE/'regions'/path.name).convert('RGBA');after=Image.open(path).convert('RGBA')
        assert before.size==after.size,name
        index=int(name.rsplit('.',1)[1])
        if index<4:assert before.tobytes()==after.tobytes(),name
        collapse.append(dict(name=name,sensorDark=index>=4,rgbaPixelsChanged=before.tobytes()!=after.tobytes()))
    else:
        assert sha(path)==region['sha256'],name
        assert sha(OUT/'source'/masters[name]['path'])==masters[name]['sha256'],name
        unchanged.append(name)

report=dict(scope='bounded export/source verification; no native or artistic acceptance',totalRegions=len(by_name),byteIdenticalNoncollapseRasters=len(unchanged),byteIdenticalNoncollapseMasters=len(unchanged),rerenderedCollapseRasters=len(collapse),earlyCollapseRgbaPixelsPreserved=sum(not c['sensorDark'] for c in collapse),lateCollapsePixelsChanged=sum(c['rgbaPixelsChanged'] for c in collapse),checks=collapse,full02TotalSeconds=json.loads((BEFORE/'timing.json').read_text())['totalSeconds'],correctionTotalSeconds=json.loads((OUT/'review/timing.json').read_text())['totalSeconds'],sourceBlendSha256=sha(OUT/'source/origin-war-opening-station.blend'),currentBuildSha256=sha(ROOT/'tools/media/station_production/build.py'),currentMotionSha256=sha(ROOT/'tools/media/station_production/motion.py'))
(OUT/'review/collapse-correction-verification.json').write_text(json.dumps(report,indent=2)+'\n')

board=Image.new('RGBA',(1280,690),'#202724');d=ImageDraw.Draw(board);font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',16)
d.text((20,15),'SIRS FINAL COLLAPSE — MATCHED SOURCE PIXELS / SENSOR CUE CORRECTED',font=font,fill='#ddd8c5')
d.text((20,42),'160px cell diagnosis. This is not native gameplay or finish acceptance.',font=font,fill='#aeb6a9')
for row,label in enumerate(['full02 — rejected live amber cue','final — dark disabled sensor']):
    baseline=300+row*320;d.text((20,baseline-185),label,font=font,fill='#ddd8c5')
    for col,facing in enumerate(['north','east','south','west']):
        name=f'machine.sirs.destroyed.{facing}.09';r=by_name[name]
        path=ROOT/'assets'/r['path'] if row else BEFORE/'regions'/Path(r['path']).name
        im=Image.open(path).convert('RGBA');im=im.resize((round(r['widthCells']*160),round(r['heightCells']*160)),Image.Resampling.LANCZOS)
        anchor=160+col*310
        board.alpha_composite(im,(round(anchor-r['pivotX']*im.width),round(baseline-(1-r['pivotY'])*im.height)))
        d.text((anchor-30,baseline+35),facing,font=font,fill='#aeb6a9')
board.convert('RGB').save(OUT/'review/collapse-sensor-before-after.jpg',quality=96)
print(json.dumps({k:v for k,v in report.items() if k!='checks'}))
