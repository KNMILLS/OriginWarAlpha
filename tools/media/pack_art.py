"""Deterministic atlas export: crop generated source cells, anchor frames, resize.
This pipeline packs source artwork; it does not paint substitute sprites.
Source sheet rasters remain unchanged under assets/art/source/.
"""
from __future__ import annotations
import hashlib,json
from pathlib import Path
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/"assets"/"art"
NAMES=["docking_floor","habitation_floor","medical_floor","industrial_floor","command_floor","wall","wall_corner","glass_window",
"door_closed","door_open","door_locked","floor_low_gravity","floor_no_gravity","hazard","cover","switch_off",
"switch_on","hatch_off","hatch_on","terminal","oxygen_tank","battery","medicine","pistol",
"rifle","scattergun","armor_light","armor_heavy","pipes","console","debris","alarm",
"player_walk_0","player_walk_1","player_walk_2","player_walk_3","alice_walk_0","alice_walk_1","alice_walk_2","alice_walk_3",
"sirs_walk_0","sirs_walk_1","sirs_walk_2","sirs_walk_3","add_walk_0","add_walk_1","add_walk_2","add_walk_3",
"rover_walk_0","rover_walk_1","rover_walk_2","rover_walk_3","west","carmack","operator","constructor",
"hacking","impact","explosion","phantom","selection","shadow","beam","blank"]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def cell(im,i,cols,rows):
    x,y=i%cols,i//cols;w,h=im.size
    return im.crop((round(x*w/cols),round(y*h/rows),round((x+1)*w/cols),round((y+1)*h/rows)))
def strong_bbox(im):
    alpha=np.asarray(im.getchannel("A"))
    mask=Image.fromarray(np.uint8(alpha>=72)*255)
    return mask.getbbox()
