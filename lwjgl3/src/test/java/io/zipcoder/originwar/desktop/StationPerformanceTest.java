package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class StationPerformanceTest {
 private final StationPerformance.Timing timing=new StationPerformance.Timing(){public float seconds(String actor,String action,int face,float fallback){return action.equals("destroyed")?.6f:.3f;}public float marker(String actor,String action,int face,String marker,float fallback){return .1f;}};
 @Test void unobservedNeighborsCannotSelectTheDoorBearing(){var s=new RunEngine(8).saveState();int x=s.playerX,y=s.playerY;s.visible[x-1][y]=s.visible[x+1][y]=false;s.rememberedTiles[x-1][y]=s.rememberedTiles[x+1][y]=null;int facing=StationPerformance.doorFacing(s,x,y);s.tiles[x-1][y]=Tile.WALL;s.tiles[x+1][y]=Tile.FLOOR;assertEquals(facing,StationPerformance.doorFacing(s,x,y));}
 @Test void unpoweredCameraKeepsItsDarkPoseUntilObservedImpact(){
  var before=new RunEngine(8).saveState();var a=before.actors.getFirst();a.kind=GameState.ActorKind.CAMERA;before.visible[a.x][a.y]=true;before.circuitPower[before.circuits[a.x][a.y]]=false;var after=before.copy();after.actors.getFirst().hp--;
  var hit=new GameEvent("hit",a.x,a.y,"Hit",a.id,a.x,a.y,1,1,1,true,true,"contact");var result=new TurnResult(true,1,List.of(hit),"",1,true);var timeline=new SurvivalTimeline();timeline.accept(before,after,result);var view=new StationPerformance();view.accept(before,after,result,timeline,timing);assertEquals("off",view.machine(after.actors.getFirst(),.05f,"off").action());
 }
 @Test void observedMachineReleaseMatchesItsAuthoredMarkerAndDestructionSettles(){
  var before=new RunEngine(8).saveState();var a=before.actors.getFirst();before.visible[a.x][a.y]=true;a.aiming=true;
  var after=before.copy();var end=after.actors.getFirst();end.dead=true;end.hp=0;end.aiming=false;
  var gun=new GameEvent("gun",a.x,a.y,"Shot",a.id,a.x,a.y,1,1,1,true,true,"contact");var hit=new GameEvent("hit",a.x,a.y,"Hit",a.id,a.x,a.y,1,1,1,true,true,"contact");
  var timeline=new SurvivalTimeline();timeline.machineReleaseTimings(Map.of(a.id,.1f));timeline.accept(before,after,new TurnResult(true,1,List.of(gun,hit),"",1,true));var view=new StationPerformance();view.accept(before,after,new TurnResult(true,1,List.of(gun,hit),"",1,true),timeline,timing);
  var fire=view.machine(end,.05f,"destroyed");assertEquals("fire",fire.action());assertEquals(1,fire.facing());assertEquals(.05f/.3f,fire.progress(),.0001);
  assertEquals("destroyed",view.machine(end,.13f,"destroyed").action());assertTrue(timeline.duration()>=.72f);
  assertEquals(1,view.machine(end,10,"destroyed").progress());assertFalse(a.dead);assertEquals(0,before.tick);
 }
 @Test void newlyRevealedMachineNeverReplaysConcealedDamageOrPath(){
  var before=new RunEngine(8).saveState();var a=before.actors.getFirst();before.visible[a.x][a.y]=false;var after=before.copy();after.visible[a.x][a.y]=true;
  var event=new GameEvent("hit",a.x,a.y,"Hit",a.id,a.x,a.y,1,1,1,true,true,"contact");var result=new TurnResult(true,1,List.of(event),"",1,true);var timeline=new SurvivalTimeline();timeline.accept(before,after,result);var view=new StationPerformance();view.accept(before,after,result,timeline,timing);
  assertEquals("off",view.machine(after.actors.getFirst(),.13f,"off").action());
 }
 @Test void onlyObservedDoorTransitionAnimatesAndItFinishesWithoutRepeating(){
  var before=new RunEngine(8).saveState();int x=before.playerX,y=before.playerY;before.tiles[x][y]=Tile.DOOR_CLOSED;before.visible[x][y]=true;var after=before.copy();after.tiles[x][y]=Tile.DOOR_OPEN;
  var event=new GameEvent("door",x,y,"Open");var result=new TurnResult(true,1,List.of(event),"",1,true);var timeline=new SurvivalTimeline();timeline.accept(before,after,result);var view=new StationPerformance();view.accept(before,after,result,timeline,timing);
  assertEquals("closed",view.door(after,x,y,.05f,"open").action());assertEquals("opening",view.door(after,x,y,.2f,"open").action());assertEquals("open",view.door(after,x,y,1,"open").action());
  before.visible[x][y]=false;view.accept(before,after,result,timeline,timing);assertEquals("open",view.door(after,x,y,.05f,"open").action());
 }
}
