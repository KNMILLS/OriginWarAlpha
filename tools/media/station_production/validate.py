"""Check asset delivery truth, separate from visual/gameplay acceptance."""
from pathlib import Path
from PIL import Image
import hashlib, json

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT/'assets/art/production/station'
manifest = json.loads((OUT/'regions.json').read_text(encoding='utf-8'))
clips = json.loads((OUT/'clips.json').read_text(encoding='utf-8'))
assert manifest['referenceCellPixels'] == 192
assert manifest['editableReferenceCellPixels'] == 256
names = set()
hashes = {}
masters=[]
for region in manifest['regions']:
    name = region['name']
    assert name not in names, name
    names.add(name)
    path = ROOT/'assets'/region['path']
    assert path.resolve().is_relative_to((OUT/'regions').resolve()), path
    im = Image.open(path)
    assert im.mode == 'RGBA', path
    assert im.size == (region['pixelWidth'], region['pixelHeight']), path
    assert im.width == round(region['widthCells']*192), path
    assert im.height == round(region['heightCells']*192), path
    assert 0 <= region['pivotX'] <= 1 and 0 <= region['pivotY'] <= 1, path
    alpha = im.getchannel('A')
    assert alpha.getbbox() and alpha.getextrema()[0] == 0, path
    left,top,right,bottom=alpha.getbbox()
    assert left>0 and top>0 and right<im.width and bottom<im.height, ('clipped alpha',name,alpha.getbbox())
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    assert digest == region['sha256'], path
    hashes[name] = digest
    master_path=OUT/'source/masters-256'/path.name
    master = Image.open(master_path)
    assert master.size == (round(region['widthCells']*256),round(region['heightCells']*256)), path
    masters.append(dict(name=name,path='masters-256/'+path.name,pixelWidth=master.width,pixelHeight=master.height,sha256=hashlib.sha256(master_path.read_bytes()).hexdigest()))
clip_ids = {clip['id'] for clip in clips['clips']}
assert len(clip_ids) == len(clips['clips'])
for binding in clips['bindings']:
    assert binding['clip'] in clip_ids, binding
    assert binding['facing'] in ('north','east','south','west'), binding
for clip in clips['clips']:
    assert clip['frames'] and clip['frameSeconds'] > 0, clip['id']
    assert all(frame['name'] in names for frame in clip['frames']), clip['id']
    for marker in clip.get('markers',[]):
        assert 0 <= marker.get('frame',0) < len(clip['frames']), clip['id']
    if '.idle.' in clip['id'] or '.anticipate.' in clip['id']:
        assert not clip['loop'], clip['id']
cardinal = [hashes['machine.sirs.idle.'+f] for f in ('north','east','south','west')]
assert len(set(cardinal)) == 4, 'Four bearings must be four distinct renders'
closed = Image.open(OUT/'regions/door.closed.png')
opened = Image.open(OUT/'regions/door.open.png')
center = (closed.width//2,closed.height//2)
assert closed.getpixel(center)[3] > 240, 'Closed pressure leaves must physically occlude'
assert opened.getpixel(center)[3] == 0, 'Open pressure aperture must be genuine transparency'
if not clips.get('missing'):
    binding_set={(b['actor'],b['action'],b['facing']) for b in clips['bindings']}
    for actor,actions,facings in [('sirs',['idle','walk','anticipate','fire','hit','destroyed'],['north','east','south','west']),('camera',['on','off','aim','disabled'],['north','east','south','west']),('door',['closed','open','opening','closing','locked','braced','damaged'],['north','east'])]:
        for action in actions:
            for facing in facings:assert (actor,action,facing) in binding_set, (actor,action,facing)
    for c in clips['footCalibration']:assert c['sampledStanceResidualSourcePixels'] < .01, c
    for clip in clips['clips']:
        if '.fire.' in clip['id']:assert any(m['kind']=='release' for m in clip['markers']),clip['id']
        if '.damaged.' in clip['id']:assert all(f['forcedOpen'] and f['passageOpen'] for f in clip['frames']),clip['id']
        if '.braced.' in clip['id']:assert all(f['hasPhysicalBrace'] and not f['passageOpen'] for f in clip['frames']),clip['id']
        if clip['id'].startswith('machine.camera.off.') or clip['id'].startswith('machine.camera.disabled.'):
            assert all(not f['powered'] for f in clip['frames']),clip['id']
report = dict(scope='package truth only; not visual, native-play or owner acceptance',regions=len(names),clips=len(clip_ids),bindings=len(clips['bindings']),checks=['192 px/cell actual raster','256 px/cell separately rendered masters','source hashes','true alpha','four distinct SIRS bearings','open/closed physical aperture','valid named clips and explicit bindings'])
(OUT/'source/MASTER_HASHES.json').write_text(json.dumps(dict(masters=masters),indent=2)+'\n',encoding='utf-8',newline='\n')
provenance=json.loads((OUT/'source/PROVENANCE.json').read_text(encoding='utf-8'))
for script,key in [('build.py','scriptSha256'),('motion.py','motionScriptSha256')]:
    assert hashlib.sha256((Path(__file__).parent/script).read_bytes()).hexdigest()==provenance[key],script
assert hashlib.sha256((OUT/'source'/provenance['sourceBlend']['path']).read_bytes()).hexdigest()==provenance['sourceBlend']['sha256']
(OUT/'review/validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps(report))
