package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.engine.StationSystems;
import java.util.*;

/** Finite mechanical performances, latched only from legitimately observed committed facts. */
public final class StationPerformance {
 public record Pose(String action,float progress,int facing) {}
 public interface Timing {
  float seconds(String actor,String action,int facing,float fallback);
  float marker(String actor,String action,int facing,String marker,float fallback);
 }
 private record Beat(String action,float start,float span,int facing,boolean terminal) {}
 private record Door(String action,float start,float span) {}
 private final Map<String,List<Beat>> machines=new HashMap<>();
 private final Map<String,Pose> previous=new HashMap<>();
 private final Map<Integer,Door> doors=new HashMap<>();
 public void reset(){machines.clear();previous.clear();doors.clear();}
 public void accept(GameState before,GameState after,TurnResult result,SurvivalTimeline timeline,Timing timing){
  reset();if(before.deck!=after.deck)return;float primarySpan=timeline.duration();
  for(var a:after.actors){
   if(!after.visible[a.x][a.y])continue;
   var old=before.actors.stream().filter(o->o.id.equals(a.id)&&before.visible[o.x][o.y]).findFirst().orElse(null);
   if(old==null)continue;
   String actor=a.kind.name().toLowerCase(Locale.ROOT);
   previous.put(a.id,new Pose(status(before,old),1,old.facing));
   var beats=new ArrayList<Beat>();
   for(var e:result.events()){
    if(!e.visible()||!e.sourceId().equals(a.id))continue;
    String action=switch(e.type()){case "gun"->"fire";case "aim"->"anticipate";case "hit"->a.dead?"destroyed":"hit";default->"";};
    if(action.isEmpty())continue;
    int face=e.facing()>=0&&e.facing()<4?e.facing():a.facing;
    float at=timeline.cueTime(e,0),span=timing.seconds(actor,action,face,.25f);
    if(action.equals("fire"))at=Math.max(0,at-timing.marker(actor,action,face,"release",0));
    beats.add(new Beat(action,at,span,face,action.equals("destroyed")||action.equals("anticipate")&&a.aiming));
    timeline.holdUntil(at+span);
   }
   beats.sort(Comparator.comparingDouble(Beat::start));machines.put(a.id,List.copyOf(beats));
  }
  for(int x=0;x<after.width;x++)for(int y=0;y<after.height;y++){
   if(!before.visible[x][y]||!after.visible[x][y])continue;
   var old=before.tiles[x][y];var now=after.tiles[x][y];
   String action=now==Tile.DOOR_OPEN&&(old==Tile.DOOR_CLOSED||old==Tile.LOCKED_DOOR)?"opening":old==Tile.DOOR_OPEN&&now==Tile.DOOR_CLOSED?"closing":"";
   if(action.isEmpty())continue;
   float at=Math.max(0,primarySpan-.5f);
   for(var e:result.events())if(e.visible()&&e.type().equals("door")&&((e.x()==x&&e.y()==y)||(e.fromX()==x&&e.fromY()==y))){at=timeline.cueTime(e,at);break;}
   int face=doorFacing(after,x,y);float span=timing.seconds("door",action,face,.3f);
   doors.put(y*after.width+x,new Door(action,at,span));timeline.holdUntil(at+span);
  }
 }
 public Pose machine(GameState.Actor actor,float elapsed,String fallback){
  var beats=machines.get(actor.id);if(beats==null)return new Pose(fallback,1,actor.facing);
  Pose base=new Pose(fallback,1,actor.facing);
  if(!beats.isEmpty()&&elapsed<beats.getFirst().start)return previous.getOrDefault(actor.id,base);
  for(var beat:beats)if(elapsed>=beat.start&&(elapsed<beat.start+beat.span||beat.terminal))base=new Pose(beat.action,phase(elapsed-beat.start,beat.span),beat.facing);
  return base;
 }
 public Pose door(GameState state,int x,int y,float elapsed,String fallback){var d=doors.get(y*state.width+x);int face=doorFacing(state,x,y);if(d==null||elapsed>=d.start+d.span)return new Pose(fallback,1,face);if(elapsed<d.start)return new Pose(d.action.equals("opening")?"closed":"open",1,face);return new Pose(d.action,phase(elapsed-d.start,d.span),face);}
 public static String status(GameState s,GameState.Actor a){boolean fixed=Set.of(GameState.ActorKind.CAMERA,GameState.ActorKind.TURRET,GameState.ActorKind.CONSTRUCTOR,GameState.ActorKind.CONTROLLER).contains(a.kind);return a.dead?(a.kind==GameState.ActorKind.CAMERA?"disabled":"destroyed"):s.flags.contains("disabled:"+a.id)?"disabled":fixed&&!StationSystems.powered(s,a.x,a.y)?"off":a.kind==GameState.ActorKind.CAMERA?(a.awareness==GameState.Awareness.PURSUE?"aim":"on"):a.aiming?"anticipate":"idle";}
 public static int doorFacing(GameState s,int x,int y){if(x<=0||x>=s.width-1)return 0;var left=StationSystems.knownTile(s,x-1,y);var right=StationSystems.knownTile(s,x+1,y);if(left!=null&&!left.blocksMovement||right!=null&&!right.blocksMovement)return 1;return 0;}
 private float phase(float time,float span){return span<=0?1:Math.max(0,Math.min(1,time/span));}
}
