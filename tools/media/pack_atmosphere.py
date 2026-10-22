"""Pack ATM-ART generated rasters. Cropping/resampling/alpha cleanup only; no painted assets.
Baseline32px cells remain preserved in source/baseline. Current atlas interfaces stay128px.
"""
from pathlib import Path
from collections import deque
import hashlib, json
import numpy as np
from PIL import Image, ImageOps, ImageDraw

ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'assets/art'
BASE=ART/'atmosphere'
SRC=BASE/'source'
N=64

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def load(name): return Image.open(SRC/name).convert('RGBA')
def tile(im,index,cols=8,cell=64): return im.crop(((index%cols)*cell,(index//cols)*cell,(index%cols+1)*cell,(index//cols+1)*cell))
def put(im,part,index,cols=8): im.paste(part,((index%cols)*N,(index//cols)*N))
def source_cell(name,col,row,cols,rows,ys=None):
    im=load(name)
    bounds=(round(col*im.width/cols),ys[row] if ys else round(row*im.height/rows),round((col+1)*im.width/cols),ys[row+1] if ys else round((row+1)*im.height/rows))
    return im.crop(bounds)
def clean(im):
    p=np.array(im.convert('RGBA'));p[p[:,:,3]<32]=0
    return Image.fromarray(p,'RGBA')
def component(im):
    """Remove a stray neighboring-frame fragment from a source action crop."""
    p=np.array(im);mask=p[:,:,3]>=32;seen=np.zeros(mask.shape,bool);best=[]
    for yy,xx in zip(*np.nonzero(mask)):
        if seen[yy,xx]:continue
        q=deque([(int(yy),int(xx))]);seen[yy,xx]=True;part=[]
        while q:
            y,x=q.pop();part.append((y,x))
            for dy,dx in ((-1,0),(1,0),(0,-1),(0,1),(-1,-1),(-1,1),(1,-1),(1,1)):
                ny,nx=y+dy,x+dx
                if 0<=ny<mask.shape[0] and 0<=nx<mask.shape[1] and mask[ny,nx] and not seen[ny,nx]:seen[ny,nx]=True;q.append((ny,nx))
        if len(part)>len(best):best=part
    keep=np.zeros(mask.shape,bool)
    for y,x in best:keep[y,x]=True
    p[~keep]=0
    return Image.fromarray(p,'RGBA')
def fit_group(images,limits=(60,58),baseline=60):
    boxes=[clean(im).getbbox() for im in images];assert all(boxes)
    scale=min(limits[0]/max(b[2]-b[0] for b in boxes),limits[1]/max(b[3]-b[1] for b in boxes))
    out=[]
    for im,b in zip(images,boxes):
        crop=im.crop(b);size=(max(1,round(crop.width*scale)),max(1,round(crop.height*scale)))
        glyph=clean(crop.resize(size,Image.Resampling.LANCZOS));dest=Image.new('RGBA',(N,N))
        dest.alpha_composite(glyph,(round((N-size[0])/2),baseline-size[1]));out.append(clean(dest))
    return out
def save_atlas(im,path,scale=2):
    path.parent.mkdir(parents=True,exist_ok=True)
    im.save(path.with_name(path.stem+'-native.png'),optimize=True)
    im.resize((im.width*scale,im.height*scale),Image.Resampling.NEAREST).save(path,optimize=True)
def old(name):return load('baseline/'+name).resize((512,512 if name in ('station-native.png','actors-native.png') else 192 if name=='attack-native.png' else 256),Image.Resampling.NEAREST)
def review_sheet(im,name,cols=8):
    # Contact-sheet layout/labels are review UI, not painted game artwork.
    rows=im.height//N;board=Image.new('RGB',(cols*132,rows*156),(12,16,19));draw=ImageDraw.Draw(board)
    for i in range(rows*cols):
        p=tile(im,i,cols);board.paste(p.resize((128,128),Image.Resampling.NEAREST),(i%cols*132,i//cols*156+20),p.resize((128,128),Image.Resampling.NEAREST))
        draw.text((i%cols*132+4,i//cols*156+3),str(i),fill=(180,194,195))
    (BASE/'review').mkdir(exist_ok=True);board.save(BASE/'review'/name)

def review_materials(sectors,station):
    previous=old('sectors-native.png')
    board=Image.new('RGB',(5*132,4*156),(12,16,19));draw=ImageDraw.Draw(board)
    for row,(im,offset,label) in enumerate(((previous,0,'prior floor'),(sectors,0,'new floor'),(previous,5,'prior wall'),(sectors,5,'new wall'))):
        for col in range(5):
            p=tile(im,offset+col).resize((128,128),Image.Resampling.NEAREST)
            board.paste(p,(col*132,row*156+20));draw.text((col*132+3,row*156+3),f'{label} {col}',fill=(180,194,195))
    board.save(BASE/'review/material-before-after.png')
    board=Image.new('RGB',(5*196,3*220),(12,16,19));draw=ImageDraw.Draw(board)
    for row in range(3):
        for col in range(5):
            p=tile(sectors,row*5+col) if row<2 else tile(station,(6,7,11,12,13)[col])
            for y in range(3):
                for x in range(3):board.paste(p,(col*196+x*64,row*220+20+y*64))
            draw.text((col*196+3,row*220+3),f'{("floor","wall","special")[row]} {col}',fill=(180,194,195))
    board.save(BASE/'review/terrain-tiling.png')

def pack():
    station,actors,attacks,sectors=(old(n) for n in ('station-native.png','actors-native.png','attack-native.png','sectors-native.png'))
    for i in range(10):
        part=source_cell('materials.png',i%5,i//5,5,2).resize((N,N),Image.Resampling.LANCZOS)
        part.putalpha(255);put(sectors,part,i)
        if i<5:put(station,part,i)
        elif i==5:put(station,part,5)
    # Actual generated content occupies y126..594; white outer padding is excluded.
    terrain=load('terrain.png')
    for col,dest in enumerate((6,7,11,12,13)):
        part=terrain.crop((round(col*terrain.width/5),126,round((col+1)*terrain.width/5),595)).resize((N,N),Image.Resampling.LANCZOS)
        part.putalpha(255);put(station,part,dest)
    props=[]
    for i in range(20):props.append(source_cell('props.png',i%5,i//5,5,4,[0,286,552,819,1122]))
    # Shared scale within each role band keeps material/object scale coherent.
    for row in range(4):
        for col,part in enumerate(fit_group(props[row*5:row*5+5],(58,58))):put(sectors,part,10+row*5+col)
    for dest,source in ((14,10),(15,16),(16,16),(19,25),(29,19),(30,23)):
        put(station,tile(sectors,source),dest)
    # Hatch identity remains a heavy physical doorway, distinct from a terminal.
    doors=fit_group([source_cell('doors.png',i,0,3,1) for i in range(3)],(62,62),63)
    for i,p in enumerate(doors):put(station,p,8+i)
    put(station,doors[0],17);put(station,doors[1],18)

    # Selected cardinal body sources: one corrected side gait plus separately authored N/S.
    # The first4x4 source's wrong-facing east4 is retained but is never exported.
    walk=Image.new('RGBA',(N*4,N*4))
    n_parts=[source_cell('player-north-south-four.png',c,0,4,2,[0,426,887]) for c in (0,1,3)]
    n_parts.insert(2,source_cell('player-walk-four-initial.png',3,0,4,4,[0,313,621,932,1241]))
    s_parts=[source_cell('player-north-south-four.png',c,1,4,2,[0,426,887]) for c in (0,1,3)]
    s_parts.insert(2,source_cell('player-walk-four-initial.png',1,2,4,4,[0,313,621,932,1241]))
    # Mixed generated grids have different source pixel dimensions; normalize cell size
    # before sharing a scale, so selected poses retain the same physical body size.
    def normalized_pose(p):
        p=component(clean(p));p=p.crop(p.getbbox())
        return p.resize((round(p.width*256/p.height),256),Image.Resampling.LANCZOS)
    n_parts=[normalized_pose(p) for p in n_parts]
    s_parts=[normalized_pose(p) for p in s_parts]
    east=fit_group([source_cell('player-east-four.png',c,0,4,1) for c in range(4)],(60,58))
    for facing,parts in ((0,fit_group(n_parts,(60,58))),(1,east),(2,fit_group(s_parts,(60,58))),(3,[ImageOps.mirror(p) for p in east])):
        for phase,p in enumerate(parts):put(walk,p,facing*4+phase,4)
        for phase,source in enumerate((0,2)):put(actors,parts[source],facing*2+phase)
    for phase in range(4):put(station,tile(walk,8+phase,4),32+phase)
    for facing in range(4):
        parts=[component(source_cell('player-actions.png',facing,phase,5,2,[0,421,793])) for phase in range(2)]
        frames=fit_group(parts,(60,60))
        for phase,p in enumerate(frames):put(attacks,p,facing*2+phase)
    put(actors,tile(attacks,4),40);put(actors,tile(attacks,5),41)
    # Death frame extents differ intentionally; common64px cell/feet anchor remains.
    deaths=fit_group([component(source_cell('player-actions.png',4,r,5,2,[0,421,793])) for r in range(2)],(60,54))
    for phase,p in enumerate(deaths):put(actors,p,42+phase)
    save_atlas(station,ART/'station.png');station.save(ART/'source/station-native.png',optimize=True)
    save_atlas(actors,ART/'animation/actors.png');save_atlas(attacks,ART/'animation/directional-actions/attack.png');save_atlas(sectors,ART/'sectors/sectors.png')
    save_atlas(walk,BASE/'player-walk.png')
    review_sheet(sectors,'sectors-2x.png');review_sheet(station,'station-2x.png');review_sheet(actors,'actors-2x.png');review_sheet(attacks,'attacks-2x.png');review_sheet(walk,'player-walk-2x.png',4)
    review_materials(sectors,station)
    frames=[]
    for phase in range(4):
        canvas=Image.new('RGB',(4*128,128),(12,16,19))
        for facing in range(4):
            p=tile(walk,facing*4+phase,4).resize((128,128),Image.Resampling.NEAREST);canvas.paste(p,(facing*128,0),p)
        frames.append(canvas)
    frames[0].save(BASE/'review/player-walk.gif',save_all=True,append_images=frames[1:],duration=150,loop=0,disposal=2)
    outputs=['assets/art/station.png','assets/art/source/station-native.png','assets/art/animation/actors.png','assets/art/animation/actors-native.png','assets/art/animation/directional-actions/attack.png','assets/art/animation/directional-actions/attack-native.png','assets/art/sectors/sectors.png','assets/art/sectors/sectors-native.png','assets/art/atmosphere/player-walk.png','assets/art/atmosphere/player-walk-native.png']
    # save_atlas wrote this convenience native beside station.png; the canonical one is source/.
    extra=ART/'station-native.png'
    if extra.exists():extra.unlink()
    manifest={'task':'ATM-ART','baseline':'374e27c764d55adaae7a9a3b4003a5ff883de157','native_cell_pixels':64,'runtime_cell_pixels':128,'runtime_scale':2,
      'method':'OpenAI built-in image_gen source; deterministic crop/resample/alpha cleanup only; no Python painting of game assets',
      'reference':'owner-player-reference.png; original owner helmet illustration, unchanged',
      'changed_slots':{'station':list(range(20))+[29,30,32,33,34,35],'sectors':list(range(30)),'actors':list(range(8))+[40,41,42,43],'attacks':list(range(8))},
      'source_layouts':{'materials.png':{'columns':5,'rows':2,'slots':'sector0-4 floors,5-9 walls; station0-5'},'terrain.png':{'columns':5,'source_y':[126,595],'station_slots':[6,7,11,12,13]},'props.png':{'columns':5,'row_edges':[0,286,552,819,1122],'slots':'sector10-29'},'doors.png':{'columns':3,'rows':1,'station_slots':[8,9,10],'hatch_reuse':[17,18]},'player-actions.png':{'columns':5,'row_edges':[0,421,793],'selection':'first4 columns N/E/S/W ready/recoil; fifth collapse/prone; largest component removes adjacent-frame fragments'},'player-east-four.png':{'columns':4,'rows':1,'selection':'east phases; mirrored west'},'player-north-south-four.png':{'columns':4,'row_edges':[0,426,887],'selection':'cols0,1,3 become phases0,1,3 for N/S; largest component and tight-height normalization remove boundary fragments and inconsistent source-grid scale'},'player-walk-four-initial.png':{'columns':4,'row_edges':[0,313,621,932,1241],'selection':'N row0col3, S row2col1 become phase2; wrong-facing east frame excluded'},'player-walk-initial.png':{'selection':'retained initial identity reference; not exported'}},
      'player_walk':{'path':'assets/art/atmosphere/player-walk.png','columns':4,'rows':4,'row_facings':['north','east','south','west'],'phase_columns':4,'anchor_native':[32,60],'west':'horizontal reflection of the corrected east poses'},
      'sources':{str(p.relative_to(ROOT)).replace('\\','/'):sha(p) for p in sorted(SRC.rglob('*.png'))},
      'authoring_request_records':{str(p.relative_to(ROOT)).replace('\\','/'):sha(p) for p in sorted((SRC/'prompts').glob('*.txt'))},
      'generation_record':{'path':'assets/art/atmosphere/source/GENERATION.json','sha256':sha(SRC/'GENERATION.json')},
      'baseline_runtime_identities':{'path':'assets/art/atmosphere/source/baseline/runtime-identities.json','sha256':sha(SRC/'baseline/runtime-identities.json')},
      'outputs':{p:sha(ROOT/p) for p in outputs},
      'limits':['Not a runtime capture or owner artistic acceptance.','Four selected source poses are supplied; walking anatomy/cadence still needs actual integration review.','Side profile weapon handedness is mirrored for west.','Attack extent differs from walking; check north aim/recoil size in engine.','All other actor/robot/human/finale frames remain unchanged pixels at runtime.']}
    (BASE/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8',newline='\n')
    identities=json.loads((SRC/'baseline/runtime-identities.json').read_text(encoding='utf-8'))['sha256']
    for path in (ART/'manifest.json',ART/'animation/manifest.json',ART/'sectors/manifest.json',ART/'animation/directional-actions/manifest.json'):
        d=json.loads(path.read_text(encoding='utf-8'))
        if 'reference_station_sha256' in d:d['reference_station_sha256']=identities['assets/art/station.png']
        d['current_reference_station_sha256']=sha(ART/'station.png')
        d['atmosphere_override']='assets/art/atmosphere/manifest.json'
        d['atmosphere_note']='Historical source/layout entries below describe the preserved prior pass. The ATM-ART override identifies current64px native crops and changed slots; runtime128px cells are unchanged.'
        if 'runtime_atlas' in d:
            d['runtime_atlas']['native_cell_pixels']=64
            d['runtime_atlas']['nearest_neighbor_scale']=2
        if 'atlas' in d:
            d['atlas']['native_cell_pixels']=64;d['atlas']['nearest_scale']=2
            if 'native_size' in d['atlas']:d['atlas']['native_size']=[512,192]
        d['current_export']='tools/media/pack_atmosphere.py'
        path.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8',newline='\n')
    if (ART/'material-separation/source/floors.png').exists():
        from pack_material_separation import pack as pack_separation
        pack_separation()
    print('ATM-ART packed: unchanged runtime dimensions;64px native materials/player;30sector,26station,12actor,8attack slots.')

if __name__=='__main__':pack()
