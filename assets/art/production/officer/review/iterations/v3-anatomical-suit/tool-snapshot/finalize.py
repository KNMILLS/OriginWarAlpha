"""Record exact original/CC0 authoring source and prepare bounded delivery file list."""
from pathlib import Path
import json,hashlib,time,datetime,ast
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/officer';SRC=OUT/'source';TOOLS=Path(__file__).parent
def sha(p):
    data=p.read_bytes()
    if p.suffix in ['.py','.md']:data=data.replace(b'\r\n',b'\n')
    return hashlib.sha256(data).hexdigest()
m=json.loads((OUT/'clips.json').read_text());v=json.loads((OUT/'validation.json').read_text())
assert len(m['clips'])==76 and sum(len(c['frames']) for c in m['clips'])==660
assert not any(c['equipment']=='unarmed' and '.fire.' in c['id'] for c in m['clips'])
assert max(e['maximumChannelDifference'] for e in v['endpointChecks'])==0
for p in TOOLS.glob('*.py'):ast.parse(p.read_text(),filename=str(p))
begin=(TOOLS/'previous_exporter_reference.py').stat().st_ctime
timing=json.loads((OUT/'timing.json').read_text());timing.update({'status':'Development candidate for native evaluation; artistic finish OPEN','boundedLookCorrections':4,'authoringReviewWallMinutes':round((time.time()-begin)/60,2),'wallTimeMeaning':'Elapsed since new tool subtree creation; includes source authoring, rendering, review and waits; not a billed labor claim'})
(OUT/'timing.json').write_text(json.dumps(timing,indent=2)+'\n',newline='\n')
head=SRC/'anatomy/realistic-head-animation-cc0.blend'
assert sha(head)=='ea1a71ded363d4ae32248e914ea9e9c23e04a834692bdf85b4eedbb70abe6cff'
sources=[SRC/'officer-illustrated-pilot.blend',head]+list(TOOLS.glob('*.py'))
provenance={'task':'OFFICER-PRODUCTION','integrationBase':'814253f','status':'Approved development candidate for native evaluation; commercial finish OPEN','tool':{'name':'Blender','version':'4.5.3 LTS','buildHash':'67807e1800cc','officialArchive':'https://download.blender.org/release/Blender4.5/blender-4.5.3-windows-x64.zip','archiveSha256':'6b657c8bdd3a7b65b07b9e1ae17eb4be7dd4aa23121da7f3d3354fc2551330a7','runtimeDependency':False},'anatomy':{'source':'https://www.blender.org/download/demo-files/','officialArchive':'https://download.blender.org/demo/asset-bundles/human-base-meshes/human-base-meshes-bundle-v1.4.1.zip','collection':'Head (Animation) - Realistic','author':'Dan Ulrich','license':'CC0','licenseUrl':'https://creativecommons.org/publicdomain/zero/1.0/','selectedObjects':['GEO-head_animation_realistic','GEO-head_animation_realistic.iris.L','GEO-head_animation_realistic.iris.R','GEO-head_animation_realistic.sclera.L','GEO-head_animation_realistic.sclera.R'],'productionChanges':['Multires evaluated','Uniform fit and parent-transform correction','Project materials and head-bone binding'],'unrelatedRainRigImported':False},'originalProjectWork':['Suit and practical shell geometry','Helmet and U-shaped visor enclosure','Equipment and rig','Authored motion rules and projected foot calibration','Cel materials, selected model-space edges and lighting'],'purchaseSpend':0,'sourceHashPolicy':'Binary files exact bytes; Python source canonicalLF','sources':{p.relative_to(ROOT).as_posix():sha(p) for p in sources},'raster':{'editableMasterReferenceCellPixels':256,'editableMasterCanvas':[384,448],'runtimeReferenceCellPixels':192,'runtimeCanvas':[288,336],'method':'Direct75% Blender render, no per-frame pixel filtering'},'timing':timing,'limits':['No native runtime evidence by worker','No commercial illustration or animation acceptance','No90/180degree turns','Death is slumped collapse, not full prone acting','Other equipment/armor and campaign art remain outside bounded opening coverage']}
(SRC/'PROVENANCE.json').write_text(json.dumps(provenance,indent=2)+'\n',newline='\n')
paths={ROOT/'assets'/f['path'] for c in m['clips'] for f in c['frames']}
paths.update(p for p in OUT.rglob('*') if p.is_file() and '/regions/' not in p.relative_to(OUT).as_posix() and p.parent!=OUT/'regions' and not p.name.endswith('.blend1'))
# Immutable rejected-iteration regions are evidence and included; orphan current trial PNGs are not.
paths.update(p for p in (OUT/'review/iterations').rglob('*') if p.is_file() and not p.name.endswith('.blend1'))
paths.update(TOOLS.glob('*.py'));paths.add(ROOT/'docs/design/art/OFFICER_PRODUCTION.md')
paths={p for p in paths if p.name!='DELIVERY_FILES.json'}
(SRC/'DELIVERY_FILES.json').write_text(json.dumps({'paths':sorted(p.relative_to(ROOT).as_posix() for p in paths)},indent=2)+'\n',newline='\n')
print('PASS exact source/license hashes,76clip/660frame contract,endpoint equality,source syntax; delivery paths',len(paths)+1)
