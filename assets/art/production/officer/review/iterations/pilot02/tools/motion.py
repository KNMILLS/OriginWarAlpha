"""Camera-calibrated authored contact cycle. Executed in build context."""
foot_state={}
def bone_pose(name,a,b):
    a=Vector(a);b=Vector(b);rest=rig.data.bones[name]
    rotation=(rest.tail_local-rest.head_local).rotation_difference(b-a)
    mat=(rotation.to_matrix()@rest.matrix_local.to_3x3()).to_4x4();mat.translation=a
    mat=mat@Matrix.Diagonal((1,(b-a).length/rest.length,1,1));rig.pose.bones[name].matrix=mat

def knee_for(hip,ankle,upper=.47,lower=.415):
    hip=Vector(hip);ankle=Vector(ankle);delta=ankle-hip;actual=delta.length;d=min(upper+lower-.0005,actual)
    direction=delta.normalized();along=(upper*upper-lower*lower+d*d)/(2*d)
    bend=Vector((0,-1,0));bend=(bend-direction*bend.dot(direction)).normalized()
    return hip+direction*along+bend*math.sqrt(max(0,upper*upper-along*along))

def pose(phase,direction,action='walk'):
    rig.rotation_euler.z=directions[direction];bpy.context.view_layer.update()
    angle=directions[direction];rotation=Matrix.Rotation(angle,3,'Z')
    origin=project((0,0,0));forward=rotation@Vector((0,-1,0));screen=project(forward)-origin
    pixels_per_world=float(np.linalg.norm(screen));unit=screen/pixels_per_world
    # Half-cycle stance must move back exactly half a256px cell in this camera.
    travel=64/pixels_per_world
    footpoints={};result={}
    # No stock sine bob: torso stays settled; garment and joints carry load changes.
    transition=phase if action=='start' else 1-phase if action=='stop' else 0 if action=='idle' else 1
    offset=-.02-.045*transition;twist=.007*math.sin(phase*math.tau) if action=='walk' else 0
    bone_pose('pelvis',(0,0,.97+offset),(0,0,1.08+offset))
    bone_pose('torso',(0,0,1.07+offset),(twist,-.012,1.50+offset))
    bone_pose('head',(twist,-.012,1.50+offset),(twist,-.018,1.85+offset))
    for side,label,sgn,shift in [('L','left',1,0),('R','right',-1,.5)]:
        p=(phase+shift)%1
        neutral=-.016 if side=='L' else .016
        contact=-travel/2 if side=='L' else travel/2
        if action=='idle':
            y=neutral;lift=0;stance=True
        elif action in ['start','stop']:
            # Two short deliberate foot placements; stationary support foot stays planted.
            moving_first=(side=='L' if action=='start' else side=='R')
            t=max(0,min(1,phase*2 if moving_first else phase*2-1));s=t*t*(3-2*t)
            y=neutral+(contact-neutral)*s if action=='start' else contact+(neutral-contact)*s
            lift=.026*math.sin(math.pi*t);stance=not(0<t<1)
        elif p<.5:
            y=-travel/2+2*travel*p;lift=0;stance=True
        else:
            t=(p-.5)*2;s=t*t*(3-2*t)
            y=travel/2-travel*s;lift=.046*math.sin(math.pi*t);stance=False
        x=sgn*.115;hip=(x,0,.97+offset);ankle=(x,y,.085+lift)
        knee=knee_for(hip,ankle)
        bone_pose('thigh.'+side,hip,knee);bone_pose('shin.'+side,knee,ankle)
        bone_pose('foot.'+side,ankle,(x,y-.15,.085+lift))
        sole=rotation@Vector((x,y-.04,.012+lift));local=project(sole)-origin
        root=unit*128*phase if action=='walk' else np.zeros(2);contact=root+local
        result[label]={'phase':p,'stance':stance,'anchorWorld':list(sole),'localPixels':local.tolist(),'screenCells':(local/256).tolist(),'rootPixels':root.tolist(),'rootPlusLocalPixels':contact.tolist(),'liftWorld':lift}
    # Guarded low-ready, actual right-hand equipment follows its socket without drift.
    for side,sgn in [('R',-1),('L',1)]:
        sx=sgn*.235;swing=.022*math.sin(phase*math.tau+(math.pi if side=='R' else 0)) if action=='walk' else 0
        shoulder=(sx+twist,-.01,1.44+offset)
        elbow=(sgn*.265,-.025+swing,1.19+offset)
        wrist=(sgn*.26,-.07+swing,.965+offset);hand=(sgn*.26,-.10+swing,.875+offset)
        bone_pose('upperarm.'+side,shoulder,elbow);bone_pose('forearm.'+side,elbow,wrist);bone_pose('hand.'+side,wrist,hand)
    bpy.context.view_layer.update()
    return result
