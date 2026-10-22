"""Original rigid-machine choreography, calibrated against the export camera."""
def smooth(t):
    t=max(0,min(1,t));return t*t*(3-2*t)

def project(point):
    p=world_to_camera_view(scene,camera,Vector(point))
    return np.array([p.x*288,p.y*384],dtype=float)

def bone_pose(name,a,b):
    a=Vector(a);b=Vector(b);rest=rig.data.bones[name]
    rotate=(rest.tail_local-rest.head_local).rotation_difference(b-a)
    mat=(rotate.to_matrix()@rest.matrix_local.to_3x3()).to_4x4();mat.translation=a
    mat=mat@Matrix.Diagonal((1,(b-a).length/rest.length,1,1))
    rig.pose.bones[name].matrix=mat
    bpy.context.view_layer.update()

def joint(a,b,upper,lower,bend):
    a=Vector(a);b=Vector(b);delta=b-a;actual=delta.length
    assert actual <= upper+lower+.001, ('unreachable rigid joint',list(a),list(b),actual,upper+lower)
    d=min(upper+lower-.0001,actual);direction=delta.normalized()
    along=(upper*upper-lower*lower+d*d)/(2*d)
    bend=Vector(bend);bend=(bend-direction*bend.dot(direction)).normalized()
    return a+direction*along+bend*math.sqrt(max(0,upper*upper-along*along))

def placement(side,phase,action,travel):
    p=(phase+(0 if side=='L' else .5))%1
    contact=-travel/2 if side=='L' else travel/2
    if action=='walk':
        if p<.5:return -travel/2+2*travel*p,0,True,p
        t=(p-.5)*2;return travel/2-travel*smooth(t),.055*math.sin(math.pi*t),False,p
    if action in ['start','stop']:
        first=side=='L' if action=='start' else side=='R';t=max(0,min(1,phase*2 if first else phase*2-1))
        y=contact*smooth(t) if action=='start' else contact*(1-smooth(t))
        return y,.032*math.sin(math.pi*t),not(0<t<1),p
    return 0,0,True,p

def pose(phase,facing,action='idle',index=0):
    # Actual disabled hardware loses its operating sensor cue during collapse.
    # Change only this object's material slot, never the shared amber material.
    disabled=action=='destroyed' and index>=4
    bpy.data.objects['single vertical amber sensor aperture'].data.materials[0]=dark if disabled else amber
    bpy.data.objects['sensor lens inner slit'].data.materials[0]=black if disabled else muted
    rig.rotation_euler.z=DIRECTIONS[facing];rig.location=(0,0,0);rig.rotation_euler.x=0
    for b in rig.pose.bones:b.matrix_basis=Matrix.Identity(4)
    bpy.context.view_layer.update()
    rotation=Matrix.Rotation(DIRECTIONS[facing],3,'Z');origin=project((0,0,0))
    screen=project(rotation@Vector((0,-1,0)))-origin
    px_per_world=float(np.linalg.norm(screen));unit=screen/px_per_world
    # Two feet alternate a half-cycle plant. Root advances one cell per .5s
    # cycle: four contacts/s rather than the prior eight-contact shuffle.
    travel=96/px_per_world
    settle=.07*(smooth(phase) if action=='start' else 1-smooth(phase) if action=='stop' else 1) if action in ['walk','start','stop'] else 0
    hit=[0,.7,1,.3,0][min(4,index)] if action=='hit' else 0
    fall=smooth(phase) if action=='destroyed' else 0
    readiness=smooth(phase) if action=='anticipate' else 1 if action=='fire' else 0
    chassis_recoil=[0,0,.018,.010,.004,0,0,0][min(7,index)] if action=='fire' else 0
    bodyz=-settle-.42*fall-.025*readiness;bodyy=.05*hit+.19*fall+.02*readiness+chassis_recoil
    if action in ['idle','walk','start','stop']:
        # Lower the actual chassis when fixed-length links require clearance.
        # Planted foot paths are never clipped, displaced or stretched to pass.
        for side in ['L','R']:
            y,lift,_,_=placement(side,phase,action,travel)
            upper=(Vector(rests['thigh.'+side][1])-Vector(rests['thigh.'+side][0])).length
            lower=(Vector(rests['shin.'+side][1])-Vector(rests['shin.'+side][0])).length
            reach=math.sqrt(max(0,(upper+lower-.004)**2-.035**2-y*y))
            bodyz=min(bodyz,.12+lift+reach-.70)
    bone_pose('pelvis',(0,bodyy,.70+bodyz),(0,bodyy,.86+bodyz))
    upper_rotation=Matrix.Rotation(-1.35*fall-.08*hit+.045*readiness,3,'X')
    def upper_point(p):return Vector((0,bodyy,.86+bodyz))+upper_rotation@(Vector(p)-Vector((0,0,.86)))
    bone_pose('torso',upper_point((0,0,.86)),upper_point((0,0,1.34)))
    bone_pose('head',upper_point((0,0,1.39)),upper_point((0,0,1.65)))
    result={}
    for side,label,s,shift in [('L','left',1,0),('R','right',-1,.5)]:
        y,lift,stance,p=placement(side,phase,action,travel)
        hip=(s*.18,bodyy,.70+bodyz);ankle=(s*.215,y+.24*fall,.12+lift)
        upper=(Vector(rests['thigh.'+side][1])-Vector(rests['thigh.'+side][0])).length
        lower=(Vector(rests['shin.'+side][1])-Vector(rests['shin.'+side][0])).length
        knee=joint(hip,ankle,upper,lower,(0,-1,0))
        bone_pose('thigh.'+side,hip,knee);bone_pose('shin.'+side,knee,ankle)
        bone_pose('foot.'+side,ankle,(s*.215,y-.20+.24*fall,.08+lift))
        sole=rotation@Vector((s*.215,y-.055+.24*fall,.006+lift));local=project(sole)-origin
        root=unit*192*phase if action=='walk' else np.zeros(2)
        result[label]=dict(phase=p,stance=stance,anchorWorld=list(sole),localPixels=local.tolist(),screenCells=(local/192).tolist(),rootPixels=root.tolist(),rootPlusLocalPixels=(root+local).tolist(),liftWorld=lift)
    blend=smooth(phase) if action=='anticipate' else 1 if action=='fire' else 0
    recoil=[0,0,.055,.025,.008,0,0,0][min(7,index)] if action=='fire' else 0
    for side,s in [('R',-1),('L',1)]:
        shoulder=Vector((s*.35,0,1.285))
        rest_elbow=Vector((s*.43,-.015,.98))
        rest_wrist=Vector((s*.47,-.11,.74))
        if side=='R':
            elbow=rest_elbow.copy()
            forearm=Vector(rests['forearm.R'][1])-Vector(rests['forearm.R'][0])
            wrist=elbow+Matrix.Rotation(math.radians(-18.4)*blend,3,'X')@forearm
            # Recoil is an actual joint rotation, not a displaced duplicate cannon.
            wrist=elbow+Matrix.Rotation(recoil*2,3,'X')@(wrist-elbow)
        else:
            movement=.025*math.sin(phase*math.tau) if action=='walk' else 0
            elbow=rest_elbow+Vector((0,movement,0));wrist=rest_wrist+Vector((0,movement,0))
        bone_pose('upperarm.'+side,upper_point(shoulder),upper_point(elbow));bone_pose('forearm.'+side,upper_point(elbow),upper_point(wrist))
    for ob in flash_parts:ob.hide_render=not(action=='fire' and index==2)
    bpy.context.view_layer.update()
    return result
