"""V4 workwear construction and restrained illustrated material hierarchy.

All finish is editable geometry/material nodes evaluated on the actual rig. Broad
panels and overlap shadows survive the approximately 60-pixel gameplay silhouette;
fine weave is subordinate. No rendered-image paint or per-frame filtering occurs.
"""
def painted_finish(mat,base,kind='cloth'):
    nodes=mat.node_tree.nodes;nodes.clear();links=mat.node_tree.links
    out=nodes.new('ShaderNodeOutputMaterial');out.label='Native illustrated surface'
    diffuse=nodes.new('ShaderNodeBsdfDiffuse');diffuse.inputs['Color'].default_value=(.8,.8,.8,1)
    diffuse.inputs['Roughness'].default_value=1
    rgb=nodes.new('ShaderNodeShaderToRGB');links.new(diffuse.outputs[0],rgb.inputs[0])
    ramp=nodes.new('ShaderNodeValToRGB');ramp.label='Painted light and cool overlap shadow'
    ramp.color_ramp.interpolation='EASE';ramp.color_ramp.elements.remove(ramp.color_ramp.elements[1])
    levels=[(.10,.30),(.33,.57),(.57,.87),(.79,1.03)]
    for i,(position,value) in enumerate(levels):
        e=ramp.color_ramp.elements[0] if i==0 else ramp.color_ramp.elements.new(position)
        e.position=position
        # Slightly cool the dark planes; highlights stay warm and restricted.
        tint=(.87,.97,1.07) if i<2 else (1.02,1.0,.95)
        e.color=(*(min(1,c*value*t) for c,t in zip(base,tint)),1)
    links.new(rgb.outputs[0],ramp.inputs[0]);color=ramp.outputs[0]
    coord=nodes.new('ShaderNodeTexCoord');coord.label='Rest-surface material coordinates'
    noise=nodes.new('ShaderNodeTexNoise');noise.label='Low contrast broad painted variation'
    noise.inputs['Scale'].default_value=5 if kind=='cloth' else 10
    noise.inputs['Detail'].default_value=1.5;noise.inputs['Roughness'].default_value=.55
    links.new(coord.outputs['Generated'],noise.inputs['Vector'])
    variation=nodes.new('ShaderNodeValToRGB');variation.color_ramp.elements[0].color=(.80,.84,.85,1)
    variation.color_ramp.elements[1].color=(1,1,1,1);links.new(noise.outputs['Fac'],variation.inputs[0])
    multiply=nodes.new('ShaderNodeMixRGB');multiply.blend_type='MULTIPLY';multiply.inputs[0].default_value=.62
    links.new(color,multiply.inputs[1]);links.new(variation.outputs[0],multiply.inputs[2]);color=multiply.outputs[0]
    ao=nodes.new('ShaderNodeAmbientOcclusion');ao.label='Actual garment and equipment contact shadow'
    ao.inputs['Distance'].default_value=.105;ao.inputs['Color'].default_value=(1,1,1,1)
    crevice=nodes.new('ShaderNodeMixRGB');crevice.blend_type='MULTIPLY';crevice.inputs[0].default_value=.70
    links.new(color,crevice.inputs[1]);links.new(ao.outputs['Color'],crevice.inputs[2]);color=crevice.outputs[0]
    emission=nodes.new('ShaderNodeEmission');emission.inputs['Strength'].default_value=1
    links.new(color,emission.inputs[0]);links.new(emission.outputs[0],out.inputs['Surface'])
    mat['constructionProfile']=kind

painted_finish(ivory,(.77,.72,.58),'enamel')
painted_finish(garment,(.51,.54,.47),'cloth')
painted_finish(fabric,(.47,.50,.44),'cloth')
painted_finish(darkcloth,(.050,.067,.061),'rubber and woven webbing')
painted_finish(steel,(.115,.15,.145),'matte hardware')
painted_finish(edge,(.35,.38,.32),'worn exposed metal')
panel=material('cool reinforced fabric panel',(.27,.34,.31),True)
painted_finish(panel,(.28,.35,.33),'cloth')
seam=material('stitched seam shadow',(.13,.18,.16))
painted_finish(seam,(.16,.22,.19),'cloth')
hem=material('pale sewn seam catchlight',(.57,.59,.50))
painted_finish(hem,(.60,.63,.53),'cloth')
scuff=material('enamel restrained contact wear',(.33,.35,.28))
painted_finish(scuff,(.35,.37,.30),'matte wear')

