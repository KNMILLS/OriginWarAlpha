"""Extract only a CC0 anatomical body collection; immutable upstream remains untouched."""
from pathlib import Path
import bpy,json,hashlib
import numpy as np
# Process-local guard: extraction must not rotate pre-existing untracked backups.
bpy.context.preferences.filepaths.save_version=0
root=Path(__file__).resolve().parents[3];out=root/'assets/art/production/officer/source/anatomy'
source=Path('C:/OriginWarReboot/.tools/human-base-meshes/human-base-meshes-bundle-v1.4.1/human_base_meshes_bundle.blend')
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
with bpy.data.libraries.load(str(source),link=False) as (data,target):target.collections=['Body Male - Realistic']
c=target.collections[0];bpy.context.scene.collection.children.link(c)
asset=c.asset_data;assert asset and asset.license=='CC0'
rows=[]
for o in c.all_objects:
    assert o.type=='MESH' and o.library is None
    rows.append({'name':o.name,'vertices':len(o.data.vertices),'location':list(o.location),'rotation':list(o.rotation_euler),'scale':list(o.scale),'parent':o.parent.name if o.parent else None,'modifiers':[(m.name,m.type) for m in o.modifiers]})
destination=out/'realistic-body-male-cc0.blend'
bpy.data.libraries.write(str(destination),{c},fake_user=True,compress=True)
receipt={'sourceOfficialArchive':'https://download.blender.org/demo/asset-bundles/human-base-meshes/human-base-meshes-bundle-v1.4.1.zip','sourcePage':'https://www.blender.org/download/demo-files/','collection':c.name,'author':asset.author,'description':asset.description,'license':asset.license,'copyright':asset.copyright,'sha256':hashlib.sha256(destination.read_bytes()).hexdigest(),'objects':rows,'scope':'Only selected collection; no unrelated rig or license text imported. Unchanged geometry, transforms, materials and modifiers.'}
(out/'body-only-extraction.json').write_text(json.dumps(receipt,indent=2)+'\n',newline='\n');print(json.dumps(receipt,indent=2))
main=next(o for o in c.all_objects if o.name=='GEO-body_male_realistic')
vertices=np.array([tuple(v.co) for v in main.data.vertices]);sections=[]
for z in np.arange(.05,1.451,.05):
    for label,xlimit in [('all positive',0),('outer arm',.20)]:
        points=vertices[(abs(vertices[:,2]-z)<.016)&(vertices[:,0]>xlimit)]
        if len(points):sections.append({'z':round(float(z),3),'section':label,'count':len(points),'min':points.min(0).tolist(),'max':points.max(0).tolist(),'mean':points.mean(0).tolist()})
(out/'body-sections.json').write_text(json.dumps(sections,indent=2)+'\n',newline='\n')
