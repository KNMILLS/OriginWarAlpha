"""Targeted export/provenance validation. This cannot grant artistic acceptance."""
from build import ROOT, ASSETS, ENV, PAINTED, sha, text_sha, environment, dump
from build_painted import build
from PIL import Image
import json

def snapshot():
    return {str(p.relative_to(ROOT)):sha(p) for base in [ENV,PAINTED] for p in base.rglob('*') if p.is_file() and p.name!='validation.json'}

def check():
    counts={};hashes=set()
    for base in [ENV,PAINTED]:
        data=json.loads((base/'regions.json').read_text());names=[]
        for r in data['regions']:
            names.append(r['name']);p=ASSETS/r['path'];im=Image.open(p).convert('RGBA')
            assert p.is_file() and sha(p)==r['sha256'],r['name']
            assert abs(im.width/256-r['widthCells'])<1e-9 and abs(im.height/256-r['heightCells'])<1e-9,r['name']
            assert 0<=r['pivotX']<=1 and 0<=r['pivotY']<=1,r['name']
            a=im.getchannel('A')
            if r.get('layer')=='ground':assert a.getextrema()==(255,255)
            else:assert a.getextrema()[0]==0 and a.getextrema()[1]>100
            if base==PAINTED:hashes.add(sha(p))
        assert len(names)==len(set(names))
        counts[base.name]=len(names)
        gen=json.loads((base/'source/GENERATION.json').read_text())
        for source in gen['sources']:
            assert sha(base/'source'/source.get('file',source.get('source')))==source['sha256']
            assert text_sha(base/'source'/source['prompt'])==source['promptSha256NormalizedLF']
        if 'supersededAliasRecord' in gen:
            alias=gen['supersededAliasRecord'];assert text_sha(base/alias['path'])==alias['sha256NormalizedLF']
    clips=json.loads((PAINTED/'clips.json').read_text());byid={c['id']:c for c in clips['clips']}
    assert len(byid)==24 and len(clips['bindings'])==24
    for binding in clips['bindings']:
        c=byid[binding['clip']]
        assert c['frames']
        for m in c['markers']:assert 0<=m['frame']<len(c['frames'])
        if binding['action']=='walk':
            assert abs(len(c['frames'])*c['frameSeconds']-.5)<1e-9
            assert c['cycleDistanceCells']==1
            assert len(c['frames'])==(4 if binding['facing']=='south' else 12)
        if binding['armor']=='heavy':assert binding['action']=='idle'
        if binding['weapon']=='rifle':assert binding['action']=='idle'
        for frame in c['frames']:
            if 'baselinePath' in frame:assert sha(ASSETS/frame['path'])==sha(ASSETS/frame['baselinePath'])
    aperture=Image.open(ENV/'regions/door.open.png').getchannel('A')
    assert aperture.getpixel((aperture.width//2,aperture.height//2))==0
    return dict(status='PASS',scope='exports and provenance only',regionBindings=counts,clips=len(byid),uniquePaintedExportHashes=len(hashes),newSelectedDrawings=8,newSouthGaitDrawings=4,commercialArtisticAcceptance='OPEN',nativeRuntime='lead-owned; not tested by exporter')

if __name__=='__main__':
    environment();build();first=snapshot();initial=check()
    environment();build();second=snapshot();assert first==second,'Export bytes changed on repeated build'
    result=check();result['repeatability']='PASS: exact export/source manifest/catalog/review bytes on two builds'
    dump(PAINTED/'validation.json',result)
    print(json.dumps(result,indent=2))
