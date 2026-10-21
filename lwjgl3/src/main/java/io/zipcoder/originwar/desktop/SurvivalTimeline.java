package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.*;
import java.util.*;
import java.util.function.Consumer;

/** Presentation clock: never advances simulation, and never interpolates a concealed path. */
public final class SurvivalTimeline {
    public record Position(float x,float y){}
    private record Track(float fromX,float fromY,float toX,float toY,float start,float span){}
    private record Cue(float at,GameEvent event){}
    private final Map<String,List<Track>> tracks=new HashMap<>();
    private final ArrayDeque<Cue> cues=new ArrayDeque<>();
    private final ArrayList<GameEvent> effects=new ArrayList<>();
    private float elapsed,duration,gaitStart;private int plantedSteps;
    public void reset(){tracks.clear();cues.clear();effects.clear();elapsed=duration=gaitStart=0;plantedSteps=0;}
    public void accept(GameState before,GameState after,TurnResult result){
        tracks.clear();cues.clear();effects.clear();elapsed=0;duration=result.cost()*.25f;
        if(before.deck==after.deck){
            if(before.playerX!=after.playerX||before.playerY!=after.playerY){gaitStart=(plantedSteps++%2)*.5f;tracks.put("officer",List.of(new Track(before.playerX,before.playerY,after.playerX,after.playerY,Math.max(0,duration-.25f),.25f)));}
            for(var a:after.actors)if(!a.dead&&after.visible[a.x][a.y]){
                var old=before.actors.stream().filter(o->o.id.equals(a.id)&&!o.dead&&before.visible[o.x][o.y]).findFirst().orElse(null);
                if(old!=null&&Math.abs(a.x-old.x)+Math.abs(a.y-old.y)<=result.cost()&&old.x==a.x&&old.y==a.y)continue;
                if(old!=null&&Math.abs(a.x-old.x)+Math.abs(a.y-old.y)<=1)tracks.put(a.id,List.of(new Track(old.x,old.y,a.x,a.y,Math.max(0,duration-.25f),.25f)));
            }
            for(var a:after.actors)if(!a.dead&&after.visible[a.x][a.y]){var segments=new ArrayList<Track>();for(var e:result.events())if(e.type().equals("robot_step")&&e.visible()&&e.sourceId().equals(a.id)&&e.fromX()>=0&&e.fromY()>=0&&before.visible[e.fromX()][e.fromY()]&&after.visible[e.fromX()][e.fromY()]&&after.visible[e.x()][e.y()])segments.add(new Track(e.fromX(),e.fromY(),e.x(),e.y(),Math.max(0,Math.min(duration-.25f,(e.tick()-before.tick-1)*.25f)),.25f));if(!segments.isEmpty())tracks.put(a.id,List.copyOf(segments));}
        }
        for(var e:result.events()){
            float slot=Math.max(0,Math.min(duration-.25f,(e.tick()-before.tick-1)*.25f));
            float at=duration==0?0:slot+.12f;
            if(e.type().equals("step"))at=duration;
            if(e.type().equals("robot_step"))at=slot+.25f;
            if(e.marker().equals("release"))at=slot+.08f;
            if(e.type().equals("speech"))at=duration;
            cues.add(new Cue(at,e));
        }
        var ordered=new ArrayList<>(cues);ordered.sort(Comparator.comparingDouble(Cue::at));cues.clear();cues.addAll(ordered);
    }
    public void advance(float dt,Consumer<GameEvent> deliver){
        elapsed+=dt;while(!cues.isEmpty()&&cues.peek().at<=elapsed){var e=cues.remove().event;if(e.visible()&&Set.of("gun","hit","impact","explosion","death").contains(e.type()))effects.add(e);deliver.accept(e);}
        if(elapsed>duration+.18f)effects.clear();
    }
    public Position position(String id,int x,int y){var list=tracks.get(id);if(list==null)return new Position(x,y);Position p=new Position(x,y);for(var t:list){if(elapsed<t.start)return new Position(t.fromX,t.fromY);float f=Math.min(1,(elapsed-t.start)/t.span);p=new Position(t.fromX+(t.toX-t.fromX)*f,t.fromY+(t.toY-t.fromY)*f);if(f<1)return p;}return p;}
    public boolean moving(String id){var list=tracks.get(id);return list!=null&&list.stream().anyMatch(t->elapsed>=t.start&&elapsed<t.start+t.span);}
    public float gaitPhase(){var list=tracks.get("officer");if(list==null)return gaitStart;var t=list.getFirst();float f=Math.max(0,Math.min(1,(elapsed-t.start)/t.span));return (gaitStart+f*.5f)%1;}
    public float progress(){return duration==0?1:Math.min(1,elapsed/duration);}
    public boolean busy(){return elapsed<duration||!cues.isEmpty();}
    public List<GameEvent> effects(){return List.copyOf(effects);}
}
