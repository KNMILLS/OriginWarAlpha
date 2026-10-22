"""Inspect actual source renders, planted turns and floor-collapse bounds; no frame repair."""
from pathlib import Path
import json,hashlib,sys
import numpy as np
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/officer'
PILOT='--full' not in sys.argv;BODY='--body-look' in sys.argv
HERE=OUT/'review/iterations/v3-anatomical-suit' if BODY else OUT/'review/iterations/v2-pilot' if PILOT else OUT
REVIEW=HERE if PILOT else OUT/'review';m=json.loads((HERE/'clips.json').read_text())
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',14);back=(22,30,29)
def image(f):return Image.open(ROOT/'assets'/f['path']).convert('RGBA')
def stamp(bg,im,f,anchor,cell):
    width=round(f['widthCells']*cell);height=round(f['heightCells']*cell)
    im=im.resize((width,height),Image.Resampling.LANCZOS)
    bg.alpha_composite(im,(round(anchor[0]-f['pivotX']*width),round(anchor[1]-(1-f['pivotY'])*height)))
def thumb(im,height):
    im=im.crop(im.getbbox());return im.resize((round(im.width*height/im.height),height),Image.Resampling.LANCZOS)
checks=[];turn_checks=[]
byid={c['id']:c for c in m['clips']};endpoint_checks=[]
for c in m['clips']:
    for f in c['frames']:
        p=ROOT/'assets'/f['path'];im=image(f);b=im.getbbox()
        assert hashlib.sha256(p.read_bytes()).hexdigest()==f['sha256']
        assert im.size==(f['pixelWidth'],f['pixelHeight']) and b
        assert b[0]>1 and b[1]>1 and b[2]<im.width-1 and b[3]<im.height-1,(c['id'],b)
    if '.turn_from_' in c['id']:
        source=c['id'].split('.')[-2].removeprefix('turn_from_');destination=c['id'].split('.')[-1]
        for index,facing in [(0,source),(-1,destination)]:
            idle=byid.get(f'officer.{c["equipment"]}.suit.idle.{facing}')
            if idle:
                delta=np.abs(np.asarray(image(c['frames'][index])).astype(np.int16)-np.asarray(image(idle['frames'][0])).astype(np.int16))
                assert delta.max()==0,(c['id'],index,'neutral pose mismatch')
                endpoint_checks.append({'clip':c['id'],'frame':index,'maximumPixelChannelDifference':int(delta.max())})
        support_runs={}
        for f in c['frames']:
            for side,foot in f['feet'].items():
                if foot.get('stance') and 'placement' in foot:
                    support_runs.setdefault((side,foot['placement']),[]).append(foot['anchorWorld'])
        residual=max((float(np.max(np.linalg.norm(np.array(run)-run[0],axis=1))) for run in support_runs.values()),default=0)
        assert residual<1e-7,(c['id'],residual)
        turn_checks.append({'clip':c['id'],'sampledSupportResidualWorld':residual,'supportRuns':len(support_runs)})
    if '.turn_from_' not in c['id'] and '.death.' not in c['id']:continue
    previews=[];n=len(c['frames']);board=Image.new('RGB',(170*n,245),back);dd=ImageDraw.Draw(board)
    dd.text((6,5),c['id'],font=font,fill=(210,220,207))
    for i,f in enumerate(c['frames']):
        bg=Image.new('RGBA',(520,410),(*back,255));draw=ImageDraw.Draw(bg);cell=96
        for k in range(-4,8):
            draw.line((260+k*cell,0,260+k*cell,410),fill=(39,51,45))
            draw.line((0,260+k*cell,520,260+k*cell),fill=(39,51,45))
        stamp(bg,image(f),f,(260,260),cell)
        draw.text((10,10),c['id'],font=font,fill=(210,220,207))
        draw.text((10,385),f'Actual render {i+1}/{n}; source preview only',font=font,fill=(170,190,173))
        previews.append(bg.convert('RGB'))
        crop=Image.new('RGBA',(170,210),(*back,255));stamp(crop,image(f),f,(85,130),58)
        board.paste(crop.convert('RGB'),(i*170,30));dd.text((i*170+7,220),str(i),font=font,fill=(210,220,207))
    duration=[round(f.get('seconds',c['frameSeconds'])*1000) for f in c['frames']]
    previews[0].save(REVIEW/(c['id']+'.gif'),save_all=True,append_images=previews[1:],duration=duration,loop=0,disposal=2)
    board.save(REVIEW/(c['id']+'-contact.jpg'),quality=95)
    checks.append({'clip':c['id'],'frames':n,'opaqueBoundsFinal':list(image(c['frames'][-1]).getbbox())})
sheet=Image.new('RGB',(460,710) if BODY else (920,540),back);d=ImageDraw.Draw(sheet)
v1=OUT/'review/iterations/v1-delivered';painted=json.loads((ROOT/'assets/art/proof/painted/clips.json').read_text())
for col,direction in enumerate(['south','east'] if BODY else ['south','east','north','west']):
    x=col*230+12;d.text((x,8),direction,font=font,fill=(220,230,213))
    for row,label in enumerate(['painted target','v1 preserved','v2 shell pilot','v3 continuous suit'] if BODY else ['painted target','v1 preserved','v2 pilot']):
        d.text((x,row*170+28),label,font=font,fill=(180,196,180))
        if row==0:im=image(next(c for c in painted['clips'] if c['id']==f'officer.pistol.suit.idle.{direction}')['frames'][0])
        elif row==1:im=Image.open(v1/f'officer.pistol.suit.idle.{direction}.00.png').convert('RGBA')
        elif BODY and row==2:im=Image.open(OUT/f'review/iterations/v2-pilot/regions/officer.pistol.suit.idle.{direction}.00.png').convert('RGBA')
        else:im=image(next(c for c in m['clips'] if c['id']==f'officer.pistol.suit.idle.{direction}')['frames'][0])
        for height,dx in [(100,0),(60,112)]:
            p=thumb(im,height);sheet.paste(p,(x+dx,row*170+52),p)
sheet.save(REVIEW/'material-before-after-100px-60px.jpg',quality=96)
result={'scope':'Source renders and analytical geometry; not native movement or artistic acceptance','clips':len(m['clips']),'frames':sum(len(c['frames']) for c in m['clips']),'turnSupportChecks':turn_checks,'turnEndpointChecks':endpoint_checks,'reviewedClips':checks,'artisticAcceptance':'OPEN'}
(REVIEW/'v2-validation.json').write_text(json.dumps(result,indent=2)+'\n',newline='\n')
print('PASS actual hashes,canvas bounds, planted-turn support:',len(m['clips']),'clips',result['frames'],'frames')
