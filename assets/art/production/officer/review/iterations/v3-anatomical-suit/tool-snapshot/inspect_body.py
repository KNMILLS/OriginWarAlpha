"""Read-only anatomical body inventory from the immutable official CC0 bundle."""
import bpy,json
from pathlib import Path
from mathutils import Vector
source=Path('C:/OriginWarReboot/.tools/human-base-meshes/human-base-meshes-bundle-v1.4.1/human_base_meshes_bundle.blend')
bpy.ops.wm.open_mainfile(filepath=str(source))
rows=[]
for c in bpy.data.collections:
    if 'body' not in c.name.lower():continue
    rows.append({'collection':c.name,'properties':dict(c.items()),'objects':[{'name':o.name,'type':o.type,'vertices':len(o.data.vertices) if o.type=='MESH' else 0,'bounds':[[min((o.matrix_world@Vector(corner))[a] for corner in o.bound_box) for a in range(3)],[max((o.matrix_world@Vector(corner))[a] for corner in o.bound_box) for a in range(3)]],'properties':dict(o.items())} for o in c.all_objects]})
out=Path(__file__).resolve().parents[3]/'assets/art/production/officer/review/iterations/body-source-inspection.json'
out.write_text(json.dumps(rows,indent=2,default=str)+'\n',newline='\n');print(json.dumps(rows,indent=2,default=str))
