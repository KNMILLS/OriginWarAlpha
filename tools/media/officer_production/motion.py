"""Camera-calibrated authored contact cycle. Executed in build context."""
foot_state={};hand_state={}
def contact_hands():
    global hand_state
    hand_state={};origin=project((0,0,0))
    for side,label,sign in [('L','left',1),('R','right',-1)]:
        matrix=rig.matrix_world@rig.pose.bones['hand.'+side].matrix@rig.data.bones['hand.'+side].matrix_local.inverted()
        points={}
        for name,restpoint in [('wrist',(sign*.26,-.07,.965)),('palm',(sign*.26,-.10,.92)),('fingerTip',(sign*.26,-.13,.875))]:
            point=matrix@Vector(restpoint);local=project(point)-origin
            points[name]={'world':list(point),'localPixels':local.tolist(),'screenCells':(local/256).tolist()}
        hand_state[label]=points
    return hand_state
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

def arm_joint(shoulder,wrist,sign):
    a=Vector(shoulder);b=Vector(wrist);delta=b-a;u=.251;v=.232;d=min(u+v-.0005,delta.length);direction=delta.normalized()
    along=(u*u-v*v+d*d)/(2*d);bend=Vector((sign,0,-.35));bend=(bend-direction*bend.dot(direction)).normalized()
    return a+direction*along+bend*math.sqrt(max(0,u*u-along*along))
def smooth(t):t=max(0,min(1,t));return t*t*(3-2*t)
WALK_CELLS=.75
WALK_SECONDS=.9
WALK_FRAMES=54
INTERACTION_FRAMES={'work':12,'terminal':12,'breaker':18,'pickup':24,'drop':24,'supply':12,'equip':12}
INTERACTION_CONTACT={'work':4,'terminal':4,'breaker':6,'pickup':10,'drop':10,'supply':4,'equip':4}
INTERACTION_RECOVERY={'work':7,'terminal':8,'breaker':12,'pickup':17,'drop':17,'supply':8,'equip':8}
def gait_foot(phase,side,travel):
    p=(phase+(0 if side=='L' else .5))%1
    if p<.5:
        # Heel contact -> compression -> passing -> toe pressure. The sole rotates
        # around a fixed plantar marker, so pitch never invents root displacement.
        y=-travel/2+2*travel*p;lift=0;stance=True
        roll=-.13*(1-smooth(p/.11))+.22*smooth((p-.35)/.15)
    else:
        t=(p-.5)*2;s=smooth(t)
        y=travel/2-travel*s;lift=.065*math.sin(math.pi*t);stance=False
        roll=.22*(1-smooth(t/.32))-.13*smooth((t-.68)/.32)
    return p,y,lift,stance,roll
