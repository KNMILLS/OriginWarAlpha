"""Matched ordinary-size source diagnostics. Never alters production pixels."""
from pathlib import Path
import json,sys
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/officer'
PILOT=OUT/'review/iterations'/('v4-motion' if '--motion' in sys.argv else 'v4-look')
prior_path=OUT/'review/iterations/v3-before-benchmark/clips.json'
prior=json.loads((prior_path if prior_path.exists() else OUT/'clips.json').read_text());new=json.loads((PILOT/'clips.json').read_text())
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',15)
def load(f):return Image.open(ROOT/'assets'/f['path']).convert('RGBA')
def thumb(im,height):
    im=im.crop(im.getbbox());return im.resize((round(im.width*height/im.height),height),Image.Resampling.LANCZOS)
sheet=Image.new('RGB',(980,550),(20,29,28));d=ImageDraw.Draw(sheet)
for col,facing in enumerate(['south','east','north','west']):
    x=25+col*242;d.text((x,10),facing,font=font,fill=(215,220,205))
    for row,(label,m) in enumerate([('current V3',prior),('V4 material pilot',new)]):
        c=next(c for c in m['clips'] if c['id']==f'officer.pistol.suit.idle.{facing}')
        d.text((x,45+row*245),label,font=font,fill=(177,193,177))
        for height,dx in [(150,0),(60,100),(40,175)]:
            im=thumb(load(c['frames'][0]),height);sheet.paste(im,(x+dx,75+row*245),im)
            d.text((x+dx,232+row*245),str(height)+'px',font=font,fill=(130,151,135))
sheet.save(PILOT/'matched-material-150-60-40px.jpg',quality=96)
if '--motion' in sys.argv:
    lookup={c['id']:c for c in new['clips']}
    for action,facing in [('walk','east'),('terminal','south'),('breaker','east'),('pickup','south'),('supply','east')]:
        c=lookup[f'officer.pistol.suit.{action}.{facing}'];pictures=[]
        contact=Image.new('RGB',(960,235),(20,29,28));contact_draw=ImageDraw.Draw(contact)
        for column,index in enumerate([round(i*(len(c['frames'])-1)/7) for i in range(8)]):
            im=thumb(load(c['frames'][index]),150);contact.paste(im,(column*120+60-im.width//2,35),im)
            contact_draw.text((column*120+10,10),f'{index*c["frameSeconds"]:.3f}s',font=font,fill=(180,195,179))
        contact_draw.text((15,205),action+' / same exported pixels at150px opaque height; timing labels are source exposures',font=font,fill=(180,195,179))
        contact.save(PILOT/(action+'-'+facing+'-poses.jpg'),quality=95)
        source_frames=[(i,f) for cycle in range(4 if action=='walk' else 1) for i,f in enumerate(c['frames'])]
        for sequence_index,(i,f) in enumerate(source_frames):
            bg=Image.new('RGBA',(640,360),(20,29,28,255));draw=ImageDraw.Draw(bg)
            progress=(sequence_index/len(c['frames']))*c.get('cycleCells',0)
            # Camera-following grid: four.75cell cycles travel three whole cells,
            # so the loop preserves world-grid phase instead of jumping the actor.
            grid_offset=round(progress*47)%47
            for x in range(-47,688,47):draw.line((x-grid_offset,20,x-grid_offset,320),fill=(31,42,38))
            for y in range(20,321,47):draw.line((0,y,640,y),fill=(31,42,38))
            im=load(f);scale=47/(192);im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.LANCZOS)
            anchor=(275,205)
            bg.alpha_composite(im,(round(anchor[0]-f['pivotX']*im.width),round(anchor[1]-(1-f['pivotY'])*im.height)))
            draw=ImageDraw.Draw(bg);draw.text((15,12),action+' / source preview / 47px cell',font=font,fill=(207,215,197))
            # Diagnostic enlargement uses precisely the same exported image.
            large=thumb(load(f),180);bg.alpha_composite(large,(470-large.width//2,90))
            draw.text((400,320),'diagnostic 180px',font=font,fill=(160,179,159))
            pictures.append(bg.convert('RGB'))
        durations=[round(1000*(i+1)*c['frameSeconds']/10)*10-round(1000*i*c['frameSeconds']/10)*10 for i in range(len(pictures))]
        pictures[0].save(PILOT/(action+'-'+facing+'.gif'),save_all=True,append_images=pictures[1:],duration=durations,loop=0,disposal=2)
print(PILOT/'matched-material-150-60-40px.jpg')
