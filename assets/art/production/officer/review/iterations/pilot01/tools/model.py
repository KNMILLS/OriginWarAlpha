"""Original tailored pressure suit construction, executed in build context."""
# One readable adult ribcage/waist mass and a connected garment under practical shells.
loft('continuous tailored torso',[(0,0,.99,.185,.125),(0,0,1.09,.165,.115),(0,0,1.24,.20,.135),(0,0,1.38,.228,.141),(0,0,1.47,.205,.112),(0,0,1.51,.105,.084)],fabric,'torso')
loft('continuous seat and hip garment',[(0,0,.86,.16,.115),(0,0,.94,.19,.13),(0,0,1.035,.18,.124)],fabric,'pelvis')
plate('single shaped ribcage shell',[(-.158,-.145,1.205),(-.198,-.151,1.365),(-.145,-.145,1.465),(.145,-.145,1.465),(.198,-.151,1.365),(.158,-.145,1.205)],.022,ivory,'torso',True)
tube('chest bottom selective contour',[(-.158,-.159,1.205),(0,-.167,1.19),(.158,-.159,1.205)],.003,steel,'torso')
plate('abdomen overlapping flexible enamel',[(-.13,-.132,1.075),(-.15,-.148,1.17),(0,-.16,1.19),(.15,-.148,1.17),(.13,-.132,1.075)],.014,fabric,'torso')
for x in [-.14,.14]:
    tube('load harness strap',[(x,-.105,1.49),(x*1.04,-.167,1.39),(x*.91,-.163,1.24),(x*.82,-.133,1.09)],.019,darkcloth,'torso')
    box('harness practical buckle',(x,-.18,1.36),(.043,.015,.046),steel,'torso',.003)
    box('harness buckle slot',(x,-.189,1.36),(.026,.004,.02),ink,'torso',.001)
box('small suit identifier',(-.055,-.171,1.385),(.055,.006,.012),edge,'torso',.001)
box('restrained status tag',(.085,-.171,1.325),(.013,.006,.02),amber,'torso',.001)
tube('waist belt',[(-.175,-.06,1.04),(-.13,-.13,1.04),(0,-.147,1.04),(.13,-.13,1.04),(.175,-.06,1.04)],.026,darkcloth,'pelvis')
box('belt quick release',(0,-.17,1.04),(.068,.021,.047),steel,'pelvis',.005)
box('belt release recess',(0,-.182,1.04),(.034,.006,.024),ink,'pelvis',.002)
for x in [-.145,.145]:box('flat fabric belt pouch',(x,-.12,1.00),(.07,.045,.09),darkcloth,'pelvis',.009)

# Compact back support follows the ribcage instead of becoming a robot backpack block.
plate('back harness shaped bed',[(-.14,.125,1.12),(-.16,.126,1.39),(-.11,.10,1.47),(.11,.10,1.47),(.16,.126,1.39),(.14,.125,1.12)],.032,darkcloth,'torso')
for x in [-.061,.061]:
    loft('life support slim bottle',[(x,.19,1.16,.036,.036),(x,.19,1.18,.044,.043),(x,.19,1.38,.044,.043),(x,.19,1.415,.032,.032)],edge,'torso')
    for z in [1.21,1.36]:box('bottle restraint',(x,.234,z),(.09,.014,.021),darkcloth,'torso',.003)
    box('bottle stop valve',(x,.19,1.435),(.022,.026,.025),steel,'torso',.003)
tube('functional supply hose',[(.105,.19,1.4),(.18,.13,1.44),(.16,.03,1.51),(.105,-.06,1.54)],.014,darkcloth,'torso')

# Small functional helmet; a skin-valued face window stays readable under the visor brow.
loft('pressure neck seal',[(0,0,1.49,.09,.077),(0,0,1.56,.085,.077),(0,0,1.585,.095,.082)],darkcloth,'head')
for z in [1.515,1.535,1.555]:
    pts=[(.091*math.cos(a),.079*math.sin(a),z) for a in np.linspace(0,math.tau,32)]
    tube('collar narrow bellows rib',pts,.0035,steel,'head')
