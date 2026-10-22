"""Finalize the bounded V4 source candidate after validation; no art approval."""
from pathlib import Path
import json,hashlib,subprocess,sys
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/officer';SRC=OUT/'source';TOOLS=Path(__file__).parent
def sha(p):return hashlib.sha256(p.read_bytes().replace(b'\r\n',b'\n') if p.suffix in ['.py','.md'] else p.read_bytes()).hexdigest()
for flags in [[],['--rifle']]:
    subprocess.run([sys.executable,str(TOOLS/'validate_benchmark.py'),'--canonical-neutral',*flags],check=True)
primary=json.loads((OUT/'clips.json').read_text());sample=json.loads((OUT/'samples/rifle-heavy/clips.json').read_text())
head=SRC/'anatomy/realistic-head-animation-cc0.blend';body=SRC/'anatomy/realistic-body-male-cc0.blend'
assert sha(head)=='ea1a71ded363d4ae32248e914ea9e9c23e04a834692bdf85b4eedbb70abe6cff'
body_receipt=json.loads((SRC/'anatomy/body-only-extraction.json').read_text());assert sha(body)==body_receipt['sha256']
assert body_receipt['license']=='CC0' and body_receipt['author']=='Dan Ulrich'
previous=json.loads((SRC/'PROVENANCE.json').read_text())
source_files=[SRC/'officer-illustrated-pilot.blend',OUT/'samples/rifle-heavy/officer-illustrated-pilot.blend',head,body,*TOOLS.glob('*.py')]
provenance={
    'task':'BM-OFFICER','integrationBase':'c3ad57376f083b5a05f3d81b11764f857c502bec',
    'status':'Bounded V4 development candidate; native visual/motion, owner and experienced animator acceptance OPEN',
    'tool':previous['tool'],'anatomy':previous['anatomy'],'anatomicalBody':body_receipt,
    'construction':['Continuous derived CC0 pressure garment retains pale suit and adult proportions','Broad cool textile panels separate from warm restrained rigid shell and graphite hardware','Native modeled sewn darts, hems, cuff/hip folds, selected silhouette and limited high-contact wear','Material-node broad painted values and actual overlap shadows; no per-frame pixel filtering'],
    'motion':['54pose0.75cell/0.9second cycle;1.2seconds/cell unchanged','Actual heel-flat-toe boot roll about support contact patches','Double-support compression, passing rise, lateral pelvis transfer, damped spine/head and delayed equipment-weighted arms','Exact walk phase0/1/3/2/3 stop endpoints','Specific terminal/breaker/floor pickup/drop/supply/equip with projected wrist/palm/fingertip anchors','Optional four-facing rifle/heavy sample in separate manifest'],
    'primary':{'clips':len(primary['clips']),'frameExposures':sum(len(c['frames']) for c in primary['clips'])},
    'rifleHeavySample':{'manifest':'assets/art/production/officer/samples/rifle-heavy/clips.json','clips':len(sample['clips']),'frameExposures':sum(len(c['frames']) for c in sample['clips'])},
    'raster':{'editableMasterReferenceCellPixels':256,'editableMasterCanvas':[384,512],'runtimeReferenceCellPixels':192,'runtimeCanvas':[288,384],'normalLogicalBounds':[1.5,2.0],'deathMasterCanvas':[1024,1024],'deathRuntimeCanvas':[768,768],'deathLogicalBounds':[4,4],'method':'Direct75% Blender render; larger transparent stride bounds preserve exact density and opaque height'},
    'preservation':previous.get('preservation',{}),
    'v3Preservation':{'baseCommit':'c3ad57376f083b5a05f3d81b11764f857c502bec','snapshot':'assets/art/production/officer/review/iterations/v3-before-benchmark','scope':'V3 source/manifest/idles/checks preserved; remaining raw frames in exact Git base'},
    'failuresPreserved':['review/iterations/v4-motion-low-contact-rejected','review/iterations/v4-stride-bounds-rejected'],
    'purchaseSpend':0,'externalSpecialistReview':'NOT_PERFORMED','commercialUse':'Original project work plus verified CC0 anatomy; no new externally sourced asset',
    'sourceHashPolicy':'Binary exact bytes; Python canonicalLF','sources':{p.relative_to(ROOT).as_posix():sha(p) for p in source_files},
    'limits':['Source validation is not native gameplay acceptance','Human acting and commercial illustration finish remain open','No experienced human animator assessment has been obtained','Texture allocation and active-equipment loading remain lead-owned','Rifle/heavy is a bounded locomotion/fire sample; campaign equipment coverage is not supplied','No purchase, external message, campaign expansion or publication']}
(SRC/'PROVENANCE.json').write_text(json.dumps(provenance,indent=2)+'\n',newline='\n')
paths=set()
for manifest in [primary,sample]:
    for c in manifest['clips']:
        for f in c['frames']:
            paths.add(ROOT/'assets'/f['path'])
            if 'authoredSourceRender' in f:paths.add(ROOT/'assets'/f['authoredSourceRender']['path'])
paths.update(p for p in OUT.rglob('*') if p.is_file() and 'atlases' not in p.relative_to(OUT).parts and p.name not in ['atlas.json','coverage.json','DELIVERY_FILES.json'] and not p.name.endswith(('.blend1','.log')) and p.parent!=OUT/'regions')
paths.update(TOOLS.glob('*.py'));paths.add(ROOT/'docs/design/art/OFFICER_PRODUCTION.md')
(SRC/'DELIVERY_FILES.json').write_text(json.dumps({'scope':'Source candidate paths; shared atlas/coverage remain lead-owned','paths':sorted(p.relative_to(ROOT).as_posix() for p in paths)},indent=2)+'\n',newline='\n')
print('PASS BM-OFFICER source/anatomy preservation and delivery list; artistic gates OPEN')
