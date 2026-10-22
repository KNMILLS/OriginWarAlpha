"""Original mechanical station hardware. Blender-only model/rig -> offline sprites.

No production bitmap is painted or repaired by this exporter. Run --pilot first.
"""
from pathlib import Path
import bpy, math, json, hashlib, sys
from mathutils import Vector, Matrix
from bpy_extras.object_utils import world_to_camera_view

ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'assets/art/production/station'
SOURCE=OUT/'source'; REGIONS=OUT/'regions'; MASTERS=SOURCE/'masters-256'
for folder in [SOURCE,REGIONS,MASTERS,OUT/'review']:folder.mkdir(parents=True,exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE_NEXT'
scene.render.resolution_x=288;scene.render.resolution_y=384;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
scene.render.film_transparent=True;scene.render.fps=60
scene.view_settings.view_transform='Standard';scene.view_settings.look='Medium High Contrast'
scene.world.color=(.10,.12,.12)
objects=[];asset_objects={};current='sirs';bone_for={}

def material(name,color):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    n=m.node_tree.nodes;n.clear();out=n.new('ShaderNodeOutputMaterial')
    diffuse=n.new('ShaderNodeBsdfDiffuse');diffuse.inputs['Color'].default_value=(1,1,1,1);diffuse.inputs['Roughness'].default_value=1
    rgb=n.new('ShaderNodeShaderToRGB');ramp=n.new('ShaderNodeValToRGB');ramp.color_ramp.interpolation='LINEAR'
    for i,(position,factor) in enumerate([(.12,.36),(.43,.80),(.76,1.08),(.98,1.23)]):
        e=ramp.color_ramp.elements[i] if i<2 else ramp.color_ramp.elements.new(position)
        e.position=position;e.color=(*[min(.92,c*factor) for c in color],1)
    emission=n.new('ShaderNodeEmission');emission.inputs['Strength'].default_value=1
    links=m.node_tree.links;links.new(diffuse.outputs['BSDF'],rgb.inputs['Shader']);links.new(rgb.outputs['Color'],ramp.inputs['Fac']);links.new(ramp.outputs['Color'],emission.inputs['Color']);links.new(emission.outputs['Emission'],out.inputs['Surface'])
    return m

graphite=material('graphite structural steel',(.14,.17,.16))
sage=material('worn mineral sage enamel',(.37,.39,.30))
cream=material('aged institutional ivory',(.61,.59,.47))
steel=material('dull exposed steel',(.27,.30,.28))
edge=material('selected rubbed edge metal',(.51,.49,.39))
dark=material('rubber seals and black recess',(.035,.046,.043))
black=material('deep aperture',(.012,.018,.017))
copper=material('old hydraulic copper',(.39,.25,.13))
amber=material('diffuse amber sensor glass',(.59,.29,.07))
muted=material('unpowered instrument glass',(.05,.085,.074))
warning=material('dull physical safety yellow',(.57,.43,.12))

def finish(ob,name,mat,bone=None,bevel=0):
    ob.name=name;ob.data.materials.append(mat)
    bpy.context.view_layer.objects.active=ob;ob.select_set(True)
    bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);ob.select_set(False)
    if bevel:
        mod=ob.modifiers.new('machined softened corners','BEVEL');mod.width=bevel;mod.segments=2
        normal=ob.modifiers.new('weighted broad metal normals','WEIGHTED_NORMAL')
    objects.append(ob);asset_objects.setdefault(current,[]).append(ob)
    if bone:bone_for[ob]=bone
    return ob

def box(name,center,size,mat,bone=None,bevel=.01):
    bpy.ops.mesh.primitive_cube_add(size=1,location=center);ob=bpy.context.object;ob.dimensions=size
    return finish(ob,name,mat,bone,bevel)

def cylinder(name,a,b,r,mat,bone=None,vertices=16):
    a,b=Vector(a),Vector(b);delta=b-a
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=r,depth=delta.length,location=(a+b)/2)
    ob=bpy.context.object;ob.rotation_euler=delta.to_track_quat('Z','Y').to_euler()
    return finish(ob,name,mat,bone,.004)