uv('functional ivory helmet shell',(0,.01,1.71),(.132,.131,.148),ivory,'head',True)
uv('visor opening dark mass',(0,-.103,1.695),(.112,.052,.111),glass,'head')
# Original faceted adult facial construction: jaw, cheek planes, nose and brow.
faceverts=[(-.041,-.15,1.59),(.041,-.15,1.59),(-.068,-.156,1.63),(.068,-.156,1.63),(-.069,-.153,1.70),(.069,-.153,1.70),(-.054,-.145,1.748),(.054,-.145,1.748),(0,-.174,1.681),(0,-.166,1.635)]
facefaces=[(0,1,9),(0,9,2),(1,3,9),(2,9,8,4),(3,5,8,9),(4,8,6),(5,7,8),(6,8,7)]
me=bpy.data.meshes.new('adult cheek and brow planes');me.from_pydata(faceverts,[],facefaces);me.update();ob=bpy.data.objects.new('human face behind visor',me);bpy.context.collection.objects.link(ob);finish(ob,'human face behind visor',skin,'head')
plate('forehead visor shade',[(-.072,-.163,1.737),(-.06,-.163,1.766),(.06,-.163,1.766),(.072,-.163,1.737)],.006,glass,'head')
tube('guarded right brow',[(-.061,-.166,1.711),(-.040,-.171,1.72),(-.016,-.173,1.713)],.004,skinShadow,'head')
tube('guarded left brow',[(.016,-.173,1.713),(.040,-.171,1.72),(.061,-.166,1.711)],.004,skinShadow,'head')
for x in [-.037,.037]:box('shadowed eye plane',(x,-.173,1.702),(.027,.004,.007),skinShadow,'head',.001)
tube('closed tense mouth',[(-.024,-.169,1.631),(0,-.172,1.634),(.024,-.169,1.631)],.0026,skinShadow,'head')
tube('helmet face rim',[(-.092,-.116,1.759),(-.098,-.128,1.69),(-.075,-.143,1.614),(0,-.149,1.578),(.075,-.143,1.614),(.098,-.128,1.69),(.092,-.116,1.759)],.012,ivory,'head')
for x in [-.13,.13]:
    box('helmet flush temple latch',(x,-.007,1.692),(.016,.053,.061),steel,'head',.009)
    box('temple latch central enamel',(x*1.055,-.007,1.692),(.008,.026,.029),ivory,'head',.004)
box('small helmet lamp enclosure',(-.065,-.01,1.848),(.049,.067,.022),steel,'head',.005)
box('unlit helmet lamp lens',(-.065,-.047,1.848),(.031,.006,.011),edge,'head',.001)

