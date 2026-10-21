"""AP-RENDERED: original editable Blender officer and offline sprite exporter.

Run with Blender 4.5.3 --background --factory-startup --python this_file -- --pilot
or --full. No external models, photographs, textures or animation are imported.
"""
from pathlib import Path
import bpy, math, json, hashlib, sys, random
from mathutils import Vector, Matrix
from bpy_extras.object_utils import world_to_camera_view

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'assets/art/proof/rendered'
SRC = OUT / 'source'
FRAMES = OUT / 'regions'
for p in [OUT, SRC, FRAMES]: p.mkdir(parents=True, exist_ok=True)
FULL = '--full' in sys.argv
ADD_HEAVY_RIFLE = '--add-heavy-rifle' in sys.argv
PILOT = not FULL
random.seed(517)
bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)
scene = bpy.context.scene
scene.render.engine = 'BLENDER_EEVEE_NEXT'
scene.render.resolution_x = 384
scene.render.resolution_y = 448
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = 'PNG'
scene.render.image_settings.color_mode = 'RGBA'
scene.render.film_transparent = True
scene.render.fps = 24
scene.render.image_settings.color_depth = '8'
scene.render.use_file_extension = True
scene.render.use_compositing = False
scene.render.use_sequencer = False
scene.world.color = (.17, .17, .17)
scene.view_settings.view_transform = 'Standard'
scene.view_settings.look = 'Medium High Contrast'
scene.view_settings.exposure = 0
scene.render.image_settings.compression = 70

def material(name, color, worn=False, metallic=0):
    m = bpy.data.materials.new(name); m.use_nodes = True
    nodes=m.node_tree.nodes; links=m.node_tree.links
    p=nodes.get('Principled BSDF'); p.inputs['Base Color'].default_value=(*color,1)
    p.inputs['Roughness'].default_value=.83; p.inputs['Metallic'].default_value=metallic
    if worn:
        tex=nodes.new('ShaderNodeTexNoise'); tex.inputs['Scale'].default_value=51
        tex.inputs['Detail'].default_value=2.8; tex.inputs['Roughness'].default_value=.75
        ramp=nodes.new('ShaderNodeValToRGB')
        ramp.color_ramp.elements[0].position=.27
        ramp.color_ramp.elements[0].color=(*(c*.83 for c in color),1)
        ramp.color_ramp.elements[1].position=.72
        ramp.color_ramp.elements[1].color=(*color,1)
        links.new(tex.outputs['Fac'],ramp.inputs[0]); links.new(ramp.outputs[0],p.inputs['Base Color'])
        bump=nodes.new('ShaderNodeBump'); bump.inputs['Strength'].default_value=.055; bump.inputs['Distance'].default_value=.006
        links.new(tex.outputs['Fac'],bump.inputs['Height']); links.new(bump.outputs[0],p.inputs['Normal'])
    return m

cream=material('worn warm porcelain enamel',(.67,.66,.57),True)
creamLight=material('edge ivory',(.84,.81,.68),True)
creamDark=material('ceramic shadow',(.39,.41,.36),True)
cloth=material('graphite pressure weave',(.054,.067,.065),True)
rubber=material('rubber seam',(.018,.025,.025),True)
steel=material('brushed slate steel',(.15,.19,.185),True,.3)
edge=material('worn metal edge',(.34,.38,.34),True,.35)
visor=material('smoked visor black',(.012,.028,.03),False,.15)
glass=material('visor subdued reflection',(.075,.135,.137),False,.2)
skin=material('face behind visor',(.038,.053,.05),True)
amber=material('caution amber',(.59,.27,.065),True)
whiteMark=material('bone stencil',(.58,.62,.53))
black=material('fasteners',(.022,.03,.027))
heavyMat=material('heavy armor graphite olive',(.16,.21,.185),True,.2)
red=material('warning seal brick',(.28,.07,.035),True)