def sphere(name,center,size,mat,bone=None):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=16,ring_count=8,radius=1,location=center)
    ob=bpy.context.object;ob.scale=size
    return finish(ob,name,mat,bone)

def plate(name,points,depth,mat,bone=None):
    vs=points+[(x,y+depth,z) for x,y,z in points];n=len(points)
    fs=[tuple(reversed(range(n))),tuple(range(n,n*2))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(vs,[],fs);mesh.update();ob=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(ob)
    return finish(ob,name,mat,bone,.008)

def hose(name,points,r,mat,bone=None):
    curve=bpy.data.curves.new(name,'CURVE');curve.dimensions='3D';curve.bevel_depth=r;curve.bevel_resolution=2
    sp=curve.splines.new('BEZIER');sp.bezier_points.add(len(points)-1)
    for p,co in zip(sp.bezier_points,points):p.co=co;p.handle_left_type=p.handle_right_type='AUTO'
    ob=bpy.data.objects.new(name,curve);bpy.context.collection.objects.link(ob);bpy.context.view_layer.objects.active=ob;ob.select_set(True);bpy.ops.object.convert(target='MESH');ob=bpy.context.object
    return finish(ob,name,mat,bone)

def bolt(name,x,y,z,bone=None):
    cylinder(name,(x,y,z),(x,y-.012,z),.012,edge,bone,8)

# An original biped machine with a small sensor, asymmetrical cannon and real joints.
# It intentionally has no human face, ears or decorative robot eyes.
box('central load bearing chassis',(0,.005,1.105),(.55,.36,.51),graphite,'torso',.045)
plate('upper service armor',[(-.24,-.20,1.31),(-.30,-.21,1.15),(-.23,-.22,.965),(.23,-.22,.965),(.30,-.21,1.15),(.24,-.20,1.31)],.055,sage,'torso')
plate('ivory chest serial plate',[(-.205,-.262,1.26),(-.215,-.265,1.15),(.20,-.265,1.15),(.19,-.262,1.26)],.014,cream,'torso')
for x in [-.177,.177]:
    for z in [1.18,1.24]:bolt('chest captive screw',x,-.28,z,'torso')
for z in [1.02,1.05,1.08]:box('front chassis cooling slot',(0,-.244,z),(.28,.018,.012),dark,'torso',.001)
box('waist mechanical swivel',(0,0,.86),(.31,.27,.16),steel,'pelvis',.02)
cylinder('waist rotation bearing',(0,0,.80),(0,0,.90),.175,dark,'pelvis')
box('pelvic transmission case',(0,0,.745),(.44,.33,.14),graphite,'pelvis',.025)
for x in [-.15,.15]:bolt('pelvic fastener',x,-.18,.76,'pelvis')

box('tall rear power and control cabinet',(0,.285,1.16),(.50,.22,.69),graphite,'torso',.025)
box('rear sage maintenance cover',(0,.415,1.16),(.39,.025,.54),sage,'torso',.012)
for x in [-.20,.20]:
    box('rear cabinet protection rail',(x,.43,1.16),(.025,.026,.58),edge,'torso',.005)
    for z in [.95,1.37]:bolt('rear access fastener',x,.435,z,'torso')
for z in [1.03,1.08,1.13,1.18]:box('rear passive cooling louvre',(0,.442,z),(.25,.012,.016),dark,'torso',.001)
cylinder('rear power cable gland',(.17,.32,1.48),(.17,.32,1.57),.036,steel,'torso')
hose('protected arm power loom',[(.14,.31,1.48),(.31,.24,1.43),(.38,.10,1.28)],.022,dark,'torso')
cylinder('rear flexible antenna',(-.18,.29,1.49),(-.18,.29,1.83),.010,dark,'torso',8)
cylinder('antenna ferrule',(-.18,.29,1.48),(-.18,.29,1.54),.023,copper,'torso')

cylinder('neck mechanical turntable',(0,0,1.35),(0,0,1.42),.105,steel,'head')
box('small armored sensor housing',(0,-.025,1.535),(.255,.265,.27),graphite,'head',.028)
box('hood pale outer protection',(0,.005,1.67),(.28,.285,.052),sage,'head',.014)
box('sensor black recessed face',(0,-.172,1.54),(.181,.025,.176),black,'head',.008)
box('single vertical amber sensor aperture',(-.040,-.188,1.545),(.025,.005,.111),amber,'head',.002)
box('sensor lens inner slit',(-.040,-.193,1.545),(.009,.004,.065),muted,'head',.001)
for x in [-.13,.13]:
    cylinder('head side bearing',(x-.014,0,1.535),(x+.014,0,1.535),.075,steel,'head')
    box('head narrow side service cap',(x,.06,1.54),(.026,.08,.12),sage,'head',.005)

for side,sgn in [('R',-1),('L',1)]:
    x=sgn*.35
    cylinder('shoulder exposed rotary bearing '+side,(x-sgn*.075,0,1.285),(x+sgn*.075,0,1.285),.145,steel,'upperarm.'+side)
    cylinder('shoulder dark seal '+side,(x+sgn*.077,0,1.285),(x+sgn*.094,0,1.285),.116,dark,'upperarm.'+side)
    box('shoulder chamfered load cap '+side,(x,-.01,1.405),(.24,.30,.075),sage,'upperarm.'+side,.022)
    box('upper arm rigid box beam '+side,(sgn*.425,0,1.115),(.14,.16,.30),graphite,'upperarm.'+side,.014)
    cylinder('upper arm piston housing '+side,(sgn*.43,-.115,1.25),(sgn*.43,-.115,1.10),.041,copper,'upperarm.'+side)
    cylinder('upper arm exposed piston '+side,(sgn*.43,-.115,1.10),(sgn*.43,-.115,.98),.024,edge,'upperarm.'+side)
    cylinder('elbow hinge '+side,(sgn*.39,-.015,.98),(sgn*.49,-.015,.98),.086,steel,'forearm.'+side)
    cylinder('forearm rigid spar '+side,(sgn*.455,-.02,.97),(sgn*.47,-.10,.74),.058,graphite,'forearm.'+side)
    plate('forearm ivory guard '+side,[(sgn*.47-.075,-.101,.935),(sgn*.47+.075,-.101,.935),(sgn*.47+.055,-.15,.77),(sgn*.47-.055,-.15,.77)],.035,cream,'forearm.'+side)
    if side=='L':
        box('left service manipulator',(.47,-.12,.70),(.115,.11,.10),steel,'forearm.L',.01)
        for dx in [-.038,.038]:
            box('left squared gripping finger',(.47+dx,-.17,.635),(.023,.04,.10),graphite,'forearm.L',.006)
    else:
        cylinder('cannon receiver casing',(-.47,-.10,.87),(-.47,-.39,.78),.093,graphite,'forearm.R')
        cylinder('cannon forward barrel',(-.47,-.34,.80),(-.47,-.64,.70),.054,steel,'forearm.R')
        for k in range(4):
            y=-.35-k*.065;z=.80-k*.022
            cylinder('cannon machined cooling ring',(-.47,y+.012,z+.004),(-.47,y-.012,z-.004),.068,graphite,'forearm.R')
        cylinder('cannon dark muzzle',(-.47,-.641,.70),(-.47,-.656,.695),.046,black,'forearm.R')
        cylinder('cannon recessed bore',(-.47,-.657,.695),(-.47,-.660,.694),.022,black,'forearm.R')
        box('cannon restraint plate',(-.47,-.26,.915),(.12,.22,.042),sage,'forearm.R',.006)
    hip=(sgn*.18,0,.72);knee=(sgn*.215,-.02,.415);ankle=(sgn*.215,0,.12)
    cylinder('hip rotary pivot '+side,(sgn*.12,0,.70),(sgn*.23,0,.70),.10,steel,'thigh.'+side)
    box('thigh transmission beam '+side,(sgn*.20,0,.56),(.19,.22,.28),graphite,'thigh.'+side,.025)
    plate('sloped sage thigh armor '+side,[(sgn*.20-.085,-.13,.68),(sgn*.20+.085,-.13,.68),(sgn*.20+.065,-.15,.46),(sgn*.20-.065,-.15,.46)],.025,sage,'thigh.'+side)
    cylinder('outer thigh hydraulic '+side,(sgn*.30,0,.65),(sgn*.30,0,.44),.034,copper,'thigh.'+side)
    cylinder('knee trunnion '+side,(sgn*.16,-.02,.415),(sgn*.27,-.02,.415),.094,steel,'shin.'+side)
    cylinder('knee dark center '+side,(sgn*.27,-.02,.415),(sgn*.28,-.02,.415),.060,dark,'shin.'+side)
    box('shin structural beam '+side,(sgn*.215,.015,.26),(.135,.17,.25),graphite,'shin.'+side,.014)
    plate('shin pale replaceable protection '+side,[(sgn*.215-.057,-.10,.365),(sgn*.215+.057,-.10,.365),(sgn*.215+.073,-.12,.17),(sgn*.215-.073,-.12,.17)],.022,cream,'shin.'+side)
    cylinder('ankle pivot '+side,(sgn*.16,0,.13),(sgn*.27,0,.13),.06,steel,'foot.'+side)
    box('grounded rubber tread '+side,(sgn*.215,-.055,.038),(.235,.345,.065),dark,'foot.'+side,.018)
    box('reinforced mechanical foot '+side,(sgn*.215,-.055,.096),(.215,.32,.075),graphite,'foot.'+side,.018)
    box('toe armor strip '+side,(sgn*.215,-.16,.13),(.20,.09,.035),sage,'foot.'+side,.006)
    for xx in [-.065,.065]:bolt('foot access screw '+side,sgn*.215+xx,-.21,.11,'foot.'+side)

# Real rigid mechanical armature, with editable weighted parts and no runtime 3D.
arm=bpy.data.armatures.new('SIRS mechanical armature');rig=bpy.data.objects.new('SIRS mechanical rig',arm);bpy.context.collection.objects.link(rig)
bpy.context.view_layer.objects.active=rig;rig.select_set(True);bpy.ops.object.mode_set(mode='EDIT')
rests={
 'pelvis':((0,0,.70),(0,0,.86),None),'torso':((0,0,.86),(0,0,1.34),'pelvis'),'head':((0,0,1.39),(0,0,1.65),'torso')}
for side,s in [('R',-1),('L',1)]:
    rests['upperarm.'+side]=((s*.35,0,1.285),(s*.43,-.015,.98),'torso')
    rests['forearm.'+side]=((s*.43,-.015,.98),(s*.47,-.11,.74),'upperarm.'+side)
    rests['thigh.'+side]=((s*.18,0,.70),(s*.215,-.02,.415),'pelvis')
    rests['shin.'+side]=((s*.215,-.02,.415),(s*.215,0,.12),'thigh.'+side)
    rests['foot.'+side]=((s*.215,0,.12),(s*.215,-.20,.08),'shin.'+side)
for name,(a,b,parent) in rests.items():bone=arm.edit_bones.new(name);bone.head=a;bone.tail=b
for name,(a,b,parent) in rests.items():
    if parent:arm.edit_bones[name].parent=arm.edit_bones[parent]
bpy.ops.object.mode_set(mode='OBJECT');rig.select_set(False)
for ob,bone in bone_for.items():
    vg=ob.vertex_groups.new(name=bone);vg.add(list(range(len(ob.data.vertices))),1,'REPLACE');mod=ob.modifiers.new('rigid mechanical joint weighting','ARMATURE');mod.object=rig

# A heavy three-section telescoping double pressure door. Open state is real empty alpha.
current='door';door_leaves=[];door_leaf_origins=[]
for s in [-1,1]:
    box('pressure door side structural housing',(s*.57,.03,.88),(.24,.30,1.76),graphite,bevel=.025)
    box('sage jamb raised service strip',(s*.57,-.14,.88),(.145,.035,1.53),sage,bevel=.01)
    for z in [.20,1.55]:bolt('jamb maintenance fastener',s*.57,-.165,z)
    for z in [.55,.60,.65]:box('door jamb pressure vent',(s*.57,-.165,z),(.08,.012,.016),dark,bevel=.001)
    box('unpowered door status recess',(s*.57,-.175,1.18),(.055,.016,.08),dark,bevel=.003)
box('door header heavy beam',(0,.03,1.75),(1.38,.32,.16),graphite,bevel=.025)
box('door upper guide rail',(0,-.16,1.65),(.94,.045,.075),steel,bevel=.008)
box('door floor pressure threshold',(0,-.01,.045),(1.38,.40,.09),steel,bevel=.012)
for i in range(9):
    ob=box('physical threshold safety tab',(-.54+i*.135,-.215,.093),(.065,.025,.009),warning,bevel=.001)
for s in [-1,1]:
    for k in range(3):
        x=s*(.075+.15*k);start=len(objects)
        box('telescoping pressure leaf',(x,.015,.86),(.15,.10,1.48),sage,bevel=.010)
        box('leaf structural seam',(x+s*.062,-.043,.86),(.018,.018,1.36),steel,bevel=.002)
        if k==0:box('central thick pressure seal',(x-s*.065,-.047,.86),(.024,.022,1.44),dark,bevel=.002)
        for z in [.24,1.46]:bolt('leaf captive flush screw',x,-.052,z)
        group=objects[start:];door_leaves.append(group);door_leaf_origins.append((s,k,x))

def set_door(opening):
    for group,(s,k,x) in zip(door_leaves,door_leaf_origins):
        dx=(s*.565-x)*opening
        for ob in group:ob.location.x=dx;ob.location.y=(k*.018+.015)*opening

def light(name,location,power,size,color):
    data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;data.color=color
    ob=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(ob);ob.location=location;ob.rotation_euler=(Vector((0,0,.9))-ob.location).to_track_quat('-Z','Y').to_euler()
light('broad painted form key',(-3,-4,6),400,4,(1,.93,.79));light('quiet neutral fill',(4,-1,3),130,4,(.78,.88,.86));light('restrained structural rim',(0,4,5),180,4,(.86,.91,.86))
camera_data=bpy.data.cameras.new('orthographic station sprite camera');camera=bpy.data.objects.new('orthographic station sprite camera',camera_data);bpy.context.collection.objects.link(camera);scene.camera=camera
camera.location=(0,-6,5.5);target=Vector((0,0,.91));camera.rotation_euler=(target-camera.location).to_track_quat('-Z','Y').to_euler();camera_data.type='ORTHO';camera_data.ortho_scale=2.6

def select_asset(asset):
    for name,group in asset_objects.items():
        for ob in group:ob.hide_render=name!=asset
    rig.hide_render=asset!='sirs'

def projected_height():
    deps=bpy.context.evaluated_depsgraph_get();ys=[]
    for ob in asset_objects['sirs']:
        evaluated=ob.evaluated_get(deps);mesh=evaluated.to_mesh()
        ys.extend(world_to_camera_view(scene,camera,evaluated.matrix_world@v.co).y for v in mesh.vertices)
        evaluated.to_mesh_clear()
    return (max(ys)-min(ys))*scene.render.resolution_y

select_asset('sirs');bpy.context.view_layer.update()
heights=[]
for angle in [0,math.pi/2,math.pi,-math.pi/2]:rig.rotation_euler.z=angle;bpy.context.view_layer.update();heights.append(projected_height())
camera_data.ortho_scale*=max(heights)/(1.4*192);rig.rotation_euler.z=0;bpy.context.view_layer.update()
SIRS_SCALE=camera_data.ortho_scale
clips=[];regions=[];bindings=[]

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def write(path,data):path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8',newline='\n')
def render(name,asset='sirs'):
    anchor=world_to_camera_view(scene,camera,Vector((0,0,0)))
    path=REGIONS/(name+'.png');scene.render.filepath=str(path);scene.render.resolution_x=288;scene.render.resolution_y=384;bpy.ops.render.render(write_still=True)
    entry=dict(name=name,path=path.relative_to(ROOT/'assets').as_posix(),widthCells=1.5,heightCells=2.0,pivotX=float(anchor.x),pivotY=float(anchor.y),pixelWidth=288,pixelHeight=384,layer='world',sha256=sha(path),asset=asset)
    # Separately rendered masters, not a claim that upscaling creates detail.
    scene.render.resolution_x=384;scene.render.resolution_y=512;scene.render.filepath=str(MASTERS/(name+'.png'));bpy.ops.render.render(write_still=True)
    scene.render.resolution_x=288;scene.render.resolution_y=384
    return entry

for facing,angle in [('north',math.pi),('east',math.pi/2),('south',0),('west',-math.pi/2)]:
    select_asset('sirs');camera_data.ortho_scale=SIRS_SCALE;rig.rotation_euler.z=angle;bpy.context.view_layer.update()
    name='machine.sirs.idle.'+facing;entry=render(name);regions.append(entry)
    clips.append(dict(id=name,frameSeconds=.5,loop=False,frames=[entry],markers=[],cycleCells=0))
    bindings.append(dict(actor='SIRS',action='idle',facing=facing,clip=name))
select_asset('door');rig.rotation_euler.z=0;camera_data.ortho_scale=2.65
for name,opening in [('door.closed',0),('door.open',1)]:
    set_door(opening);bpy.context.view_layer.update();entry=render(name,'door');regions.append(entry)

write(OUT/'regions.json',dict(version=1,referenceCellPixels=192,editableReferenceCellPixels=256,pivotOrigin='bottom-left',pathRoot='assets',regions=regions))
write(OUT/'clips.json',dict(version=1,referenceCellPixels=192,editableReferenceCellPixels=256,pivotOrigin='bottom-left',clips=clips,bindings=bindings,status='pilot-only; no artistic acceptance',missing=['SIRS movement/anticipation/attack/hit/destroyed','all camera and mechanism states','door motion/locked/braced/damaged']))
select_asset('sirs');set_door(0);rig.rotation_euler.z=0;camera_data.ortho_scale=SIRS_SCALE
scene.render.resolution_x=384;scene.render.resolution_y=512
scene['runtimeRasterPixelsPerCell']=192;scene['authoringReferencePixelsPerCell']=256;scene['authority']='Core owns position, facing, aiming, circuit and door state. No presentation scanning or runtime3D.'
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'origin-war-opening-station.blend'))
write(SOURCE/'PROVENANCE.json',dict(task='AR-STATION',base='d80aa83b32706b7058e80176befe73468592765f',tool=bpy.app.version_string,medium='original Blender mechanical meshes/materials/armature; offline rendered candidate, not hand-painted brushwork',externalAssets=[],rights='Original authored construction from project visual identity references; no outside model/texture/animation',sourceBlend=dict(path='origin-war-opening-station.blend',sha256=sha(SOURCE/'origin-war-opening-station.blend')),scriptSha256=sha(Path(__file__)),referencePaths=['art/survival/regions/machine.sirs.png','art/survival/regions/machine.camera.png','art/survival/regions/door.closed.png','art/survival/regions/door.open.png','art/survival/regions/hatch.closed.png','art/survival/regions/control.switch.png'],limitations='First SIRS4cardinal and door2state look pilot only; no native or commercial acceptance.'))
print('STATION_PILOT_DONE',json.dumps(dict(regions=len(regions),clips=len(clips),sirsOrthoScale=SIRS_SCALE)))
