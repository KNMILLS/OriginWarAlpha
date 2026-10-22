"""Verify raw authored pixels/contacts and optionally share exact neutral renders.

This verifies source exports, not native cadence, physical acceptance or acting.
It never edits PNGs. Original endpoint render paths/hashes remain in the manifest.
"""
from pathlib import Path
import json,hashlib,sys,ast
import numpy as np
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[3];OUT=ROOT/'assets/art/production/officer'
HERE=OUT/'samples/rifle-heavy' if '--rifle' in sys.argv else OUT
m=json.loads((HERE/'clips.json').read_text());byid={c['id']:c for c in m['clips']}
if '--pistol' in sys.argv:
    assert '--canonical-neutral' not in sys.argv,'Partial validation must never rewrite the full manifest'
    m['clips']=[c for c in m['clips'] if c['equipment']=='pistol']
    m['footCalibration']=[f for f in m['footCalibration'] if '.pistol.' in f['clip']]
    byid={c['id']:c for c in m['clips']}
def image(f):return Image.open(ROOT/'assets'/f['path']).convert('RGBA')
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
seen={};bounds=[]
for c in m['clips']:
    assert len(c['frames']) and len(c['markers'])>=0
    for f in c['frames']:
        path=ROOT/'assets'/f['path'];im=image(f);box=im.getbbox()
        assert digest(path)==f['sha256'],path
        if 'authoredSourceRender' in f:
            raw=f['authoredSourceRender'];assert digest(ROOT/'assets'/raw['path'])==raw['sha256'],raw['path']
        assert im.size==(f['pixelWidth'],f['pixelHeight'])
        assert box and box[0]>1 and box[1]>1 and box[2]<im.width-1 and box[3]<im.height-1,(c['id'],box)
        assert set(f['hands'])=={'left','right'}
        seen[f['path']]=f
    bounds.append({'clip':c['id'],'lastOpaqueBounds':list(image(c['frames'][-1]).getbbox())})
    if c['id'].split('.')[-2]=='walk':
        assert len(c['frames'])==54 and c['frameSeconds']==1/60 and c['cycleCells']==.75
        roles={f['feet']['left']['poseRole'] for f in c['frames']}
        assert roles=={'contact','compression','passing','push_off','swing'},roles

endpoints=[]
def check(c,index,target,label):
    authored=c['frames'][index]
    raw=authored.get('authoredSourceRender',authored)
    raw_frame=dict(authored,path=raw['path'],sha256=raw['sha256'])
    delta=np.abs(np.asarray(image(raw_frame)).astype(np.int16)-np.asarray(image(target)).astype(np.int16))
    maximum=int(delta.max());changed=int(np.any(delta,axis=2).sum())
    assert maximum<=2,(c['id'],index,label,maximum,changed)
    endpoints.append({'clip':c['id'],'frame':index,'expected':label,'authoredRawPath':raw['path'],'maximumChannelDifference':maximum,'changedPixels':changed})
    if '--canonical-neutral' in sys.argv and authored['path']!=target['path']:
        c['frames'][index]=dict(target,canonicalNeutral=True,authoredSourceRender={'path':authored['path'],'sha256':authored['sha256']})
for c in m['clips']:
    action=c['id'].split('.')[-2];facing=c['id'].split('.')[-1]
    prefix=f'officer.{c["equipment"]}.{c["armor"]}.'
    idle=byid[prefix+'idle.'+facing]['frames'][0]
    walk=byid.get(prefix+'walk.'+facing)
    if action=='start':
        check(c,0,idle,'idle');check(c,-1,walk['frames'][0],'walk phase0')
    elif action=='stop' or action.startswith('stop_phase_'):
        phase=int(action[-1]) if action.startswith('stop_phase_') else 0
        check(c,0,walk['frames'][phase*18],f'walk phase{phase}/3');check(c,-1,idle,'idle')
    elif action.startswith('turn_from_'):
        start=action.removeprefix('turn_from_');check(c,0,byid[prefix+'idle.'+start]['frames'][0],'source idle');check(c,-1,idle,'destination idle')
    elif action in ['work','terminal','breaker','pickup','drop','supply','equip','hit','melee','fire']:
        check(c,0,idle,'idle');check(c,-1,idle,'idle')

residency={}
for weapon in {c['equipment'] for c in m['clips']}:
    crops={};exposures=0
    for c in m['clips']:
        if c['equipment']!=weapon:continue
        exposures+=len(c['frames'])
        for f in c['frames']:
            im=image(f);box=im.getbbox();crop=im.crop(box)
            crops[(crop.size,hashlib.sha256(crop.tobytes()).hexdigest())]=(crop.width+4)*(crop.height+4)
    area=sum(crops.values());residency[weapon]={'exposures':exposures,'uniqueExactRGBACrops':len(crops),'deduplicatedPaddedRGBABytes':area*4,'minimum2048PagesByArea':(area+2048**2-1)//2048**2,'scope':'Exact-crop area lower bound; actual allocated packing is lead-owned'}

contacts=[]
for c in m['clips']:
    marker=next((e for e in c['markers'] if e['kind']=='work_contact'),None)
    if marker:
        f=c['frames'][marker['frame']]
        contacts.append({'clip':c['id'],'contactSeconds':marker['seconds'],'recoverySeconds':next(e['seconds'] for e in c['markers'] if e['kind']=='recovery'),'leftHand':f['hands']['left']})
        if c['id'].split('.')[-2] in ['pickup','drop']:
            assert .025<f['hands']['left']['fingerTip']['world'][2]<.10,(c['id'],'floor hand too far from small floor item',f['hands']['left'])
for metric in m['footCalibration']:assert metric['sampledStanceResidualPixels']<.001,metric
for path in Path(__file__).parent.glob('*.py'):ast.parse(path.read_text(),filename=str(path))
report={'scope':'Raw source sprites, rig contacts and source projection only; native/human/animator gates OPEN','artisticAcceptance':'OPEN','clips':len(m['clips']),'frameExposures':sum(len(c['frames']) for c in m['clips']),'rawReferencedPNGs':len(seen),'endpointChecks':endpoints,'bounds':bounds,'footCalibration':m['footCalibration'],'residencyByEquipment':residency,'interactionContacts':contacts,'canonicalSharing':'Only after original endpoint verified within2 channel values; original paths/hashes retained' if '--canonical-neutral' in sys.argv else 'not changed'}
(HERE/('review/v4-pistol-validation.json' if '--pistol' in sys.argv else 'validation.json')).write_text(json.dumps(report,indent=2)+'\n',newline='\n')
if '--canonical-neutral' in sys.argv:(HERE/'clips.json').write_text(json.dumps(m,indent=2)+'\n',newline='\n')
print(json.dumps({'status':'PASS','clips':report['clips'],'exposures':report['frameExposures'],'endpointChecks':len(endpoints),'residency':residency},indent=2))