# Rest skeleton. Disconnected branches deliberately retain absolute pose controls.
rests={
 'pelvis':((0,0,.94),(0,0,1.14)),
 'torso':((0,0,1.12),(0,0,1.61)),
 'head':((0,0,1.60),(0,0,1.91)),
 'thigh.L':((-.145,0,1.00),(-.145,0,.56)),
 'shin.L':((-.145,0,.56),(-.145,0,.16)),
 'foot.L':((-.145,0,.16),(-.145,-.19,.12)),
 'thigh.R':((.145,0,1.00),(.145,0,.56)),
 'shin.R':((.145,0,.56),(.145,0,.16)),
 'foot.R':((.145,0,.16),(.145,-.19,.12)),
 'upperarm.L':((-.30,0,1.53),(-.32,-.025,1.22)),
 'forearm.L':((-.32,-.025,1.22),(-.32,-.13,.99)),
 'hand.L':((-.32,-.13,.99),(-.32,-.17,.88)),
 'upperarm.R':((.30,0,1.53),(.32,-.025,1.22)),
 'forearm.R':((.32,-.025,1.22),(.32,-.13,.99)),
 'hand.R':((.32,-.13,.99),(.32,-.17,.88)),
}
armdata=bpy.data.armatures.new('OfficerSkeleton'); rig=bpy.data.objects.new('Officer_Rig',armdata)
bpy.context.collection.objects.link(rig); bpy.context.view_layer.objects.active=rig; rig.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
for name,(a,b) in rests.items():
    bone=armdata.edit_bones.new(name); bone.head=a; bone.tail=b
bpy.ops.object.mode_set(mode='OBJECT'); rig.select_set(False)
rig.show_in_front=True
objects=[]; heavies=[]; weapons={}; flash=None

def finish(obj,name,mat,bone=None,bevel=0,heavy=False):
    obj.name=name; obj.data.materials.append(mat)
    if bevel:
        mod=obj.modifiers.new('manufactured edge radii','BEVEL'); mod.width=bevel; mod.segments=2
    if hasattr(obj.data,'polygons'):
        for poly in obj.data.polygons: poly.use_smooth=True
    if bone:
        # Freeze primitive transforms before binding world-space vertices to rest bone.
        bpy.context.view_layer.objects.active=obj; obj.select_set(True)
        bpy.ops.object.transform_apply(location=True,rotation=True,scale=True); obj.select_set(False)
        group=obj.vertex_groups.new(name=bone); group.add(range(len(obj.data.vertices)),1,'REPLACE')
        mod=obj.modifiers.new('Officer bone weights','ARMATURE'); mod.object=rig
        obj.parent=rig
    objects.append(obj)
    if heavy: heavies.append(obj)
    return obj

def uv(name,loc,scale,mat,bone=None,segments=24,rings=16,heavy=False):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=rings,location=loc)
    ob=bpy.context.object; ob.scale=scale
    return finish(ob,name,mat,bone,heavy=heavy)

def box(name,loc,scale,mat,bone=None,bevel=.012,heavy=False):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc); ob=bpy.context.object; ob.scale=scale
    return finish(ob,name,mat,bone,bevel,heavy)

def cyl(name,loc,radius,depth,mat,bone=None,rotation=(0,0,0),vertices=24,heavy=False):
    bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=radius,depth=depth,location=loc,rotation=rotation)
    return finish(bpy.context.object,name,mat,bone,.003,heavy)

def tube(name,points,radius,mat,bone=None):
    curve=bpy.data.curves.new(name,'CURVE'); curve.dimensions='3D'; curve.bevel_depth=radius; curve.bevel_resolution=2
    spline=curve.splines.new('POLY'); spline.points.add(len(points)-1)
    for p,co in zip(spline.points,points): p.co=(*co,1)
    ob=bpy.data.objects.new(name,curve); bpy.context.collection.objects.link(ob)
    bpy.context.view_layer.objects.active=ob; ob.select_set(True); bpy.ops.object.convert(target='MESH'); ob.select_set(False)
    return finish(ob,name,mat,bone)

def profile(name,zlevels,mat,bone):
    # Authored elliptical cross-sections, with front and side tailoring.
    verts=[]; faces=[]; n=24
    for z,rx,ry,cy in zlevels:
        for i in range(n):
            a=math.tau*i/n; verts.append((math.cos(a)*rx,cy+math.sin(a)*ry,z))
    for j in range(len(zlevels)-1):
        for i in range(n): faces.append((j*n+i,j*n+(i+1)%n,(j+1)*n+(i+1)%n,(j+1)*n+i))
    faces.extend([tuple(reversed(range(n))),tuple((len(zlevels)-1)*n+i for i in range(n))])
    mesh=bpy.data.meshes.new(name); mesh.from_pydata(verts,[],faces); mesh.update()
    ob=bpy.data.objects.new(name,mesh); bpy.context.collection.objects.link(ob); return finish(ob,name,mat,bone)