def pose(phase,direction,action='walk',weapon='pistol',sample_index=0):
    rig.rotation_euler.z=directions[direction];bpy.context.view_layer.update()
    angle=directions[direction];rotation=Matrix.Rotation(angle,3,'Z')
    origin=project((0,0,0));forward=rotation@Vector((0,-1,0));screen=project(forward)-origin
    pixels_per_world=float(np.linalg.norm(screen));unit=screen/pixels_per_world
    # Half-cycle stance must move back exactly half a256px cell in this camera.
    travel=(256*WALK_CELLS/2)/pixels_per_world
    footpoints={};result={}
    stop=action=='stop' or action.startswith('stop_phase_')
    gait_phase=(int(action[-1])/3 if action.startswith('stop_phase_') else 0) if stop else phase if action=='walk' else 0
    transition=smooth(phase) if action=='start' else 1-smooth(phase) if stop else 1 if action=='walk' else 0
    # Double-support compression, rising passing pose and lateral weight transfer
    # are separately authored; the spine absorbs most of the pelvic vertical motion.
    compression=-.013*math.cos(gait_phase*math.tau*2)*transition
    offset=-.02-.035*transition+compression
    sway=.016*math.sin(gait_phase*math.tau)*transition
    twist=.010*math.sin(gait_phase*math.tau)*transition
    upper_offset=-.02-.035*transition+compression*.35
    hit=([0,.85,1,.45,0][min(4,sample_index)] if action=='hit' else 0)
    fall=smooth(phase) if action=='death' else 0
    # Buckle briefly, then extend into a supported floor fall. Final is prone, not kneeling.
    offset-=.18*math.sin(math.pi*fall) if action=='death' else 0
    interactions=list(INTERACTION_FRAMES)
    amount=0
    if action in interactions:
        seconds=phase*(INTERACTION_FRAMES[action]-1)/24
        contact_time=INTERACTION_CONTACT[action]/24;recovery_time=INTERACTION_RECOVERY[action]/24
        end_time=(INTERACTION_FRAMES[action]-1)/24
        amount=smooth(seconds/contact_time) if seconds<contact_time else 1 if seconds<=recovery_time else 1-smooth((seconds-recovery_time)/(end_time-recovery_time))
        if phase<=0 or phase>=1:amount=0
    floor_action=action in ['pickup','drop']
    crouch=.60*amount if floor_action else .075*amount if action=='breaker' else 0
    hip_rear=.18*amount if floor_action else 0
    offset-=crouch;upper_offset-=crouch+(.30*amount if floor_action else 0)
    lean=.42*amount if floor_action else .055*amount if action in ['terminal','breaker'] else 0
    bone_pose('pelvis',(sway,hip_rear,.97+offset),(sway,hip_rear-.03*amount if floor_action else 0,1.08+offset))
    bone_pose('torso',(sway,hip_rear,1.07+offset),(twist,hip_rear-.012-lean+.025*hit-.10*fall,1.50+upper_offset))
    # A guarded upward glance keeps real cheek/eye planes visible from the elevated game camera.
    bone_pose('head',(twist,hip_rear-.012-lean+.025*hit-.10*fall,1.50+upper_offset),(twist*.35+.105*fall,hip_rear+.032-lean*.85-(.15*amount if floor_action else 0)+.04*hit-.24*fall,1.85+upper_offset-compression*.22-.10*fall))
    for side,label,sgn,shift in [('L','left',1,0),('R','right',-1,.5)]:
        p=(phase+shift)%1
        neutral=-.016 if side=='L' else .016
        contact=-travel/2 if side=='L' else travel/2
        roll=0
        if action!='walk' and action!='start' and not stop:
            y=neutral;lift=0;stance=True
        elif action=='start' or stop:
            # Two short deliberate foot placements; stationary support foot stays planted.
            initial=gait_foot(gait_phase,side,travel)
            moving_first=(side=='L' if action=='start' else not initial[3])
            if stop and abs(gait_phase)<.001:moving_first=side=='R'
            t=max(0,min(1,phase*2 if moving_first else phase*2-1));s=t*t*(3-2*t)
            y=neutral+(contact-neutral)*s if action=='start' else initial[1]+(neutral-initial[1])*s
            initial_lift=0 if action=='start' else initial[2]
            lift=initial_lift*(1-s)+.031*math.sin(math.pi*t);stance=not(0<t<1) and initial_lift<.001
            roll=initial[4]*s if action=='start' else initial[4]*(1-s)
        else:
            p,y,lift,stance,roll=gait_foot(phase,side,travel)
        x=sgn*.115;hip=(x+sway,hip_rear,.97+offset)
        # Rotate a rigid boot around its plantar marker: the foot rolls while its
        # contact point stays exact. Heel/toe pitch is real rig rotation, not pixels.
        foot_rotation=Matrix.Rotation(roll,3,'X')
        # Heel or toe is the pivot while its edge carries the load. At a flat sole
        # the marker returns to the original plantar anchor. Adjacent contact
        # patches have different physical positions and are measured separately.
        contact_offset=.055 if roll<-.001 else -.15 if roll>.001 else -.04
        contact_patch='heel' if roll<-.001 else 'toe' if roll>.001 else 'sole'
        sole_local=Vector((x,y+contact_offset,.012+lift))
        ankle_vec=sole_local+foot_rotation@Vector((0,-contact_offset,.073))
        ankle_vec.x+=.035*fall if side=='L' else 0
        ankle=tuple(ankle_vec)
        knee=knee_for(hip,ankle)
        bone_pose('thigh.'+side,hip,knee);bone_pose('shin.'+side,knee,ankle)
        toe=ankle_vec+foot_rotation@Vector((.018*fall if side=='L' else 0,-.15,0))
        bone_pose('foot.'+side,ankle,toe)
        # Exact rigid-sole mapping from the authored foot endpoint. Reading a pose-bone
        # matrix here can return stale dependency state until the final scene update.
        marker_x=ankle[0]+(.04*.018/.15*fall if side=='L' else 0)
        sole=rotation@sole_local;local=project(sole)-origin
        root=unit*256*WALK_CELLS*phase if action=='walk' else np.zeros(2);contact=root+local
        result[label]={'phase':p,'stance':stance,'contactPatch':contact_patch,'anchorWorld':list(sole),'localPixels':local.tolist(),'screenCells':(local/256).tolist(),'rootPixels':root.tolist(),'rootPlusLocalPixels':contact.tolist(),'liftWorld':lift,'bootPitchRadians':roll,'poseRole':'contact' if p<.10 else 'compression' if p<.22 else 'passing' if p<.36 else 'push_off' if p<.5 else 'swing'}
    # Guarded low-ready, genuine action poses and actual right-hand equipment socket.
    for side,sgn in [('R',-1),('L',1)]:
        sx=sgn*.235
        swing=.045*math.sin(gait_phase*math.tau+(math.pi if side=='R' else 0)-.16)*transition
        if weapon=='pistol' and side=='R':swing*=.48
        shoulder=(sx+twist,hip_rear-.01-lean*.8,1.44+upper_offset)
        wrist=np.array((sgn*.26,-.07+swing,.965+upper_offset));hand=np.array((sgn*.26,-.10+swing,.875+upper_offset))
        if weapon=='rifle':
            # A weighted two-hand carry keeps the support palm at the fore-end.
            wrist=np.array((-.12,-.255,1.12+upper_offset)) if side=='R' else np.array((-.07,-.39,1.11+upper_offset))
            hand=wrist+np.array((0,-.075,-.015))
            wrist[1]+=swing*.25;hand[1]+=swing*.25
        blend=phase if action=='ready' else 1 if action=='aim' else smooth(phase/.28) if action=='fire' and phase<.28 else 1-smooth((phase-.55)/.29) if action=='fire' else 0
        if blend>0:
            aw=np.array((-.08,-.35,1.30+offset)) if side=='R' else np.array((-.025,-.29,1.27+offset))
            ah=aw+np.array((0,-.09,0))
            if weapon=='unarmed':aw=np.array((sgn*.15,-.24,1.21+offset));ah=aw+np.array((0,-.035,-.045))
            if weapon=='rifle':
                aw=np.array((-.11,-.29,1.315+offset)) if side=='R' else np.array((-.11,-.46,1.30+offset))
                ah=aw+np.array((0,-.085,0))
            wrist=wrist*(1-blend)+aw*blend;hand=hand*(1-blend)+ah*blend
        if action=='fire' and weapon in ['pistol','rifle'] and sample_index in [2,3]:
            recoil=.025 if sample_index==2 else .010;wrist[1]+=recoil;hand[1]+=recoil
        if action in interactions and side=='L':
            contacts={'work':(.12,-.365,1.255),'terminal':(.025,-.45,1.12),'breaker':(.08,-.46,1.31),'pickup':(.22,-.35,.045),'drop':(.22,-.35,.045),'supply':(-.045,-.24,1.27),'equip':(-.16,-.17,1.05)}
            ah=np.array(contacts[action]);aw=ah+np.array((0,.065,.015))
            if action=='breaker':
                # The fingers meet the handle first, then the wrist pulls it down.
                pull=.065*smooth((seconds-contact_time)/(recovery_time-contact_time));aw[2]-=pull;ah[2]-=pull
            if action in ['pickup','drop']:
                # Reaching is shared; pickup curls/lifts while drop opens/withdraws.
                lift=.06*smooth((seconds-contact_time)/(recovery_time-contact_time))*(1 if action=='pickup' else .25)
                aw[2]+=lift;ah[2]+=lift
            wrist=wrist*(1-amount)+aw*amount;hand=hand*(1-amount)+ah*amount
        if action in ['pickup','drop'] and side=='R':
            # Keep the held pistol clear of the floor while the other hand works.
            wrist[2]+=.24*amount;hand[2]+=.24*amount
        if action=='melee':
            amount=[0,.3,.7,1,.75,.45,.15,0][min(7,sample_index)]
            aw=np.array((-.12,-.39,1.25+offset)) if side=='R' else np.array((.15,-.19,1.22+offset))
            ah=aw+np.array((0,-.065,-.02));wrist=wrist*(1-amount)+aw*amount;hand=hand*(1-amount)+ah*amount
        if hit>0:wrist[1]+=.025*hit;hand[1]+=.025*hit
        if fall>0:
            wrist[1]-=.12*fall;hand[1]-=.12*fall
            if side=='L':
                # Left arm reaches to brace and remains folded beside the head on the floor.
                amount=smooth(fall/.7);aw=np.array((.20,-.11,1.34+offset));ah=np.array((.15,-.17,1.36+offset))
                wrist=wrist*(1-amount)+aw*amount;hand=hand*(1-amount)+ah*amount
        elbow=arm_joint(shoulder,wrist,sgn)
        bone_pose('upperarm.'+side,shoulder,elbow);bone_pose('forearm.'+side,elbow,wrist);bone_pose('hand.'+side,wrist,hand)
    for ob in weapon_objects:ob.hide_render=weapon!='pistol'
    for ob in rifle_objects+heavy_objects:ob.hide_render=weapon!='rifle'
    flash.hide_render=not(action=='fire' and weapon=='pistol' and sample_index==2)
    rifle_flash.hide_render=not(action=='fire' and weapon=='rifle' and sample_index==2)
    bpy.context.view_layer.update()
    if action=='death' and phase>0:
        # A whole-body fall follows the authored buckle; mesh bounds determine floor contact.
        # The same transform moves hands and attached equipment, avoiding detached gear.
        fall_matrix=Matrix.Rotation(math.pi*.5*fall,4,'X')@Matrix.Rotation(.12*fall,4,'Y')
        matrices={b.name:fall_matrix@b.matrix.copy() for b in rig.pose.bones}
        for b in rig.pose.bones:b.matrix=matrices[b.name]
        bpy.context.view_layer.update();deps=bpy.context.evaluated_depsgraph_get();minimum=1e9
        for ob in scene.objects:
            if ob.type!='MESH' or ob.hide_render:continue
            evaluated=ob.evaluated_get(deps);mesh=evaluated.to_mesh()
            if mesh.vertices:
                coords=np.empty(len(mesh.vertices)*3);mesh.vertices.foreach_get('co',coords)
                points=coords.reshape(-1,3);matrix=np.array(evaluated.matrix_world)
                minimum=min(minimum,float(np.min(points@matrix[2,:3]+matrix[2,3])))
            evaluated.to_mesh_clear()
        floor_shift=.012-minimum
        # Keep frame zero exactly neutral; after release, actual deformed mesh rests on floor.
        for b in rig.pose.bones:
            matrix=b.matrix.copy();matrix.translation.z+=floor_shift;b.matrix=matrix
        bpy.context.view_layer.update()
        for foot in result.values():
            local=rotation.inverted()@Vector(foot['anchorWorld']);point=fall_matrix@local
            point.z+=floor_shift;point=rotation@point;projected=project(point)-origin
            foot.update({'stance':False,'anchorWorld':list(point),'localPixels':projected.tolist(),'screenCells':(projected/256).tolist(),'rootPlusLocalPixels':projected.tolist(),'fallRadians':math.pi*.5*fall,'evaluatedFloorMinimum':.012,'floorTranslation':floor_shift})
    contact_hands();return result

