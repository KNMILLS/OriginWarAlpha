"""Blender source rig endpoint checks, independent of raster metadata/time stamps."""
from pathlib import Path
import json

source=Path(__file__).with_name('build.py')
namespace={'__file__':str(source),'__name__':'station_pose_check'}
exec(compile(source.read_text().split('metrics=[]')[0],str(source),'exec'),namespace)
pose=namespace['pose'];rig=namespace['rig'];bpy=namespace['bpy']

def snapshot(action,phase,facing):
    pose(phase,facing,action)
    bpy.context.view_layer.update()
    return {b.name:[v for row in b.matrix for v in row] for b in rig.pose.bones}

checks=[]
for facing in ['north','east','south','west']:
    idle=snapshot('idle',0,facing);start=snapshot('start',1,facing)
    walk=snapshot('walk',0,facing);stop=snapshot('stop',0,facing)
    stopped=snapshot('stop',1,facing)
    for label,a,b in [('start-end/walk0',start,walk),('walk0/stop-start',walk,stop),('stop-end/idle',stopped,idle)]:
        error=max(abs(x-y) for name in a for x,y in zip(a[name],b[name]))
        bone_errors={name:max(abs(x-y) for x,y in zip(a[name],b[name])) for name in a}
        checks.append(dict(facing=facing,transition=label,maxBoneMatrixDelta=error,boneErrors=bone_errors))
out=namespace['OUT']/'review/source-rig-transition-checks.json'
out.write_text(json.dumps(dict(scope='rig matrices at authored phase0 bridges only; source reach asserts execute during export; arbitrary phase interruption/native motion acceptance not inferred',checks=checks),indent=2)+'\n')
print('SOURCE_RIG_ENDPOINTS',json.dumps(checks))
assert all(c['maxBoneMatrixDelta']<1e-5 for c in checks),'See source-rig-transition-checks.json for preserved bone-level endpoint discrepancies'
