"""Original generated animation/sector export. Existing station art is read-only.
--prepare-references extracts small existing-project references for image_gen.
Final packing is added after inspecting the generated source sheets.
"""
from __future__ import annotations
import argparse,hashlib,json
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/"assets"/"art"
ANIM=ART/"animation"
SECT=ART/"sectors"
FAMILIES=("player","alice","sirs","add","rover")
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def region(im,i,cols=8,rows=8):
    x,y=i%cols,i//cols;w,h=im.size
    return im.crop((round(x*w/cols),round(y*h/rows),round((x+1)*w/cols),round((y+1)*h/rows)))
def prepare_references():
    base=Image.open(ART/"source"/"station-native.png").convert("RGBA")
    target=ANIM/"source"/"references";target.mkdir(parents=True,exist_ok=True)
    for row,family in enumerate(FAMILIES):
        strip=Image.new("RGBA",(128,32))
        for frame in range(4):strip.alpha_composite(region(base,32+row*4+frame),(frame*32,0))
        strip.resize((512,128),Image.Resampling.NEAREST).save(target/(family+".png"))
    strip=Image.new("RGBA",(64,32))
    strip.alpha_composite(region(base,54),(0,0));strip.alpha_composite(region(base,55),(32,0))
    strip.resize((256,128),Image.Resampling.NEAREST).save(target/"operator-constructor.png")
    print("Prepared six compact reference images from unchanged existing project art.")
def source_cell(path,index,cols,rows):
    path=Path(path);im=Image.open(path).convert("RGBA")
    return region(im,index,cols,rows).resize((128,128),Image.Resampling.LANCZOS)
def bbox(im,cutoff=72):
    return Image.fromarray(np.uint8(np.asarray(im.getchannel("A"))>=cutoff)*255).getbbox()
def cleared(im,cutoff=32):
    p=np.asarray(im).copy();p[p[:,:,3]<cutoff]=0
    return Image.fromarray(p,"RGBA")
def fit_pairs(parts,groups):
    exported={};details={}
    for group,limits,centered in groups:
        boxes={i:bbox(parts[i]) for i in group}
        assert all(boxes.values()),"Empty generated actor"
        maxw=max(b[2]-b[0] for b in boxes.values());maxh=max(b[3]-b[1] for b in boxes.values())
        scale=min(limits[0]/maxw,limits[1]/maxh)
        for i in group:
            crop=parts[i].crop(boxes[i]);size=(max(1,round(crop.width*scale)),max(1,round(crop.height*scale)))
            glyph=crop.resize(size,Image.Resampling.LANCZOS);dest=Image.new("RGBA",(32,32))
            x=round((32-size[0])/2);y=round((32-size[1])/2) if centered else 30-size[1]
            dest.alpha_composite(glyph,(x,y))
            exported[i]=dest if i in (54,55) else cleared(dest)
            details[i]={"source_bbox_on_128px_cell":boxes[i],"native_size":size,"anchor":[16,16] if centered else [16,30],
                        "alpha_cutoff":0 if i in (54,55) else 32}
    return exported,details
