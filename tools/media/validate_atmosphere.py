"""ATM-ART source/export checks. No native-game or human artistic acceptance claim."""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[2]
ART=ROOT/'assets/art'
BASE=ART/'atmosphere'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def cell(x,i,cols=8):return x[i//cols*64:(i//cols+1)*64,i%cols*64:(i%cols+1)*64]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--verify-repeatability',action='store_true');args=parser.parse_args()
    manifest=json.loads((BASE/'manifest.json').read_text(encoding='utf-8'))
    retained={p:sha(ROOT/p) for p in manifest['outputs']}
    sidecars=[BASE/'manifest.json',ART/'manifest.json',ART/'animation/manifest.json',ART/'sectors/manifest.json',ART/'animation/directional-actions/manifest.json']
    before={str(p.relative_to(ROOT)):sha(p) for p in sidecars}
    if args.verify_repeatability:
        subprocess.run([sys.executable,str(ROOT/'tools/media/pack_atmosphere.py')],check=True,capture_output=True)
        assert retained=={p:sha(ROOT/p) for p in retained},'Runtime/native packing is not byte-repeatable'
        assert before=={str(p.relative_to(ROOT)):sha(p) for p in sidecars},'Manifest packing is not byte-repeatable'
    for path,digest in manifest['sources'].items():assert sha(ROOT/path)==digest,path
    for path,digest in manifest['authoring_request_records'].items():assert sha(ROOT/path)==digest,path
    record=manifest['generation_record'];assert sha(ROOT/record['path'])==record['sha256']
    identity=manifest['baseline_runtime_identities'];assert sha(ROOT/identity['path'])==identity['sha256']
    identities=json.loads((ROOT/identity['path']).read_text(encoding='utf-8'))['sha256']
    for path,digest in manifest['outputs'].items():assert sha(ROOT/path)==digest,path
    layouts=[('station','source/station-native.png','station.png',8,8),('sectors','sectors/sectors-native.png','sectors/sectors.png',8,4),('actors','animation/actors-native.png','animation/actors.png',8,8),('attacks','animation/directional-actions/attack-native.png','animation/directional-actions/attack.png',8,3),('walk','atmosphere/player-walk-native.png','atmosphere/player-walk.png',4,4)]
    arrays={};checks={}
    for name,native,export,cols,rows in layouts:
        n=Image.open(ART/native);a=Image.open(ART/export)
        assert n.mode==a.mode=='RGBA' and n.size==(cols*64,rows*64) and a.size==(cols*128,rows*128),name
        x=np.asarray(n);assert np.array_equal(np.asarray(a),np.repeat(np.repeat(x,2,axis=0),2,axis=1)),name
        arrays[name]=x;checks[name]={'native':list(n.size),'runtime':list(a.size),'nearest_scale':2,'sha256':sha(ART/export)}
        if name=='walk':
            checks[name]['new_slots']=16
            continue
        baseline={'station':'station','sectors':'sectors','actors':'actors','attacks':'attack'}[name]
        old=np.asarray(Image.open(BASE/'source/baseline'/f'{baseline}-native.png').convert('RGBA'))
        old=np.repeat(np.repeat(old,2,axis=0),2,axis=1)
        changed={i for i in range(rows*cols) if not np.array_equal(cell(x,i,cols),cell(old,i,cols))}
        assert changed==set(manifest['changed_slots'][name]),(name,sorted(changed))
        checks[name]['changed_slots']=sorted(changed)
        checks[name]['unchanged_slots_preserved_at_runtime']=True
    # All actual ordinary/special floor, wall and glass surfaces are new and opaque.
    for slot in list(range(8))+[11,12,13]:assert np.all(cell(arrays['station'],slot)[:,:,3]==255)
    for slot in range(10):assert np.all(cell(arrays['sectors'],slot)[:,:,3]==255)
    assert cell(arrays['station'],9)[32,32,3]==0,'Open door must reveal the floor'
    assert not cell(arrays['station'],63)[:,:,3].any(),'Reserved station blank'
    walk=[]
    for facing in range(4):
        poses=[cell(arrays['walk'],facing*4+i,4) for i in range(4)]
        assert len({hashlib.sha256(p.tobytes()).hexdigest() for p in poses})==4,'Duplicate gait phase'
        bounds=[];deltas=[]
        for i,p in enumerate(poses):
            assert (p[:,:,3]==0).any() and not ((p[:,:,3]>0)&(p[:,:,3]<32)).any()
            box=Image.fromarray(p).getbbox();assert box and box[3]==60 and box[1]>=1
            bounds.append(list(box));deltas.append(int(np.any(p!=poses[(i+1)%4],axis=2).sum()))
        walk.append({'facing':facing,'bounding_boxes':bounds,'adjacent_phase_changed_pixels':deltas})
    for name,slots in [('actors',list(range(8))+[40,41,42,43]),('attacks',list(range(8))),('sectors',list(range(10,30)))]:
        for slot in slots:
            a=cell(arrays[name],slot)[:,:,3];assert (a==0).any() and not ((a>0)&(a<32)).any(),(name,slot)
    for path in sidecars[1:]:
        d=json.loads(path.read_text(encoding='utf-8'));assert d['atmosphere_override']=='assets/art/atmosphere/manifest.json'
        for source in d.get('source_rasters',[]):assert sha(ROOT/source['path'])==source['sha256']
    # Unmodified finale/human exports still pass their own format/nearest-scale contracts.
    for label,native,export,dimensions,scale,mode in [('humans','animation/humans/humans-native.png','animation/humans/humans.png',(256,32),4,'RGBA'),('finale','finale/finale-native.png','finale/finale.png',(512,256),2,'RGB')]:
        n=Image.open(ART/native);a=Image.open(ART/export)
        assert n.mode==a.mode==mode and n.size==dimensions
        assert np.array_equal(np.asarray(a),np.repeat(np.repeat(np.asarray(n),scale,axis=0),scale,axis=1))
        assert sha(ART/export)==identities['assets/art/'+export],'Retained export changed: '+label
        checks[label]={'native':list(n.size),'runtime':list(a.size),'nearest_scale':scale,'sha256':sha(ART/export),'modified':False}
    report={'task':'ATM-ART','result':'PASS','scope':'Static source hashes, RGBA/dimensions, exact nearest2x export, unchanged-slot pixels, complete terrain replacement, open-door transparency, player anchor/phase differences and deterministic packing only. Contact-sheet inspection is separately recorded in the handoff; no runtime/gameplay/emotional acceptance.', 'repeatability':'PASS' if args.verify_repeatability else 'NOT_RUN','atlases':checks,'player_walk':walk,'source_hashes_verified':len(manifest['sources'])}
    (BASE/'validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'result':report['result'],'repeatability':report['repeatability'],'source_hashes_verified':report['source_hashes_verified'],'runtime_sizes':{k:v['runtime'] for k,v in checks.items()},'changed_slots':{k:len(v['changed_slots']) for k,v in checks.items() if 'changed_slots' in v},'new_walk_frames':16},indent=2))
if __name__=='__main__':main()