# Adult body: tailored segmented pressure layers, external harness and enamel armor.
profile('pressurized waist textile',[(.96,.19,.135,0),(1.06,.205,.145,0),(1.15,.18,.132,0)],cloth,'pelvis')
profile('torso enamel shell',[(1.13,.185,.132,0),(1.23,.22,.153,0),(1.43,.245,.155,0),(1.56,.25,.124,.0),(1.61,.15,.105,0)],cream,'torso')
profile('lower chest raised plate',[(1.17,.187,.14,-.012),(1.29,.206,.154,-.018),(1.41,.213,.15,-.014)],creamLight,'torso')
for x in [-.18,.18]:
    box('reinforced shoulder harness', (x,-.149,1.45),(.045,.035,.25),cloth,'torso',.008)
    box('harness buckle',(x,-.173,1.36),(.058,.025,.047),edge,'torso',.004)
box('sternum service plate',(.025,-.177,1.45),(.16,.027,.092),steel,'torso',.006)
box('sternum status amber',(.079,-.196,1.468),(.017,.008,.024),amber,'torso',.002)
for k in range(4): box('sternum grille',(k*.022-.045,-.195,1.43),(.009,.007,.011),rubber,'torso',.001)
for x in [-.085,.085]: box('chest serial stencil',(x,-.171,1.51),(.052,.009,.012),whiteMark,'torso',.001)
for z in [1.17,1.19,1.21]:
    tube('ribbed abdomen seam',[(-.16,-.11,z),(0,-.145,z-.015),(.16,-.11,z)],.007,cloth,'torso')
box('utility waist band',(0,-.13,1.09),(.39,.045,.08),rubber,'pelvis')
box('waist quick buckle',(0,-.158,1.087),(.08,.025,.055),edge,'pelvis',.005)
for x in [-.16,.16]: box('belt pressure pouch',(x,-.142,1.04),(.09,.068,.12),creamDark,'pelvis',.012)
# Back life support genuinely occupies back; modest asymmetric hose and orange tag.
box('oxygen harness backing',(0,.17,1.38),(.325,.11,.40),cloth,'torso',.035)
box('oxygen support enamel case',(0,.23,1.395),(.285,.14,.335),creamDark,'torso',.032)
for x in [-.087,.087]:
    cyl('oxygen reserve cylinder',(x,.29,1.39),.057,.30,cream,'torso')
    for z in [1.29,1.49]: cyl('oxygen tank restraint',(x,.29,z),.06,.025,steel,'torso')
    cyl('oxygen valve',(x,.29,1.555),.025,.035,amber,'torso')
box('back identity panel',(0,.367,1.40),(.102,.012,.074),cloth,'torso',.003)
box('back identifier stripe',(0,.376,1.417),(.061,.005,.011),whiteMark,'torso',.001)
tube('breathing supply hose',[(.12,.23,1.50),(.23,.20,1.56),(.22,.05,1.65),(.135,-.08,1.68)],.022,rubber,'torso')
for i in range(6):
    a=i*.19; uv('hose ferrule',(.18+.04*math.sin(a),.10-i*.024,1.60+i*.011),(.024,.026,.02),steel,'torso',12,8)
# Layered helmet: compact, heavy sealed rim, opaque dark visor; face subdued behind it.
cyl('lower collar seal',(0,0,1.625),.142,.055,rubber,'head')
cyl('collar ceramic lip',(0,0,1.65),.152,.023,creamLight,'head')
uv('helmet enamel shell',(0,0,1.792),(.153,.145,.167),cream,'head',32,24)
uv('visor gasket',(0,-.115,1.792),(.132,.058,.117),rubber,'head',32,20)
uv('smoked visor',(0,-.142,1.798),(.119,.032,.102),visor,'head',32,20)
uv('dim face obscured by smoke',(0,-.168,1.774),(.067,.012,.074),skin,'head',24,16)
uv('smoked upper visor',(0,-.173,1.839),(.112,.009,.034),glass,'head',24,12)
for x in [-.033,.033]:
    box('faint visor reflection',(x,-.184,1.80),(.043,.003,.003),glass,'head',.001)
