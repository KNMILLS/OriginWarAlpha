"""Structural inspection and faithful contact/GIF previews. Does not paint assets."""
from pathlib import Path
import json, hashlib
from PIL import Image, ImageDraw, ImageFont
import numpy as np
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/art/proof/rendered'; REVIEW=OUT/'review'; REVIEW.mkdir(exist_ok=True)
m=json.loads((OUT/'clips.json').read_text()); checks=[]
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',15)
def picture(frame): return Image.open(ROOT/'assets'/frame['path']).convert('RGBA')
def cell(im,size=(192,224)):
    bg=Image.new('RGBA',size,(19,26,27,255)); im=im.resize(size,Image.Resampling.LANCZOS); bg.alpha_composite(im); return bg
def sheet(clips,path):
    n=max(len(c['frames']) for c in clips); w=192*n; h=254*len(clips)
    out=Image.new('RGB',(w,h),(19,26,27)); d=ImageDraw.Draw(out)
    for y,c in enumerate(clips):
        d.text((8,y*254+4),c['id'],font=font,fill=(205,216,205))
        for x,f in enumerate(c['frames']): out.paste(cell(picture(f)),(x*192,y*254+26))
    out.save(REVIEW/path,quality=92)
for c in m['clips']:
    frames=c['frames']; arrays=[]; boxes=[]
    for f in frames:
        path=ROOT/'assets'/f['path']; im=picture(f)
        assert im.size==(f['pixelWidth'],f['pixelHeight'])==(384,448)
        assert hashlib.sha256(path.read_bytes()).hexdigest()==f['sha256']
        assert im.getextrema()[3][0]==0 and im.getextrema()[3][1]==255
        box=im.getbbox(); assert box and box[0]>1 and box[1]>1 and box[2]<383 and box[3]<447,(c['id'],box)
        assert f['pivotX']==.5 and 0<f['pivotY']<.25
        arrays.append(np.asarray(im)); boxes.append(box)
    if c['loop']:
        assert len(frames)==12 and abs(c['frameSeconds']-1/24)<1e-9
        assert len({hashlib.sha256(a.tobytes()).hexdigest() for a in arrays})==12
        assert any(np.count_nonzero(arrays[0][:,:,3]!=a[:,:,3])>80 for a in arrays[1:]),c['id']
        previews=[cell(picture(f)).convert('RGB') for f in frames]
        previews[0].save(REVIEW/(c['id']+'.gif'),save_all=True,append_images=previews[1:],duration=[40,40,50]*4,loop=0,disposal=2)
    checks.append({'id':c['id'],'frames':len(frames),'bounds':boxes,'alphaGeometryDistinct':len({hashlib.sha256(a[:,:,3].tobytes()).hexdigest() for a in arrays})})
sheet([c for c in m['clips'] if c['id'].startswith('officer.pistol.suit.idle.')],'officer-cardinal-idle.jpg')
sheet([c for c in m['clips'] if c['id'].startswith('officer.pistol.suit.walk.')],'officer-cardinal-walk.jpg')
sheet([c for c in m['clips'] if c['id'].startswith('officer.pistol.suit.fire.')],'officer-cardinal-fire.jpg')
sheet([c for c in m['clips'] if '.idle.' in c['id']],'equipment-armor-idle.jpg')
result={'technicalOnly':True,'artisticAcceptance':'OPEN','clips':len(m['clips']),'frames':sum(len(c['frames']) for c in m['clips']),'checks':checks,'limits':['No native runtime capture by worker','No commercial illustration-quality acceptance','No guarantee of planted feet at one-cell translation; native gait calibration remains open','No real operator equipment-action acceptance']}
(OUT/'validation.json').write_text(json.dumps(result,indent=2)+'\n',newline='\n')
print('PASS structural export:',result['clips'],'clips',result['frames'],'frames; artistic/native gates OPEN')
