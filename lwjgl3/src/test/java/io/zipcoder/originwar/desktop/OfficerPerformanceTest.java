package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OfficerPerformanceTest {
 @Test void distinctHandlingActionsAndLongerReachHoldAtTheFinalTicksContact(){
  var s=new RunEngine(2115).saveState();var p=new OfficerPerformance();p.workTiming(.75f,.25f);p.accept(s,s,PlayerAction.of(PlayerAction.Type.CIRCUIT),accepted(3),1.75f,.3f,.8f);
  assertEquals("breaker",p.pose(1.25f).action());assertEquals(1f/3,p.pose(1.25f).progress(),.0001);assertEquals(1,p.pose(1.75f).progress());
  p.workTiming(1,.4166667f);p.accept(s,s,PlayerAction.of(PlayerAction.Type.PICKUP),accepted(1),1,.3f,.8f);assertEquals("pickup",p.pose(.42f).action());assertEquals(1,p.seconds());
  assertEquals("drop",OfficerPerformance.workAction(PlayerAction.Type.DROP));assertEquals("equip",OfficerPerformance.workAction(PlayerAction.Type.EQUIP));assertEquals("supply",OfficerPerformance.workAction(PlayerAction.Type.USE));
 }
 @Test void longMaintenanceReachesAndHoldsItsToolThenRecoversOnTheFinalTick(){
  var s=new RunEngine(2115).saveState();var p=new OfficerPerformance();p.workTiming(.5f,1f/6);p.accept(s,s,PlayerAction.of(PlayerAction.Type.HACK),accepted(3),1.5f,.3f,.8f);
  assertEquals(.2f,p.pose(.1f).progress(),.0001);assertEquals(1f/3,p.pose(.8f).progress(),.0001);assertEquals(1f/3,p.pose(1+1f/6).progress(),.0001);assertEquals(.8f,p.pose(1.4f).progress(),.0001);assertEquals(1,p.pose(1.5f).progress());assertEquals(0,s.tick);
 }
 @Test void anEarlyNonfatalReactionEndsBeforeLaterMovementResumes(){
  var before=new RunEngine(2115).saveState();var after=before.copy();after.playerX++;after.hp--;var p=new OfficerPerformance();
  p.accept(before,after,PlayerAction.of(PlayerAction.Type.CAREFUL_MOVE),accepted(2),2,.3f,.8f,.12f,2);
  assertEquals("hit",p.pose(.2f).action());assertEquals("walk",p.pose(.5f).action());assertEquals(2,p.seconds());
 }
 private TurnResult accepted(int cost){return new TurnResult(true,cost,List.of(),"",1,true);}
 @Test void actualWorkAndPaidTurnsAreDistinctFromFreeInspectionAndBlockedMoves(){
  var before=new RunEngine(2115).saveState();var after=before.copy();var p=new OfficerPerformance();
  p.accept(before,after,PlayerAction.of(PlayerAction.Type.CIRCUIT),accepted(1),.5f,.3f,.8f);assertEquals("breaker",p.pose(.2f).action());
  p.accept(before,after,PlayerAction.move(1,0),accepted(1),.5f,.3f,.8f);assertEquals("idle",p.pose(.2f).action());
  after.facing=(before.facing+1)%4;p.accept(before,after,new PlayerAction(PlayerAction.Type.TURN,after.facing,0,""),accepted(1),.5f,.3f,.8f);assertTrue(p.pose(.2f).action().startsWith("turn_from_"));
  p.accept(before,after,PlayerAction.of(PlayerAction.Type.WAIT),new TurnResult(false,0,List.of(),"blocked",0,false),0,.3f,.8f);assertEquals(0,p.seconds());
 }
 @Test void machineHitDoesNotFlinchOfficerAndOwnInjuryFollowsMovement(){
  var before=new RunEngine(2115).saveState();var after=before.copy();after.playerX++;
  var event=new GameEvent("hit",after.playerX+2,after.playerY,"Machine hit.","sirs",0,0,0,1,1,true,true,"contact");
  var result=new TurnResult(true,1,List.of(event),"",1,true);var p=new OfficerPerformance();
  p.accept(before,after,PlayerAction.move(1,0),result,.5f,.3f,.8f);assertEquals(.5f,p.seconds());assertEquals("walk",p.pose(.49f).action());
  after.hp--;p.accept(before,after,PlayerAction.move(1,0),result,.5f,.3f,.8f);assertEquals("walk",p.pose(.49f).action());assertEquals("hit",p.pose(.5f).action());assertEquals(.8f,p.seconds(),.0001);
 }
 @Test void deathIsAuthoritativeWhileItsBoundedPresentationRemainsVisible(){
  var before=new RunEngine(2115).saveState();var after=before.copy();after.hp=0;after.dead=true;var p=new OfficerPerformance();
  p.accept(before,after,PlayerAction.of(PlayerAction.Type.WAIT),accepted(1),.5f,.3f,.8f);
  assertTrue(after.dead);assertTrue(p.deathPending(.6f));assertEquals("death",p.pose(.6f).action());assertTrue(p.deathPending(1.3f));assertEquals(1,p.pose(1.3f).progress(),.0001);assertFalse(p.deathPending(1.66f));assertEquals(1,p.pose(2).progress());assertEquals(100,before.hp);
 }
 @Test void extendingReactionDoesNotDelayAPlantedStepOrConsumeWorldTime(){
  var before=new RunEngine(2115).saveState();var after=before.copy();after.playerX++;after.hp--;after.tick++;
  var t=new SurvivalTimeline();t.locomotionTiming(.5f,1);t.accept(before,after,accepted(1));t.holdUntil(.8f);t.advance(.5f,e->{});
  assertEquals(after.playerX,t.position("officer",after.playerX,after.playerY).x());assertFalse(t.moving("officer"));assertTrue(t.busy());
  t.advance(.3f,e->{});assertFalse(t.busy());assertEquals(1,after.tick);assertEquals(0,before.tick);
 }
 @Test void ninetyAndOneEightyDegreeTurnsRetainActualStartingDirection(){
  var before=new RunEngine(2115).saveState();before.facing=0;var after=before.copy();var p=new OfficerPerformance();
  for(int face:new int[]{1,2}){after.facing=face;p.accept(before,after,new PlayerAction(PlayerAction.Type.TURN,face,0,""),accepted(1),.5f,.3f,.8f);assertEquals("turn_from_north",p.pose(.25f).action());assertEquals(face,after.facing);}
 }
}