tube('jaw guard',[(-.10,-.13,1.73),(-.074,-.17,1.692),(0,-.183,1.68),(.074,-.17,1.692),(.10,-.13,1.73)],.022,creamLight,'head')
box('visor center latch',(0,-.196,1.686),(.037,.018,.019),steel,'head',.003)
for x in [-.15,.15]:
    cyl('helmet ear housing',(x,0,1.785),.053,.028,steel,'head',(0,math.pi/2,0))
    cyl('helmet ear ivory cap',(x*1.06,0,1.785),.039,.027,creamLight,'head',(0,math.pi/2,0))
    for z in [1.773,1.791]: box('helmet ear slots',(x*1.17,-.007,z),(.012,.033,.004),rubber,'head',.001)
box('helmet lamp bracket',(.098,-.024,1.931),(.07,.09,.025),steel,'head',.005)
box('helmet lamp unlit lens',(.098,-.073,1.932),(.049,.009,.014),whiteMark,'head',.002)
box('crown service inlay',(-.045,.015,1.95),(.052,.08,.01),creamDark,'head',.002)

for side,x in [('L',-.145),('R',.145)]:
    thigh='thigh.'+side; shin='shin.'+side; foot='foot.'+side
    uv('hip fabric '+side,(x,0,.95),(.101,.112,.13),cloth,'pelvis')
    uv('thigh pressure fabric '+side,(x,0,.79),(.111,.109,.245),cream,thigh)
    uv('thigh anterior armor '+side,(x,-.083,.81),(.094,.045,.178),creamLight,thigh)
    box('thigh cargo pocket '+side,(x+(-.092 if side=='L' else .092),.012,.765),(.045,.13,.13),creamDark,thigh,.013)
    box('thigh cargo retention '+side,(x+(-.12 if side=='L' else .12),-.01,.78),(.012,.12,.016),rubber,thigh,.002)
    uv('knee dark bellows '+side,(x,0,.55),(.093,.09,.105),rubber,shin)
    uv('knee floating cap '+side,(x,-.082,.56),(.096,.038,.079),creamDark,shin)
    box('knee guard inset '+side,(x,-.12,.565),(.082,.008,.028),steel,shin,.004)
    uv('shin pressure fabric '+side,(x,.006,.355),(.085,.087,.19),cloth,shin)
    uv('shin enameled greave '+side,(x,-.054,.345),(.075,.046,.163),cream,shin)
    box('greave layered shell '+side,(x,-.094,.345),(.115,.023,.234),creamLight,shin,.015)
    box('greave dark inset '+side,(x,-.109,.335),(.037,.005,.13),steel,shin,.004)
    for z in [.22,.245]: cyl('ankle seal rib '+side,(x,0,z),.085,.015,steel,shin)
    box('boot outsole '+side,(x,-.055,.064),(.19,.335,.09),rubber,foot,.022)
    uv('armored boot body '+side,(x,-.045,.131),(.093,.17,.097),creamDark,foot)
    box('boot reinforced toe '+side,(x,-.179,.112),(.175,.094,.07),steel,foot,.024)
    for z in [.105,.14]: box('boot front tread '+side,(x,-.227,z),(.14,.01,.009),rubber,foot,.001)
    for k in range(3): box('boot upper closure '+side,(x,-.089-k*.025,.202-k*.01),(.103,.011,.009),rubber,foot,.001)

