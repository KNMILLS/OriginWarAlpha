"""Export real painting exposures; no tweening, warping or painted pixels synthesized."""
from build import ROOT, ASSETS, PAINTED, CELL, sha, text_sha, dump, catalog
from PIL import Image, ImageDraw
import numpy as np
from scipy import ndimage
import copy, shutil

def components(path, minimum=10000):
    im=Image.open(path).convert('RGBA')
    labels,n=ndimage.label(np.asarray(im.getchannel('A'))>100)
    counts=np.bincount(labels.ravel()); objects=ndimage.find_objects(labels)
    boxes=[]
    for i,obj in enumerate(objects,1):
        if obj is not None and counts[i]>=minimum:
            ys,xs=obj;boxes.append((max(0,xs.start-4),max(0,ys.start-4),min(im.width,xs.stop+4),min(im.height,ys.stop+4)))
    boxes.sort(key=lambda b:b[0])
    return im,boxes

def export_poses(path, selected, names, height=333, common_scale=None, canvas=(256,368)):
    im,boxes=components(path)
    assert len(boxes)>max(selected),(path,len(boxes),selected)
    chosen=[boxes[i] for i in selected]
    scale=common_scale or height/max(b[3]-b[1]-8 for b in chosen)
    out=[]
    for box,name in zip(chosen,names):
        crop=im.crop(box)
        # Bounding boxes can overlap another object's tiny edge. Isolate the actual
        # largest connected silhouette, preserving its original antialias fringe.
        pixels=np.asarray(crop).copy()
        mask_labels,mask_count=ndimage.label(pixels[:,:,3]>100)
        areas=np.bincount(mask_labels.ravel());areas[0]=0
        keep=ndimage.binary_dilation(mask_labels==int(areas.argmax()),iterations=4)
        pixels[:,:,3]*=keep.astype(np.uint8)
        crop=Image.fromarray(pixels,'RGBA')
        crop=crop.resize((round(crop.width*scale),round(crop.height*scale)),Image.Resampling.LANCZOS)
        assert crop.width<=canvas[0] and crop.height<=canvas[1],(name,crop.size)
        frame=Image.new('RGBA',canvas)
        # Bottom contact pivot is shared by every exposure. No per-frame body stretching.
        frame.alpha_composite(crop,((canvas[0]-crop.width)//2,canvas[1]-12-crop.height))
        dest=PAINTED/'regions'/f'{name}.png';dest.parent.mkdir(parents=True,exist_ok=True);frame.save(dest)
        out.append(dict(name=name,path=str(dest.relative_to(ASSETS)).replace('\\','/'),widthCells=canvas[0]/CELL,heightCells=canvas[1]/CELL,pivotX=.5,pivotY=12/canvas[1],layer='world',source=path.name,sourceCrop=list(box),sourceScale=scale,pixelWidth=canvas[0],pixelHeight=canvas[1],sha256=sha(dest)))
    return out

def baseline_frame(frame,name):
    original=ASSETS/frame['path'];dest=PAINTED/'regions'/f'{name}.png';dest.parent.mkdir(parents=True,exist_ok=True)
    shutil.copy2(original,dest)
    result=copy.deepcopy(frame)
    result.update(name=name,path=str(dest.relative_to(ASSETS)).replace('\\','/'),sourceOrigin='existing original painted survival asset',baselinePath=frame['path'],sha256=sha(dest))
    return result

def build():
    motion=__import__('json').loads((ASSETS/'art/survival/motion/clips.json').read_text())
    equipment=__import__('json').loads((ASSETS/'art/survival/equipment-motion/clips.json').read_text())
    equipment_regions=__import__('json').loads((ASSETS/'art/survival/equipment-motion/regions.json').read_text())
    equipment_by_name={r['name']:r for r in equipment_regions['regions']}
    by_id={c['id']:c for c in motion['clips']+equipment['clips']}
    static=__import__('json').loads((ASSETS/'art/survival/regions.json').read_text())
    static_by_name={r['name']:r for r in static['regions']}
    clips=[];regions=[];bindings=[]
    # Three newly generated SOUTH keys plus an independent corrected fourth passing pose.
    south=export_poses(PAINTED/'source/officer-south-keys.png',[0,1,2],[f'officer.pistol.suit.walk.south.{i:02}' for i in range(3)])
    south+=export_poses(PAINTED/'source/officer-south-passing.png',[0],['officer.pistol.suit.walk.south.03'])
    for facing in ['north','east','south','west']:
        cid=f'officer.pistol.suit.walk.{facing}'
        if facing=='south':
            frames=south;seconds=.125;markers=[dict(frame=0,kind='contact'),dict(frame=2,kind='contact')]
            source_note='4 actual new independent painted drawings held 125ms each; not12 distinct phases'
        else:
            old=by_id[f'officer.walk.{facing}']
            frames=[baseline_frame(f,f'{cid}.{i:02}') for i,f in enumerate(old['frames'])]
            seconds=1/24;markers=old['markers'];source_note='12 existing independent paintings, preserved coherent original clip; choreography remains unaccepted'
        regions+=frames
        clips.append(dict(id=cid,frameSeconds=seconds,loop=True,cycleDistanceCells=1,referencePoseHeightCells=1.3,equipment='pistol',armor='suit',facing=facing,frames=frames,markers=markers,uniqueDrawings=len(frames),sourceNote=source_note,artisticAcceptance='candidate only; no commercial gait acceptance'))
        bindings.append(dict(actor='officer',weapon='pistol',armor='suit',action='walk',facing=facing,clip=cid))
        for action in ['fire','aim']:
            old=by_id[f'officer.{action}.{facing}'];cid=f'officer.pistol.suit.{action}.{facing}'
            # Aim shares its actual fire/aim painting and is not a new original drawing.
            frames=[baseline_frame(f,f'{cid}.{i:02}') for i,f in enumerate(old['frames'])]
            regions+=frames
            clip=copy.deepcopy(old);clip.update(id=cid,frames=frames,sourceNote='Existing original actual pistol painting, no new action art claimed')
            clips.append(clip)
            bindings.append(dict(actor='officer',weapon='pistol',armor='suit',action=action,facing=facing,clip=cid))
        r=baseline_frame(static_by_name[f'officer.{facing}'],f'officer.pistol.suit.idle.{facing}')
        regions.append(r);cid=f'officer.pistol.suit.idle.{facing}'
        clips.append(dict(id=cid,frameSeconds=.5,loop=True,frames=[r],markers=[],uniqueDrawings=1,sourceNote='Existing static actual pistol facing; no breathing/bob fabricated'))
        bindings.append(dict(actor='officer',weapon='pistol',armor='suit',action='idle',facing=facing,clip=cid))
    heavy=export_poses(PAINTED/'source/officer-rifle-heavy.png',[0,1,2,3],[f'officer.rifle.heavy.idle.{d}' for d in ['north','east','south','west']])
    regions+=heavy
    for facing,r in zip(['north','east','south','west'],heavy):
        cid=f'officer.rifle.heavy.idle.{facing}'
        clips.append(dict(id=cid,frameSeconds=.5,loop=True,frames=[r],markers=[],uniqueDrawings=1,sourceNote='New actual rifle/heavy equipment drawing; static silhouette proof only'))
        bindings.append(dict(actor='officer',weapon='rifle',armor='heavy',action='idle',facing=facing,clip=cid))
    for facing in ['north','east','south','west']:
        cid=f'officer.rifle.suit.idle.{facing}'
        r=baseline_frame(equipment_by_name[f'officer.rifle.{facing}'],cid);regions.append(r)
        clips.append(dict(id=cid,frameSeconds=.5,loop=True,frames=[r],markers=[],uniqueDrawings=1,sourceNote='Unchanged existing original rifle/suit equipment facing; static equipment proof only'))
        bindings.append(dict(actor='officer',weapon='rifle',armor='suit',action='idle',facing=facing,clip=cid))
    dump(PAINTED/'clips.json',dict(version=1,referenceCellPixels=CELL,pivotOrigin='bottom-left',clips=clips,bindings=bindings,unsupported=['rifle/suit and rifle/heavy locomotion, aim and fire','unarmed and scattergun motion','suit/heavy hit, work and death','new fully choreographed12-phase cardinal gait'],acceptance='A/B proof candidate only. Exposure slots and unique drawings are different.'))
    dump(PAINTED/'regions.json',dict(version=1,referenceCellPixels=CELL,pivotOrigin='bottom-left',pathRoot='assets',regions=regions))
    catalog(PAINTED,regions)
    # Contact strip for actual new south gait and heavy equipment. Review labels only.
    selected=south+heavy
    preview=Image.new('RGB',(256*4,410*2),(23,29,28));draw=ImageDraw.Draw(preview)
    for i,r in enumerate(selected):
        im=Image.open(ASSETS/r['path']);preview.paste(im,((i%4)*256,(i//4)*410+28),im)
        draw.text(((i%4)*256+8,(i//4)*410+8),r['name'].replace('officer.',''),fill=(220,223,217))
    review=PAINTED/'review';review.mkdir(parents=True,exist_ok=True);preview.save(review/'new-paintings-contact.jpg',quality=95)
    for c in clips:
        if '.walk.' not in c['id']:continue
        sequence=[]
        for r in c['frames']:
            im=Image.open(ASSETS/r['path']).convert('RGBA');frame=Image.new('RGBA',(300,400),(23,29,28,255));frame.alpha_composite(im,((300-im.width)//2,400-im.height));sequence.append(frame.convert('RGB'))
        sequence[0].save(review/(c['id']+'.webp'),save_all=True,append_images=sequence[1:],duration=round(c['frameSeconds']*1000),loop=0,lossless=True)
    sources=[]
    for image,prompt,status,reason in [
        ('officer-walk-east-v1.png','officer-walk-east-v1-prompt.txt','rejected','Actual passing poses, but lead-leg swap unreliable.'),
        ('officer-walk-east-v2.png','officer-walk-east-v2-prompt.txt','rejected','Targeted bottom-row edit still repeats lead-leg choreography.'),
        ('officer-south-keys.png','officer-south-keys-prompt.txt','partly-selected','First3 key paintings selected; fourth passing pose wrong support side and not selected.'),
        ('officer-walk-south-v1.png','officer-walk-south-v1-prompt.txt','rejected','12-pose expansion collapses most frames into one lead-leg stance.'),
        ('officer-south-passing.png','officer-south-passing-prompt.txt','selected','Single independently edited opposite passing pose; integrated continuity still requires review.'),
        ('officer-rifle-heavy.png','officer-rifle-heavy-prompt.txt','selected-static-only','4 actual cardinal low-ready equipment silhouettes, not motion.')]:
        sources.append(dict(file=image,sha256=sha(PAINTED/'source'/image),prompt=prompt,promptSha256NormalizedLF=text_sha(PAINTED/'source'/prompt),status=status,reason=reason,tool='built-in image_gen'))
    refs=['art/survival/source/owner-player-reference.png','art/survival/source/officer-turnaround.png','art/survival/regions/item.armor-heavy.png','art/survival/regions/item.rifle.png']
    dump(PAINTED/'source/GENERATION.json',dict(task='AP-PAINTED',base='604c910',sources=sources,newUniqueSelectedDrawings=8,newSouthGaitDrawings=4,newEquipmentDrawings=4,references=[dict(path=p,sha256=sha(ASSETS/p)) for p in refs],baselineSources=['art/survival/motion/clips.json','art/survival/equipment-motion/clips.json','art/survival/equipment-motion/regions.json','art/survival/regions.json'],editableCatalog='Named flattened region layers, not original brushstroke layers or a skeleton.',rules='No morphing, puppet warp, mirrored limbs, generated code painting or fabricated unique-phase counts.'))
    print('painted',len(clips),'clips;',len(regions),'region bindings; new unique selected paintings8; south4 drawings at8fps')

if __name__=='__main__':build()