def main():
    initial=ART/"source"/"station-generated.png"
    original=ART/"source"/"station-cleaned-generated.png"
    floorpath=ART/"source"/"station-floors-generated.png"
    src=Image.open(original).convert("RGBA");floors=Image.open(floorpath).convert("RGBA")
    parts=[cell(src,i,8,8) for i in range(64)]
    # Floor source is a new coherent eight-surface sheet, not room miniatures.
    floor_map={0:0,1:1,2:2,3:3,4:4,11:5,12:6,13:7}
    for dest,source in floor_map.items():parts[dest]=cell(floors,source,4,2)
    native=Image.new("RGBA",(256,256))
    actor_groups=[list(range(32,36)),list(range(36,40)),list(range(40,44)),
                  list(range(44,48)),list(range(48,52)),[52],[53],[54],[55]]
    actor_info={}
    for group in actor_groups:
        boxes={i:strong_bbox(parts[i]) for i in group}
        assert all(boxes.values())
        maxw=max(b[2]-b[0] for b in boxes.values());maxh=max(b[3]-b[1] for b in boxes.values())
        target_height=25 if group[0]==48 else 28
        scale=min(28/maxw,target_height/maxh)
        for i in group:
            crop=parts[i].crop(boxes[i])
            size=(max(1,round(crop.width*scale)),max(1,round(crop.height*scale)))
            glyph=crop.resize(size,Image.Resampling.LANCZOS)
            dest=Image.new("RGBA",(32,32))
            anchor=(round((32-size[0])/2),30-size[1])
            dest.alpha_composite(glyph,anchor)
            actor_info[i]={"foot_anchor_native":[16,30],"bounds":boxes[i],"export_size_native":size}
            parts[i]=dest
    entries=[]
    for i,p in enumerate(parts):
        if i==63:p=Image.new("RGBA",(32,32))
        elif i not in actor_info:p=p.resize((32,32),Image.Resampling.LANCZOS)
        if i not in floor_map and i < 56:
            # Agreed export cleanup: discard only near-transparent extraction
            # noise on isolated objects/actors. Preserve every alpha>=32 pixel;
            # floor and effect alpha are never thresholded.
            pixels=np.asarray(p).copy()
            pixels[pixels[:,:,3]<32]=0
            p=Image.fromarray(pixels,"RGBA")
        native.alpha_composite(p,((i%8)*32,(i//8)*32))
        entry={"id":i,"name":NAMES[i],"column":i%8,"row":i//8,
               "region_pixels":[(i%8)*128,(i//8)*128,128,128],
               "source":"station-floors-generated.png" if i in floor_map else "station-cleaned-generated.png"}
        if i in actor_info:entry.update(actor_info[i])
        entries.append(entry)
    native.save(ART/"source"/"station-native.png",optimize=True)
    native.resize((1024,1024),Image.Resampling.NEAREST).save(ART/"station.png",optimize=True)
    preview=Path(__file__).parent/".render";preview.mkdir(exist_ok=True)
    frames=[]
    for f in range(4):
        canvas=Image.new("RGBA",(5*128,128),(20,30,36,255))
        for g,start in enumerate((32,36,40,44,48)):
            glyph=native.crop(((start+f)%8*32,(start+f)//8*32,(start+f)%8*32+32,(start+f)//8*32+32))
            canvas.alpha_composite(glyph.resize((128,128),Image.Resampling.NEAREST),(g*128,0))
        frames.append(canvas.convert("RGB"))
    frames[0].save(preview/"walk-cycles.gif",save_all=True,append_images=frames[1:],duration=150,loop=0)
    manifest={"task":"M1-MEDIA","status":"representative illustrated candidate; in-engine acceptance pending",
       "generation_method":"OpenAI built-in image_gen: transparent original atlas, focused transparency-edge cleanup, and separate opaque floor sheet; no external artwork or legacy assets.",
       "authoring_prompts":["tools/media/prompts/station_atlas.txt","tools/media/prompts/station_cleanup.txt","tools/media/prompts/station_floors.txt"],
       "export_source":"tools/media/pack_art.py",
       "runtime_atlas":{"path":"assets/art/station.png","width":1024,"height":1024,"columns":8,"rows":8,
          "cell_pixels":128,"native_cell_pixels":32,"nearest_neighbor_scale":4,"alpha":"RGBA; slot63 intentionally empty"},
       "source_rasters":[{"path":"assets/art/source/station-cleaned-generated.png","size":src.size,"sha256":sha(original)},
          {"path":"assets/art/source/station-generated.png","role":"initial edit target retained as authoring source","sha256":sha(initial)},
          {"path":"assets/art/source/station-floors-generated.png","size":floors.size,"sha256":sha(floorpath)}],
       "export_notes":["Generated source dimensions differ from requested dimensions; equal normalized source regions are cropped and exported deterministically.",
          "Floor sheet replaces slots 0-4 and 11-13; initial furniture-like generated floor cells are not runtime art.",
          "Actor groups share fixed per-group scale and bottom-center anchor. No renderer, animation timing or model behavior is changed.",
          "Native 32px export is upscaled 4x with nearest-neighbor sampling; artwork is generated raster reduced into that grid.",
          "Lead-authorized alpha cleanup: clear alpha below 32 on isolated object/actor slots only (5-10,14-55); all alpha>=32 pixels and all floor/effect alpha remain unchanged."],
       "animation_groups":{"player":[32,33,34,35],"alice":[36,37,38,39],"sirs":[40,41,42,43],"add":[44,45,46,47],"rover":[48,49,50,51]},
       "limitations":["Only south-facing four-frame cycles; other facings require a later authored production pass.",
          "Generated fine detail is reduced at native scale; weapon/prop readability requires actual game inspection.",
          "Some props use shallow illustrated depth rather than pure plan view; review consistency in-engine.",
          "Floor texture repetition/seams, animation cadence, effects and all sector coverage still require running-game acceptance.",
          "Production art/UI/animation completion is not claimed."],
       "slots":entries}
    (ART/"manifest.json").write_text(json.dumps(manifest,indent=2)+"\n")
    print(json.dumps({"atlas":"assets/art/station.png","size":[1024,1024],"native":[256,256],"slots":64,
                      "source_dimensions":[src.size,floors.size],"sha256":sha(ART/"station.png")},indent=2))
if __name__=="__main__":main()
