"""Continuous pressure garment fitted from Dan Ulrich's CC0 realistic body topology.

This replaces the old loft/capsule construction. The immutable anatomical source is
kept separately; smoothing, inflation, fitting, cloth folds and rig weights are derived work.
"""
import bmesh
def garment_smooth(t):
    t=max(0,min(1,t));return t*t*(3-2*t)
# Preserve head, helmet, equipment and hardware; replace the entire cloth/shell limb build.
replaced=('continuous tailored torso','continuous seat and hip garment','single curved ribcage shell','chest bottom selective contour','abdomen overlapping flexible enamel','continuous trouser','thin thigh practical','trouser side seam','knee tailored flex','thin shaped knee','tapered shin enamel','thin rubber outsole','tailored armored boot','boot shallow toe','boot fitted ankle','continuous pressure sleeve','shallow curved deltoid','elbow sleeve fold','narrow bracer shell')
for ob in scene.objects:
    if ob.name.startswith(replaced):ob.hide_render=True
with bpy.data.libraries.load(str(SRC/'anatomy/realistic-body-male-cc0.blend'),link=False) as (source,target):target.objects=['GEO-body_male_realistic']
body=target.objects[0];bpy.context.collection.objects.link(body)
body.parent=None;body.matrix_world=Matrix.Identity(4)
for modifier in list(body.modifiers):body.modifiers.remove(modifier)
# The source's local coordinates already center the actor; remove unused head/hand tips.
bm=bmesh.new();bm.from_mesh(body.data)
bmesh.ops.delete(bm,geom=[v for v in bm.verts if v.co.z>1.425 or (abs(v.co.x)>.29 and v.co.z<.90)],context='VERTS')
bm.to_mesh(body.data);bm.free();body.data.update()
source_bones={'pelvis':((0,0,.85),(0,0,.97)),'torso':((0,0,.97),(0,0,1.38))}
for side,sign in [('L',1),('R',-1)]:
    source_bones['thigh.'+side]=((sign*.09,0,.90),(sign*.14,.012,.465))
    source_bones['shin.'+side]=((sign*.14,.012,.465),(sign*.172,.055,.09))
    source_bones['foot.'+side]=((sign*.172,.055,.09),(sign*.20,-.12,.05))
    source_bones['upperarm.'+side]=((sign*.195,0,1.35),(sign*.285,0,1.105))
    source_bones['forearm.'+side]=((sign*.285,0,1.105),(sign*.37,-.05,.90))
    source_bones['hand.'+side]=((sign*.37,-.05,.90),(sign*.40,-.10,.80))
fitted=[];weights=[]
for vertex in body.data.vertices:
    p=vertex.co.copy();candidates=[]
    if p.z<.88:candidate_names=['pelvis','thigh.L' if p.x>0 else 'thigh.R','shin.L' if p.x>0 else 'shin.R','foot.L' if p.x>0 else 'foot.R']
    else:
        side='L' if p.x>0 else 'R'
        threshold=.175+.16*max(0,1.35-p.z)
        candidate_names=['torso','pelvis'] if abs(p.x)<threshold else ['upperarm.'+side,'forearm.'+side,'hand.'+side,'torso']
    for name in candidate_names:
        a,b=map(Vector,source_bones[name]);d=b-a;t=max(0,min(1,(p-a).dot(d)/d.length_squared));nearest=a+d*t;distance=(p-nearest).length
        candidates.append((distance,name,t,p-nearest,d))
    candidates.sort(key=lambda item:item[0]);candidates=candidates[:2]
    # Smooth shared topology keeps joint transitions closed instead of exposing a band.
    values=[math.exp(-((c[0]-candidates[0][0])/.048)**2) for c in candidates];total=sum(values)
    point=Vector((0,0,0));w={}
    for value,(_,name,t,perp,direction) in zip(values,candidates):
        fraction=value/total;a,b=map(Vector,rests[name]);target_direction=b-a
        rotate=direction.rotation_difference(target_direction)
        point+=(a+target_direction*t+rotate@perp)*fraction;w[name]=fraction
    fitted.append(point);weights.append(w)
