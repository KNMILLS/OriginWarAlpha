"""Fit an exact selected CC0 adult head inside an original open pressure helmet.

Dan Ulrich, Head (Animation) - Realistic, official Blender Human Base Meshes1.4.1.
No unrelated bundle objects, rig, material or license text is imported.
"""
import bmesh
path=SRC/'anatomy/realistic-head-animation-cc0.blend'
names=['GEO-head_animation_realistic','GEO-head_animation_realistic.sclera.L','GEO-head_animation_realistic.sclera.R','GEO-head_animation_realistic.iris.L','GEO-head_animation_realistic.iris.R']
with bpy.data.libraries.load(str(path),link=False) as (source,target):target.objects=names
for ob in target.objects:bpy.context.collection.objects.link(ob)
bpy.context.view_layer.update()
main=next(o for o in target.objects if o.name=='GEO-head_animation_realistic');source_origin=main.matrix_world.translation.copy()
destination=Vector((0,-.03,1.501));scale=.85
sclera=material('subdued anatomical sclera',(.38,.38,.32))
# Snapshot every source transform before detaching the parented eye objects.
# Applying a new head transform first otherwise moves its still-parented eyes twice.
bpy.ops.object.select_all(action='DESELECT');bpy.context.view_layer.objects.active=main;main.select_set(True)
for modifier in list(main.modifiers):
    if modifier.type=='MULTIRES':
        modifier.levels=1;modifier.render_levels=1;bpy.ops.object.modifier_apply(modifier=modifier.name)
main.select_set(False);bpy.context.view_layer.update()
source_matrices={ob:ob.matrix_world.copy() for ob in target.objects}
source_vertices={ob:[destination+(source_matrices[ob]@v.co-source_origin)*scale for v in ob.data.vertices] for ob in target.objects}
for ob in target.objects:ob.parent=None;ob.matrix_world=Matrix.Identity(4)
for ob in target.objects:
    for vertex,co in zip(ob.data.vertices,source_vertices[ob]):vertex.co=co
    mat=eye if '.iris.' in ob.name else sclera if '.sclera.' in ob.name else skin
    finish(ob,ob.name+' / CC0 Dan Ulrich',mat,'head')

# Restrained brows follow the actual forehead surface; no painted mask or enlarged eyes.
for sign in [-1,1]:
    points=[]
    for x,z in [(.016,1.753),(.033,1.762),(.051,1.758)]:
        hit,co,normal,index=main.closest_point_on_mesh(Vector((sign*x,-.3,z)))
        if hit:points.append(tuple(co+normal*.0011))
    if len(points)==3:tube('surface-following guarded brow',points,.0025,skinShadow,'head')

# A real open shell and dark U-shaped enclosure, rather than a black front faceplate.
helmet=uv('open practical ivory pressure helmet',(0,.01,1.71),(.145,.14,.156),ivory,'head',False)
bm=bmesh.new();bm.from_mesh(helmet.data)
remove=[]
for face in bm.faces:
    center=face.calc_center_median()
    if center.y<-.046 and 1.565<center.z<1.793:remove.append(face)
bmesh.ops.delete(bm,geom=remove,context='FACES');bm.to_mesh(helmet.data);bm.free();helmet.data.update()
outline_objects.append(helmet)
darkU=[(-.104,-.073,1.787),(-.108,-.09,1.70),(-.081,-.12,1.625),(0,-.131,1.586),(.081,-.12,1.625),(.108,-.09,1.70),(.104,-.073,1.787)]
ivoryU=[(-.116,-.063,1.787),(-.119,-.084,1.70),(-.092,-.117,1.612),(0,-.129,1.572),(.092,-.117,1.612),(.119,-.084,1.70),(.116,-.063,1.787)]
tube('dark U visor surround',darkU,.018,darkcloth,'head')
tube('ivory helmet cheek and chin guard',ivoryU,.012,ivory,'head')
tube('curved visor brow seal',[(-.104,-.073,1.787),(-.055,-.116,1.79),(0,-.128,1.79),(.055,-.116,1.79),(.104,-.073,1.787)],.013,darkcloth,'head')