# The garment has large, purposeful pattern pieces: darker inner/outer trouser
# reinforcement and flexible elbow/knee compression panels, with a pale front.
body.data.materials.append(panel);panel_index=len(body.data.materials)-1
body.data.materials.append(seam);seam_index=len(body.data.materials)-1
for face in body.data.polygons:
    p=sum((body.data.vertices[i].co for i in face.vertices),Vector())/len(face.vertices)
    x,y,z=p
    if .19<z<.91:
        legx=.115 if x>0 else -.115
        if y>.032 or abs(x-legx)>.077:face.material_index=panel_index
        if .445<z<.565 and y<-.035:face.material_index=panel_index
    if abs(x)>.205 and 1.145<z<1.275 and y<-.035:face.material_index=panel_index
    # Small rib-cage/underarm panel, rather than a dark ring severing the torso.
    if 1.08<z<1.40 and abs(x)>.165 and y>.015:face.material_index=panel_index

# Shallow, broad compression folds change the native textile silhouette and its
# painted light planes. They concentrate above cuffs, hip creases and knee darts.
for vertex in body.data.vertices:
    x,y,z=vertex.co
    if .55<z<.93 and y<-.025:
        strength=.008*math.exp(-((z-.845)/.060)**2)+.006*math.exp(-((z-.61)/.045)**2)
        vertex.co.y+=strength*math.sin(z*83+abs(x)*15)
    if .17<z<.28:
        vertex.co.y+=.005*math.sin(z*104)*math.exp(-((z-.215)/.04)**2)
body.data.update()

# One thin garment silhouette joins the painted shell's selected contour edges.
# It is not a stack of outlines around every limb component.
contour=body.copy();contour.data=body.data.copy();contour.name='continuous garment selected silhouette'
bpy.context.collection.objects.link(contour);contour.data.materials.clear();contour.data.materials.append(ink)
for vertex in contour.data.vertices:vertex.co+=vertex.normal*.0085
for face in contour.data.polygons:face.material_index=0;face.flip()

def fitted_line(name,approximate,radius,mat,bone):
    points=[]
    for point in approximate:
        hit,co,normal,index=body.closest_point_on_mesh(Vector(point))
        if hit:points.append(tuple(co+normal*.0035))
    if len(points)>1:tube(name,points,radius,mat,bone)

for side,sign in [('L',1),('R',-1)]:
    x=sign*.115
    # Double-felled seams, knee darts and a broad actual cloth knee overlap.
    fitted_line('trouser outside felled seam '+side,[(x+sign*.080,-.025,z) for z in [.88,.82,.74,.66,.60]],.0045,seam,'thigh.'+side)
    fitted_line('trouser front pressed seam '+side,[(x,-.14,z) for z in [.88,.81,.74,.66,.59]],.0025,hem,'thigh.'+side)
    fitted_line('knee upper sewn dart '+side,[(x+dx,-.10,.572-abs(dx)*.12) for dx in [-.060,-.03,0,.03,.060]],.004,hem,'thigh.'+side)
    fitted_line('shin outside seam '+side,[(x+sign*.060,-.02,z) for z in [.42,.34,.25,.18]],.004,seam,'shin.'+side)
    fitted_line('knee lower sewn dart '+side,[(x+dx,-.10,.438+abs(dx)*.10) for dx in [-.054,-.03,0,.03,.054]],.004,hem,'shin.'+side)
    # Compact practical knee pads read as overlapping fabric, not exposed robot joints.
    plate('soft knee reinforcement '+side,[(x-.046,-.084,.465),(x-.058,-.089,.508),(x-.038,-.092,.544),(x+.041,-.092,.544),(x+.057,-.089,.508),(x+.045,-.084,.465)],.007,panel,'shin.'+side)
    tube('kneepad folded upper hem '+side,[(x-.038,-.101,.540),(x,-.109,.547),(x+.041,-.101,.540)],.004,hem,'shin.'+side)
    # Purposeful wide cuff folds above the boot, rather than noise on every surface.
    fitted_line('boot gaiter crease '+side,[(x+dx,-.10,.19) for dx in [-.044,-.022,0,.022,.044]],.005,seam,'shin.'+side)
    # A few bent elbow seams; short lines break the smooth inflated sleeve.
    sx=sign*.245
    fitted_line('upper sleeve tailored outer seam '+side,[(sx+sign*.062,-.035,z) for z in [1.43,1.36,1.29]],.004,seam,'upperarm.'+side)
    fitted_line('elbow reinforcement dart '+side,[(sx+dx,-.105,1.267-abs(dx)*.1) for dx in [-.045,-.02,0,.02,.045]],.004,hem,'upperarm.'+side)
    fitted_line('forearm outside seam '+side,[(sign*.26+sign*.052,-.065,z) for z in [1.155,1.10,1.04]],.0035,seam,'forearm.'+side)
    # Contact wear is limited to the actual knee lip, boot toe and latch edge.
    plate('worn kneepad contact corner '+side,[(x+.025,-.101,.52),(x+.045,-.101,.514),(x+.043,-.101,.506)],.001,scuff,'shin.'+side)