for side,x in [('L',-.30),('R',.30)]:
    upper='upperarm.'+side; fore='forearm.'+side; hand='hand.'+side
    uv('shoulder flexible joint '+side,(x,0,1.52),(.114,.105,.115),rubber,upper)
    box('shoulder chamfered hard shell '+side,(x,0,1.53),(.19,.195,.20),cream,upper,.024)
    box('shoulder layered dorsal shield '+side,(x,.005,1.63),(.17,.17,.025),creamLight,upper,.009)
    box('shoulder black perimeter seal '+side,(x,0,1.45),(.194,.196,.022),rubber,upper,.007)
    uv('bicep pressure fabric '+side,(x*1.06,-.012,1.365),(.087,.087,.16),cream,upper)
    uv('elbow bellows '+side,(x*1.066,-.025,1.22),(.078,.082,.079),rubber,fore)
    uv('elbow cup '+side,(x*1.1,.035,1.22),(.076,.05,.065),creamDark,fore)
    uv('gauntlet shell '+side,(x*1.066,-.071,1.11),(.073,.070,.141),creamDark,fore)
    box('gauntlet dorsal plate '+side,(x*1.066,-.125,1.115),(.105,.025,.155),creamLight,fore,.012)
    box('gauntlet thin black service panel '+side,(x*1.066,-.141,1.13),(.037,.006,.084),cloth,fore,.003)
    box('gauntlet seal '+side,(x*1.066,-.12,1.016),(.13,.11,.035),rubber,fore,.01)
    uv('glove palm '+side,(x*1.066,-.154,.963),(.061,.05,.073),cloth,hand)
    uv('glove knuckle armor '+side,(x*1.066,-.19,.978),(.061,.023,.036),steel,hand)
    for k in range(3): uv('glove finger '+side,(x*1.066+(k-1)*.027,-.166,.919),(.017,.028,.031),rubber,hand,12,8)
    uv('glove thumb '+side,(x*1.066+(.047 if side=='L' else -.047),-.173,.957),(.024,.022,.037),cloth,hand,12,8)
    box('arm rank panel '+side,(x*1.35,-.02,1.46),(.011,.064,.073),steel,upper,.004)
    box('arm amber chevron '+side,(x*1.38,-.02,1.473),(.009,.046,.012),amber,upper,.001)

# Heavy armor shares actual weights, changing shoulder/chest silhouette without a new rig.
profile('heavy chest ballistic shell',[(1.19,.21,.172,0),(1.28,.258,.19,0),(1.49,.27,.178,0),(1.59,.20,.127,0)],heavyMat,'torso').hide_render=True
heavies.append(objects[-1])
for x in [-.31,.31]: uv('heavy external pauldron',(x,0,1.55),(.159,.14,.14),heavyMat,'upperarm.'+('L' if x<0 else 'R'),heavy=True)
box('heavy breast angled plate',(0,-.21,1.40),(.31,.045,.20),heavyMat,'torso',.025,heavy=True)
for x in [-.11,.11]: box('heavy plate keeper',(x,-.24,1.41),(.018,.018,.16),edge,'torso',.002,heavy=True)

# Equipment is fully modeled and bound to the right-hand socket; left hand uses IK target.
def weapon_model(kind):
    start=len(objects)
    if kind=='pistol':
        box('pistol receiver',(.32,-.23,.985),(.062,.19,.061),steel,'hand.R',.008)
        box('pistol slide',(.32,-.247,1.018),(.055,.205,.024),edge,'hand.R',.004)
        box('pistol muzzle',(.32,-.345,.989),(.054,.023,.048),rubber,'hand.R',.003)
        box('pistol muzzle bore',(.32,-.359,.996),(.023,.006,.018),black,'hand.R',.002)
        box('pistol fore sight',(.32,-.323,1.035),(.01,.014,.012),rubber,'hand.R',.001)
        box('pistol grip',(.32,-.165,.945),(.047,.055,.078),cloth,'hand.R',.005)
    else:
        box(kind+' receiver',(.32,-.26,1.00),(.075,.28,.085),steel,'hand.R',.009)
        box(kind+' stock',(.32,-.059,1.001),(.068,.16,.074),cloth,'hand.R',.012)
        box(kind+' stock butt',(.32,.024,1.002),(.079,.025,.096),rubber,'hand.R',.005)
        box(kind+' foregrip',(.32,-.41,.997),(.077,.17,.072),creamDark,'hand.R',.007)
        cyl(kind+' barrel',(.32,-.54,1.009),.023,.21,steel,'hand.R',(math.pi/2,0,0))
        cyl(kind+' bore',(.32,-.649,1.009),.014,.008,black,'hand.R',(math.pi/2,0,0))
        box(kind+' sight rail',(.32,-.277,1.055),(.026,.31,.023),rubber,'hand.R',.002)
        box(kind+' power magazine',(.32,-.24,.93),(.05,.08,.095),rubber,'hand.R',.004)
        for k in range(4): box(kind+' grip slots',(.32,-.37-k*.022,1.036),(.082,.009,.012),rubber,'hand.R',.001)
    return objects[start:]
