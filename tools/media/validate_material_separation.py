"""Independent MAT2 unchanged-pixel checks, plus full current art validation/repacking."""
from pathlib import Path
import argparse,hashlib,json,subprocess,sys
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2];ART=ROOT/'assets/art';BASE=ART/'material-separation'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def cell(a,i):return a[i//8*64:(i//8+1)*64,i%8*64:(i%8+1)*64]
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--verify-repeatability',action='store_true');args=parser.parse_args()
    m=json.loads((BASE/'manifest.json').read_text(encoding='utf-8'))
    sidecars=[BASE/'manifest.json',ART/'atmosphere/manifest.json',ART/'manifest.json',ART/'sectors/manifest.json',ART/'animation/manifest.json',ART/'animation/directional-actions/manifest.json']
    before={str(p.relative_to(ROOT)):sha(p) for p in sidecars};before.update({p:sha(ROOT/p) for p in m['outputs']})
    command=[sys.executable,str(ROOT/'tools/media/validate_atmosphere.py')]
    if args.verify_repeatability:command.append('--verify-repeatability')
    subprocess.run(command,check=True,capture_output=True)
    if args.verify_repeatability:assert before=={p:sha(ROOT/p) for p in before},'Layered packing not byte-repeatable'
    for path,digest in m['sources'].items():assert sha(ROOT/path)==digest,path
    for path,digest in m['outputs'].items():assert sha(ROOT/path)==digest,path
    identities=json.loads((BASE/'source/baseline-identities.json').read_text(encoding='utf-8'))
    protected=0
    for path,digest in identities['art_png_sha256'].items():
        if path in m['outputs']:continue
        assert sha(ROOT/path)==digest,'Unrelated art changed: '+path;protected+=1
    arrays={};preserved={}
    for name,native in [('station','source/station-native.png'),('sectors','sectors/sectors-native.png')]:
        a=np.asarray(Image.open(ART/native).convert('RGBA'));b=np.asarray(Image.open(BASE/f'source/before/{name}-native.png').convert('RGBA'))
        changed={i for i in range(a.shape[0]//64*8) if not np.array_equal(cell(a,i),cell(b,i))}
        assert changed==set(m['changed_slots'][name]),(name,sorted(changed))
        assert all((cell(a,i)[:,:,3]==255).all() for i in changed)
        arrays[name]=a;preserved[name]={'changed_slots':sorted(changed),'unchanged_cells':a.shape[0]//64*8-len(changed),'unchanged_pixels_outside_declared_cells':True}
    fixtures=[]
    for slot in range(15,20):
        alpha=cell(arrays['sectors'],slot)[:,:,3];corners=[int(alpha[y,x]) for x,y in [(0,0),(63,0),(0,63),(63,63)]]
        assert all(a==0 for a in corners) and (alpha==0).mean()>.3
        fixtures.append({'slot':slot,'unchanged_pixels':True,'transparent_percent':round(float((alpha==0).mean()*100),3),'corner_alpha':corners})
    metrics=[]
    for zone in range(5):
        floor=cell(arrays['sectors'],zone)[:,:,:3].astype(float);wall=cell(arrays['sectors'],5+zone)[:,:,:3].astype(float)
        f=float(floor.mean());w=float(wall.mean());cap=float(wall[:7].mean());foot=float(wall[-9:].mean())
        # Objective construction/value checks, not a surrogate for subjective readability.
        assert f>w*1.6,(zone,'floor/wall value separation',f,w)
        assert cap>foot*1.7,(zone,'raised cap/foot contrast',cap,foot)
        metrics.append({'zone':zone,'floor_mean_rgb':round(f,3),'wall_mean_rgb':round(w,3),'floor_wall_mean_ratio':round(f/w,3),'wall_cap_mean_rgb':round(cap,3),'wall_foot_mean_rgb':round(foot,3),'same_x25_multiplier_means':[round(f*.25,3),round(w*.25,3)]})
    report={'task':'MAT2','result':'PASS','baseline':m['baseline'],'repeatability':'PASS' if args.verify_repeatability else 'NOT_RUN','independent_cell_checks':preserved,'unrelated_original_png_sha256_verified':protected,'current_atmosphere_validator':'PASS','fixture_alpha_and_preservation':fixtures,'metrics':metrics,'scope':'Source/export/byte-repeatability/unchanged-asset and independent native-cell checks, plus measurable value/cap-foot contrast. Static dark/frozen-room review is separately documented; no real game shader/FOV, navigation, performance or owner artistic acceptance.'}
    (BASE/'validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(report,indent=2))
if __name__=='__main__':main()
