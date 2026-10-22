"""Original Blender officer refinement. Pilot only until independent visual review.

Blender --background --factory-startup --python tools/media/officer_production/build.py
Every image is a real scene render; no frame filtering or image-based pose repair.
"""
from pathlib import Path
import bpy, math, json, hashlib, sys, time
import numpy as np
from mathutils import Vector, Matrix
from bpy_extras.object_utils import world_to_camera_view

START=time.perf_counter()
LOOK_ONLY='--look-only' in sys.argv
PILOT_V2='--pilot-v2' in sys.argv
BODY_LOOK='--body-look' in sys.argv
RUNTIME_SCALE=1 if '--master' in sys.argv else .75
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/art/production/officer'; SRC=OUT/'source'; FRAMES=OUT/'regions'; REVIEW=OUT/'review'
for p in [OUT,SRC,FRAMES,REVIEW]:p.mkdir(parents=True,exist_ok=True)
EXPORT_OUT=OUT
if PILOT_V2:
    EXPORT_OUT=REVIEW/'iterations/v2-pilot';EXPORT_OUT.mkdir(parents=True,exist_ok=True)
    REVIEW=EXPORT_OUT;FRAMES=EXPORT_OUT/'regions';FRAMES.mkdir(exist_ok=True)
if BODY_LOOK:
    EXPORT_OUT=OUT/'review/iterations/v3-anatomical-suit';EXPORT_OUT.mkdir(parents=True,exist_ok=True)
    REVIEW=EXPORT_OUT;FRAMES=EXPORT_OUT/'regions';FRAMES.mkdir(exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE_NEXT'
scene.render.resolution_x=384;scene.render.resolution_y=448;scene.render.resolution_percentage=round(RUNTIME_SCALE*100)
scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA';scene.render.image_settings.color_depth='8'
scene.render.film_transparent=True;scene.render.fps=60;scene.render.use_compositing=False;scene.render.use_sequencer=False
scene.render.image_settings.compression=65
scene.world.color=(.13,.14,.135)
scene.view_settings.view_transform='Standard';scene.view_settings.look='Medium High Contrast'

def material(name,base,worn=False):
    """Bounded diffuse cel values, low-frequency wear and occluded seam shadows."""
    m=bpy.data.materials.new(name);m.use_nodes=True
    n=m.node_tree.nodes;n.clear();l=m.node_tree.links
    out=n.new('ShaderNodeOutputMaterial');diff=n.new('ShaderNodeBsdfDiffuse');diff.inputs['Color'].default_value=(.8,.8,.8,1)
    diff.inputs['Roughness'].default_value=1;rgb=n.new('ShaderNodeShaderToRGB');l.new(diff.outputs[0],rgb.inputs[0])
    ramp=n.new('ShaderNodeValToRGB');ramp.color_ramp.interpolation='EASE'
    ramp.color_ramp.elements.remove(ramp.color_ramp.elements[1])
    for i,(position,factor) in enumerate([(.12,.38),(.38,.68),(.68,.94)]):
        e=ramp.color_ramp.elements[0] if i==0 else ramp.color_ramp.elements.new(position)
        e.position=position;e.color=(*(c*factor for c in base),1)
    l.new(rgb.outputs['Color'],ramp.inputs[0]);color=ramp.outputs['Color']
    if worn:
        noise=n.new('ShaderNodeTexNoise');noise.inputs['Scale'].default_value=24;noise.inputs['Detail'].default_value=2
        wear=n.new('ShaderNodeValToRGB');wear.color_ramp.elements[0].color=(.80,.81,.78,1);wear.color_ramp.elements[1].color=(1,1,1,1)
        l.new(noise.outputs['Fac'],wear.inputs[0]);mix=n.new('ShaderNodeMixRGB');mix.blend_type='MULTIPLY';mix.inputs[0].default_value=1
        l.new(color,mix.inputs[1]);l.new(wear.outputs[0],mix.inputs[2]);color=mix.outputs[0]
    # Geometry-aware crevice shadow supplies the painted dark overlap hierarchy.
    # This is evaluated on the deformed model, never a per-frame image filter.
    ao=n.new('ShaderNodeAmbientOcclusion');ao.inputs['Distance'].default_value=.12
    ao.inputs['Color'].default_value=(1,1,1,1)
    crevice=n.new('ShaderNodeMixRGB');crevice.blend_type='MULTIPLY';crevice.inputs[0].default_value=.52
    l.new(color,crevice.inputs[1]);l.new(ao.outputs['Color'],crevice.inputs[2]);color=crevice.outputs[0]
    emission=n.new('ShaderNodeEmission');emission.inputs['Strength'].default_value=1;l.new(color,emission.inputs[0]);l.new(emission.outputs[0],out.inputs['Surface'])
    return m

ivory=material('ivory shell: bounded broad painted values',(.70,.63,.46),True)
fabric=material('tailored pressure fabric warm gray ivory',(.44,.415,.32),True)
darkcloth=material('charcoal continuous bellows weave',(.035,.046,.037),True)
steel=material('matte graphite hardware',(.135,.155,.14),True)
edge=material('worn edge muted pewter',(.29,.31,.27),True)
ink=material('selected dark contours',(.018,.025,.020))
ink.use_backface_culling=True
skin=material('guarded human face plane',(.43,.325,.225))
skinShadow=material('brow and cheek shadow',(.20,.17,.135))
eye=material('dark blue eyes',(.045,.10,.12))
amber=material('functional amber tag',(.47,.235,.055))
glass=material('dark visor interior',(.022,.045,.043))
objects=[];outline_objects=[];weapon_objects=[]

rests={
 'pelvis':((0,0,.97),(0,0,1.08)),'torso':((0,0,1.07),(0,0,1.50)),'head':((0,0,1.50),(0,0,1.85)),
}
for side,sgn in [('R',-1),('L',1)]:
    x=sgn*.115
    rests['thigh.'+side]=((x,0,.97),(x,0,.50));rests['shin.'+side]=((x,0,.50),(x,0,.085));rests['foot.'+side]=((x,0,.085),(x,-.15,.085))
    rests['upperarm.'+side]=((sgn*.235,0,1.44),(sgn*.255,-.015,1.19))
    rests['forearm.'+side]=((sgn*.255,-.015,1.19),(sgn*.26,-.07,.965))
    rests['hand.'+side]=((sgn*.26,-.07,.965),(sgn*.26,-.10,.875))
data=bpy.data.armatures.new('Officer adult garment rig');rig=bpy.data.objects.new('Officer_Rig',data);bpy.context.collection.objects.link(rig)
bpy.context.view_layer.objects.active=rig;rig.select_set(True);bpy.ops.object.mode_set(mode='EDIT')
for name,(a,b) in rests.items():bone=data.edit_bones.new(name);bone.head=a;bone.tail=b
bpy.ops.object.mode_set(mode='OBJECT');rig.select_set(False);rig.show_in_front=True

def finish(ob,name,mat,bone=None,weights=None,outline=False):
    ob.name=name;ob.data.materials.append(mat)
    bpy.context.view_layer.objects.active=ob;ob.select_set(True);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);ob.select_set(False)
    for p in ob.data.polygons:p.use_smooth=True
    if bone or weights:
        for bn in ([bone] if bone else sorted({bn for w in weights for bn in w})):
            group=ob.vertex_groups.new(name=bn)
            if bone:group.add(range(len(ob.data.vertices)),1,'REPLACE')
            else:
                for i,w in enumerate(weights):
                    if w.get(bn,0)>0:group.add([i],w[bn],'REPLACE')
        mod=ob.modifiers.new('actual weighted garment deformation','ARMATURE');mod.object=rig;ob.parent=rig
    objects.append(ob)
    if outline:outline_objects.append(ob)
    return ob