weapons['pistol']=weapon_model('pistol'); weapons['rifle']=weapon_model('rifle')

# Small restrained release spark is its own object, enabled only for an actual fire frame.
flash=uv('authored pistol release flash',(.32,-.389,.997),(.028,.06,.028),amber,'hand.R',12,8)
# The model's barrel follows the hand's authored forward axis, not world -Y.
# This avoids pitching the muzzle into the ceiling as the hand rises into aim.
grip=Vector((.32,-.17,.96))
socket_rotation=Vector((0,-1,0)).rotation_difference(Vector((0,-.04,-.11)))
for ob in [part for parts in weapons.values() for part in parts]+[flash]:
    for vertex in ob.data.vertices: vertex.co=grip+socket_rotation@(vertex.co-grip)

# Restrained exposed-edge chips and narrow serial marks are modeled surface details.
for x,z,w in [(-.16,1.36,.036),(.13,1.25,.025),(-.06,1.20,.021),(.19,1.48,.024)]:
    box('worn enamel edge scratch',(x,-.172,z),(w,.002,.0025),edge,'torso',.0006)
for side,x in [('L',-.145),('R',.145)]:
    box('greave restrained edge wear '+side,(x-.041,-.108,.42),(.022,.003,.003),edge,'shin.'+side,.0005)
    box('greave serial ivory '+side,(x+.03,-.108,.40),(.014,.003,.02),whiteMark,'shin.'+side,.001)

# Add leg length in the actual mesh and matching rest rig, preserving mature proportions.
def long_z(z): return z + .13*max(0,min(1,(z-.16)/.84))
for ob in objects:
    for vertex in ob.data.vertices: vertex.co.z=long_z(vertex.co.z)
bpy.context.view_layer.objects.active=rig; rig.select_set(True); bpy.ops.object.mode_set(mode='EDIT')
for bone in armdata.edit_bones:
    bone.head.z=long_z(bone.head.z); bone.tail.z=long_z(bone.tail.z)
bpy.ops.object.mode_set(mode='OBJECT'); rig.select_set(False)

# Fixed neutral studio lighting; no station light, floor, cast shadow or false sensor cue.
def area(name,loc,energy,size,color):
    d=bpy.data.lights.new(name,'AREA'); d.energy=energy; d.shape='DISK'; d.size=size; d.color=color
    ob=bpy.data.objects.new(name,d); bpy.context.collection.objects.link(ob); ob.location=loc
    ob.rotation_euler=(Vector((0,0,1))-ob.location).to_track_quat('-Z','Y').to_euler()
area('large neutral painting key',(-3,-4,6),380,5,(1,.96,.87))
area('broad restrained fill',(3,-1,4),170,4,(.82,.9,1))
area('back silhouette fill',(0,4,5),190,5,(.88,.94,1))
camdata=bpy.data.cameras.new('orthographic sprite camera'); cam=bpy.data.objects.new('orthographic sprite camera',camdata)
bpy.context.collection.objects.link(cam); scene.camera=cam
cam.location=(0,-6,5.265); target=Vector((0,0,1.105)); cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
camdata.type='ORTHO'; camdata.ortho_scale=2.50
scene.render.fps=24

def bone_pose(name,a,b):
    a=Vector(a); b=Vector(b); a.z=long_z(a.z); b.z=long_z(b.z)
    # Preserve the modeled rest roll; a generic look-at silently twists limbs 180deg.
    rest=rig.data.bones[name]
    rotation=(rest.tail_local-rest.head_local).rotation_difference(b-a)
    mat=(rotation.to_matrix() @ rest.matrix_local.to_3x3()).to_4x4(); mat.translation=a
    mat=mat @ Matrix.Diagonal((1,(b-a).length/rest.length,1,1))
    pb=rig.pose.bones[name]; pb.matrix=mat