# Chest fabric construction appears beneath the load-bearing reinforcement.
tube('chest reinforcement lower bound seam',[(-.142,-.146,1.17),(-.08,-.172,1.16),(0,-.181,1.156),(.08,-.172,1.16),(.142,-.146,1.17)],.005,seam,'torso')
for sign in [-1,1]:
    tube('abdomen cloth compression '+str(sign),[(sign*.045,-.163,1.112),(sign*.092,-.15,1.103),(sign*.145,-.123,1.114)],.004,hem,'torso')
    plate('chest shell restrained worn corner '+str(sign),[(sign*.11,-.171,1.205),(sign*.13,-.159,1.222),(sign*.142,-.15,1.218),(sign*.12,-.166,1.20)],.001,scuff,'torso')
# A real asymmetric stitched shoulder work patch and narrow reflective identifier.
box('left shoulder faded workplace patch',(.288,-.046,1.398),(.039,.012,.041),panel,'upperarm.L',.003)
box('workplace patch worn ivory marking',(.288,-.054,1.400),(.017,.004,.009),hem,'upperarm.L',.001)
tube('helmet crown seam',[(0,.14,1.73),(0,.12,1.80),(0,.06,1.854),(0,-.015,1.865),(0,-.073,1.835)],.0035,seam,'head')
tube('helmet selective brow abrasion',[(-.075,-.111,1.801),(-.050,-.127,1.810),(-.03,-.135,1.810)],.0025,hem,'head')

# Optional bounded rifle/heavy-armor sample. This equipment is authored in the
# same rest-hand space and remains rigidly attached through every pose.
rifle_start=len(objects)
rx=-.26
box('rifle compact receiver',(rx,-.185,.942),(.059,.185,.068),steel,'hand.R',.006)
box('rifle narrow upper',(rx,-.21,.983),(.046,.224,.020),edge,'hand.R',.003)
box('rifle padded stock',(rx,-.025,.942),(.051,.125,.055),darkcloth,'hand.R',.006)
box('rifle cheek stock top',(rx,-.020,.967),(.041,.10,.014),panel,'hand.R',.003)
box('rifle wrapped fore-end',(rx,-.315,.945),(.055,.108,.052),darkcloth,'hand.R',.004)
box('rifle short barrel',(rx,-.407,.954),(.022,.10,.025),steel,'hand.R',.003)
box('rifle recessed muzzle',(rx,-.462,.954),(.029,.017,.031),ink,'hand.R',.002)
box('rifle grip',(rx,-.127,.902),(.037,.041,.071),darkcloth,'hand.R',.004)
box('rifle seated magazine',(rx,-.217,.895),(.034,.044,.074),panel,'hand.R',.003)
box('rifle amber chamber witness',(rx+.031,-.158,.945),(.004,.025,.008),amber,'hand.R',.001)
rifle_objects=objects[rifle_start:]
for ob in rifle_objects:
    for vertex in ob.data.vertices:vertex.co=grip+rot@(vertex.co-grip)
    ob.hide_render=True
rifle_flash=uv('rifle release-only muzzle spark',(rx,-.492,.954),(.020,.038,.020),flash_material,'hand.R')
for vertex in rifle_flash.data.vertices:vertex.co=grip+rot@(vertex.co-grip)
rifle_flash.hide_render=True
heavy_start=len(objects)
plate('heavy fitted chest over-vest',[(-.16,-.172,1.18),(-.172,-.173,1.39),(-.125,-.153,1.45),(.125,-.153,1.45),(.172,-.173,1.39),(.16,-.172,1.18)],.025,panel,'torso')
for sign in [-1,1]:
    tube('heavy chest bound edge '+str(sign),[(sign*.125,-.18,1.45),(sign*.17,-.20,1.39),(sign*.16,-.20,1.18)],.008,darkcloth,'torso')
    plate('heavy shoulder load plate '+str(sign),[(sign*.17,-.065,1.48),(sign*.26,-.077,1.50),(sign*.31,-.068,1.42),(sign*.26,-.082,1.37),(sign*.19,-.085,1.40)],.025,ivory,'upperarm.L' if sign>0 else 'upperarm.R')
    box('heavy vest equipment pouch '+str(sign),(sign*.085,-.212,1.25),(.115,.040,.10),darkcloth,'torso',.008)
heavy_objects=objects[heavy_start:]
for ob in heavy_objects:ob.hide_render=True

scene['benchmarkConstruction']='V4 broad cloth panels, sewn seams, selective wear, material-specific painted values'
