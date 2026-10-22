"""Read-only inspection of the exact selected upstream CC0 anatomy objects."""
import bpy,json
from mathutils import Vector
path=r'C:\OriginWarReboot\.tools\human-base-meshes\human-base-meshes-bundle-v1.4.1\human_base_meshes_bundle.blend'
names=['GEO-head_animation_realistic','GEO-head_animation_realistic.sclera.L','GEO-head_animation_realistic.sclera.R','GEO-head_animation_realistic.iris.L','GEO-head_animation_realistic.iris.R']
with bpy.data.libraries.load(path,link=False) as (source,target):target.objects=names
for ob in target.objects:
    bpy.context.collection.objects.link(ob);bounds=[ob.matrix_world@Vector(v) for v in ob.bound_box]
    print('SELECTED_ANATOMY',json.dumps({'name':ob.name,'min':[min(p[i] for p in bounds) for i in range(3)],'max':[max(p[i] for p in bounds) for i in range(3)],'location':list(ob.location),'rotation':list(ob.rotation_euler),'scale':list(ob.scale),'vertices':len(ob.data.vertices),'materials':[m.name for m in ob.data.materials],'modifiers':[(m.name,m.type) for m in ob.modifiers]}))