def solve_knee(hip,ankle,upper=.44,lower=.40):
    hip=Vector(hip); ankle=Vector(ankle); delta=ankle-hip; d=min(upper+lower-.002,delta.length)
    direction=delta.normalized(); along=(upper*upper-lower*lower+d*d)/(2*d)
    normal=Vector((0,-1,0)); normal=(normal-direction*normal.dot(direction)).normalized()
    knee=hip+direction*along+normal*math.sqrt(max(.001,upper*upper-along*along))
    return knee

def frame_pose(phase=0,action='walk',weapon='pistol',armor='suit',action_t=0):
    # In-place gait with long planted stance and an explicit bent-knee swing arc.
    travel=.60; bob=.009*(1-math.cos(phase*math.tau*2)) if action=='walk' else 0
    pelvisZ=.94+bob; twist=.012*math.sin(phase*math.tau) if action=='walk' else 0
    bone_pose('pelvis',(0,0,.94+bob),(0,0,1.14+bob))
    bone_pose('torso',(0,0,1.12+bob),(twist,-.014,1.61+bob))
    bone_pose('head',(twist,-.014,1.60+bob),(twist,-.019,1.91+bob))
    for side,x,offset in [('L',-.145,0),('R',.145,.5)]:
        p=(phase+offset)%1
        if action=='walk':
            if p<.6: y=-travel/2+travel*p/.6; lift=0
            else:
                q=(p-.6)/.4; y=travel/2-travel*q; lift=.060*math.sin(math.pi*q)
        else: y=-.045 if side=='L' else .035; lift=0
        hip=(x,0,pelvisZ); ankle=(x,y,.16+lift)
        knee=solve_knee(hip,ankle)
        bone_pose('thigh.'+side,hip,knee); bone_pose('shin.'+side,knee,ankle)
        bone_pose('foot.'+side,ankle,(x,y-.19,.12+lift))
    aim=action in ['aim','fire','ready']
    blend=1
    if action=='ready': blend=action_t
    if action=='fire' and action_t>.65: blend=max(0,1-(action_t-.65)/.35)
    recoil=.035 if action=='fire' and .60<=action_t<=.74 else 0
    for side,x in [('L',-.30),('R',.30)]:
        shoulder=(x+twist,-.005,1.53+bob)
        if aim and side=='R':
            elbow=(.27,-.21*blend,1.22+.14*blend+bob)
            wx=.0 if weapon=='rifle' else .12
            wy=-.25 if weapon=='rifle' else -.40
            wrist=(.32*(1-blend)+wx*blend,-.13*(1-blend)+wy*blend+recoil,.99+.43*blend+bob)
            hand=(wrist[0],wrist[1]-.09*blend-.04*(1-blend),wrist[2]-.11*(1-blend))
        elif aim and side=='L':
            elbow=(-.25,-.18*blend,1.22+.10*blend+bob)
            wx=0 if weapon=='rifle' else .07
            wy=-.43 if weapon=='rifle' else -.35
            wrist=(wx*blend-.32*(1-blend),-.13*(1-blend)+wy*blend+recoil,.99+.41*blend+bob)
            hand=(wrist[0],wrist[1]-.055,wrist[2]-.025)
        elif weapon=='rifle':
            elbow=(x*.89,-.16,1.23+bob)
            wrist=((.18 if side=='R' else .05),-.29 if side=='R' else -.46,1.15+bob)
            hand=(wrist[0],wrist[1]-.05,wrist[2]-.05)
        else:
            swing=.035*math.sin(phase*math.tau+(.5 if side=='R' else 0)) if action=='walk' else 0
            elbow=(x*1.066,-.025+swing,1.22+bob)
            wrist=(x*1.066,-.13+swing,.99+bob)
            hand=(x*1.066,-.17+swing,.88+bob)
        bone_pose('upperarm.'+side,shoulder,elbow)
        bone_pose('forearm.'+side,elbow,wrist)
        bone_pose('hand.'+side,wrist,hand)
    bpy.context.view_layer.update()
    for obj in heavies: obj.hide_render=armor!='heavy'
    for kind,parts in weapons.items():
        for obj in parts: obj.hide_render=kind!=weapon
    flash.hide_render=not(action=='fire' and .60<=action_t<=.74 and weapon=='pistol')

