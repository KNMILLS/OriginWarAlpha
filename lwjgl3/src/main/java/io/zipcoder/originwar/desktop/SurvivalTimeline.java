package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.*;
import java.util.*;
import java.util.function.Consumer;

/** Presentation clock: never advances simulation, and never interpolates a concealed path. */
public final class SurvivalTimeline {
    public record Position(float x,float y){}
    public record LocomotionPose(String action,float progress){}
    public record MachineMotion(float cycleCells,List<Float> contacts,float startSeconds,float stopSeconds){public MachineMotion(float cells,List<Float> contacts){this(cells,contacts,0,0);}public MachineMotion{if(!Float.isFinite(cycleCells)||cycleCells<=0||!Float.isFinite(startSeconds)||!Float.isFinite(stopSeconds)||startSeconds<0||stopSeconds<0||startSeconds>.5f||stopSeconds>.5f||contacts.stream().anyMatch(p->!Float.isFinite(p)||p<0||p>1))throw new IllegalArgumentException("Invalid machine stride");contacts=List.copyOf(contacts);}}
    private record Track(float fromX,float fromY,float toX,float toY,float start,float span){}
    private record Cue(float at,GameEvent event){}
    private final Map<String,List<Track>> tracks=new HashMap<>();
    private final ArrayDeque<Cue> cues=new ArrayDeque<>();
    private final ArrayList<GameEvent> effects=new ArrayList<>();
    private final Map<GameEvent,Float> effectStarts=new IdentityHashMap<>();
    private float elapsed,duration,gaitStart,activeStartSeconds,settleDuration;private int plantedSteps,continuations;private boolean continueStride;
    private boolean officerFiring,orderedOfficerResponses;private float releaseSeconds=.08f,fireSeconds=.25f;
    private float stepSeconds=.25f,movementSeconds=.25f,cycleCells=2,startSeconds,stopSeconds,turnSeconds;private String turnAction="";private List<Float> contactPhases=List.of();
    private final Map<String,Integer> machineSteps=new HashMap<>();private final Map<String,Float> machineGaits=new HashMap<>();
    private Map<String,Float> machineReleases=Map.of();
    private Map<String,MachineMotion> machineMotions=Map.of();
    private float workContact=-1,workContactAt;
    private static final Set<String> WORK_EVENTS=Set.of("door","switch","circuit","hack","brace_place","brace_recover","pickup","drop","oxygen","medicine","analyze");
    private float machineStart(String id){var value=machineMotions.get(id);return value==null?0:value.startSeconds;}
    private float machineStop(String id){var value=machineMotions.get(id);return value==null?0:value.stopSeconds;}
    public void reset(){tracks.clear();cues.clear();effects.clear();effectStarts.clear();machineSteps.clear();machineGaits.clear();elapsed=duration=gaitStart=0;plantedSteps=continuations=0;officerFiring=continueStride=false;}
    public void officerFireTiming(float release,float span){releaseSeconds=release;fireSeconds=span;}
    public void orderedOfficerResponses(boolean enabled){orderedOfficerResponses=enabled;}
    public void machineReleaseTimings(Map<String,Float> values){if(values.values().stream().anyMatch(v->!Float.isFinite(v)||v<0||v>1))throw new IllegalArgumentException("Invalid machine release");machineReleases=Map.copyOf(values);}
    public void machineMotionTiming(Map<String,MachineMotion> values){machineMotions=Map.copyOf(values);}
    public void officerWorkContact(float seconds){if(!Float.isFinite(seconds)||seconds>1)throw new IllegalArgumentException("Invalid work contact");workContact=seconds;}
    public float workContactAt(){return workContactAt;}
    public void locomotionTiming(float seconds,float cells){if(!Float.isFinite(seconds)||seconds<=0||!Float.isFinite(cells)||cells<=0)throw new IllegalArgumentException("Invalid presentation stride");stepSeconds=movementSeconds=seconds;cycleCells=cells;}
    public void officerMovementSeconds(float seconds){if(!Float.isFinite(seconds)||seconds<=0)throw new IllegalArgumentException("Invalid officer travel duration");movementSeconds=seconds;}
    public void locomotionContacts(List<Float> phases){if(phases.stream().anyMatch(p->!Float.isFinite(p)||p<0||p>1))throw new IllegalArgumentException("Invalid contact phase");contactPhases=List.copyOf(phases);}
    public void locomotionTransitions(float start,float stop){if(!Float.isFinite(start)||!Float.isFinite(stop)||start<0||stop<0||start>.5f||stop>.5f)throw new IllegalArgumentException("Invalid movement transition");startSeconds=start;stopSeconds=stop;}
    public void officerTurnSeconds(float seconds){if(!Float.isFinite(seconds)||seconds<0||seconds>1)throw new IllegalArgumentException("Invalid planted turn");turnSeconds=seconds;}
    public void accept(GameState before,GameState after,TurnResult result){
        boolean continued=continueStride&&result.cost()==1&&before.facing==after.facing;float carriedPhase=gaitPhase();
        float startSeconds=continued?0:this.startSeconds;
        activeStartSeconds=startSeconds;if(continued)continuations++;continueStride=false;
        tracks.clear();cues.clear();effects.clear();effectStarts.clear();elapsed=0;duration=result.cost()*stepSeconds;workContactAt=workContact<0?0:Math.max(0,duration-stepSeconds)+workContact;gaitStart=(plantedSteps/cycleCells)%1;turnAction=before.facing!=after.facing&&turnSeconds>0?"turn_from_"+new String[]{"north","east","south","west"}[before.facing]:"";
        officerFiring=result.events().stream().anyMatch(e->e.type().equals("gun")&&e.sourceId().equals("officer"));if(officerFiring)duration=Math.max(duration,fireSeconds);
        if(before.deck==after.deck){
            if(before.playerX!=after.playerX||before.playerY!=after.playerY){gaitStart=this.startSeconds>0&&Math.abs(cycleCells-.75f)<.001f?(continued?carriedPhase:0):(plantedSteps/cycleCells)%1;plantedSteps++;tracks.put("officer",List.of(new Track(before.playerX,before.playerY,after.playerX,after.playerY,Math.max(0,duration-stepSeconds)+startSeconds+(turnAction.isEmpty()?0:turnSeconds),movementSeconds)));}
            for(var a:after.actors)if(!a.dead&&after.visible[a.x][a.y]){
                var old=before.actors.stream().filter(o->o.id.equals(a.id)&&!o.dead&&before.visible[o.x][o.y]).findFirst().orElse(null);
                if(old!=null&&Math.abs(a.x-old.x)+Math.abs(a.y-old.y)<=result.cost()&&old.x==a.x&&old.y==a.y)continue;
                if(old!=null&&Math.abs(a.x-old.x)+Math.abs(a.y-old.y)<=1)tracks.put(a.id,List.of(new Track(old.x,old.y,a.x,a.y,Math.max(0,duration-stepSeconds)+machineStart(a.id),stepSeconds)));
            }
            for(var a:after.actors)if(!a.dead&&after.visible[a.x][a.y]){var segments=new ArrayList<Track>();for(var e:result.events())if(e.type().equals("robot_step")&&e.visible()&&e.sourceId().equals(a.id)&&e.fromX()>=0&&e.fromY()>=0&&before.visible[e.fromX()][e.fromY()]&&after.visible[e.fromX()][e.fromY()]&&after.visible[e.x()][e.y()])segments.add(new Track(e.fromX(),e.fromY(),e.x(),e.y(),Math.max(0,Math.min(duration-stepSeconds,(e.tick()-before.tick-1)*stepSeconds))+machineStart(a.id),stepSeconds));if(!segments.isEmpty())tracks.put(a.id,List.copyOf(segments));}
        }
        float primaryEnd=duration+(tracks.containsKey("officer")?movementSeconds-stepSeconds+startSeconds+stopSeconds+(turnAction.isEmpty()?0:turnSeconds):0);
        var incomingReleases=new HashMap<Integer,Float>();
        for(var e:result.events()){
            boolean emit=true;
            float slot=Math.max(0,Math.min(duration-stepSeconds,(e.tick()-before.tick-1)*stepSeconds));
            float at=duration==0?0:slot+.12f;
            if(e.type().equals("step")){at=duration;if(tracks.containsKey("officer")){var track=tracks.get("officer").getFirst();at=track.start+track.span;for(float phase:contactPhases)for(int loop=0;loop<Math.ceil(1/cycleCells)+1;loop++){float contact=phase+loop,travel=1/cycleCells;if(contact>gaitStart+.001f&&contact<gaitStart+travel-.001f)cues.add(new Cue(track.start+(contact-gaitStart)/travel*track.span,e));}if(!contactPhases.isEmpty()){float endpoint=(gaitStart+1/cycleCells)%1;emit=contactPhases.stream().anyMatch(p->Math.min(Math.abs(endpoint-p),1-Math.abs(endpoint-p))<.001f);}}}
            if(e.type().equals("robot_step")){at=slot+stepSeconds+machineStart(e.sourceId());var motion=machineMotions.get(e.sourceId());var segments=tracks.get(e.sourceId());if(motion!=null&&segments!=null){for(int n=0;n<segments.size();n++){var t=segments.get(n);if(t.toX!=e.x()||t.toY!=e.y()||Math.abs(t.start-slot-machineStart(e.sourceId()))>.001f)continue;float phase=(machineSteps.getOrDefault(e.sourceId(),0)+n)/motion.cycleCells%1,travel=1/motion.cycleCells;at=t.start+t.span;for(float contact:motion.contacts)for(int loop=0;loop<Math.ceil(travel)+1;loop++){float value=contact+loop;if(value>phase+.001f&&value<phase+travel-.001f)cues.add(new Cue(t.start+(value-phase)/travel*t.span,e));}}}}
            if(e.marker().equals("release"))at=slot+(e.sourceId().equals("officer")?releaseSeconds:.08f);
            if(e.type().equals("gun")&&!e.sourceId().equals("officer")&&machineReleases.containsKey(e.sourceId()))at=slot+machineReleases.get(e.sourceId());
            if(workContact>=0&&WORK_EVENTS.contains(e.type())&&(e.sourceId().isEmpty()||e.sourceId().equals("officer")))at=slot+workContact;
            if(officerFiring&&(!e.sourceId().equals("officer")||Set.of("hit","death").contains(e.type()))&&!e.type().equals("step"))at=Math.max(at,releaseSeconds+.015f);
            if(e.type().equals("speech"))at=duration;
            // The final tick's incoming response follows the completed player translation.
            // Earlier ticks keep their original order, including interrupted careful work.
            if(orderedOfficerResponses&&tracks.containsKey("officer")&&e.tick()==after.tick){
                if((e.type().equals("gun")&&!e.sourceId().equals("officer"))||e.type().equals("explosion"))at=Math.max(at,primaryEnd);
                if(e.sourceId().equals("officer")&&Set.of("hit","death").contains(e.type()))at=Math.max(at,primaryEnd+.08f);
            }
            if(orderedOfficerResponses&&workContact>=0&&!tracks.containsKey("officer")&&e.tick()==after.tick){
                if((e.type().equals("gun")&&!e.sourceId().equals("officer"))||e.type().equals("explosion"))at=Math.max(at,workContactAt);
                if(e.sourceId().equals("officer")&&Set.of("hit","death").contains(e.type()))at=Math.max(at,workContactAt+.08f);
            }
            if((e.type().equals("gun")&&!e.sourceId().equals("officer"))||e.type().equals("explosion"))incomingReleases.merge(e.tick(),at,Math::max);
            if(e.sourceId().equals("officer")&&Set.of("hit","death").contains(e.type())&&incomingReleases.containsKey(e.tick()))at=Math.max(at,incomingReleases.get(e.tick())+.015f);
            if(emit)cues.add(new Cue(at,e));
        }
        var ordered=new ArrayList<>(cues);ordered.sort(Comparator.comparingDouble(Cue::at));cues.clear();cues.addAll(ordered);
        if(tracks.containsKey("officer"))duration=primaryEnd;
        for(String id:tracks.keySet())if(!id.equals("officer")){int count=machineSteps.getOrDefault(id,0);float cells=machineMotions.containsKey(id)?machineMotions.get(id).cycleCells:2;machineGaits.put(id,count/cells%1);machineSteps.put(id,count+tracks.get(id).size());for(var t:tracks.get(id))duration=Math.max(duration,t.start+t.span+machineStop(id));}
    }
    public void advance(float dt,Consumer<GameEvent> deliver){
        elapsed+=dt;while(!cues.isEmpty()&&cues.peek().at<=elapsed){var cue=cues.remove();var e=cue.event;if(e.visible()&&Set.of("gun","hit","impact","explosion","death").contains(e.type())){effects.add(e);effectStarts.put(e,cue.at);}deliver.accept(e);}
        if(elapsed>duration+.18f){effects.clear();effectStarts.clear();}
    }
    public Position position(String id,int x,int y){var list=tracks.get(id);if(list==null)return new Position(x,y);Position p=new Position(x,y);for(var t:list){if(elapsed<t.start)return new Position(t.fromX,t.fromY);float f=Math.min(1,(elapsed-t.start)/t.span);p=new Position(t.fromX+(t.toX-t.fromX)*f,t.fromY+(t.toY-t.fromY)*f);if(f<1)return p;}return p;}
    public boolean moving(String id){var list=tracks.get(id);return list!=null&&list.stream().anyMatch(t->elapsed>=t.start&&elapsed<t.start+t.span);}
    public LocomotionPose machineLocomotionPose(String id){
        var list=tracks.get(id);if(list==null)return new LocomotionPose("idle",0);
        for(var t:list)if(elapsed>=t.start&&elapsed<t.start+t.span)return new LocomotionPose("walk",machineGaitPhase(id));
        float start=machineStart(id),stop=machineStop(id);
        for(int n=0;n<list.size();n++){var t=list.get(n);
            boolean beginsRun=n==0||list.get(n-1).start+list.get(n-1).span<t.start-.0001f;
            boolean endsRun=n==list.size()-1||list.get(n+1).start>t.start+t.span+.0001f;
            if(beginsRun&&start>0&&elapsed>=t.start-start&&elapsed<t.start)return new LocomotionPose("start",(elapsed-t.start+start)/start);
            if(endsRun&&stop>0&&elapsed>=t.start+t.span&&elapsed<t.start+t.span+stop)return new LocomotionPose("stop",(elapsed-t.start-t.span)/stop);
        }return new LocomotionPose("idle",0);
    }
    public LocomotionPose locomotionPose(){var list=tracks.get("officer");if(list==null)return new LocomotionPose("idle",0);var t=list.getFirst();float turnStart=t.start-activeStartSeconds-turnSeconds;if(!turnAction.isEmpty()&&elapsed<turnStart+turnSeconds)return new LocomotionPose(turnAction,Math.max(0,(elapsed-turnStart)/turnSeconds));if(activeStartSeconds>0&&elapsed>=t.start-activeStartSeconds&&elapsed<t.start)return new LocomotionPose("start",(elapsed-t.start+activeStartSeconds)/activeStartSeconds);if(moving("officer"))return new LocomotionPose("walk",gaitPhase());if(stopSeconds>0&&elapsed>=t.start+t.span&&elapsed<t.start+t.span+stopSeconds){int phase=Math.round(gaitPhase()*3)%3;String stop=Math.abs(cycleCells-.75f)<.001f&&phase>0?"stop_phase_"+phase:"stop";return new LocomotionPose(stop,(elapsed-t.start-t.span)/stopSeconds);}return new LocomotionPose("idle",0);}
    /** Omit only an unstarted settle when another straight step is ready to commit.
     * Consequences and machine tails must finish first; no event is discarded. */
    public boolean continueOfficer(int dx,int dy){
        var list=tracks.get("officer");if(list==null||list.size()!=1||stopSeconds<=0||!cues.isEmpty())return false;
        var t=list.getFirst();float end=t.start+t.span;
        if(t.toX-t.fromX!=dx||t.toY-t.fromY!=dy||elapsed<end||elapsed>end+.05f||duration>end+stopSeconds+.0001f)return false;
        for(var entry:tracks.entrySet())if(!entry.getKey().equals("officer"))for(var machine:entry.getValue())if(machine.start+machine.span+machineStop(entry.getKey())>elapsed)return false;
        settleDuration=duration;duration=elapsed;continueStride=true;return true;
    }
    public void cancelContinuation(){if(continueStride)duration=settleDuration;continueStride=false;}
    public int officerContinuations(){return continuations;}
    public float gaitPhase(){var list=tracks.get("officer");if(list==null)return gaitStart;var t=list.getFirst();float f=Math.max(0,Math.min(1,(elapsed-t.start)/t.span));return (gaitStart+f/cycleCells)%1;}
    public float progress(){return duration==0?1:Math.min(1,elapsed/duration);}
    public float elapsed(){return elapsed;}
    public float duration(){return duration;}
    public float cueTime(String type,String source,float fallback){return cues.stream().filter(c->c.event.type().equals(type)&&c.event.sourceId().equals(source)).map(Cue::at).min(Float::compare).orElse(fallback);}
    public float cueTime(GameEvent event,float fallback){return cues.stream().filter(c->c.event==event).map(Cue::at).findFirst().orElse(fallback);}
    /** Add a bounded reaction tail after tracks/cues are fixed; never delays a movement track. */
    public void holdUntil(float seconds){if(!Float.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("Invalid presentation hold");duration=Math.max(duration,seconds);}
    public boolean officerFiring(){return officerFiring&&elapsed<duration;}
    public float machineGaitPhase(String id){var list=tracks.get(id);if(list==null)return 0;float cells=machineMotions.containsKey(id)?machineMotions.get(id).cycleCells:2;int index=0;for(var t:list){if(elapsed<t.start)return (machineGaits.getOrDefault(id,0f)+index/cells)%1;float f=Math.max(0,Math.min(1,(elapsed-t.start)/t.span));if(f<1)return (machineGaits.getOrDefault(id,0f)+(index+f)/cells)%1;index++;}return (machineGaits.getOrDefault(id,0f)+index/cells)%1;}
    public boolean busy(){return elapsed<duration||!cues.isEmpty();}
    public List<GameEvent> effects(){return List.copyOf(effects);}
    public float effectAge(GameEvent event){var at=effectStarts.get(event);return at==null?Float.POSITIVE_INFINITY:Math.max(0,elapsed-at);}
}