for side,sgn in [('R',-1),('L',1)]:
    x=sgn*.115
    leg=[(x,0,.98,.106,.112),(x,0,.87,.108,.108),(x,-.004,.73,.096,.095),(x,0,.59,.078,.081),(x,-.004,.51,.071,.074),(x,0,.44,.073,.073),(x,.006,.31,.067,.068),(x,0,.18,.053,.055),(x,0,.10,.05,.052)]
    weights=[{'thigh.'+side:1}]*3+[{'thigh.'+side:.9,'shin.'+side:.1},{'thigh.'+side:.5,'shin.'+side:.5},{'thigh.'+side:.12,'shin.'+side:.88}]+[{'shin.'+side:1}]*3
    loft('continuous trouser and ankle '+side,leg,fabric,weights=weights)
    # Broad gently tapered thigh shell, no separate cream capsule or exposed knee sphere.
    plate('thin thigh practical guard '+side,[(x-.067,-.084,.64),(x-.082,-.091,.83),(x-.061,-.10,.90),(x+.061,-.10,.90),(x+.082,-.091,.83),(x+.067,-.084,.64)],.013,ivory,'thigh.'+side)
    tube('trouser side seam '+side,[(x+sgn*.093,.004,.86),(x+sgn*.086,.006,.70),(x+sgn*.066,.01,.56)],.0028,darkcloth,'thigh.'+side)
    for z in [.472,.487,.502,.517]:
        tube('knee tailored flex fold '+side,[(x-.06,-.057,z),(x,-.077,z-.006),(x+.06,-.057,z)],.004,darkcloth,'shin.'+side)
    plate('thin shaped knee guard '+side,[(x-.056,-.087,.474),(x-.064,-.088,.516),(x-.038,-.09,.537),(x+.038,-.09,.537),(x+.064,-.088,.516),(x+.056,-.087,.474)],.012,steel,'shin.'+side)
    plate('tapered shin enamel '+side,[(x-.039,-.062,.175),(x-.057,-.076,.35),(x-.047,-.079,.43),(x+.047,-.079,.43),(x+.057,-.076,.35),(x+.039,-.062,.175)],.012,ivory,'shin.'+side,True)
    # Anatomical heel/ankle/toe with a thin dark sole rather than an extended platform.
    footprint=[(-.055,.06),(.055,.06),(.067,-.09),(.049,-.156),(-.049,-.156),(-.067,-.09)]
    for name,zlo,zhi,mat in [('thin rubber outsole',.012,.034,darkcloth),('tailored armored boot',.034,.104,steel)]:
        vs=[(x+dx,y,zlo) for dx,y in footprint]+[(x+dx*.92,y*.94,zhi) for dx,y in footprint]
        n=len(footprint);fs=[tuple(reversed(range(n))),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
        me=bpy.data.meshes.new(name);me.from_pydata(vs,[],fs);me.update();ob=bpy.data.objects.new(name,me);bpy.context.collection.objects.link(ob)
        bevel=ob.modifiers.new('boot soft edge','BEVEL');bevel.width=.009;bevel.segments=2;finish(ob,name+' '+side,mat,'foot.'+side)
    plate('boot shallow toe shell '+side,[(x-.047,-.137,.077),(x-.055,-.072,.122),(x+.055,-.072,.122),(x+.047,-.137,.077)],.009,ivory,'foot.'+side)
    loft('boot fitted ankle cuff '+side,[(x,0,.072,.049,.048),(x,0,.14,.05,.047),(x,0,.17,.049,.046)],darkcloth,'foot.'+side)

    sx=sgn*.235
    sleeve=[(sx,0,1.47,.083,.085),(sgn*.248,0,1.39,.078,.075),(sgn*.253,-.006,1.29,.067,.067),(sgn*.255,-.015,1.205,.061,.063),(sgn*.257,-.027,1.15,.063,.064),(sgn*.26,-.05,1.045,.052,.056),(sgn*.26,-.07,.968,.043,.044)]
    weights=[{'upperarm.'+side:1}]*2+[{'upperarm.'+side:.9,'forearm.'+side:.1},{'upperarm.'+side:.5,'forearm.'+side:.5},{'upperarm.'+side:.12,'forearm.'+side:.88}]+[{'forearm.'+side:1}]*2
    loft('continuous pressure sleeve '+side,sleeve,fabric,weights=weights)
    # Shoulder shell follows the deltoid without a detached ball or square pauldrons.
    plate('low angular shoulder shell '+side,[(sx-.064,-.077,1.385),(sx-.080,-.07,1.465),(sx-.044,-.054,1.503),(sx+.044,-.054,1.503),(sx+.080,-.07,1.465),(sx+.064,-.077,1.385)],.083,ivory,'upperarm.'+side,True)
    for z in [1.18,1.194,1.208]:tube('elbow sleeve fold '+side,[(sgn*.255-.045,-.066,z),(sgn*.255,-.080,z-.005),(sgn*.255+.045,-.066,z)],.0035,darkcloth,'forearm.'+side)
    plate('narrow bracer shell '+side,[(sgn*.26-.035,-.105,.995),(sgn*.26-.048,-.106,1.112),(sgn*.26-.034,-.097,1.147),(sgn*.26+.034,-.097,1.147),(sgn*.26+.048,-.106,1.112),(sgn*.26+.035,-.105,.995)],.011,ivory,'forearm.'+side)
    box('wrist seal '+side,(sgn*.26,-.067,.972),(.082,.085,.027),darkcloth,'forearm.'+side,.008)
    loft('fitted glove palm '+side,[(sgn*.26,-.085,.96,.044,.038),(sgn*.26,-.091,.92,.047,.038),(sgn*.26,-.1,.886,.038,.029)],darkcloth,'hand.'+side)
    for k in range(3):
        fx=sgn*.26+(k-1)*.021
        tube('bent glove finger '+side,[(fx,-.102,.91),(fx,-.125,.892),(fx,-.13,.875)],.011,darkcloth,'hand.'+side)
    tube('glove thumb '+side,[(sgn*.26-sgn*.034,-.09,.94),(sgn*.26-sgn*.047,-.12,.918),(sgn*.26-sgn*.035,-.13,.902)],.013,darkcloth,'hand.'+side)
    plate('glove narrow knuckle guard '+side,[(sgn*.26-.036,-.13,.917),(sgn*.26-.035,-.132,.939),(sgn*.26+.035,-.132,.939),(sgn*.26+.036,-.13,.917)],.008,steel,'hand.'+side)

# Right hand is negative screen X in south view, matching the painted officer.
weapon_start=len(objects)
wx=-.26
box('compact pistol matte receiver',(wx,-.165,.929),(.048,.143,.044),steel,'hand.R',.006)
box('compact pistol narrow slide',(wx,-.18,.956),(.044,.16,.016),edge,'hand.R',.003)
box('compact pistol muzzle',(wx,-.251,.931),(.042,.017,.038),ink,'hand.R',.002)
box('pistol bore',(wx,-.261,.935),(.016,.003,.013),ink,'hand.R',.001)
box('pistol covered grip',(wx,-.111,.898),(.036,.045,.065),darkcloth,'hand.R',.004)
box('pistol front sight',(wx,-.235,.967),(.007,.01,.008),ink,'hand.R',.001)
weapon_objects=objects[weapon_start:]
# Rest-hand socket axis is downward; muzzle rises with actual hand rotation into aim.
grip=Vector((wx,-.1,.918));rot=Vector((0,-1,0)).rotation_difference(Vector((0,-.03,-.09)))
for ob in weapon_objects:
    for vertex in ob.data.vertices:vertex.co=grip+rot@(vertex.co-grip)

# Only outer shell overlaps carry thin contour hulls, not every cloth segment.
for source in outline_objects:
    ob=source.copy();ob.data=source.data.copy();ob.name=source.name+' selected ink edge';bpy.context.collection.objects.link(ob)
    ob.data.materials.clear();ob.data.materials.append(ink)
    for v in ob.data.vertices:v.co+=v.normal*.0025
    for p in ob.data.polygons:p.flip()