dirs={'south':0,'east':math.pi/2,'north':math.pi,'west':-math.pi/2}
if ADD_HEAVY_RIFLE:
    existing=json.loads((OUT/'clips.json').read_text())
    clips=[c for c in existing['clips'] if not c['id'].startswith('officer.rifle.heavy.')]
    bindings=[b for b in existing['bindings'] if not b['clip'].startswith('officer.rifle.heavy.')]
else: clips=[]; bindings=[]
def clip_export(weapon,armor,action,direction,n=12):
    clipid=f'officer.{weapon}.{armor}.{action}.{direction}'
    rig.rotation_euler.z=dirs[direction]
    frames=[]; duration=.5 if action=='walk' else .28 if action in ['fire','ready'] else 1/24
    for i in range(n):
        t=i/(n-1) if n>1 else 0
        frame_pose(i/n,action,weapon,armor,t)
        name=f'{clipid}.{i:02}.png'; target=FRAMES/name
        scene.render.filepath=str(target); bpy.ops.render.render(write_still=True)
        anchor=world_to_camera_view(scene,cam,Vector((0,0,0)))
        frames.append({'path':target.relative_to(ROOT/'assets').as_posix(),'widthCells':1.5,'heightCells':1.75,'pivotX':float(anchor.x),'pivotY':float(anchor.y),'pixelWidth':384,'pixelHeight':448,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()})
    markers=([{'kind':'contact','frame':0,'foot':'left'},{'kind':'contact','frame':6,'foot':'right'}] if action=='walk' else [{'kind':'release','frame':2},{'kind':'recovery','frame':3}] if action=='fire' else [])
    clips.append({'id':clipid,'frameSeconds':duration/n,'loop':action=='walk','cycleDistanceCells':1 if action=='walk' else 0,'equipment':weapon,'armor':armor,'frames':frames,'markers':markers})
    bindings.append({'actor':'officer','weapon':weapon,'armor':armor,'action':action,'facing':direction,'clip':clipid})
    (OUT/'clips.json').write_text(json.dumps({'version':1,'referenceCellPixels':256,'pivotOrigin':'bottom-left','clips':clips,'bindings':bindings},indent=2)+'\n',newline='\n')

# Keep a truly editable weighted .blend with inspectable animation keyframes.
for frame in range(1,14):
    scene.frame_set(frame); frame_pose((frame-1)/12)
    for bone in rig.pose.bones:
        bone.rotation_mode='QUATERNION'; bone.keyframe_insert('location',frame=frame); bone.keyframe_insert('rotation_quaternion',frame=frame)
scene.frame_start=1; scene.frame_end=12
scene.frame_set(1); frame_pose(0,'idle'); rig.rotation_euler.z=0
if not ADD_HEAVY_RIFLE:
    bpy.ops.wm.save_as_mainfile(filepath=str(SRC/'origin-war-officer.blend'))
# Render explicitly authored pose samples, rather than reevaluating the saved walk action.
rig.animation_data.action=None
if ADD_HEAVY_RIFLE:
    for d in dirs:
        clip_export('rifle','heavy','walk',d,12)
        clip_export('rifle','heavy','idle',d,1)
        clip_export('rifle','heavy','aim',d,1)
elif PILOT:
    for d in dirs: clip_export('pistol','suit','idle',d,1)
    clip_export('pistol','suit','walk','south',12)
else:
    for d in dirs:
        clip_export('pistol','suit','walk',d,12)
        clip_export('pistol','suit','idle',d,1)
        clip_export('pistol','suit','ready',d,4)
        clip_export('pistol','suit','aim',d,1)
        clip_export('pistol','suit','fire',d,4)
        clip_export('rifle','suit','walk',d,12)
        clip_export('rifle','suit','idle',d,1)
        clip_export('rifle','suit','aim',d,1)
        clip_export('pistol','heavy','walk',d,12)
        clip_export('pistol','heavy','idle',d,1)
        clip_export('rifle','heavy','walk',d,12)
        clip_export('rifle','heavy','idle',d,1)
        clip_export('rifle','heavy','aim',d,1)
print('AP_RENDERED_COMPLETE',len(clips),'clips',sum(len(c['frames']) for c in clips),'frames')