def actors():
    src=ANIM/"source";parts={};mapping={}
    def put(dest,path,index,cols,rows,note):
        parts[dest]=source_cell(src/path,index,cols,rows)
        mapping[dest]={"source":"assets/art/animation/source/"+path,"source_cell":index,"source_grid":[cols,rows],"note":note}
    for row,family in ((0,"player"),(2,"sirs"),(3,"add")):
        for c in range(8):put(row*8+c,family+".png",c,4,2,"Inspected four cardinal walk pairs.")
    for c in range(2):
        put(8+c,"alice-north.png",c,2,1,"Focused corrected rear view.")
        put(10+c,"alice-east.png",c,2,1,"Focused corrected right-facing pair.")
        put(12+c,"failures/alice-initial.png",4+c,4,2,"Only valid south pair from initial mixed-direction source is used.")
        put(14+c,"alice-west.png",c,2,1,"Focused corrected left-facing pair.")
        put(32+c,"rover-north.png",c,2,1,"Focused rear correction: head/neck point up; lower amber element is rear service lamp.")
    for c in range(2,8):put(32+c,"failures/rover-initial.png",c,4,2,"Only valid east/south/west pairs are used.")
    for c in range(16):put(40+c,"actions.png",c,4,4,"Authored attack/collapse/wreck/armed/explosion phases.")
    for c in range(8):put(56+c,"machines.png",c,4,2,"Selected source after image_gen removed painted ground patches.")
    groups=[]
    for row in range(5):
        for d in range(4):groups.append(([row*8+d*2,row*8+d*2+1],(28,25 if row==4 else 28),False))
    for first in range(40,64,2):
        groups.append(([first,first+1],(30,30) if first==54 else ((28,25) if first==52 else (28,28)),first==54))
    packed,detail=fit_pairs(parts,groups)
    native=Image.new("RGBA",(256,256));slots=[]
    action_names=["player_attack","player_death","sirs_attack","sirs_death","add_attack","add_death","rover_armed","rover_explosion",
                  "camera","turret","operator","constructor"]
    for i in range(64):
        native.alpha_composite(packed[i],((i%8)*32,(i//8)*32))
        name=(FAMILIES[i//8]+"_"+("north","east","south","west")[(i%8)//2] if i<40 else action_names[(i-40)//2])+"_"+str(i%2)
        slots.append({"id":i,"name":name,"column":i%8,"row":i//8,**mapping[i],**detail[i]})
    native.save(ANIM/"actors-native.png",optimize=True)
    native.resize((1024,1024),Image.Resampling.NEAREST).save(ANIM/"actors.png",optimize=True)
    review=ANIM/"review";review.mkdir(exist_ok=True)
    dark=Image.new("RGBA",(1024,1024),(20,30,36,255));dark.alpha_composite(native.resize((1024,1024),Image.Resampling.NEAREST))
    dark.convert("RGB").save(review/"actors-4x-on-dark.png",optimize=True)
    phases=[]
    for phase in range(2):
        board=Image.new("RGBA",(512,640),(20,30,36,255))
        for row in range(5):
            for direction in range(4):
                tile=packed[row*8+direction*2+phase].resize((128,128),Image.Resampling.NEAREST)
                board.alpha_composite(tile,(direction*128,row*128))
        phases.append(board.convert("RGB"))
    phases[0].save(review/"directions.gif",save_all=True,append_images=phases[1:],duration=180,loop=0)
    actions_review=[]
    for phase in range(2):
        board=Image.new("RGBA",(6*128,2*128),(20,30,36,255))
        for pair,first in enumerate(range(40,64,2)):
            board.alpha_composite(packed[first+phase].resize((128,128),Image.Resampling.NEAREST),((pair%6)*128,(pair//6)*128))
        actions_review.append(board.convert("RGB"))
    actions_review[0].save(review/"actions.gif",save_all=True,append_images=actions_review[1:],duration=220,loop=0)
    manifest={"task":"M5-ART","status":"directional/action source and native export inspected; in-game acceptance pending",
        "atlas":{"path":"assets/art/animation/actors.png","native":"assets/art/animation/actors-native.png",
                 "columns":8,"rows":8,"cell_pixels":128,"native_cell_pixels":32,"nearest_scale":4,"alpha":"RGBA"},
        "layout":"Rows0-4 player/alice/sirs/add/rover; columns north2,east2,south2,west2. Row5 playerAttack2/playerDeath2/SIRSattack2/SIRSdeath2. Row6 ADDattack2/ADDdeath2/ROVERarmed2/ROVERexplosion2. Row7 camera2/turret2/operator2/Constructor2.",
        "provenance":"Original OpenAI built-in image_gen production from recorded prompts and existing original project-generated actor references. No external artwork/pack or legacy media.",
        "source_export":"tools/media/pack_animation.py","reference_station_sha256":sha(ART/"station.png"),
        "cleanup":"Normalize source cells; preserve source rasters; fixed per-pair scale/anchor; clear alpha below32 only on non-explosion isolated sprites. Explosion alpha retained.",
        "first_failures":["Initial ALICE sheet had front-looking north and mixed side directions. Only its valid south pair is used; north/east/west were newly generated as focused pairs.",
                          "Initial ROVER north faced forward. Only its valid other directions are used; a new rear pair supplies north.",
                          "Initial machine sheet painted gray ground patches. Focused built-in image edit removed them; initial target remains preserved."],
        "manual_review":["Cardinal facings and actual limb-phase differences inspected in source sheets.",
                         "Humans have two legs/arms; ROVER remains four-legged; ALICE has no attack/weapon frames.",
                         "Attack/death pairs show aim/recoil then collapse/prone/wreck; explosion phases differ in extent and debris.",
                         "Camera pan, turret discharge, operator steps and Constructor arms are distinct authored poses."],
        "limitations":["Two-frame cycles/actions are compact and need in-engine cadence/motion review; death pairs are sequential states, not looping walks.",
                       "Attacks/deaths currently face south; four-direction attacks/deaths would require more slots and a separate contract.",
                       "No new West/Carmack directional set was requested; their existing station atlas sprites remain unchanged.",
                       "ROVER north rear service lamp remains amber; verify orientation clarity against the front optic at native scale.",
                       "View uses elevated illustrated top-down/three-quarter depth matching existing art, not a strict orthographic CAD plan.",
                       "No running-game, human acceptance, combat timing or final full-art completion is claimed."],
        "slots":slots}
    manifest["source_rasters"]=[{"path":str(p.relative_to(ROOT)).replace("\\","/"),"sha256":sha(p)} for p in sorted(src.rglob("*.png"))]
    (ANIM/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    return {"atlas_sha256":sha(ANIM/"actors.png"),"native_sha256":sha(ANIM/"actors-native.png"),"slots":64}
def floor_surface(im):
    alpha=np.asarray(im.getchannel("A"));box=bbox(im,250)
    assert box,"No opaque generated floor region"
    # Select real opaque interior pixels; do not paint a substitute floor or
    # turn the transparent sheet padding into black walkable corners.
    for fraction in (.08,.10,.12,.15,.18,.22,.25):
        x0,y0,x1,y1=box;dx=round((x1-x0)*fraction);dy=round((y1-y0)*fraction)
        crop=(x0+dx,y0+dy,x1-dx,y1-dy)
        if alpha[crop[1]:crop[3],crop[0]:crop[2]].min()>=250:
            tile=im.crop(crop).resize((32,32),Image.Resampling.LANCZOS);tile.putalpha(255)
            return tile,crop
    raise ValueError("Generated floor has no suitable solid interior; preserve source and report gap.")
def sectors():
    source=SECT/"source"/"sector-kit.png";special=SECT/"source"/"escape-mars.png"
    kit=Image.open(source).convert("RGBA");end=Image.open(special).convert("RGBA")
    native=Image.new("RGBA",(256,128));slots=[]
    # Selected generated sheet is 1145x1374 and did not honor equal row
    # spacing. These inspected transparent troughs separate complete rows;
    # equal-height crops would splice neighboring furniture into each tile.
    assert kit.size==(1145,1374),"Sector source dimensions changed; inspect row boundaries before exporting."
    row_edges=(0,202,404,631,858,1072,1374)
    sector_names=("docking_security","habitation_maintenance","medical_research","defense_industry","command_evacuation")
    kinds=("floor","wall","furniture","console_lighting","clutter","story_anchor")
    for i in range(32):
        if i<30:
            column,row=i%5,i//5
            source_box=(round(column*kit.width/5),row_edges[row],round((column+1)*kit.width/5),row_edges[row+1])
            p=kit.crop(source_box).resize((128,128),Image.Resampling.LANCZOS)
        else:
            source_box=None;p=region(end,i-30,2,1).resize((128,128),Image.Resampling.LANCZOS)
        if i<5:
            tile,crop=floor_surface(p);note={"opaque_source_interior_crop_on_128px":crop}
        else:
            box=bbox(p);assert box
            cropped=p.crop(box)
            if i<10:
                tile=cleared(cropped.resize((32,32),Image.Resampling.LANCZOS));note={"wall_fit":"cell fill"}
            else:
                scale=min(28/cropped.width,28/cropped.height)
                size=(max(1,round(cropped.width*scale)),max(1,round(cropped.height*scale)))
                tile=Image.new("RGBA",(32,32))
                tile.alpha_composite(cropped.resize(size,Image.Resampling.LANCZOS),(round((32-size[0])/2),30-size[1]))
                tile=cleared(tile);note={"prop_size_native":size,"anchor":[16,30],"alpha_cutoff":32}
        native.alpha_composite(tile,((i%8)*32,(i//8)*32))
        name=(sector_names[i%5]+"_"+kinds[i//5]) if i<30 else ("escape_pod" if i==30 else "mars_window")
        slots.append({"id":i,"name":name,"column":i%8,"row":i//8,"source_cell":i if i<30 else i-30,
                      "source_grid":[5,6] if i<30 else [2,1],"source_pixel_box":source_box,
                      "source":"sector-kit.png" if i<30 else "escape-mars.png",**note})
    native.save(SECT/"sectors-native.png",optimize=True)
    native.resize((1024,512),Image.Resampling.NEAREST).save(SECT/"sectors.png",optimize=True)
    review=SECT/"review";review.mkdir(exist_ok=True)
    board=Image.new("RGBA",(1024,512),(20,30,36,255));board.alpha_composite(native.resize((1024,512),Image.Resampling.NEAREST))
    board.convert("RGB").save(review/"sectors-4x-on-dark.png",optimize=True)
    manifest={"task":"M5-ART","status":"five-sector dressing source/native export inspected; in-game acceptance pending",
        "atlas":{"path":"assets/art/sectors/sectors.png","native":"assets/art/sectors/sectors-native.png","columns":8,"rows":4,"cell_pixels":128,"native_cell_pixels":32,"nearest_scale":4},
        "layout":"0-4 floors;5-9 matching walls;10-14 furniture;15-19 console/light;20-24 clutter;25-29 story anchors, always docking/habitation/medical/industry/command.30 escapePod,31 MarsWindow.",
        "provenance":"Original OpenAI built-in image_gen artwork using existing original project art as palette/material reference, with recorded prompts. No external pack, stock or legacy artwork.",
        "source_export":"tools/media/pack_animation.py","reference_station_sha256":sha(ART/"station.png"),
        "export_notes":["Opaque floors are sampled from actual opaque interiors of generated floor cells; no opaque black sheet padding is used.",
                        "Decorative props have true transparent empty space, fixed native anchors and low-alpha speck cleanup.",
                        "Raw source sheets are unchanged; explicit inspected row boundaries, regions/anchors and 4x nearest export are deterministic.",
                        "First equal-row pack included neighboring prop fragments because the source ignored equal row spacing; selected source row edges are0,202,404,631,858,1072,1374."],
        "manual_review":["Five sectors differ in panel structure, materials, furniture, equipment and clutter, not only hue.",
                         "Arrival, abandoned personal objects, research cradle, industrial control and broadcast interlock are visual story dressing; no invented text/lore is embedded.",
                         "Escape pod and Mars observation viewport are distinct illustrated objects with opaque interiors/transparent outside."],
        "limitations":["Generated dressing uses shallow three-quarter depth consistent with existing props; verify occlusion and scale in actual overhead gameplay.",
                       "Floors use cropped repeating panels; final seam/repetition quality and lighting require real floor-per-sector captures.",
                       "The five style sets supplement existing doors/corners/terrain; they do not replace every combination of directional wall/corner/door art.",
                       "Story-anchor placement and narrative behavior remain lead-owned; dressing is not proof of implemented story beats.",
                       "No actual gameplay screenshots or final-art acceptance are claimed."],
        "source_rasters":[{"path":str(p.relative_to(ROOT)).replace("\\","/"),"size":Image.open(p).size,"sha256":sha(p)} for p in (source,special)],
        "slots":slots}
    (SECT/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    return {"atlas_sha256":sha(SECT/"sectors.png"),"native_sha256":sha(SECT/"sectors-native.png"),"slots":32}

def finale():
    base=ART/"finale";source=base/"source"/"finale-generated.png"
    im=Image.open(source).convert("RGB")
    assert im.width==im.height*2,"Finale source aspect changed; inspect before exporting."
    native=im.resize((512,256),Image.Resampling.LANCZOS)
    native.save(base/"finale-native.png",optimize=True)
    native.resize((1024,512),Image.Resampling.NEAREST).save(base/"finale.png",optimize=True)
    manifest={"task":"M5-ART finale addendum","status":"source/export visually inspected; ending-only epilogue integration acceptance pending",
       "source":{"path":"assets/art/finale/source/finale-generated.png","size":im.size,"sha256":sha(source)},
       "prompt":"assets/art/finale/source/prompt.txt","export_source":"tools/media/pack_animation.py",
       "runtime":{"path":"assets/art/finale/finale.png","size":[1024,512],"aspect":"2:1","mode":"RGB opaque",
                  "native":"assets/art/finale/finale-native.png","native_size":[512,256],"nearest_scale":2},
       "provenance":"Original OpenAI built-in image_gen illustration, using only original project escape-pod/Mars-window art as visual reference; no external artwork or legacy source media.",
       "reference_sha256":sha(SECT/"source"/"escape-mars.png"),
       "content":"Mars horizon, one departing ivory/steel/cyan escape pod, distant orbital science station destructive burst/debris, dark upper-center space; no text/logos or extra story characters.",
       "manual_review":["Mars/pod/station destruction read clearly and match the industrial palette.","No UI frame, atlas labels, people, extra craft, mech or invented colony is present.","Dark upper-center space remains available for the epilogue overlay; lead selected ending-only use so the title does not spoil the sacrifice."],
       "limitations":["Static illustration, not an animated broadcast/destruction/pod sequence.","Actual overlay readability, event timing and audio remain root integration gates.","No runtime screenshot or canonical-ending acceptance is claimed."]}
    (base/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    return {"atlas_sha256":sha(base/"finale.png"),"native_sha256":sha(base/"finale-native.png"),"size":[1024,512]}

def main():
    p=argparse.ArgumentParser();p.add_argument("--prepare-references",action="store_true");a=p.parse_args()
    if a.prepare_references:prepare_references()
    else:
        print(json.dumps({"actors":actors(),"sectors":sectors(),"finale":finale()},indent=2))
if __name__=="__main__":main()