def uv(name,loc,scale,mat,bone,outline=False):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32,ring_count=20,location=loc);ob=bpy.context.object;ob.scale=scale
    return finish(ob,name,mat,bone,outline=outline)
def box(name,loc,scale,mat,bone,bevel=.006,outline=False):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc);ob=bpy.context.object;ob.scale=scale
    bpy.context.view_layer.objects.active=ob;bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
    mod=ob.modifiers.new('small practical chamfer','BEVEL');mod.width=bevel;mod.segments=2
    return finish(ob,name,mat,bone,outline=outline)
def tube(name,points,radius,mat,bone):
    curve=bpy.data.curves.new(name,'CURVE');curve.dimensions='3D';curve.bevel_depth=radius;curve.bevel_resolution=2
    s=curve.splines.new('POLY');s.points.add(len(points)-1)
    for p,c in zip(s.points,points):p.co=(*c,1)
    ob=bpy.data.objects.new(name,curve);bpy.context.collection.objects.link(ob);bpy.context.view_layer.objects.active=ob;ob.select_set(True)
    bpy.ops.object.convert(target='MESH');ob.select_set(False);return finish(ob,name,mat,bone)
def loft(name,rings,mat,bone=None,weights=None,outline=False):
    verts=[];faces=[];vweights=[];n=24
    for j,(x,y,z,rx,ry) in enumerate(rings):
        for i in range(n):
            a=math.tau*i/n;verts.append((x+rx*math.cos(a),y+ry*math.sin(a),z))
            if weights:vweights.append(weights[j])
    for j in range(len(rings)-1):
        for i in range(n):faces.append((j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i))
    faces.extend([tuple(reversed(range(n))),tuple((len(rings)-1)*n+i for i in range(n))])
    me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update();ob=bpy.data.objects.new(name,me);bpy.context.collection.objects.link(ob)
    return finish(ob,name,mat,bone,vweights if weights else None,outline)