def pose_turn(phase,source,destination,weapon='pistol'):
    if phase<=0:return pose(0,source,'idle',weapon)
    if phase>=1:return pose(0,destination,'idle',weapon)
    pose(0,destination,'idle',weapon)
    start=directions[source];delta=(directions[destination]-start+math.pi)%(math.tau)-math.pi
    # Three alternating placements for a half turn; a two-step half turn crosses the feet.
    half=abs(abs(delta)-math.pi)<.01
    steps=3 if half else 2;step=min(steps-1,int(phase*steps));t=phase*steps-step;s=smooth(t)
    angle=start+delta*smooth(phase);rig.rotation_euler.z=angle;bpy.context.view_layer.update()
    rotate=Matrix.Rotation(angle,3,'Z');inv=rotate.inverted();origin=project((0,0,0))
    result={};support='L' if step%2==0 else 'R'
    def foot_target(side):
        sgn=1 if side=='L' else -1;neutral=Vector((sgn*.115,-.016 if side=='L' else .016,.085))
        if not half:
            footstep=0 if side=='R' else 1;a=start;b=start+delta
            progress=0 if step<footstep else 1 if step>footstep else s
        elif side=='L':
            a=start;b=start+delta;progress=0 if step<1 else 1 if step>1 else s
        else:
            a=start if step<2 else start+delta*.5;b=start+delta*.5 if step<2 else start+delta
            progress=s if step in [0,2] else 1
        yaw=a+(b-a)*progress;point=Matrix.Rotation(yaw,3,'Z')@neutral
        moving=(side!=support);lift=.045*math.sin(math.pi*t) if moving else 0
        # Slight outside arc opens the crossing space without moving the planted support.
        if moving:
            radial=Vector((point.x,point.y,0)).normalized();point+=radial*.025*math.sin(math.pi*t)
        point.z+=lift
        return point,yaw,lift,moving
    targets={side:foot_target(side) for side in ['R','L']}
    weight=targets[support][0].copy();weight.z=0;weight*=.18*math.sin(math.pi*t)
    local_weight=inv@weight
    for b in rig.pose.bones:
        matrix=b.matrix.copy();matrix.translation+=local_weight;b.matrix=matrix
    for side,sgn,label in [('R',-1,'right'),('L',1,'left')]:
        ankle_world,yaw,lift,moving=targets[side];ankle=inv@ankle_world
        hip=Vector((sgn*.115,0,.95))+local_weight;knee=knee_for(hip,ankle)
        toe=inv@(ankle_world+Matrix.Rotation(yaw,3,'Z')@Vector((0,-.15,0)))
        bone_pose('thigh.'+side,hip,knee);bone_pose('shin.'+side,knee,ankle);bone_pose('foot.'+side,ankle,toe)
        sole=ankle_world+Matrix.Rotation(yaw,3,'Z')@Vector((0,-.04,-.073));local=project(sole)-origin
        result[label]={'stance':not moving,'anchorWorld':list(sole),'localPixels':local.tolist(),'screenCells':(local/256).tolist(),'rootPixels':[0,0],'rootPlusLocalPixels':local.tolist(),'liftWorld':lift,'footHeadingRadians':yaw,'bodyHeadingRadians':angle,'placement':step,'fromFacing':source,'toFacing':destination}
    bpy.context.view_layer.update();contact_hands();return result
