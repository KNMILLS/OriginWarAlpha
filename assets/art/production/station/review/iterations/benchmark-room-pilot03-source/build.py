"""Original mechanical station hardware. Blender-only model/rig -> offline sprites.

No production bitmap is painted or repaired by this exporter. Run --pilot first.
"""
from pathlib import Path
import bpy, math, json, hashlib, sys, time
import numpy as np
from mathutils import Vector, Matrix
from bpy_extras.object_utils import world_to_camera_view

# Process-local authoring guard; preserve any existing automatic backup bytes.
bpy.context.preferences.filepaths.save_version=0

ROOT=Path(__file__).resolve().parents[3]
START=time.perf_counter();BENCHMARK_PILOT='--benchmark-pilot' in sys.argv;FULL='--full' in sys.argv or BENCHMARK_PILOT;MOTION_PILOT='--motion-pilot' in sys.argv
REUSE_NON_DOOR='--door-cutaway-only' in sys.argv
OUT=ROOT/'assets/art/production/station'
if BENCHMARK_PILOT:OUT=OUT/('review/iterations/benchmark-room-pilot03' if '--pilot03' in sys.argv else 'review/iterations/benchmark-room-pilot02' if '--pilot02' in sys.argv else 'review/iterations/benchmark-room-pilot01')
SOURCE=OUT/'source'; REGIONS=OUT/'regions'; MASTERS=SOURCE/'masters-256'
PREVIOUS_REGIONS={r['name']:r for r in json.loads((OUT/'regions.json').read_text())['regions']} if REUSE_NON_DOOR else {}
for folder in [SOURCE,REGIONS,MASTERS,OUT/'review']:folder.mkdir(parents=True,exist_ok=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
scene=bpy.context.scene;scene.render.engine='BLENDER_EEVEE_NEXT'
scene.render.resolution_x=288;scene.render.resolution_y=384;scene.render.resolution_percentage=100
scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
scene.render.film_transparent=True;scene.render.fps=60
scene.render.use_compositing=False;scene.render.use_sequencer=False
scene.view_settings.view_transform='Standard';scene.view_settings.look='Medium High Contrast'
scene.world.color=(.10,.12,.12)
objects=[];asset_objects={};current='sirs';bone_for={}

def material(name,color):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    n=m.node_tree.nodes;n.clear();out=n.new('ShaderNodeOutputMaterial')
    diffuse=n.new('ShaderNodeBsdfDiffuse');diffuse.inputs['Color'].default_value=(1,1,1,1);diffuse.inputs['Roughness'].default_value=1
    rgb=n.new('ShaderNodeShaderToRGB');ramp=n.new('ShaderNodeValToRGB');ramp.color_ramp.interpolation='LINEAR'
    for i,(position,factor) in enumerate([(.10,.24),(.40,.56),(.68,1.02),(.93,1.26)]):
        e=ramp.color_ramp.elements[i] if i<2 else ramp.color_ramp.elements.new(position)
        def linear(c):return c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4
        e.position=position;e.color=(*[linear(min(.92,c*factor)) for c in color],1)
    emission=n.new('ShaderNodeEmission');emission.inputs['Strength'].default_value=1
    # Broad designed shadow planes and selected modeled edge wear carry the
    # finish. Deliberately no uniform procedural grain/noise treatment.
    links=m.node_tree.links;links.new(diffuse.outputs['BSDF'],rgb.inputs['Shader']);links.new(rgb.outputs['Color'],ramp.inputs['Fac']);links.new(ramp.outputs['Color'],emission.inputs['Color']);links.new(emission.outputs['Emission'],out.inputs['Surface'])
    return m

graphite=material('graphite structural steel',(.16,.19,.19))
sage=material('worn mineral sage enamel',(.32,.39,.36))
cream=material('aged institutional ivory',(.69,.66,.55))
steel=material('dull exposed steel',(.30,.34,.33))
edge=material('selected rubbed edge metal',(.57,.56,.48))
dark=material('rubber seals and black recess',(.035,.046,.043))
black=material('deep aperture',(.012,.018,.017))
copper=material('old hydraulic copper',(.39,.25,.13))
amber=material('diffuse amber sensor glass',(.59,.29,.07))
muted=material('unpowered instrument glass',(.05,.085,.074))
warning=material('dull physical safety yellow',(.57,.43,.12))
paper=material('dry warm inspection paper',(.74,.69,.55))
rust=material('localized dark oxide at worn joints',(.25,.18,.12))
sage_shadow=material('designed cool enamel shadow plane',(.19,.27,.26))
ivory_shadow=material('designed warm ivory shadow plane',(.46,.45,.37))
glass_reflection=material('restrained unpowered glass surface reflection',(.12,.19,.18))

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
box('front recessed service number field',(.02,-.285,1.205),(.22,.009,.052),graphite,'torso',.001)
for x in [-.065,-.035,.025,.060]:box('physical serial stencil mark',(x,-.292,1.205),(.013,.004,.031),cream,'torso',.001)
for x,z,w in [(-.22,1.285,.06),(.19,1.135,.05),(-.16,.98,.08)]:
    box('selected armor contact chip',(x,-.277,z),(w,.005,.011),edge,'torso',.001)
plate('chest deliberately painted broad cool plane',[(-.239,-.279,1.128),(-.045,-.279,1.128),(.15,-.279,.979),(-.19,-.279,.979)],.005,sage_shadow,'torso')
box('chest recessed cooling vent surround',(0,-.286,1.052),(.24,.014,.097),graphite,'torso',.007)
for z in [1.025,1.052,1.079]:box('chest dark cooling louvre',(0,-.299,z),(.195,.010,.012),dark,'torso',.001)
plate('chest rubbed top shoulder arris',[(-.205,-.282,1.274),(-.17,-.282,1.274),(-.145,-.282,1.264),(-.204,-.282,1.265)],.003,edge,'torso')
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
box('rear warm maintenance rating strip',(-.08,.451,1.315),(.20,.012,.045),cream,'torso',.002)
for x in [-.14,-.10,-.03]:box('rear maintenance printed mark',(x,.460,1.315),(.013,.006,.027),graphite,'torso',.001)
plate('rear cabinet broad lower shadow',[(-.19,.452,1.01),(-.19,.452,.91),(.19,.452,.91),(.19,.452,1.09)],.007,sage_shadow,'torso')
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
    box('shoulder selected rubbed load edge '+side,(x,-.166,1.423),(.15,.008,.016),edge,'upperarm.'+side,.002)
    for dx in [-.045,.015]:box('shoulder physical caution stripe '+side,(x+dx,-.174,1.406),(.018,.006,.027),warning,'upperarm.'+side,.002)
    box('upper arm rigid box beam '+side,(sgn*.425,0,1.115),(.14,.16,.30),graphite,'upperarm.'+side,.014)
    cylinder('upper arm piston housing '+side,(sgn*.43,-.115,1.25),(sgn*.43,-.115,1.10),.041,copper,'upperarm.'+side)
    cylinder('upper arm exposed piston '+side,(sgn*.43,-.115,1.10),(sgn*.43,-.115,.98),.024,edge,'upperarm.'+side)
    cylinder('elbow hinge '+side,(sgn*.39,-.015,.98),(sgn*.49,-.015,.98),.086,steel,'forearm.'+side)
    cylinder('forearm rigid spar '+side,(sgn*.455,-.02,.97),(sgn*.47,-.10,.74),.058,graphite,'forearm.'+side)
    plate('forearm ivory guard '+side,[(sgn*.47-.075,-.101,.935),(sgn*.47+.075,-.101,.935),(sgn*.47+.055,-.15,.77),(sgn*.47-.055,-.15,.77)],.035,cream,'forearm.'+side)
    plate('forearm broad shaded bevel '+side,[(sgn*.47-.074,-.111,.931),(sgn*.47-.026,-.111,.931),(sgn*.47-.011,-.162,.774),(sgn*.47-.054,-.162,.774)],.004,ivory_shadow,'forearm.'+side)
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
    plate('thigh broad cool shadow plane '+side,[(sgn*.20-.082,-.138,.677),(sgn*.20-.024,-.138,.677),(sgn*.20+.022,-.158,.465),(sgn*.20-.062,-.158,.465)],.004,sage_shadow,'thigh.'+side)
    cylinder('outer thigh hydraulic '+side,(sgn*.30,0,.65),(sgn*.30,0,.44),.034,copper,'thigh.'+side)
    cylinder('knee trunnion '+side,(sgn*.16,-.02,.415),(sgn*.27,-.02,.415),.094,steel,'shin.'+side)
    cylinder('knee dark center '+side,(sgn*.27,-.02,.415),(sgn*.28,-.02,.415),.060,dark,'shin.'+side)
    box('shin structural beam '+side,(sgn*.215,.015,.26),(.135,.17,.25),graphite,'shin.'+side,.014)
    plate('shin pale replaceable protection '+side,[(sgn*.215-.057,-.10,.365),(sgn*.215+.057,-.10,.365),(sgn*.215+.073,-.12,.17),(sgn*.215-.073,-.12,.17)],.022,cream,'shin.'+side)
    box('shin purposeful lower rub '+side,(sgn*.215+.025,-.128,.184),(.070,.008,.014),edge,'shin.'+side,.002)
    cylinder('ankle pivot '+side,(sgn*.16,0,.13),(sgn*.27,0,.13),.06,steel,'foot.'+side)
    box('grounded rubber tread '+side,(sgn*.215,-.055,.038),(.235,.345,.065),dark,'foot.'+side,.018)
    box('reinforced mechanical foot '+side,(sgn*.215,-.055,.096),(.215,.32,.075),graphite,'foot.'+side,.018)
    box('toe armor strip '+side,(sgn*.215,-.16,.13),(.20,.09,.035),sage,'foot.'+side,.006)
    for xx in [-.065,.065]:bolt('foot access screw '+side,sgn*.215+xx,-.21,.11,'foot.'+side)

# A modeled release flash is exposed only on the authored fire release frame.
flashmat=bpy.data.materials.new('actual weapon-release flash');flashmat.use_nodes=True
pn=flashmat.node_tree.nodes.get('Principled BSDF');pn.inputs['Base Color'].default_value=(1,.65,.20,1);pn.inputs['Emission Color'].default_value=(1,.54,.09,1);pn.inputs['Emission Strength'].default_value=2
verts=[(-.54,-.67,.695),(-.40,-.67,.695),(-.47,-.67,.765),(-.47,-.67,.625),(-.47,-.88,.625),(-.47,-.63,.71)]
faces=[(0,2,4),(2,1,4),(1,3,4),(3,0,4),(0,5,2),(2,5,1),(1,5,3),(3,5,0)]
mesh=bpy.data.meshes.new('release flash authored volume');mesh.from_pydata(verts,[],faces);mesh.update();flash=bpy.data.objects.new('release-only muzzle flash',mesh);bpy.context.collection.objects.link(flash);finish(flash,flash.name,flashmat,'forearm.R');flash.hide_render=True
flashcore=bpy.data.materials.new('release white-hot core');flashcore.use_nodes=True
pn=flashcore.node_tree.nodes.get('Principled BSDF');pn.inputs['Base Color'].default_value=(1,.91,.65,1);pn.inputs['Emission Color'].default_value=(1,.84,.42,1);pn.inputs['Emission Strength'].default_value=3
flash_parts=[flash,cylinder('release-only muzzle core',(-.47,-.66,.695),(-.47,-.77,.658),.030,flashcore,'forearm.R',8)]
for i,(dx,dy,dz) in enumerate([(-.11,-.13,.06),(.08,-.17,-.07),(.045,-.21,.055)]):
    flash_parts.append(cylinder('release-only hot fragment '+str(i),(-.47+dx*.6,-.68+dy*.6,.69+dz*.6),(-.47+dx,-.68+dy,.69+dz),.007,flashmat,'forearm.R',6))
for ob in flash_parts:ob.hide_render=True

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
    ob.parent=rig

# A heavy three-section telescoping double pressure door. Open state is real empty alpha.
current='door';door_leaves=[];door_leaf_origins=[]
for s in [-1,1]:
    box('pressure door side structural housing',(s*.845,.03,.88),(.24,.30,1.76),graphite,bevel=.025)
    box('sage jamb raised service strip',(s*.845,-.14,.88),(.145,.035,1.53),sage,bevel=.01)
    for z in [.20,1.55]:bolt('jamb maintenance fastener',s*.845,-.165,z)
    for z in [.55,.60,.65]:box('door jamb pressure vent',(s*.845,-.165,z),(.08,.012,.016),dark,bevel=.001)
    box('unpowered door status recess',(s*.845,-.175,1.18),(.055,.016,.08),dark,bevel=.003)
box('door header heavy beam',(0,.03,1.75),(1.93,.32,.16),graphite,bevel=.025)
box('door upper guide rail',(0,-.16,1.65),(1.49,.045,.075),steel,bevel=.008)
box('door floor pressure threshold',(0,-.01,.045),(1.93,.40,.09),steel,bevel=.012)
for i in range(9):
    ob=box('physical threshold safety tab',(-.80+i*.20,-.215,.093),(.085,.025,.009),warning,bevel=.001)
for s in [-1,1]:
    for k in range(3):
        x=s*(1.45/12+1.45/6*k);start=len(objects)
        # A pressure-rated leaf has visible depth at the side-on bearing.
        box('telescoping pressure leaf',(x,.015,.86),(1.45/6,.34,1.48),sage,bevel=.018)
        box('leaf broad lower reinforcement',(x,-.170,.28),(1.45/6-.035,.018,.19),graphite,bevel=.006)
        box('leaf structural seam',(x+s*.105,-.043,.86),(.018,.018,1.36),steel,bevel=.002)
        if k==0:box('central thick pressure seal',(x-s*.110,-.047,.86),(.024,.022,1.44),dark,bevel=.002)
        for z in [.24,1.46]:bolt('leaf captive flush screw',x,-.052,z)
        group=objects[start:];door_leaves.append(group);door_leaf_origins.append((s,k,x))

def set_door(opening):
    for group,(s,k,x) in zip(door_leaves,door_leaf_origins):
        dx=(s*.845-x)*opening
        for ob in group:ob.location.x=dx;ob.location.y=(k*.018+.015)*opening

# Pressure clearance is physically taller: actors are never reduced to fit.
for ob in asset_objects['door']:
    for vertex in ob.data.vertices:vertex.co.z*=1.72
door_root=bpy.data.objects.new('pressure door orientation rig',None);bpy.context.collection.objects.link(door_root)
for ob in asset_objects['door']:ob.parent=door_root
door_extra={}
for state in ['locked','braced','damaged']:
    start=len(objects)
    if state=='locked':
        box('physical captive lock block',(0,-.22,1.55),(.32,.22,.25),graphite,bevel=.014)
        box('lock brass mechanical slot',(0,-.338,1.56),(.09,.016,.13),warning,bevel=.005)
        box('captive locked cross-axis yoke',(0,0,1.55),(.15,.62,.11),warning,bevel=.008)
    elif state=='braced':
        cylinder('actual diagonal door brace',(-.69,-.31,.20),(.69,-.31,2.47),.065,edge)
        box('brace projecting mechanical keeper',(-.55,-.36,.45),(.24,.42,.16),warning,bevel=.014)
        for x,z in [(-.69,.20),(.69,2.47)]:box('brace wedge contact',(x,-.25,z),(.18,.14,.12),warning,bevel=.015)
    else:
        box('broken open release housing',(.84,-.19,1.88),(.13,.07,.24),graphite,bevel=.007)
        cylinder('hanging broken release lever',(.87,-.27,1.84),(.94,-.27,1.57),.016,copper)
        hose('loose severed release loom',[(.88,-.18,1.83),(.95,-.20,1.71),(.91,-.22,1.60)],.012,dark)
    door_extra[state]=objects[start:]
    for ob in objects[start:]:ob.parent=door_root;ob.hide_render=True
door_east_cutaway=[]
for ob in asset_objects['door']:
    center=sum((v.co.x for v in ob.data.vertices))/max(1,len(ob.data.vertices))
    # Retain the far full-height jamb: the near jamb/header are cut away, not the
    # entire doorway. This is a physical drawing cutaway along the same passage.
    if ob.name.startswith(('door header heavy beam','door upper guide rail')) or (center<0 and ob.name.startswith(('pressure door side structural housing','sage jamb raised service strip','jamb maintenance fastener','door jamb pressure vent','unpowered door status recess'))):door_east_cutaway.append(ob)
start=len(objects)
for s in [-1,1]:
    box('east door cutaway low jamb',(s*.845,.03,.13),(.24,.68,.26),graphite,bevel=.016)
    box('east door cutaway cap',(s*.845,.03,.272),(.25,.69,.025),sage,bevel=.005)
    box('east doorway base worn arris',(s*.845,-.323,.20),(.20,.018,.025),edge,bevel=.003)
# Exposed guides and a pressure-rated sill describe the passage in plan. Nothing
# crosses the open aperture above floor height.
for y in [-.31,.31]:box('east threshold guide channel',(0,y,.046),(1.44,.045,.036),dark,bevel=.004)
for x in [-.55,-.28,0,.28,.55]:box('east threshold broad safety inset',(x,-.21,.099),(.11,.14,.013),warning,bevel=.002)
door_east_base=objects[start:]
for ob in door_east_base:ob.parent=door_root;ob.hide_render=True
start=len(objects)
box('east damaged cutaway base release housing',(-.845,-.16,.19),(.13,.055,.12),graphite,bevel=.006)
cylinder('east snapped low release stud',(-.845,-.20,.23),(-.805,-.23,.16),.014,copper)
door_east_damage=objects[start:]
for ob in door_east_damage:ob.parent=door_root;ob.hide_render=True

# Original mounted camera hardware, proper gimbal and distinct front/back housing.
current='camera';camera_parts={};camera_housing=[]
box('camera bracket wall mounting plate',(0,.13,.60),(.32,.08,.54),graphite,bevel=.012)
for x in [-.115,.115]:
    for z in [.40,.80]:bolt('camera wall captive bolt',x,.075,z)
cylinder('camera cantilever tube',(0,.11,.62),(0,-.18,.62),.055,steel)
box('camera pan rotary base',(0,-.18,.69),(.22,.20,.10),graphite,bevel=.015)
cylinder('camera pan bearing',(0,-.18,.71),(0,-.18,.80),.080,steel)
start=len(objects)
box('CCTV rectangular armored housing',(0,-.22,.98),(.40,.58,.29),cream,bevel=.035)
box('camera top weather hood',(0,-.24,1.16),(.46,.64,.045),sage,bevel=.012)
box('camera front recessed optical panel',(0,-.521,.98),(.32,.022,.20),dark,bevel=.013)
cylinder('camera circular lens shroud',(0,-.535,.99),(0,-.573,.99),.070,graphite)
cylinder('camera black lens',(0,-.574,.99),(0,-.580,.99),.049,muted)
box('camera rear service plate',(0,.08,.98),(.30,.018,.19),sage,bevel=.009)
for x in [-.13,.13]:
    bolt('camera front fastener',x,-.54,1.05)
    for y in [-.02,.02,.06]:box('camera passive case vent',(x*1.57,y,.98),(.012,.018,.10),dark,bevel=.001)
camera_lamp=box('actual camera power status tag',(.12,-.54,.93),(.025,.012,.025),amber,bevel=.004)
camera_housing=objects[start:]
camera_pivot=bpy.data.objects.new('camera real housing tilt joint',None);bpy.context.collection.objects.link(camera_pivot);camera_pivot.location=(0,-.18,.80)
camera_root=bpy.data.objects.new('camera authoritative cardinal mount',None);bpy.context.collection.objects.link(camera_root)
camera_pivot.parent=camera_root;bpy.context.view_layer.update()
for ob in asset_objects['camera']:
    if ob in camera_housing:ob.parent=camera_pivot;ob.matrix_parent_inverse=camera_pivot.matrix_world.inverted()
    else:ob.parent=camera_root

# One-cell workplace modules, no art spanning a passable neighboring tile.
for kind in ['inspection-desk','maintenance-bench']:
    current=kind
    for x in [-.34,.34]:
        box(kind+' steel pedestal',(x,0,.36),(.20,.52,.67),graphite,bevel=.02)
        box(kind+' sage access door',(x,-.275,.39),(.14,.025,.43),sage,bevel=.008)
        box(kind+' inset pull',(x,-.294,.55),(.07,.015,.025),dark,bevel=.003)
    box(kind+' substantial work surface',(0,0,.73),(.96,.68,.09),steel,bevel=.022)
    box(kind+' rubbed work edge',(0,-.35,.725),(.95,.025,.05),edge,bevel=.006)
    if kind=='inspection-desk':
        box('inspection blotter',(0,-.055,.782),(.39,.32,.012),dark,bevel=.003)
        box('inspection paper record',(-.035,-.065,.791),(.23,.23,.008),paper,bevel=.001)
        for i in range(4):box('printed inspection lines',(-.04,-.13+i*.04,.797),(.15,.009,.002),sage,bevel=0)
        box('compact desk instrument',(.28,.11,.83),(.17,.23,.13),sage,bevel=.016)
        box('dark unpowered desk display',(.28,-.012,.85),(.12,.014,.055),dark,bevel=.002)
        cylinder('inspection document lamp',(-.30,.20,.78),(-.30,.20,1.04),.014,steel)
        box('unlit document lamp hood',(-.30,.14,1.06),(.17,.23,.055),graphite,bevel=.014)
    else:
        box('bench steel vise fixed jaw',(.26,-.10,.86),(.18,.15,.17),steel,bevel=.008)
        box('bench steel vise moving jaw',(.26,-.24,.86),(.18,.09,.17),edge,bevel=.008)
        cylinder('vise screw',(.26,-.14,.83),(.26,-.38,.83),.025,graphite)
        cylinder('vise crosshandle',(.16,-.39,.83),(.36,-.39,.83),.013,steel)
        box('bench loose maintenance part',(-.18,-.06,.82),(.23,.20,.08),copper,bevel=.012)
        for x in [-.32,-.22,-.12]:cylinder('bench resting tool',(x,.18,.80),(x,.02,.80),.014,edge)
    # Deliberate wear follows work/contact edges instead of random scratches.
    for x,w in [(-.33,.09),(-.10,.13),(.23,.06)]:
        box(kind+' rubbed front edge patch',(x,-.365,.741),(w,.009,.014),cream,bevel=.002)
    for x in [-.34,.34]:
        box(kind+' foot contact shoe',(x,0,.035),(.26,.57,.07),dark,bevel=.01)
        box(kind+' access-door lower grime seam',(x,-.294,.18),(.13,.008,.018),rust,bevel=.002)

# Bounded receiving kit. One cell of occupied cover, or one existing wall recess.
# These original objects establish inspection rather than generic storage.
current='inspection-terminal'
box('inspection terminal grounded cabinet',(0,.02,.37),(.78,.62,.72),graphite,bevel=.025)
box('inspection terminal ivory drawer',(-.04,-.30,.48),(.61,.025,.28),cream,bevel=.008)
box('inspection terminal recessed drawer pull',(-.04,-.320,.55),(.25,.012,.04),dark,bevel=.003)
box('inspection terminal dark boot clearance',(0,-.27,.13),(.60,.10,.16),dark,bevel=.008)
box('inspection terminal substantial sloped console',(0,-.015,.80),(.88,.72,.15),sage,bevel=.025)
plate('inspection terminal face shoulder',[(-.39,-.36,.85),(.39,-.36,.85),(.39,.28,1.00),(-.39,.28,1.00)],.035,graphite)
box('inspection terminal cathode screen housing',(-.10,.18,1.14),(.52,.27,.41),graphite,bevel=.035)
box('inspection terminal screen ivory bezel',(-.10,.027,1.14),(.45,.038,.34),cream,bevel=.017)
box('inspection terminal honest dark glass',(-.10,.003,1.14),(.365,.012,.255),muted,bevel=.01)
plate('inspection terminal glass controlled reflection',[(-.265,-.006,1.245),(-.14,-.006,1.245),(-.265,-.006,1.158)],.003,glass_reflection)
box('inspection terminal recessed input pad',(-.08,-.20,.891),(.41,.20,.014),dark,bevel=.005)
for row in range(3):
    for col in range(7):box('inspection terminal physical key',(-.25+col*.056,-.26+row*.057,.902),(.043,.038,.016),steel,bevel=.002)
cylinder('inspection terminal release pushbutton',(.29,-.20,.897),(.29,-.20,.930),.049,warning,vertices=16)
box('inspection terminal physical paper slot',(.29,.05,.926),(.15,.19,.015),dark,bevel=.004)
box('inspection terminal retained paper tongue',(.29,-.015,.937),(.12,.12,.006),paper,bevel=.001)
for x in [-.34,.34]:
    box('inspection terminal anchored shoe',(x,.015,.035),(.18,.68,.07),dark,bevel=.01)
    bolt('inspection terminal captive face screw',x,-.323,.60)
for x,w in [(-.34,.075),(.22,.08)]:box('inspection terminal rubbed front lip',(x,-.383,.849),(w,.012,.016),edge,bevel=.002)
plate('inspection terminal left structural gusset',[(-.433,-.33,.73),(-.433,-.25,.73),(-.433,.22,1.01),(-.433,.22,.73)],.05,graphite)
box('inspection terminal cool cabinet shadow panel',(.25,-.328,.39),(.12,.015,.38),sage_shadow,bevel=.006)
for z in [.25,.29,.33,.37]:box('inspection terminal cabinet pressure vent',(.24,-.342,z),(.09,.012,.011),dark,bevel=.001)
box('inspection terminal maintenance access hatch',(-.23,-.327,.34),(.19,.014,.16),sage,bevel=.006)
for x in [-.30,-.16]:bolt('inspection terminal maintenance captive screw',x,-.344,.35)
plate('inspection terminal worn drawer corner',[(-.34,-.337,.61),(-.29,-.337,.61),(-.34,-.337,.584)],.003,edge)
plate('inspection terminal drawer broad warm plane',[(-.335,-.337,.37),(.20,-.337,.37),(.20,-.337,.413),(-.335,-.337,.52)],.002,ivory_shadow)
for x,w in [(-.18,.10),(.01,.055)]:box('inspection terminal front working scar',(x,-.379,.846),(w,.005,.007),ivory_shadow,bevel=.001)

current='receiving-bulkhead'
box('receiving bulkhead deep structural backing',(0,.08,.66),(.98,.31,1.32),graphite,bevel=.018)
box('receiving bulkhead dark recessed service face',(0,-.091,.66),(.68,.035,1.08),dark,bevel=.008)
for x in [-.39,.39]:
    box('receiving bulkhead substantial vertical rib',(x,-.09,.67),(.17,.18,1.34),sage,bevel=.018)
    box('receiving bulkhead rib selected front plane',(x,-.189,.67),(.085,.018,1.20),steel,bevel=.004)
    for z in [.15,1.19]:bolt('receiving bulkhead structural fastener',x,-.211,z)
for z in [.06,1.28]:box('receiving bulkhead crossbeam',(0,-.075,z),(.98,.24,.12),graphite,bevel=.018)
for z in [.35,.67,.99]:
    box('receiving bulkhead removable insulated panel',(0,-.12,z),(.57,.045,.25),sage,bevel=.007)
    box('receiving bulkhead panel raised pale index',(-.19,-.149,z),(.055,.012,.15),cream,bevel=.002)
    box('receiving bulkhead panel seam',(0,-.151,z-.085),(.43,.010,.012),dark,bevel=.001)
    plate('receiving bulkhead selected broad shadow panel',[(-.27,-.154,z+.11),(-.18,-.154,z+.11),(.12,-.154,z-.10),(-.27,-.154,z-.10)],.004,sage_shadow)
cylinder('receiving bulkhead exposed vertical service pipe',(.21,-.19,.14),(.21,-.19,1.18),.032,copper)
for z in [.30,1.04]:cylinder('receiving bulkhead pipe clamp',(.21,-.19,z-.025),(.21,-.19,z+.025),.045,steel)
box('receiving bulkhead lower accumulated oxide',(0,-.20,.075),(.63,.014,.025),rust,bevel=.002)
for x,z,w in [(-.39,.22,.08),(.39,1.14,.065),(-.14,1.30,.12)]:box('receiving bulkhead selective contact wear',(x,-.217,z),(w,.006,.015),edge,bevel=.002)
cylinder('receiving bulkhead overhead service conduit',(-.30,-.21,1.20),(.29,-.21,1.20),.025,dark)

current='cargo-cradle'
box('receiving cargo restrained pallet',(0,0,.085),(.90,.72,.17),graphite,bevel=.025)
for x in [-.37,.37]:
    box('receiving cargo steel side rail',(x,.04,.41),(.095,.65,.70),sage,bevel=.018)
    box('receiving cargo lower grounded shoe',(x,0,.035),(.16,.75,.07),dark,bevel=.012)
box('receiving cargo rear crossbar',(0,.31,.63),(.79,.08,.12),steel,bevel=.012)
for x,z in [(-.17,.56),(.16,.50)]:
    cylinder('receiving cargo unmarked pressure housing',(x,.05,.18),(x,.05,z+.26),.133,cream,vertices=16)
    cylinder('receiving cargo dark neck',(x,.05,z+.25),(x,.05,z+.32),.055,graphite,vertices=12)
    box('receiving cargo protective collar',(x,.05,z+.32),(.19,.18,.04),sage,bevel=.01)
    box('receiving cargo solid retention band',(x,-.09,.43),(.24,.035,.08),graphite,bevel=.004)
    cylinder('receiving cargo worn protective cylinder base',(x,.05,.18),(x,.05,.23),.139,sage,vertices=16)
    cylinder('receiving cargo steel valve spindle',(x,.05,z+.32),(x,.05,z+.37),.018,copper,vertices=8)
    box('receiving cargo valve butterfly',(x,.05,z+.38),(.11,.032,.025),steel,bevel=.004)
    box('receiving cargo physical inspection label',(x,-.087,.59),(.10,.008,.10),paper,bevel=.001)
    for zz in [.57,.59,.61]:box('receiving cargo inspection label ink',(x,-.094,zz),(.065,.004,.007),graphite,bevel=.001)
box('receiving cargo positive lock bar',(0,-.13,.43),(.69,.06,.10),sage,bevel=.008)
box('receiving cargo unpowered latch block',(.31,-.17,.43),(.12,.08,.17),warning,bevel=.007)

current='mechanism';mechanism_handles=[];mechanism_lights=[]
box('circuit release wall case',(0,0,.50),(.52,.24,.82),graphite,bevel=.028)
box('circuit ivory service face',(0,-.13,.50),(.44,.035,.72),cream,bevel=.014)
box('circuit black lever well',(0,-.155,.42),(.23,.025,.31),dark,bevel=.008)
cylinder('circuit physical lever pivot',(-.09,-.19,.42),(.09,-.19,.42),.037,steel)
mechanism_handles.append(cylinder('circuit grounded release lever',(0,-.20,.43),(0,-.30,.64),.027,copper))
mechanism_handles.append(box('circuit insulated release grip',(0,-.30,.66),(.18,.07,.07),dark,bevel=.014))
mechanism_lights.append(box('circuit truthful power lamp',(-.10,-.158,.76),(.055,.012,.055),amber,bevel=.006))
box('circuit printed release label',(.05,-.158,.76),(.17,.012,.045),sage,bevel=.002)
for x in [-.18,.18]:
    for z in [.19,.81]:bolt('circuit captive face bolt',x,-.16,z)

current='placard'
box('department wall enamel placard',(0,.015,.60),(.77,.055,.34),sage,bevel=.012)
box('department ivory label field',(.08,-.017,.60),(.48,.012,.22),cream,bevel=.005)
for z,w in [(.65,.36),(.60,.30),(.55,.22)]:box('department engraved index line',(.08,-.026,z),(w,.006,.012),graphite,bevel=.001)
box('department physical amber index stripe',(-.26,-.025,.60),(.055,.009,.23),warning,bevel=.003)

mechanism_pivot=bpy.data.objects.new('mechanism actual throw lever',None);bpy.context.collection.objects.link(mechanism_pivot);mechanism_pivot.location=(0,-.20,.42);bpy.context.view_layer.update()
for ob in mechanism_handles:ob.parent=mechanism_pivot;ob.matrix_parent_inverse=mechanism_pivot.matrix_world.inverted()

# Three bounded original material responses. These are offline physical shard
# constructions, selected only when the engine reports an observed real event.
effect_parts={}
for kind in ['metal-impact','suit-impact','sparks']:
    current='effect-'+kind;parts=[]
    if kind!='suit-impact':
        core=sphere(kind+' brief contact core',(0,0,.52),(.045,.025,.045),flashcore)
        parts.append((core,(0,0,0),'core'))
        directions=[(-.42,0,.30),(-.18,0,.52),(.15,0,.58),(.40,0,.25),(.31,0,-.11),(-.28,0,-.13),(.06,0,.39)]
        for i,(x,y,z) in enumerate(directions):
            a=Vector((x*.13,0,.52+z*.13));b=a+Vector((x,0,z))*.20
            ob=cylinder(kind+' hot tapered fragment '+str(i),a,b,.007 if i%2 else .010,flashcore if i%3==0 else flashmat,vertices=5)
            parts.append((ob,(x,y,z),'spark'))
        for i,(x,z) in enumerate([(-.20,.12),(.22,.09),(.08,.27)]):
            ob=sphere(kind+' dull displaced metal chip '+str(i),(x*.15,.025,.52+z*.15),(.025,.013,.018),steel)
            parts.append((ob,(x,0,z),'chip'))
    else:
        for i,(x,z) in enumerate([(-.23,.31),(.26,.17),(.09,.39),(-.32,-.05),(.22,-.09)]):
            ob=plate('suit actual pressure textile fragment '+str(i),[(x*.15-.017,0,.52+z*.15-.014),(x*.15+.027,0,.52+z*.15),(x*.15,0,.52+z*.15+.025)],.008,cream if i%2 else dark)
            parts.append((ob,(x,0,z),'cloth'))
        for i,(x,z) in enumerate([(-.18,.09),(.11,.20),(.24,.08),(-.09,.31)]):
            ob=sphere('suit nonemissive seam debris '+str(i),(x*.1,.03,.52+z*.1),(.026,.015,.023),rust if i%2 else graphite)
            parts.append((ob,(x,0,z),'dust'))
    effect_parts[kind]=[(ob,velocity,role,sum((v.co for v in ob.data.vertices),Vector())/len(ob.data.vertices)) for ob,velocity,role in parts]

def effect_pose(kind,index):
    t=index/6
    for ob,velocity,role,center in effect_parts[kind]:
        ob.hide_render=role=='core' and index>1
        shrink=(1-t*.80) if role not in ['dust'] else (.65+.3*t)
        ob.location=Vector(velocity)*(t*(.75+.25*t))+center*(1-shrink)
        ob.location.z-=.16*t*t
        ob.scale=(shrink,shrink,shrink)

def light(name,location,power,size,color):
    data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;data.color=color
    ob=bpy.data.objects.new(name,data);bpy.context.collection.objects.link(ob);ob.location=location;ob.rotation_euler=(Vector((0,0,.9))-ob.location).to_track_quat('-Z','Y').to_euler()
light('broad painted form key',(-3,-4,6),470,2.6,(1,.93,.79));light('quiet neutral fill',(4,-1,3),55,4,(.78,.88,.86));light('restrained structural rim',(0,4,5),125,3,(.86,.91,.86))
camera_data=bpy.data.cameras.new('orthographic station sprite camera');camera=bpy.data.objects.new('orthographic station sprite camera',camera_data);bpy.context.collection.objects.link(camera);scene.camera=camera
camera.location=(0,-6,5.5);target=Vector((0,0,.91));camera.rotation_euler=(target-camera.location).to_track_quat('-Z','Y').to_euler();camera_data.type='ORTHO';camera_data.ortho_scale=2.6

def select_asset(asset):
    for name,group in asset_objects.items():
        for ob in group:ob.hide_render=name!=asset
    rig.hide_render=asset!='sirs'
    for group in door_extra.values():
        for ob in group:ob.hide_render=True
    for ob in door_east_base:ob.hide_render=True
    for ob in door_east_damage:ob.hide_render=True

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
def render(name,asset='sirs',height_cells=2.0,metadata=None):
    if REUSE_NON_DOOR and asset!='door':
        # Narrow correction mode: untouched models retain exact full05 raster bytes.
        entry=PREVIOUS_REGIONS[name].copy();assert sha(ROOT/'assets'/entry['path'])==entry['sha256']
        if metadata:entry.update(metadata)
        return entry
    # Transparent padding follows real projected geometry, preserving pixel scale.
    base_scale=camera_data.ortho_scale;base_height=height_cells
    scene.render.resolution_x=288;scene.render.resolution_y=round(height_cells*192)
    deps=bpy.context.evaluated_depsgraph_get();xs=[];ys=[]
    for ob in asset_objects[asset]:
        if ob.hide_render:continue
        evaluated=ob.evaluated_get(deps);mesh=evaluated.to_mesh()
        for vertex in mesh.vertices:
            p=world_to_camera_view(scene,camera,evaluated.matrix_world@vertex.co);xs.append(p.x);ys.append(p.y)
        evaluated.to_mesh_clear()
    width_cells=max(1.5,math.ceil((2*max(max(xs)-.5,.5-min(xs))*1.5+.035)*4)/4)
    height_cells=max(height_cells,math.ceil((2*max(max(ys)-.5,.5-min(ys))*height_cells+.035)*4)/4)
    camera_data.ortho_scale=base_scale*max(width_cells,height_cells)/max(1.5,base_height)
    scene.render.resolution_x=round(width_cells*192);scene.render.resolution_y=round(height_cells*192);bpy.context.view_layer.update()
    anchor=world_to_camera_view(scene,camera,Vector((0,0,0)))
    path=REGIONS/(name+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
    entry=dict(name=name,path=path.relative_to(ROOT/'assets').as_posix(),widthCells=width_cells,heightCells=height_cells,pivotX=float(anchor.x),pivotY=float(anchor.y),pixelWidth=round(width_cells*192),pixelHeight=round(height_cells*192),layer='world',sha256=sha(path),asset=asset)
    if metadata:entry.update(metadata)
    # Separately rendered masters, not a claim that upscaling creates detail.
    scene.render.resolution_x=round(width_cells*256);scene.render.resolution_y=round(height_cells*256);scene.render.filepath=str(MASTERS/(name+'.png'));bpy.ops.render.render(write_still=True)
    camera_data.ortho_scale=base_scale;scene.render.resolution_x=288;scene.render.resolution_y=round(base_height*192)
    return entry

def camera_aim(target_z=.91,ortho=None,height_cells=2):
    camera.location=(0,-6,target_z+4.59);camera.rotation_euler=(Vector((0,0,target_z))-camera.location).to_track_quat('-Z','Y').to_euler()
    if ortho:camera_data.ortho_scale=ortho
    scene.render.resolution_x=288;scene.render.resolution_y=round(height_cells*192);bpy.context.view_layer.update()

DIRECTIONS={'north':math.pi,'east':math.pi/2,'south':0,'west':-math.pi/2}
exec(Path(__file__).with_name('motion.py').read_text(),globals())
metrics=[]
for facing,angle in DIRECTIONS.items():
    select_asset('sirs');camera_data.ortho_scale=SIRS_SCALE;rig.rotation_euler.z=angle;bpy.context.view_layer.update()
    feet=pose(0,facing,'idle');name='machine.sirs.idle.'+facing;entry=render(name,metadata={'feet':feet});regions.append(entry)
    clips.append(dict(id=name,frameSeconds=.5,loop=False,frames=[entry],markers=[],cycleCells=0))
    bindings.append(dict(actor='sirs',action='idle',facing=facing,clip=name))

def export_sirs(action,facing,n,seconds):
    select_asset('sirs');camera_aim(.91,SIRS_SCALE);frames=[];footframes=[]
    cid='machine.sirs.'+action+'.'+facing
    for i in range(n):
        phase=i/n if action=='walk' else i/(n-1) if n>1 else 0
        feet=pose(phase,facing,action,i);entry=render(cid+'.'+f'{i:02}',metadata={'feet':feet});regions.append(entry);frames.append(entry);footframes.append(feet)
    markers=[dict(kind='contact',frame=0,foot='left'),dict(kind='contact',frame=n//2,foot='right')] if action=='walk' else [dict(kind='release',frame=2),dict(kind='recovery',frame=6)] if action=='fire' else []
    clips.append(dict(id=cid,frameSeconds=seconds/n,loop=action=='walk',frames=frames,markers=markers,cycleCells=.5 if action=='walk' else 0))
    bindings.append(dict(actor='sirs',action=action,facing=facing,clip=cid))
    if action=='walk':
        residual=[]
        for side in ['left','right']:
            run=[]
            for frame in footframes:
                if frame[side]['stance']:run.append(frame[side]['rootPlusLocalPixels'])
                elif run:
                    residual.append(float(np.max(np.linalg.norm(np.array(run)-run[0],axis=1))));run=[]
            if run:residual.append(float(np.max(np.linalg.norm(np.array(run)-run[0],axis=1))))
        metrics.append(dict(clip=cid,cycleCells=.5,nativeSecondsPerCell=.5,seconds=seconds,sampledStanceResidualSourcePixels=max(residual,default=0),heldFrameTravelAt47pxCell=47*.5/n))
if (FULL and not BENCHMARK_PILOT) or MOTION_PILOT:
    for facing in (DIRECTIONS if FULL else ['south','east']):
        export_sirs('walk',facing,12,.25)
        if FULL:
            for action,n,seconds in [('start',6,.15),('stop',6,.15),('anticipate',6,.30),('fire',8,.40),('hit',5,.20),('destroyed',10,.70)]:export_sirs(action,facing,n,seconds)

select_asset('door');rig.rotation_euler.z=0;camera_aim(1.35,3.3125,2.5)
for state,opening in [('closed',0),('open',1)]:
    name='door.'+state;set_door(opening);bpy.context.view_layer.update();entry=render(name,'door',2.5);regions.append(entry)
    clips.append(dict(id=name+'.north',frameSeconds=.5,loop=False,frames=[entry],markers=[],cycleCells=0))
    bindings.append(dict(actor='door',action=name.split('.')[-1],facing='north',clip=name+'.north'))

if FULL:
    def door_state(state,orientation,opening):
        select_asset('door');camera_aim(1.35,3.3125,2.5);door_root.rotation_euler.z=0 if orientation=='north' else math.pi/2
        # A deep side-on bulkhead gives the shutter a visible obstructing surface
        # and the threshold a visible passage. Axis/camera/footprint stay exact.
        door_root.scale.y=1 if orientation=='north' else 2.2
        for ob in door_extra.get(state,[]):ob.hide_render=False
        if orientation=='east':
            for ob in door_east_cutaway:ob.hide_render=True
            for ob in door_east_base:ob.hide_render=False
            if state=='damaged':
                for ob in door_extra['damaged']:ob.hide_render=True
                for ob in door_east_damage:ob.hide_render=False
            if opening>.94:
                for group,(s,k,x) in zip(door_leaves,door_leaf_origins):
                    for ob in group:ob.hide_render=True
        set_door(opening);bpy.context.view_layer.update()
    for orientation in ['north','east']:
        for state in ['closed','open','opening','closing','locked','braced','damaged']:
            if orientation=='north' and state in ['closed','open']:continue
            transition=state in ['opening','closing'];n=8 if transition else 1;frames=[]
            for i in range(n):
                phase=i/(n-1) if transition else 0
                opening=smooth(phase) if state=='opening' else 1-smooth(phase) if state=='closing' else 1 if state in ['open','damaged'] else 0
                door_state(state,orientation,opening)
                name=f'door.{state}.{orientation}'+(f'.{i:02}' if transition else '')
                entry=render(name,'door',2.5,metadata={'physicalState':state,'passageOpen':state in ['open','damaged'],'forcedOpen':state=='damaged','hasPhysicalBrace':state=='braced','poweredIndicators':False,'orientationDegrees':0 if orientation=='north' else 90,'sameCamera':True,'foregroundCutaway':orientation=='east','footprintCells':[1,1]})
                regions.append(entry);frames.append(entry)
            cid=f'door.{state}.{orientation}'
            clips.append(dict(id=cid,frameSeconds=.035 if transition else .5,loop=False,frames=frames,markers=[dict(kind='clearance',frame=6)] if state=='opening' else [],cycleCells=0))
            bindings.append(dict(actor='door',action=state,facing=orientation,clip=cid))

    for facing,angle in ([] if BENCHMARK_PILOT else DIRECTIONS.items()):
        for state in ['on','off','aim','disabled']:
            select_asset('camera');camera_aim(.91,2.65);camera_root.rotation_euler.z=angle
            camera_lamp.data.materials[0]=dark if state in ['off','disabled'] else amber
            n=5 if state=='aim' else 1;frames=[]
            for i in range(n):
                camera_pivot.rotation_euler.x=.55 if state=='disabled' else -.08*smooth(i/(n-1)) if state=='aim' else 0
                bpy.context.view_layer.update();name=f'machine.camera.{state}.{facing}'+(f'.{i:02}' if n>1 else '')
                entry=render(name,'camera',metadata={'powered':state in ['on','aim'],'disabled':state=='disabled','tacticalScan':False});regions.append(entry);frames.append(entry)
            cid=f'machine.camera.{state}.{facing}';clips.append(dict(id=cid,frameSeconds=.06,loop=False,frames=frames,markers=[],cycleCells=0));bindings.append(dict(actor='camera',action=state,facing=facing,clip=cid))

    for asset,name in [('inspection-desk','prop.inspection-desk'),('maintenance-bench','prop.maintenance-bench'),('inspection-terminal','prop.inspection-terminal'),('receiving-bulkhead','prop.receiving-bulkhead'),('cargo-cradle','prop.cargo-cradle'),('placard','placard.department')]:
        select_asset(asset);camera_aim(.91,2.65);entry=render(name,asset,metadata={'footprintCells':[0,0] if asset in ['placard','receiving-bulkhead'] else [1,1],'poweredIndicators':False,'contactWorld':[0,-.20,.92] if asset=='inspection-terminal' else None});regions.append(entry)
    for state,powered,released in [('unpowered',False,False),('powered',True,False),('released',True,True),('released-unpowered',False,True)]:
        select_asset('mechanism');camera_aim(.91,2.65);mechanism_pivot.rotation_euler.x=math.radians(115) if released else 0
        for ob in mechanism_lights:ob.data.materials[0]=amber if powered else dark
        bpy.context.view_layer.update();entry=render('control.switch.'+state,'mechanism',metadata={'powered':powered,'released':released,'footprintCells':[0,0]});regions.append(entry)

    for kind in effect_parts:
        frames=[];select_asset('effect-'+kind);camera_aim(.52,2.65)
        for index in range(7):
            effect_pose(kind,index);bpy.context.view_layer.update()
            name=f'effect.{kind}.{index:02}'
            entry=render(name,'effect-'+kind,metadata={'footprintCells':[0,0],'effectContactWorld':[0,0,.52],'materialResponse':kind,'emissive':kind!='suit-impact'});regions.append(entry);frames.append(entry)
        cid='effect.'+kind;clips.append(dict(id=cid,frameSeconds=.035,loop=False,frames=frames,markers=[dict(kind='contact',frame=0)],cycleCells=0))
        bindings.append(dict(actor='effect',action=kind,facing='north',clip=cid))

write(OUT/'regions.json',dict(version=1,referenceCellPixels=192,editableReferenceCellPixels=256,pivotOrigin='bottom-left',pathRoot='assets',regions=regions))
write(OUT/'clips.json',dict(version=1,referenceCellPixels=192,editableReferenceCellPixels=256,pivotOrigin='bottom-left',clips=clips,bindings=bindings,footCalibration=metrics,status='opening-slice development candidate; no native or artistic acceptance',missing=[] if FULL else ['SIRS remaining action coverage','all camera and mechanism states','door motion/locked/braced/damaged']))
select_asset('sirs');set_door(0);door_root.rotation_euler.z=0;door_root.scale.y=1;camera_aim(.91,SIRS_SCALE);pose(0,'south','idle')
if FULL:
    # Named editable source actions retain authored rig keys; runtime still consumes PNGs.
    for action,n in [('walk',12),('anticipate',6),('fire',8),('hit',5),('destroyed',10)]:
        rig.animation_data_create();source_action=bpy.data.actions.new('SIRS authored '+action);rig.animation_data.action=source_action
        for i in range(n):
            scene.frame_set(i+1);pose(i/n if action=='walk' else i/(n-1),'south',action,i)
            for bone in rig.pose.bones:
                bone.rotation_mode='QUATERNION';bone.keyframe_insert('location',frame=i+1);bone.keyframe_insert('rotation_quaternion',frame=i+1);bone.keyframe_insert('scale',frame=i+1)
        source_action.use_fake_user=True;rig.animation_data.action=None
    pose(0,'south','idle');scene.frame_set(1)
scene.render.resolution_x=384;scene.render.resolution_y=512
scene['runtimeRasterPixelsPerCell']=192;scene['authoringReferencePixelsPerCell']=256;scene['authority']='Core owns position, facing, aiming, circuit and door state. No presentation scanning or runtime3D.'
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'origin-war-opening-station.blend'))
write(SOURCE/'PROVENANCE.json',dict(task='BM-ROOM',base='c3ad573',tool=bpy.app.version_string,medium='original Blender mechanical meshes/materials/armature; offline rendered candidate, not hand-painted brushwork',externalAssets=[],rights='Original authored construction from project visual identity references; no outside model/texture/animation',sourceBlend=dict(path='origin-war-opening-station.blend',sha256=sha(SOURCE/'origin-war-opening-station.blend')),scriptSha256=sha(Path(__file__)),motionScriptSha256=sha(Path(__file__).with_name('motion.py')),referencePaths=['art/survival/regions/machine.sirs.png','art/survival/regions/machine.camera.png','art/survival/regions/door.closed.png','art/survival/regions/door.open.png','art/survival/regions/hatch.closed.png','art/survival/regions/control.switch.png'],limitations='Bounded receiving benchmark candidate; source boards/contact calibration are not native-play or commercial acceptance.'))
write(OUT/'review/timing.json',dict(totalSeconds=time.perf_counter()-START,mode='full' if FULL else 'motion-pilot' if MOTION_PILOT else 'look-pilot',status='development candidate'))
print('STATION_PILOT_DONE',json.dumps(dict(regions=len(regions),clips=len(clips),sirsOrthoScale=SIRS_SCALE)))
