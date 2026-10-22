"""Export-only contact boards; never creates or repairs production artwork."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import json

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'assets/art/production/station'
REGIONS = OUT / 'regions'
REVIEW = OUT / 'review'
FONT = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 16)
SMALL = ImageFont.truetype('C:/Windows/Fonts/arial.ttf', 13)

def composite(board, path, x, baseline, height):
    im = Image.open(path).convert('RGBA')
    im = im.crop(im.getchannel('A').getbbox())
    im = im.resize((round(im.width*height/im.height), height), Image.Resampling.LANCZOS)
    board.alpha_composite(im, (round(x-im.width/2), baseline-height))

board = Image.new('RGBA', (1000, 520), '#232924')
d = ImageDraw.Draw(board)
d.text((24,16), 'OPENING STATION — LOOK PILOT / ORIGINAL BLENDER HARDWARE', font=FONT, fill='#ddd8c5')
d.text((24,43), 'Candidate stills. No animation, native-play, or commercial quality acceptance claimed.', font=SMALL, fill='#aeb6a9')
for row,height in enumerate((100,60)):
    y = 205 + row*160
    d.line((24,y+1,976,y+1),fill='#465044')
    d.text((24,y-height-27), str(height)+' px SIRS height',font=SMALL,fill='#aeb6a9')
    for i,facing in enumerate(('south','east','north','west')):
        x = 160+i*155
        composite(board,REGIONS/f'machine.sirs.idle.{facing}.png',x,y,height)
        d.text((x-20,y+12),facing,font=SMALL,fill='#ddd8c5')
    for i,state in enumerate(('closed','open')):
        x = 800+i*130
        # Keep authored world-cell scale, rather than making the door as tall as the robot.
        sirs = Image.open(REGIONS/'machine.sirs.idle.south.png')
        door = Image.open(REGIONS/f'door.{state}.png')
        ratio = door.getchannel('A').getbbox()[3]-door.getchannel('A').getbbox()[1]
        ratio /= sirs.getchannel('A').getbbox()[3]-sirs.getchannel('A').getbbox()[1]
        composite(board,REGIONS/f'door.{state}.png',x,y,round(height*ratio))
        d.text((x-25,y+12),state,font=SMALL,fill='#ddd8c5')
d.text((24,462),'Four true bearings: forward sensor / asymmetric cannon / rear service cabinet. Door aperture is actual alpha.',font=SMALL,fill='#aeb6a9')
d.text((24,486),'Quiet diffuse appearance; door status recesses are dark. Runtime owns aim, power, facing and obstruction.',font=SMALL,fill='#aeb6a9')
board.convert('RGB').save(REVIEW/'pilot-sirs-door-100px-60px.jpg',quality=95)
print(REVIEW/'pilot-sirs-door-100px-60px.jpg')

clips=json.loads((OUT/'clips.json').read_text(encoding='utf-8'))
for facing in ['south','east']:
    clip=next((c for c in clips['clips'] if c['id']==f'machine.sirs.walk.{facing}'),None)
    if not clip:continue
    frames=[];cell=47;axis=(0,1) if facing=='south' else (1,0)
    for i in range(4*len(clip['frames'])):
        entry=clip['frames'][i%len(clip['frames'])]
        frame=Image.new('RGBA',(420,310),'#232924');d=ImageDraw.Draw(frame)
        for x in range(22,420,cell):d.line((x,65,x,295),fill='#343d33')
        for y in range(65,310,cell):d.line((22,y,415,y),fill='#343d33')
        d.text((14,12),'SOURCE MOTION PILOT — '+facing.upper(),font=FONT,fill='#ddd8c5')
        d.text((14,35),'.5 seconds/cell; .5-cell cycle/.25s; 12 actual poses; 47px cell',font=SMALL,fill='#aeb6a9')
        distance=(i/len(clip['frames']))*clip['cycleCells']
        anchor=(125+axis[0]*cell*distance,160+axis[1]*cell*distance)
        art=Image.open(ROOT/'assets'/entry['path']).convert('RGBA')
        art=art.resize((round(entry['widthCells']*cell),round(entry['heightCells']*cell)),Image.Resampling.LANCZOS)
        frame.alpha_composite(art,(round(anchor[0]-entry['pivotX']*art.width),round(anchor[1]-(1-entry['pivotY'])*art.height)))
        frames.append(frame.convert('RGB'))
    durations=[20]*len(frames)
    for i in range(len(clip['frames'])-1,len(frames),len(clip['frames'])):durations[i]=30
    frames[0].save(REVIEW/f'sirs-native-cadence-{facing}.gif',save_all=True,append_images=frames[1:],duration=durations,loop=0,disposal=2)
    # Full unambiguous poses are also inspectable when animated preview is unavailable.
    strip=Image.new('RGBA',(12*130,220),'#232924')
    sd=ImageDraw.Draw(strip)
    for i,entry in enumerate(clip['frames']):
        composite(strip,ROOT/'assets'/entry['path'],65+i*130,180,120)
        sd.text((i*130+12,190),f'{i:02} / {i/12:.3f}',font=SMALL,fill='#ddd8c5')
    strip.convert('RGB').save(REVIEW/f'sirs-walk-{facing}-poses.jpg',quality=95)

manifest=json.loads((OUT/'regions.json').read_text(encoding='utf-8'))
by_name={r['name']:r for r in manifest['regions']}
def world_sprite(board,name,x,y,cell=80):
    entry=by_name[name];art=Image.open(ROOT/'assets'/entry['path']).convert('RGBA')
    art=art.resize((round(entry['widthCells']*cell),round(entry['heightCells']*cell)),Image.Resampling.LANCZOS)
    board.alpha_composite(art,(round(x-entry['pivotX']*art.width),round(y-(1-entry['pivotY'])*art.height)))

if 'machine.camera.on.south' in by_name:
    review=Image.new('RGBA',(1200,780),'#232924');d=ImageDraw.Draw(review)
    d.text((20,15),'OPENING HARDWARE — SAME WORLD-CELL SCALE',font=FONT,fill='#ddd8c5')
    d.text((20,40),'Development export review; camera power / damage / door mechanism are selected from real state.',font=SMALL,fill='#aeb6a9')
    groups=[
        ['machine.camera.'+a+'.south' for a in ['on','off','disabled']]+['machine.camera.on.'+f for f in ['east','north','west']],
        ['door.'+a+'.north' if a not in ['closed','open'] else 'door.'+a for a in ['closed','open','locked','braced','damaged']]+['door.open.east'],
        ['prop.inspection-desk','prop.maintenance-bench','control.switch.unpowered','control.switch.powered','control.switch.released','placard.department']]
    for row,group in enumerate(groups):
        baseline=[225,485,700][row]
        d.line((20,baseline,1180,baseline),fill='#465044')
        for i,name in enumerate(group):
            x=110+i*197;world_sprite(review,name,x,baseline,80)
            label=name.replace('machine.','').replace('control.','').replace('prop.','')
            d.text((i*197+10,baseline+15),label,font=SMALL,fill='#ddd8c5')
    review.convert('RGB').save(REVIEW/'hardware-states-same-scale.jpg',quality=95)

    clearance=Image.new('RGBA',(760,420),'#232924');d=ImageDraw.Draw(clearance)
    d.text((20,15),'NORTH DOOR CLEARANCE — UNCHANGED ACTOR SCALE / FOOT PIVOTS',font=FONT,fill='#ddd8c5')
    world_sprite(clearance,'door.open',195,350,140)
    world_sprite(clearance,'machine.sirs.idle.south',195,350,140)
    world_sprite(clearance,'door.open',560,350,140)
    officer_file=ROOT/'assets/art/production/officer/clips.json'
    if officer_file.exists():
        officer=json.loads(officer_file.read_text(encoding='utf-8'))
        off=next(c['frames'][0] for c in officer['clips'] if c['id']=='officer.pistol.suit.idle.south')
        art=Image.open(ROOT/'assets'/off['path']).convert('RGBA');art=art.resize((round(off['widthCells']*140),round(off['heightCells']*140)),Image.Resampling.LANCZOS)
        clearance.alpha_composite(art,(round(560-off['pivotX']*art.width),round(350-(1-off['pivotY'])*art.height)))
    d.text((120,375),'SIRS1.4cell target',font=SMALL,fill='#ddd8c5');d.text((500,375),'officer1.3cell target',font=SMALL,fill='#ddd8c5')
    clearance.convert('RGB').save(REVIEW/'north-door-actor-clearance.jpg',quality=95)
    doors=Image.new('RGBA',(1260,650),'#232924');d=ImageDraw.Draw(doors)
    d.text((20,15),'DOOR ORIENTATIONS — SAME CAMERA / MAP ANCHOR / CELL SCALE',font=FONT,fill='#ddd8c5')
    d.text((20,40),'North spans map E/W. East spans map N/S with structural cutaway. Schematic grid marks the one-cell footprint.',font=SMALL,fill='#aeb6a9')
    states=['closed','open','opening','closing','locked','braced','damaged']
    for row,orientation in enumerate(['north','east']):
        baseline=285+row*290
        for col,state in enumerate(states):
            x=90+col*180;cell=80
            for dx in [-1,0,1]:d.rectangle((x+dx*cell-cell/2,baseline-cell/2,x+dx*cell+cell/2,baseline+cell/2),outline='#465044')
            d.rectangle((x-cell/2,baseline-1.5*cell,x+cell/2,baseline-.5*cell),outline='#465044')
            name='door.'+state if orientation=='north' and state in ['closed','open'] else f'door.{state}.{orientation}'
            if state in ['opening','closing']:name+='.04'
            world_sprite(doors,name,x,baseline,cell)
            d.text((x-65,baseline+55),orientation+' / '+state,font=SMALL,fill='#ddd8c5')
    doors.convert('RGB').save(REVIEW/'door-orientations-same-scale.jpg',quality=95)
