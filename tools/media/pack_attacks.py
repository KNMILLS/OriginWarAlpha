"""Directional attack source/export. All previous atlases are read-only."""
from __future__ import annotations
import argparse,hashlib,json
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
BASE=ROOT/"assets"/"art"/"animation"/"directional-actions"
PREVIOUS=ROOT/"assets"/"art"/"animation"/"actors-native.png"
FAMILIES=("player","sirs","add")
ROWS=(0,2,3)
SOUTH=(40,44,48)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def tile(im,i):return im.crop(((i%8)*32,(i//8)*32,(i%8+1)*32,(i//8+1)*32))
def prepare():
    im=Image.open(PREVIOUS).convert("RGBA");out=BASE/"source"/"references";out.mkdir(parents=True,exist_ok=True)
    for family,row,south in zip(FAMILIES,ROWS,SOUTH):
        board=Image.new("RGBA",(160,64))
        for phase in range(2):
            for direction in range(4):board.alpha_composite(tile(im,row*8+direction*2+phase),(direction*32,phase*32))
            board.alpha_composite(tile(im,south+phase),(128,phase*32))
        board.resize((640,256),Image.Resampling.NEAREST).save(out/(family+".png"))
    print("Prepared existing selected identity/direction/south-attack references.")
def source_cell(path,column,row,columns,rows):
    im=Image.open(path).convert("RGBA")
    rect=(round(column*im.width/columns),round(row*im.height/rows),
          round((column+1)*im.width/columns),round((row+1)*im.height/rows))
    return im.crop(rect),rect
def body_anchor(im):
    a=np.asarray(im.getchannel("A"));ys,xs=np.nonzero(a>=128)
    assert len(xs)
    box=(int(xs.min()),int(ys.min()),int(xs.max()+1),int(ys.max()+1))
    # Feet/legs define the world anchor, not the whole bounding box (which grows
    # sideways as muzzle flash appears). The lower35% excludes the aiming arms.
    band=max(box[1],round(box[3]-(box[3]-box[1])*.35))
    yy,xx=np.nonzero(a[band:box[3]]>=128)
    assert len(xx)
    return box,((float(xx.min())+float(xx.max()+1))/2,float(box[3]))
def export_pair(images):
    data=[body_anchor(im) for im in images]
    left=max(anchor[0]-box[0] for box,anchor in data)
    right=max(box[2]-anchor[0] for box,anchor in data)
    up=max(anchor[1]-box[1] for box,anchor in data)
    scale=min(15/max(1,left),15/max(1,right),29/max(1,up))
    result=[];detail=[]
    for im,(box,anchor) in zip(images,data):
        crop=im.crop(box);size=(max(1,round(crop.width*scale)),max(1,round(crop.height*scale)))
        glyph=crop.resize(size,Image.Resampling.LANCZOS)
        x=round(16-(anchor[0]-box[0])*scale);y=round(30-(anchor[1]-box[1])*scale)
        frame=Image.new("RGBA",(32,32));frame.alpha_composite(glyph,(x,y))
        pixels=np.asarray(frame).copy();pixels[pixels[:,:,3]<32]=0
        frame=Image.fromarray(pixels,"RGBA")
        result.append(frame)
        detail.append({"source_bbox":box,"source_foot_anchor":anchor,"scale":round(scale,8),
                       "native_foot_anchor":[16,30],"paste":[x,y],"native_glyph_size":size,"alpha_cutoff":32})
    return result,detail
def pack():
    previous=Image.open(PREVIOUS).convert("RGBA");native=Image.new("RGBA",(256,96));slots=[]
    source=BASE/"source"
    compact=source/"compact-north.png"
    for row,(family,south) in enumerate(zip(FAMILIES,SOUTH)):
        for direction,columns in (("north",(0,1)),("east",(2,3)),("south",(4,5)),("west",(6,7))):
            if direction=="south":
                for phase,column in enumerate(columns):
                    piece=tile(previous,south+phase);native.alpha_composite(piece,(column*32,row*32))
                    slots.append({"id":row*8+column,"family":family,"direction":direction,"phase":phase,
                                  "source":"existing actors-native.png","source_slot":south+phase,"reuse":"exact native32 RGBA pixels; no re-export processing"})
                continue
            is_north=direction=="north";path=compact if is_north else source/"compact-east-west.png"
            cropped=([source_cell(path,row,phase,3,2) for phase in range(2)] if is_north else
                     [source_cell(path,(0 if direction=="east" else 2)+phase,row,4,3) for phase in range(2)])
            frames,details=export_pair([im for im,rect in cropped])
            for phase,column in enumerate(columns):
                native.alpha_composite(frames[phase],(column*32,row*32))
                slots.append({"id":row*8+column,"family":family,"direction":direction,"phase":phase,
                              "source":"assets/art/animation/directional-actions/source/"+path.name,
                              "source_pixel_box":cropped[phase][1],**details[phase]})
    native.save(BASE/"attack-native.png",optimize=True)
    native.resize((1024,384),Image.Resampling.NEAREST).save(BASE/"attack.png",optimize=True)
    review=BASE/"review";review.mkdir(exist_ok=True)
    dark=Image.new("RGBA",(1024,384),(20,30,36,255));dark.alpha_composite(native.resize((1024,384),Image.Resampling.NEAREST))
    dark.convert("RGB").save(review/"attacks-4x-on-dark.png",optimize=True)
    phases=[]
    for phase in range(2):
        board=Image.new("RGBA",(512,384),(20,30,36,255))
        for row in range(3):
            for direction in range(4):
                piece=native.crop(((direction*2+phase)*32,row*32,(direction*2+phase+1)*32,(row+1)*32))
                board.alpha_composite(piece.resize((128,128),Image.Resampling.NEAREST),(direction*128,row*128))
        phases.append(board.convert("RGB"))
    phases[0].save(review/"attacks.gif",save_all=True,append_images=phases[1:],duration=170,loop=0)
    manifest={"task":"M5-DIRECTED-ATTACKS","status":"native/4x export visually inspected; runtime saved-facing and attack timing acceptance pending",
       "atlas":{"path":"assets/art/animation/directional-actions/attack.png","size":[1024,384],
                "native":"assets/art/animation/directional-actions/attack-native.png","native_size":[256,96],
                "columns":8,"rows":3,"native_cell_pixels":32,"nearest_scale":4,"mode":"RGBA"},
       "layout":"Rows PLAYER,SIRS,ADD. Columns north2,east2,south2,west2; each pair ready then recoil/firing.",
       "source_export":"tools/media/pack_attacks.py",
       "provenance":"Original OpenAI built-in image_gen directional poses from recorded prompts and current selected original project actor references. No external pack, stock or legacy art.",
       "previous_actors_native_sha256":sha(PREVIOUS),
       "source_rasters":[{"path":str(p.relative_to(ROOT)).replace("\\","/"),"sha256":sha(p)} for p in sorted(source.rglob("*.png"))],
       "corrections":["Initial mixed-direction sources had oversized barrel/flash extents and north flash tips crossing their nominal row split; all three are preserved as first passes but are not selected for runtime.",
                      "A focused compact rear-attack sheet and compact east/west sheet supply new aimed pairs without shrinking the body around oversized effects; no front-facing pose was rotated or relabeled.",
                      "Foot/leg geometry anchors new frames at native16,30, so extending muzzle flash does not recenter the whole actor."],
       "manual_review":["North actors show backs and aim upward; east/right and west/left guns and flash point the same way as the body.",
                        "Braced human/biped anatomy and recoil/arm/leg changes inspected in generated source and exported contact sheets.",
                        "South pairs are copied from accepted actors slots40/41,44/45,48/49 without raster changes."],
       "limitations":["Two-frame attacks remain compact; gun extension/recoil pose can alter silhouette height/width within32px and needs actual game comparison.",
                      "No new death facings or weapon-specific rifle/scattergun/melee animations were requested; renderer event mapping remains root-owned.",
                      "No actual saved-target-facing, hit timing, FOV/occlusion or runtime acceptance is claimed."],
       "slots":sorted(slots,key=lambda e:e["id"])}
    (BASE/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    return {"atlas_sha256":sha(BASE/"attack.png"),"native_sha256":sha(BASE/"attack-native.png")}
def validate(repeat=False):
    paths=[BASE/"attack.png",BASE/"attack-native.png",BASE/"manifest.json",BASE/"review"/"attacks.gif"]
    before={p.name:sha(p) for p in paths}
    if repeat:
        pack();assert before=={p.name:sha(p) for p in paths},"Independent attack export bytes differ."
    im=Image.open(BASE/"attack.png");n=Image.open(BASE/"attack-native.png")
    assert im.size==(1024,384) and n.size==(256,96) and im.mode==n.mode=="RGBA"
    x=np.asarray(n);assert np.array_equal(np.asarray(im),np.repeat(np.repeat(x,4,axis=0),4,axis=1))
    old=Image.open(PREVIOUS).convert("RGBA");checks=[]
    for row,south in enumerate(SOUTH):
        for direction in range(4):
            frames=[x[row*32:(row+1)*32,(direction*2+p)*32:(direction*2+p+1)*32] for p in range(2)]
            changed=int(np.count_nonzero(np.any(frames[0]!=frames[1],axis=2)));assert changed>0
            for phase,z in enumerate(frames):
                alpha=z[:,:,3];assert alpha.max()>0 and np.any(alpha==0)
                assert not np.any((alpha>0)&(alpha<32))
                if direction==2:assert np.array_equal(z,np.asarray(tile(old,south+phase))),"South reuse changed."
            checks.append({"family":FAMILIES[row],"direction":("north","east","south","west")[direction],
                           "changed_native_pixels":changed,"south_pixel_identity":direction==2})
    m=json.loads((BASE/"manifest.json").read_text())
    assert sha(PREVIOUS)==m["previous_actors_native_sha256"]
    for p in m["source_rasters"]:assert sha(ROOT/p["path"])==p["sha256"]
    report={"scope":"Static file/layout/alpha/source/pair-difference/south-identity/packing-repeatability only; no runtime animation acceptance.",
            "size":[1024,384],"native_size":[256,96],"native_cell_pixels":32,"nearest_scale":4,"occupied_slots":24,
            "atlas_sha256":sha(BASE/"attack.png"),"native_sha256":sha(BASE/"attack-native.png"),
            "packing_repeatability":"PASS" if repeat else "NOT_RUN","source_hashes_verified":True,
            "previous_actors_unchanged":True,"south_native_frames_byte_identical":6,"pairs":checks}
    (BASE/"validation.json").write_text(json.dumps(report,indent=2)+"\n")
    return report

def main():
    p=argparse.ArgumentParser();p.add_argument("--prepare-references",action="store_true");p.add_argument("--verify",action="store_true");a=p.parse_args()
    if a.prepare_references:prepare()
    else:
        print(json.dumps(pack(),indent=2))
        if a.verify:print(json.dumps(validate(True),indent=2))
if __name__=="__main__":main()