def plate(name,points,depth,mat,bone,outline=False):
    verts=[tuple(p) for p in points]+[(x,y+depth,z) for x,y,z in points];n=len(points)
    faces=[tuple(reversed(range(n))),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update();ob=bpy.data.objects.new(name,me);bpy.context.collection.objects.link(ob)
    mod=ob.modifiers.new('enamel softened cut edge','BEVEL');mod.width=.005;mod.segments=2
    return finish(ob,name,mat,bone,outline=outline)

exec((Path(__file__).with_name('model.py')).read_text(),globals())
if BODY_LOOK:exec((Path(__file__).with_name('anatomical_suit.py')).read_text(),globals())

def area(name,loc,power,size):
    d=bpy.data.lights.new(name,'AREA');d.energy=power;d.size=size;ob=bpy.data.objects.new(name,d);bpy.context.collection.objects.link(ob);ob.location=loc
    ob.rotation_euler=(Vector((0,0,1))-ob.location).to_track_quat('-Z','Y').to_euler()
area('broad neutral illustrated key',(-3,-4,5),330,5);area('diffuse quiet fill',(3,-1,4),110,5);area('back neutral wash',(0,4,4),140,5)
cd=bpy.data.cameras.new('fixed orthographic painted pilot');cam=bpy.data.objects.new('fixed orthographic painted pilot',cd);bpy.context.collection.objects.link(cam);scene.camera=cam
cam.location=(0,-6,6.6);target=Vector((0,0,1.0));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cd.type='ORTHO';cd.ortho_scale=2.25
directions={'south':0,'east':math.pi/2,'north':math.pi,'west':-math.pi/2}

def project(point):
    c=world_to_camera_view(scene,cam,Vector(point));return np.array([c.x*scene.render.resolution_x,c.y*scene.render.resolution_y],dtype=float)
exec((Path(__file__).with_name('motion.py')).read_text(),globals())

# A bounded calibration render sets common camera scale, never individual frame scales.
for trial in range(2):
    rig.rotation_euler.z=0;bpy.context.view_layer.update();pose(0,'south','idle')
    target=REVIEW/f'calibration-{trial}.png';scene.render.filepath=str(target);bpy.ops.render.render(write_still=True)
    image=bpy.data.images.load(str(target),check_existing=False)
    iw,ih=image.size;alpha=np.array(image.pixels[:]).reshape(ih,iw,4)[:,:,3];rows=np.where(alpha>.05)[0]
    height=int(rows.max()-rows.min()+1);cd.ortho_scale*=height/(332.8*RUNTIME_SCALE);bpy.data.images.remove(image)
    bpy.context.view_layer.update()

MODEL_SECONDS=time.perf_counter()-START
normal_camera_location=cam.location.copy();normal_camera_scale=cd.ortho_scale
normal_density=float(np.linalg.norm(project((1,0,0))-project((0,0,0))))
def set_canvas(action):
    cam.location=normal_camera_location;cd.ortho_scale=normal_camera_scale
    scene.render.resolution_x=384;scene.render.resolution_y=448
    if action=='death':
        # A floor-length body needs wider bounds, while preserving source pixel density.
        scene.render.resolution_x=1024;scene.render.resolution_y=1024
        forward=cam.rotation_euler.to_matrix()@Vector((0,0,-1))
        cam.location=-forward*normal_camera_location.length
        density=float(np.linalg.norm(project((1,0,0))-project((0,0,0))))
        cd.ortho_scale*=density/normal_density
    bpy.context.view_layer.update()
clips=[];bindings=[];metrics=[]
def export(direction,n=30,fps=60,action='walk',clip_action=None,weapon='pistol',exposures=None):
    clip_action=clip_action or action;clipid=f'officer.{weapon}.suit.{clip_action}.{direction}'
    set_canvas(action)
    rig.rotation_euler.z=directions[direction];bpy.context.view_layer.update()
    frames=[];feet=[]
    for i in range(n):
        phase=i/n if action=='walk' else i/(n-1) if n>1 else 0
        foot=pose_turn(phase,clip_action.removeprefix('turn_from_'),direction,weapon) if action=='turn' else pose(phase,direction,action,weapon,i)
        target=FRAMES/f'{clipid}.{i:02}.png';scene.render.filepath=str(target);bpy.ops.render.render(write_still=True)
        w,h=scene.render.resolution_x,scene.render.resolution_y
        anchor=project((0,0,0));frames.append({'path':target.relative_to(ROOT/'assets').as_posix(),'widthCells':w/256,'heightCells':h/256,'pivotX':float(anchor[0]/w),'pivotY':float(anchor[1]/h),'pixelWidth':round(w*RUNTIME_SCALE),'pixelHeight':round(h*RUNTIME_SCALE),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'feet':foot})
        if exposures:frames[-1]['seconds']=exposures[i]
        feet.append(foot)
    marker=([{'kind':'contact','frame':0,'foot':'left'},{'kind':'contact','frame':n//2,'foot':'right'}] if action=='walk' else [{'kind':'release','frame':2,'seconds':.14},{'kind':'recovery','frame':6,'seconds':.35}] if action=='fire' else [{'kind':'contact','frame':3}] if action=='melee' else [{'kind':'work_contact','frame':2}] if action=='work' else [])
    clips.append({'id':clipid,'frameSeconds':1/fps,'cycleCells':.5 if action=='walk' else 0,'cycleDistanceCells':.5 if action=='walk' else 0,'loop':action=='walk','equipment':weapon,'armor':'suit','frames':frames,'markers':marker})
    bindings.append({'actor':'officer','weapon':weapon,'armor':'suit','action':clip_action,'facing':direction,'clip':clipid})
    if action=='walk':
        residual=[]
        for side in ['left','right']:
            runs=[];run=[]
            for f in feet:
                if f[side]['stance']:run.append(f[side]['rootPlusLocalPixels'])
                elif run:runs.append(run);run=[]
            if run:runs.append(run)
            for run in runs:
                arr=np.array(run);residual.append(float(np.max(np.linalg.norm(arr-arr[0],axis=1))))
        metrics.append({'clip':clipid,'sampledStanceResidualPixels':max(residual,default=0),'maximumHeldPoseRootTravelPixels':128/n,'maximumHeldPoseRootTravelAt720p47pxCell':47*.5/n})
    (EXPORT_OUT/'clips.json').write_text(json.dumps({'version':1,'referenceCellPixels':256,'runtimeReferenceCellPixels':round(256*RUNTIME_SCALE),'pivotOrigin':'bottom-left','clips':clips,'bindings':bindings,'footCalibration':metrics},indent=2)+'\n',newline='\n')

# True editable keyframes; source action retained even though export samples explicit poses.
for i in range(37):
    scene.frame_set(i+1);pose(i/36,'south')
    for bone in rig.pose.bones:
        bone.rotation_mode='QUATERNION';bone.keyframe_insert('location',frame=i+1);bone.keyframe_insert('rotation_quaternion',frame=i+1);bone.keyframe_insert('scale',frame=i+1)
scene.frame_start=1;scene.frame_end=36;scene.frame_set(1)
scene['masterReferenceCellPixels']=256;scene['masterCanvasPixels']=[384,448];scene['runtimeReferenceCellPixels']=round(256*RUNTIME_SCALE)
bpy.ops.wm.save_as_mainfile(filepath=str((EXPORT_OUT if PILOT_V2 or BODY_LOOK else SRC)/'officer-illustrated-pilot.blend'))
rig.animation_data.action=None
if PILOT_V2:
    for d in directions:export(d,1,60,'idle')
    export('east',8,20,'turn','turn_from_south')
    export('north',8,13.3333333333,'turn','turn_from_south')
    for d in ['south','east']:export(d,12,12,'death')
if BODY_LOOK:
    for d in ['south','east']:export(d,1,60,'idle')
for weapon in ([] if PILOT_V2 or BODY_LOOK else ['pistol'] if LOOK_ONLY else ['pistol','unarmed']):
    for d in directions:export(d,1,60,'idle',weapon=weapon)
    if not LOOK_ONLY:
        for d in directions:
            export(d,6,36,'start',weapon=weapon);export(d,6,36,'stop',weapon=weapon)
            export(d,36,60,'walk',weapon=weapon)
            export(d,6,12,'ready',weapon=weapon);export(d,6,12,'work',weapon=weapon)
            export(d,5,20,'hit',weapon=weapon);export(d,8,10,'death',weapon=weapon)
            if weapon=='pistol':
                export(d,1,60,'aim',weapon=weapon)
                export(d,8,16,'fire',weapon=weapon,exposures=[.07,.07,.04,.04,.06,.07,.07,.08])
            else:export(d,8,20,'melee',weapon=weapon)
(EXPORT_OUT/'timing.json').write_text(json.dumps({'modelAndCalibrationSeconds':MODEL_SECONDS,'totalProcessSeconds':time.perf_counter()-START,'renderSeconds':time.perf_counter()-START-MODEL_SECONDS,'geometryIterations':3,'boundedLookCorrections':5,'status':'V2 pilot before independent review' if PILOT_V2 else 'Development candidate for native evaluation; artistic finish OPEN'},indent=2)+'\n',newline='\n')
print('OFFICER_PILOT_COMPLETE',len(clips),sum(len(c['frames']) for c in clips),time.perf_counter()-START)
