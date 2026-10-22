"""Faithful source/camera diagnostics and previews; never changes exported frame pixels."""
from pathlib import Path
import json,hashlib
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/officer';REVIEW=OUT/'review';REVIEW.mkdir(exist_ok=True)
m=json.loads((OUT/'clips.json').read_text());font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',15)
def frame(f):return Image.open(ROOT/'assets'/f['path']).convert('RGBA')
def thumb(im,height):
    box=im.getbbox();im=im.crop(box);return im.resize((round(im.width*height/im.height),height),Image.Resampling.LANCZOS)
dirs=['south','east','north','west'];back=(22,30,29)
sheet=Image.new('RGB',(900,610),back);d=ImageDraw.Draw(sheet)
painted=json.loads((ROOT/'assets/art/proof/painted/clips.json').read_text())
old=json.loads((ROOT/'assets/art/proof/rendered/clips.json').read_text())
for col,direction in enumerate(dirs):
    x=40+col*215;d.text((x,8),direction,font=font,fill=(220,226,210))
    for row,(label,manifest) in enumerate([('painted target',painted),('prior rig',old),('new pilot',m)]):
        c=next(c for c in manifest['clips'] if c['id']==f'officer.pistol.suit.idle.{direction}')
        im=frame(c['frames'][0]);d.text((x,row*190+30),label,font=font,fill=(170,185,172))
        for height,dx in [(100,0),(60,100)]:
            p=thumb(im,height);sheet.paste(p,(x+dx,row*190+55),p)
sheet.save(REVIEW/'before-after-four-facings-100px-60px.jpg',quality=95)
checks=[]
for c in m['clips']:
    for f in c['frames']:
        im=frame(f);assert im.size==(384,448);assert hashlib.sha256((ROOT/'assets'/f['path']).read_bytes()).hexdigest()==f['sha256']
        b=im.getbbox();assert b and b[0]>1 and b[1]>1 and b[2]<383 and b[3]<447,(c['id'],b)
    if '.walk.' not in c['id'] and '.walk24.' not in c['id']:continue
    direction=c['id'].split('.')[-1];axis={'south':(0,1),'east':(1,0),'north':(0,-1),'west':(-1,0)}[direction]
    preview=[];n=len(c['frames']);cell=77;scale=cell/256
    for i,f in enumerate(c['frames']):
        # Two true cycles with root travel and source projected feet; moving world grid shows plants.
        forloop=1
        bg=Image.new('RGBA',(480,360),(*back,255));dd=ImageDraw.Draw(bg)
        for gx in range(-1,8):dd.line((gx*cell,0,gx*cell,360),fill=(37,48,44))
        for gy in range(-1,7):dd.line((0,gy*cell,480,gy*cell),fill=(37,48,44))
        im=frame(f);im=im.resize((round(384*scale),round(448*scale)),Image.Resampling.LANCZOS)
        progress=i/n;cycle=c.get('cycleCells',1);anchor=(240+axis[0]*cell*cycle*(progress-.5),180+axis[1]*cell*cycle*(progress-.5))
        xx=round(anchor[0]-f['pivotX']*im.width);yy=round(anchor[1]-(1-f['pivotY'])*im.height);bg.alpha_composite(im,(xx,yy))
        dd=ImageDraw.Draw(bg);dd.text((12,12),c['id'],font=font,fill=(205,216,201))
        dd.text((12,335),f'Root: {cycle}cell / {n*c["frameSeconds"]:.2f}s cycle',font=font,fill=(165,181,166))
        preview.append(bg.convert('RGB'))
    durations=[round(1000*c['frameSeconds'])]*n
    # A500ms cycle is represented accurately by alternating10/20ms GIF exposures for60Hz.
    if n==30:durations=[20,10,20]*10
    elif n==36:durations=[20,10,20]*12
    elif n==12:durations=[40,40,50]*4
    preview[0].save(REVIEW/(c['id']+'.gif'),save_all=True,append_images=preview[1:],duration=durations,loop=0,disposal=2)
    checks.append({'clip':c['id'],'distinctRenders':len({f['sha256'] for f in c['frames']}),'count':n})
for direction in ['south','east']:
    axis={'south':(0,1),'east':(1,0)}[direction];cell=77;scale=cell/256
    named={c['id'].split('.')[-2]:c for c in m['clips'] if c['id'].endswith('.'+direction)}
    start=named.get('start');stop=named.get('stop');walk=named.get('walk')
    if not all([start,stop,walk]):continue
    sequence=[(f,0) for f in start['frames']]
    for cycle in range(2):sequence.extend((f,(cycle+i/len(walk['frames']))*.5) for i,f in enumerate(walk['frames']))
    sequence.extend((f,1) for f in stop['frames'])
    preview=[]
    for f,distance in sequence:
        bg=Image.new('RGBA',(480,360),(*back,255));dd=ImageDraw.Draw(bg)
        for gx in range(-1,8):dd.line((gx*cell,0,gx*cell,360),fill=(37,48,44))
        for gy in range(-1,7):dd.line((0,gy*cell,480,gy*cell),fill=(37,48,44))
        im=frame(f).resize((round(384*scale),round(448*scale)),Image.Resampling.LANCZOS)
        anchor=(205+axis[0]*cell*distance,165+axis[1]*cell*distance)
        bg.alpha_composite(im,(round(anchor[0]-f['pivotX']*im.width),round(anchor[1]-(1-f['pivotY'])*im.height)))
        dd=ImageDraw.Draw(bg);dd.text((12,12),'Start / one-cell walk / stop / '+direction,font=font,fill=(205,216,201));dd.text((12,335),'Source preview; root1.2s +two0.167s transitions',font=font,fill=(165,181,166))
        preview.append(bg.convert('RGB'))
    durations=([20,10,20]*((len(preview)+2)//3))[:len(preview)]
    preview[0].save(REVIEW/f'planted-start-walk-stop-{direction}.gif',save_all=True,append_images=preview[1:],duration=durations,loop=0,disposal=2)
result={'scope':'Source render and analytical projection only; not native gameplay','artisticAcceptance':'OPEN','clips':len(m['clips']),'frames':sum(len(c['frames']) for c in m['clips']),'footCalibration':m['footCalibration'],'checked':checks}
(OUT/'validation.json').write_text(json.dumps(result,indent=2)+'\n',newline='\n')
print('PASS structural pilot',result['clips'],result['frames'],'renders; visual/native review OPEN')