for v,p in zip(body.data.vertices,fitted):v.co=p
# Smooth scan-specific anatomy into a garment envelope, then add actual cloth thickness.
bpy.context.view_layer.objects.active=body;bpy.ops.object.select_all(action='DESELECT');body.select_set(True)
soft=body.modifiers.new('garment envelope removes scan anatomy','SMOOTH');soft.factor=.5;soft.iterations=38
bpy.ops.object.modifier_apply(modifier=soft.name);body.select_set(False);body.data.update()
for vertex_index,v in enumerate(body.data.vertices):
    v.co+=v.normal*.021
    # Pressure cloth bridges the anatomical chest/abdomen instead of printing scan muscles.
    if 1.075<v.co.z<1.465 and abs(v.co.x)<.195 and v.co.y<-.04:
        ry=.158 if v.co.z>1.22 else .146
        envelope=-ry*math.sqrt(max(0,1-(v.co.x/.215)**2))
        blend=garment_smooth((v.co.z-1.075)/.06)*garment_smooth((1.465-v.co.z)/.06)
        v.co.y=min(v.co.y,v.co.y+(envelope-v.co.y)*blend)
    # Broad cloth compression around knee/elbow, carried by one closed surface.
    z=v.co.z;fold=.0045*math.sin((z-.49)*95)*math.exp(-((z-.49)/.075)**2)
    fold+=.0035*math.sin((z-1.20)*100)*math.exp(-((z-1.20)/.06)**2)
    radial=Vector((v.co.x-(.115 if v.co.x>0 else -.115),v.co.y,0))
    if radial.length>0:v.co+=radial.normalized()*fold
    if v.co.z<.155:
        # The boot below the ankle is rigid to its actual foot, with a real flat sole.
        # This keeps the contact marker on the same rigid surface as the exported pixels.
        weights[vertex_index]={'foot.L' if v.co.x>0 else 'foot.R':1}
        v.co.z=max(.012,v.co.z)
body.data.materials.clear();garment=material('continuous warm ivory pressure garment',(.53,.485,.365),True)
finish(body,'CC0 anatomical topology / continuous fitted pressure garment',garment,weights=weights)
body.data.materials.append(darkcloth);body.data.materials.append(steel)
for face in body.data.polygons:
    center=sum((body.data.vertices[i].co for i in face.vertices),Vector())/len(face.vertices)
    if center.z<.11:face.material_index=2
    elif center.z<.17 or (abs(center.x)>.20 and center.z<.985):face.material_index=1
# Fitted reinforcement follows the continuous underlying chest and shoulder curvature.
for label,condition,bone in [
 ('fitted chest reinforcement',lambda p:1.15<p.z<1.46 and abs(p.x)<.176 and p.y<-.063,'torso'),
 ('fitted shoulder reinforcement L',lambda p:1.36<p.z<1.49 and p.x>.18 and p.y<.055,'upperarm.L'),
 ('fitted shoulder reinforcement R',lambda p:1.36<p.z<1.49 and p.x<-.18 and p.y<.055,'upperarm.R')]:
    selected=[]
    for face in body.data.polygons:
        center=sum((body.data.vertices[i].co for i in face.vertices),Vector())/len(face.vertices)
        if condition(center):selected.append(face)
    ids=sorted({i for face in selected for i in face.vertices});mapping={index:j for j,index in enumerate(ids)}
    verts=[body.data.vertices[i].co+body.data.vertices[i].normal*.009 for i in ids]
    faces=[tuple(mapping[i] for i in face.vertices) for face in selected]
    mesh=bpy.data.meshes.new(label);mesh.from_pydata(verts,[],faces);mesh.update();ob=bpy.data.objects.new(label,mesh);bpy.context.collection.objects.link(ob)
    finish(ob,label,ivory,weights=[weights[i] for i in ids])
    solid=ob.modifiers.new('fitted reinforcement edge thickness','SOLIDIFY');solid.thickness=.007
# Thin practical outer sole, shaped around the anatomical heel and toe rather than a flat ski.
for side,sign in [('L',1),('R',-1)]:
    x=sign*.115
    outline=[(x-.054,.055,.018),(x+.048,.055,.018),(x+.061,-.083,.018),(x+.035,-.158,.018),(x-.047,-.15,.018),(x-.058,-.072,.018),(x-.054,.055,.018)]
    tube('anatomical boot welt '+side,outline,.008,darkcloth,'foot.'+side)
