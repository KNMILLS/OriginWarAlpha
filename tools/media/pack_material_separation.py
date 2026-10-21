"""MAT2 deterministic raster crop/packing and labelled review composites; no artwork painting.
The preceding ATM pass remains reproducible; this bounded layer replaces materials only.
"""
from pathlib import Path
import hashlib,json
import numpy as np
from PIL import Image,ImageDraw

ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'assets/art';BASE=ART/'material-separation';SRC=BASE/'source';REVIEW=BASE/'review'
N=64
STATION=list(range(8))+[11,12,13]
SECTORS=list(range(10))
OUTPUTS=['assets/art/source/station-native.png','assets/art/station.png','assets/art/sectors/sectors-native.png','assets/art/sectors/sectors.png']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(name):return Image.open(SRC/name).convert('RGBA')
def tile(im,i):return im.crop((i%8*N,i//8*N,(i%8+1)*N,(i//8+1)*N))
def put(im,part,i):im.paste(part,(i%8*N,i//8*N))
def write_json(path,data):path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8',newline='\n')
def source_tile(name,col):
    im=read(name);left=round(col*im.width/5);right=round((col+1)*im.width/5)
    if name=='floors.png':
        # A centered square preserves the fine grain and circular rubber grip proportions.
        height=right-left;top=(im.height-height)//2;box=(left,top,right,top+height)
    elif name=='walls.png':box=(left,0,right,im.height)
    else:box=(left,66,right,657) # Inspected actual content; omit generated white outer padding.
    result=im.crop(box).resize((N,N),Image.Resampling.LANCZOS);result.putalpha(255)
    return result,list(box)
def dark(im,amount):
    a=np.array(im);a[:,:,:3]=(a[:,:,:3].astype(float)*amount).clip(0,255).astype(np.uint8)
    return Image.fromarray(a,'RGBA')
def label(draw,xy,text):draw.text(xy,text,fill=(190,199,202))
def material_contact(before,after):
    rows=[(before,0,'BEFORE floor',1),(before,5,'BEFORE wall',1),(after,0,'MAT2 floor',1),(after,5,'MAT2 wall',1),(after,0,'MAT2 floor x.25',.25),(after,5,'MAT2 wall x.25',.25)]
    board=Image.new('RGB',(5*132,len(rows)*150),(7,9,11));d=ImageDraw.Draw(board)
    for r,(im,offset,title,light) in enumerate(rows):
        for col in range(5):
            p=dark(tile(im,offset+col),light).resize((128,128),Image.Resampling.NEAREST)
            board.paste(p,(col*132,r*150+19));label(d,(col*132+2,r*150+2),f'{col} {title}')
    board.save(REVIEW/'materials-before-after-dark.png')
def special_contact(before,after):
    board=Image.new('RGB',(5*132,3*150),(7,9,11));d=ImageDraw.Draw(board)
    for r,(im,title,light) in enumerate([(before,'BEFORE',1),(after,'MAT2',1),(after,'MAT2 x.25',.25)]):
        for col,slot in enumerate([6,7,11,12,13]):
            p=dark(tile(im,slot),light).resize((128,128),Image.Resampling.NEAREST)
            board.paste(p,(col*132,r*150+19));label(d,(col*132+2,r*150+2),f'{slot} {title}')
    board.save(REVIEW/'special-before-after-dark.png')
def fixture_contact(sectors):
    board=Image.new('RGB',(5*132,3*150),(7,9,11));d=ImageDraw.Draw(board)
    for col in range(5):
        fixture=tile(sectors,15+col);combined=tile(sectors,5+col);combined.alpha_composite(fixture)
        for row,(p,title) in enumerate([(fixture,'fixture alpha'),(combined,'wall+fixture'),(dark(combined,.25),'combined x.25')]):
            p=p.resize((128,128),Image.Resampling.NEAREST);board.paste(p,(col*132,row*150+19),p)
            label(d,(col*132+2,row*150+2),f'{15+col} {title}')
    board.save(REVIEW/'wall-fixture-integration.png')
def smooth(a,b,x):
    t=np.clip((x-a)/(b-a),0,1);return t*t*(3-2*t)
def frozen_room(station,sectors,zone):
    # Asset-layout comparator, NOT an actual game frame. Same geometry/props/light for both.
    room=Image.new('RGBA',(9*N,7*N))
    for y in range(7):
        for x in range(9):
            p=tile(sectors,5+zone if x in (0,8) or y in (0,6) else zone)
            room.alpha_composite(p,(x*N,y*N))
    for x,y,slot in [(2,4,11),(3,4,12),(6,2,13),(4,6,9),(8,3,7)]:
        if slot==9:room.paste(tile(sectors,zone),(x*N,y*N))
        room.alpha_composite(tile(station,slot),(x*N,y*N))
    room.alpha_composite(tile(sectors,25+zone),(4*N,3*N))
    room.alpha_composite(tile(sectors,10+zone),(6*N,4*N))
    room.alpha_composite(tile(sectors,15+zone),(2*N,0))
    yy,xx=np.mgrid[0:7*N,0:9*N];dx=(xx+.5)/N-5.5;dy=(yy+.5)/N-3.5
    distance=np.hypot(dx,dy);angle=-dy/np.maximum(.001,distance)
    cone=smooth(.74,.92,angle)*(1-smooth(1.2,8.5,distance))*.95
    local=(1-smooth(.3,2.1,distance))*.72
    # Matches shader beam/visor formula with a fixed base field; no live SquidGrid/FOV.
    light=np.array([37,41,47])/255+(cone+local)[:,:,None]*np.array([.91,.95,1.])
    edge=np.hypot(((xx+.5)/(9*N)-.5)*1.22,(yy+.5)/(7*N)-.5)
    visor=1-.42*smooth(.38,.76,edge)
    rgba=np.asarray(room).copy();rgba[:,:,:3]=(rgba[:,:,:3]*light*visor[:,:,None]).clip(0,255).astype(np.uint8)
    result=Image.fromarray(rgba,'RGBA')
    # Exact accepted player pose, kept constant and readable just as the actor floor does.
    player=Image.open(ART/'animation/actors-native.png').convert('RGBA');p=tile(player,0)
    pl=np.maximum(light[3*N:4*N,5*N:6*N],.28)*visor[3*N:4*N,5*N:6*N,None]
    a=np.asarray(p).copy();a[:,:,:3]=(a[:,:,:3]*pl).clip(0,255).astype(np.uint8)
    result.alpha_composite(Image.fromarray(a,'RGBA'),(5*N,3*N));return result
def room_contact(old_station,old_sectors,station,sectors):
    for zone in range(5):
        board=Image.new('RGB',(9*N*2+12,7*N+42),(7,9,11));d=ImageDraw.Draw(board)
        board.paste(frozen_room(old_station,old_sectors,zone),(0,38));board.paste(frozen_room(station,sectors,zone),(9*N+12,38))
        label(d,(4,2),f'ZONE {zone} BEFORE');label(d,(9*N+16,2),f'ZONE {zone} MAT2')
        label(d,(4,19),'FROZEN ASSET COMPOSITE: same room/props/beam; synthetic fixed field, not a game capture')
        board.save(REVIEW/f'frozen-room-zone-{zone}.png')
def pack():
    REVIEW.mkdir(exist_ok=True)
    before_station=read('before/station-native.png');before_sectors=read('before/sectors-native.png')
    station=before_station.copy();sectors=before_sectors.copy();crops={}
    for col in range(5):
        floor,box=source_tile('floors.png',col);crops[f'floor{col}']={'source':'floors.png','box':box}
        wall,box=source_tile('walls.png',col);crops[f'wall{col}']={'source':'walls.png','box':box}
        put(sectors,floor,col);put(sectors,wall,5+col);put(station,floor,col)
        if col==0:put(station,wall,5)
    for col,slot in enumerate([6,7,11,12,13]):
        part,box=source_tile('special.png',col);put(station,part,slot);crops[f'station{slot}']={'source':'special.png','box':box}
    for im,native,export in [(station,ART/'source/station-native.png',ART/'station.png'),(sectors,ART/'sectors/sectors-native.png',ART/'sectors/sectors.png')]:
        im.save(native,optimize=True);im.resize((im.width*2,im.height*2),Image.Resampling.NEAREST).save(export,optimize=True)
    material_contact(before_sectors,sectors);special_contact(before_station,station);fixture_contact(sectors);room_contact(before_station,before_sectors,station,sectors)
    manifest={'task':'MAT2','baseline':'039bebc8e0d36ab2497bd648c46da6de67d0fe2b','method':'Original built-in OpenAI image_gen rasters; deterministic crop/resample/pack only. Review composites reuse assets and fixed technical lighting, not Python-painted production art.','native_cell_pixels':64,'runtime_cell_pixels':128,'changed_slots':{'station':STATION,'sectors':SECTORS},'source_crops':crops,'sources':{str(p.relative_to(ROOT)).replace('\\','/'):sha(p) for p in sorted(SRC.rglob('*')) if p.is_file()},'outputs':{p:sha(ROOT/p) for p in OUTPUTS},'review_scope':'Labelled material/dark/frozen-room composites only; not game captures, real FOV/lighting validation or owner artistic approval.','unchanged_scope':'All original artwork except the4 named native/runtime station+sector PNGs; protected by baseline identity manifest and independent per-cell comparison.'}
    write_json(BASE/'manifest.json',manifest)
    current=sha(ART/'station.png')
    atm_path=ART/'atmosphere/manifest.json';atm=json.loads(atm_path.read_text(encoding='utf-8'))
    for p in OUTPUTS:atm['outputs'][p]=sha(ROOT/p)
    atm['material_separation_override']='assets/art/material-separation/manifest.json';write_json(atm_path,atm)
    for path in (ART/'manifest.json',ART/'animation/manifest.json',ART/'sectors/manifest.json',ART/'animation/directional-actions/manifest.json'):
        d=json.loads(path.read_text(encoding='utf-8'));d['current_reference_station_sha256']=current
        d['material_separation_override']='assets/art/material-separation/manifest.json'
        d['current_export']='tools/media/pack_atmosphere.py then tools/media/pack_material_separation.py';write_json(path,d)
    print('MAT2 packed:11station/10sector material cells; other runtime PNGs/pixels unchanged.')
if __name__=='__main__':pack()
