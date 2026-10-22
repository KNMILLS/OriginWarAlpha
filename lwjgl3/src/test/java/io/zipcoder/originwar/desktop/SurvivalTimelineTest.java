package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class SurvivalTimelineTest {
    @Test void finalTickResponsesFollowFloorContactWhileEarlierWorkCanStillBeInterrupted(){
        var a=new RunEngine(2115).saveState();var b=a.copy();b.tick++;
        var gun=new GameEvent("gun",10,9,"Incoming","sirs",10,9,0,b.tick,1,true,true,"contact");var hit=new GameEvent("hit",b.playerX,b.playerY,"Injury","officer",0,0,0,b.tick,1,true,true,"contact");
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,.75f);t.officerWorkContact(10f/24);t.orderedOfficerResponses(true);t.machineReleaseTimings(Map.of("sirs",.18f));t.accept(a,b,new TurnResult(true,1,List.of(gun,hit),"",1,true));
        assertEquals(10f/24,t.cueTime(gun,0),.0001);assertTrue(t.cueTime(hit,0)>t.cueTime(gun,0));
        b.tick=a.tick+3;t.accept(a,b,new TurnResult(true,3,List.of(gun,hit),"",2,true));assertEquals(.18f,t.cueTime(gun,0),.0001);
    }
    @Test void fractionalStrideStopsAtItsActualPoseAndSoundsOnlyAtFootContacts(){
        var a=new RunEngine(2115).saveState();a.facing=1;var t=new SurvivalTimeline();t.locomotionTiming(.5f,.75f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);t.locomotionContacts(List.of(0f,.5f));var heard=new ArrayList<GameEvent>();
        var b=a.copy();b.playerX++;b.tick++;var step=new GameEvent("step",b.playerX,b.playerY,"FLOOR","officer",a.playerX,a.playerY,1,b.tick,1,true,true,"contact");
        t.accept(a,b,new TurnResult(true,1,List.of(step),"",1,true));t.advance(1.35f,heard::add);assertEquals("stop_phase_1",t.locomotionPose().action());assertEquals(2,heard.size());assertTrue(t.continueOfficer(1,0));
        var c=b.copy();c.playerX++;c.tick++;t.accept(b,c,new TurnResult(true,1,List.of(step),"",2,true));assertEquals(1f/3,t.gaitPhase(),.0001);t.advance(1.2f,heard::add);assertEquals("stop_phase_2",t.locomotionPose().action());assertEquals(5,heard.size());
        t.advance(.2f,e->{});var d=c.copy();d.playerX++;t.accept(c,d,new TurnResult(true,1,List.of(),"",3,true));assertEquals(0,t.gaitPhase(),.0001);assertEquals("start",t.locomotionPose().action());
    }
    @Test void readyStraightContinuationPreservesPositionGaitAndPaidStepsWithoutRestart(){
        var a=new RunEngine(2115).saveState();a.facing=1;var b=a.copy();b.playerX++;b.tick++;
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,.5f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);
        t.accept(a,b,new TurnResult(true,1,List.of(),"",1,true));t.advance(1.35f,e->{});
        assertEquals(b.playerX,t.position("officer",b.playerX,b.playerY).x());assertTrue(t.continueOfficer(1,0));assertFalse(t.busy());float phase=t.gaitPhase();
        var c=b.copy();c.playerX++;c.tick++;t.accept(b,c,new TurnResult(true,1,List.of(),"",2,true));
        assertEquals("walk",t.locomotionPose().action());assertEquals(phase,t.gaitPhase(),.0001);assertEquals(b.playerX,t.position("officer",c.playerX,c.playerY).x());assertEquals(1.35f,t.duration(),.0001);assertEquals(2,c.tick);
    }
    @Test void continuationCannotSkipCornersLateSettleOrPendingConsequences(){
        var a=new RunEngine(2115).saveState();var b=a.copy();b.playerX++;b.tick++;
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,.5f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);
        t.accept(a,b,new TurnResult(true,1,List.of(),"",1,true));t.advance(1.35f,e->{});assertFalse(t.continueOfficer(0,1));
        t.advance(.08f,e->{});assertFalse(t.continueOfficer(1,0));
        t.orderedOfficerResponses(true);var gun=new GameEvent("gun",10,9,"Incoming","sirs",10,9,0,b.tick,1,true,true,"contact");
        t.accept(a,b,new TurnResult(true,1,List.of(gun),"",1,true));t.advance(1.35f,e->{});assertFalse(t.continueOfficer(1,0));
    }
    @Test void flashAgeUsesScheduledReleaseAndCannotRestartAfterTheClip(){
        var s=new RunEngine(2115).saveState();var gun=new GameEvent("gun",s.playerX,s.playerY,"Shot","sirs",0,0,0,1,1,true,true,"contact");
        var t=new SurvivalTimeline();t.machineReleaseTimings(Map.of("sirs",.18f));t.accept(s,s,new TurnResult(true,1,List.of(gun),"",1,true));
        assertEquals(Float.POSITIVE_INFINITY,t.effectAge(gun));t.advance(.2f,e->{});assertEquals(.02f,t.effectAge(gun),.0001);
        t.advance(.05f,e->{});assertTrue(t.effectAge(gun)>.05f);t.advance(.5f,e->{});assertEquals(Float.POSITIVE_INFINITY,t.effectAge(gun));
    }
    @Test void consecutiveMachineStepsWalkThroughTheirSharedBoundary(){
        var before=new RunEngine(2115).saveState();var a=before.actors.getFirst();for(int dx=0;dx<=2;dx++)before.visible[a.x+dx][a.y]=true;
        var after=before.copy();var b=after.actors.getFirst();b.x+=2;after.tick+=2;
        var first=new GameEvent("robot_step",a.x+1,a.y,"Move",a.id,a.x,a.y,1,1,1,true,true,"contact");
        var second=new GameEvent("robot_step",a.x+2,a.y,"Move",a.id,a.x+1,a.y,1,2,1,true,true,"contact");
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,1);t.machineMotionTiming(Map.of(a.id,new SurvivalTimeline.MachineMotion(.5f,List.of(0f,.5f),.15f,.15f)));
        t.accept(before,after,new TurnResult(true,2,List.of(first,second),"",1,true));t.advance(.70f,e->{});
        assertEquals("walk",t.machineLocomotionPose(a.id).action());assertEquals(a.x+1.1f,t.position(a.id,b.x,b.y).x(),.0001);assertTrue(t.moving(a.id));
        t.advance(.46f,e->{});assertEquals("stop",t.machineLocomotionPose(a.id).action());assertEquals(b.x,t.position(a.id,b.x,b.y).x());
    }
    @Test void longWorkSoundUsesTheFinalTicksAuthoredContact(){
        var before=new RunEngine(2115).saveState();var after=before.copy();after.tick+=3;var door=new GameEvent("door",before.playerX+1,before.playerY,"Forced open").atTick(after.tick);
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,1);t.officerWorkContact(1f/6);t.accept(before,after,new TurnResult(true,3,List.of(door),"",1,true));assertEquals(1+1f/6,t.cueTime(door,0),.0001);assertEquals(1+1f/6,t.workContactAt(),.0001);
    }
    @Test void machineStartupAndSettleKeepTheChassisPlantedAroundTravel(){
        var before=new RunEngine(2115).saveState();var a=before.actors.getFirst();before.visible[a.x][a.y]=true;var after=before.copy();var b=after.actors.getFirst();b.x++;after.visible[b.x][b.y]=true;var step=new GameEvent("robot_step",b.x,b.y,"Move",a.id,a.x,a.y,1,1,1,true,true,"contact");var t=new SurvivalTimeline();t.locomotionTiming(.5f,1);t.machineMotionTiming(Map.of(a.id,new SurvivalTimeline.MachineMotion(.5f,List.of(0f,.5f),.1f,.1f)));t.accept(before,after,new TurnResult(true,1,List.of(step),"",1,true));t.advance(.05f,e->{});assertEquals("start",t.machineLocomotionPose(a.id).action());assertEquals(a.x,t.position(a.id,b.x,b.y).x());t.advance(.56f,e->{});assertEquals("stop",t.machineLocomotionPose(a.id).action());assertEquals(b.x,t.position(a.id,b.x,b.y).x());assertEquals(.7f,t.duration(),.0001);
    }
    @Test void machineStrideAndContactsUseAuthoredDistanceAtActualTravelSpeed(){
        var before=new RunEngine(2115).saveState();var a=before.actors.getFirst();before.visible[a.x][a.y]=true;var after=before.copy();var b=after.actors.getFirst();b.x++;after.visible[b.x][b.y]=true;
        var step=new GameEvent("robot_step",b.x,b.y,"Move",a.id,a.x,a.y,1,1,1,true,true,"contact");var t=new SurvivalTimeline();t.locomotionTiming(.5f,1);t.machineMotionTiming(Map.of(a.id,new SurvivalTimeline.MachineMotion(.5f,List.of(0f,.5f))));t.accept(before,after,new TurnResult(true,1,List.of(step),"",1,true));var heard=new ArrayList<GameEvent>();t.advance(.125f,heard::add);assertEquals(.5f,t.machineGaitPhase(a.id),.0001);assertEquals(a.x+.25f,t.position(a.id,b.x,b.y).x(),.0001);assertEquals(1,heard.size());t.advance(.375f,heard::add);assertEquals(4,heard.size());assertFalse(t.moving(a.id));assertEquals(0,before.tick);
    }
    @Test void carefulCornerHoldsItsOldFacingUntilTheFinalTickTurn(){
        var before=new RunEngine(2115).saveState();before.facing=2;var after=before.copy();after.playerX++;after.facing=1;after.tick+=2;
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,.5f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);t.officerTurnSeconds(.3f);t.accept(before,after,new TurnResult(true,2,List.of(),"",1,true));t.advance(.2f,e->{});assertEquals("turn_from_south",t.locomotionPose().action());assertEquals(0,t.locomotionPose().progress());assertFalse(t.moving("officer"));t.advance(.45f,e->{});assertEquals(.5f,t.locomotionPose().progress(),.0001);
    }
    @Test void standingOfficerImpactCannotPrecedeTheIncomingAuthoredRelease(){
        var s=new RunEngine(2115).saveState();var gun=new GameEvent("gun",s.playerX+2,s.playerY,"Incoming","sirs",0,0,3,1,1,true,true,"contact");var hit=new GameEvent("hit",s.playerX,s.playerY,"Injury","officer",0,0,0,1,1,true,true,"contact");
        var t=new SurvivalTimeline();t.machineReleaseTimings(Map.of("sirs",.18f));t.accept(s,s,new TurnResult(true,1,List.of(gun,hit),"",1,true));assertEquals(.18f,t.cueTime(gun,0),.0001);assertTrue(t.cueTime(hit,0)>t.cueTime(gun,0));
    }
    @Test void cornerPlantsAndTurnsBeforeStartupWithoutAddingAPaidTick(){
        var before=new RunEngine(2115).saveState();before.facing=2;var after=before.copy();after.playerX++;after.facing=1;after.tick++;
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,.5f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);t.officerTurnSeconds(.3f);t.accept(before,after,new TurnResult(true,1,List.of(),"",1,true));
        t.advance(.15f,e->{});assertEquals("turn_from_south",t.locomotionPose().action());assertEquals(.5f,t.locomotionPose().progress(),.0001);assertEquals(before.playerX,t.position("officer",after.playerX,after.playerY).x());
        t.advance(.16f,e->{});assertEquals("start",t.locomotionPose().action());t.advance(.15f,e->{});assertEquals("walk",t.locomotionPose().action());assertEquals(1.8f,t.duration(),.0001);assertEquals(1,after.tick);assertEquals(0,before.tick);
    }
    @Test void guardedTravelDoesNotSlowFiringOrOtherPaidCommitments(){
        var before=new RunEngine(2115).saveState();var t=new SurvivalTimeline();t.locomotionTiming(.5f,.5f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);
        var after=before.copy();after.playerX++;t.accept(before,after,new TurnResult(true,1,List.of(),"",1,true));assertEquals(1.5f,t.duration(),.0001);
        t.accept(before,before,new TurnResult(true,1,List.of(),"",2,true));assertEquals(.5f,t.duration(),.0001);assertEquals("idle",t.locomotionPose().action());
    }
    @Test void incomingFinalTickImpactAndReactionShareAClockAfterThePlantedEndpoint(){
        var before=new RunEngine(2115).saveState();var after=before.copy();after.playerX++;after.hp--;after.tick++;
        var gun=new GameEvent("gun",10,9,"Machine fires.","sirs",10,9,0,after.tick,1,true,true,"contact");
        var hit=new GameEvent("hit",after.playerX,after.playerY,"Impact","officer",after.playerX,after.playerY,0,after.tick,1,true,true,"contact");
        var t=new SurvivalTimeline();t.locomotionTiming(.5f,.5f);t.officerMovementSeconds(1.2f);t.locomotionTransitions(.15f,.15f);t.orderedOfficerResponses(true);t.accept(before,after,new TurnResult(true,1,List.of(gun,hit),"",1,true));
        assertEquals(1.5f,t.cueTime("gun","sirs",0),.0001);assertEquals(1.58f,t.cueTime("hit","officer",0),.0001);
        var p=new OfficerPerformance();p.accept(before,after,PlayerAction.move(1,0),new TurnResult(true,1,List.of(gun,hit),"",1,true),t.duration(),.3f,.8f,t.cueTime("hit","officer",0),2);t.holdUntil(p.seconds());
        var heard=new ArrayList<GameEvent>();t.advance(1.57f,heard::add);assertEquals(List.of(gun),heard);assertFalse(t.moving("officer"));assertEquals("walk",p.pose(t.elapsed()).action());
        t.advance(.02f,heard::add);assertEquals(List.of(gun,hit),heard);assertEquals("hit",p.pose(t.elapsed()).action());
    }
    @Test void taggedOwnInjuryCannotInterruptItsCommittedShotBeforeRelease(){
        var s=new RunEngine(2115).saveState();var gun=new GameEvent("gun",s.playerX,s.playerY,"Shot","officer",0,0,0,1,1,true,true,"release");var hit=new GameEvent("hit",s.playerX,s.playerY,"Impact","officer",0,0,0,1,1,true,true,"contact");
        var t=new SurvivalTimeline();t.officerFireTiming(.14f,.5f);t.accept(s,s,new TurnResult(true,1,List.of(gun,hit),"",1,true));assertTrue(t.cueTime("hit","officer",0)>t.cueTime("gun","officer",0));
    }
    @Test void halfCellGaitKeepsStartupAndSettleStillAndOrdersFourContacts(){
        var before=new RunEngine(2115).saveState();var after=before.copy();after.playerX++;after.tick++;
        var event=new GameEvent("step",after.playerX,after.playerY,"FLOOR","officer",before.playerX,before.playerY,1,1,1,true,true,"contact");
        var t=new SurvivalTimeline();t.locomotionTiming(1.2f,.5f);t.locomotionTransitions(.15f,.15f);t.locomotionContacts(List.of(0f,.5f));t.accept(before,after,new TurnResult(true,1,List.of(event),"",1,true));
        var heard=new ArrayList<GameEvent>();t.advance(.1f,heard::add);assertEquals("start",t.locomotionPose().action());assertEquals(before.playerX,t.position("officer",after.playerX,after.playerY).x());
        t.advance(.65f,heard::add);assertEquals("walk",t.locomotionPose().action());assertEquals(before.playerX+.5f,t.position("officer",after.playerX,after.playerY).x(),.0001);assertEquals(2,heard.size());
        t.advance(.61f,heard::add);assertEquals("stop",t.locomotionPose().action());assertEquals(after.playerX,t.position("officer",after.playerX,after.playerY).x());assertEquals(4,heard.size());
        t.advance(.15f,heard::add);assertEquals("idle",t.locomotionPose().action());assertFalse(t.busy());assertEquals(1,after.tick);assertEquals(0,before.tick);
    }
    @Test void proofStrideUsesRealAuthoredContactsWithoutChangingWorldTicks(){var before=new RunEngine(2115).saveState();var after=before.copy();after.playerX++;after.tick++;var event=new GameEvent("step",after.playerX,after.playerY,"FLOOR","officer",before.playerX,before.playerY,1,1,1,true,true,"contact");var t=new SurvivalTimeline();t.locomotionTiming(.5f,1);t.locomotionContacts(List.of(0f,.5f));t.accept(before,after,new TurnResult(true,1,List.of(event),"",1,true));var heard=new ArrayList<GameEvent>();t.advance(.25f,heard::add);assertEquals(before.playerX+.5f,t.position("officer",after.playerX,after.playerY).x(),.001);assertEquals(1,heard.size());t.advance(.25f,heard::add);assertEquals(2,heard.size());assertEquals(0,before.tick);assertEquals(1,after.tick);assertFalse(t.busy());}
    @Test void carefulCrossingHoldsAtSourceUntilItsFinalTickThenPlantsAtContact(){
        var before=new RunEngine(2016).saveState();var after=before.copy();after.playerX++;after.tick+=2;
        var step=new GameEvent("step",after.playerX,after.playerY,"FLOOR","officer",before.playerX,before.playerY,1,after.tick,1,true,true,"contact");
        var result=new TurnResult(true,2,List.of(step),"",1,true);var timeline=new SurvivalTimeline();var delivered=new ArrayList<GameEvent>();timeline.accept(before,after,result);
        timeline.advance(.20f,delivered::add);assertEquals(before.playerX,timeline.position("officer",after.playerX,after.playerY).x());assertTrue(delivered.isEmpty());
        timeline.advance(.175f,delivered::add);assertEquals(before.playerX+.5f,timeline.position("officer",after.playerX,after.playerY).x(),.001);assertTrue(delivered.isEmpty());
        timeline.advance(.125f,delivered::add);assertEquals(after.playerX,timeline.position("officer",after.playerX,after.playerY).x());assertEquals(List.of(step),delivered);
        assertEquals(0,before.tick);assertEquals(360,before.reserve);
    }
    @Test void consecutiveCellsUseOppositeHalvesOfTheSameGait(){
        var s=new RunEngine(2016).saveState();var t=new SurvivalTimeline();var a=s.copy();a.playerX++;t.accept(s,a,new TurnResult(true,1,List.of(),"",1,true));t.advance(.125f,e->{});assertEquals(.25f,t.gaitPhase(),.001);t.advance(.125f,e->{});
        var b=a.copy();b.playerX++;t.accept(a,b,new TurnResult(true,1,List.of(),"",2,true));t.advance(.125f,e->{});assertEquals(.75f,t.gaitPhase(),.001);t.advance(.125f,e->{});assertEquals(0,t.gaitPhase(),.001);t.accept(b,b,new TurnResult(true,1,List.of(),"",3,true));assertEquals(0,t.gaitPhase(),.001);
    }
    @Test void concealedMachineOriginIsNeverInterpolated(){
        var s=new RunEngine(2016).saveState();var actor=s.actors.getFirst();actor.x=2;actor.y=2;s.visible[2][2]=false;var after=s.copy();after.actors.getFirst().x=3;after.visible[3][2]=true;
        var t=new SurvivalTimeline();t.accept(s,after,new TurnResult(true,1,List.of(),"",1,true));assertEquals(3,t.position(actor.id,3,2).x());assertFalse(t.moving(actor.id));
    }
    @Test void essentialEventsAtTheSameMarkerKeepTheirAuthoredOrder(){
        var s=new RunEngine(2016).saveState();var events=List.of(new GameEvent("speech",s.playerX,s.playerY,"broadcast"),new GameEvent("speech",s.playerX,s.playerY,"sacrifice"));var t=new SurvivalTimeline();var delivered=new ArrayList<GameEvent>();t.accept(s,s,new TurnResult(true,0,events,"",1,true));t.advance(0,delivered::add);assertEquals(events,delivered);
    }
    @Test void paintedReleasePrecedesTargetContactAndCompletesRecovery(){var s=new RunEngine(2016).saveState();var gun=new GameEvent("gun",s.playerX,s.playerY,"Fired pistol.","officer",s.playerX,s.playerY,1,1,1,true,true,"release");var hit=new GameEvent("hit",s.playerX+1,s.playerY,"Machine hit.","machine",s.playerX+1,s.playerY,3,1,1,true,true,"contact");var t=new SurvivalTimeline();t.officerFireTiming(.14f,.28f);t.accept(s,s,new TurnResult(true,1,List.of(hit,gun),"",1,true));var delivered=new ArrayList<GameEvent>();t.advance(.13f,delivered::add);assertTrue(delivered.isEmpty());t.advance(.011f,delivered::add);assertEquals(List.of(gun),delivered);t.advance(.02f,delivered::add);assertEquals(List.of(gun,hit),delivered);assertTrue(t.busy());t.advance(.12f,delivered::add);assertFalse(t.busy());assertFalse(t.officerFiring());}
    @Test void missExplosionAndDeathCannotPrecedeAuthoredMuzzleRelease(){var s=new RunEngine(2016).saveState();var gun=new GameEvent("gun",s.playerX,s.playerY,"Fired pistol.","officer",s.playerX,s.playerY,1,1,1,true,true,"release");var consequences=List.of(new GameEvent("impact",2,2,"Miss."),new GameEvent("explosion",2,2,"Detonation."),new GameEvent("death",s.playerX,s.playerY,"Life signs lost."));var events=new ArrayList<GameEvent>();events.add(gun);events.addAll(consequences);var t=new SurvivalTimeline();t.officerFireTiming(.14f,.28f);t.accept(s,s,new TurnResult(true,1,events,"",1,true));var delivered=new ArrayList<GameEvent>();t.advance(.13f,delivered::add);assertTrue(delivered.isEmpty());t.advance(.011f,delivered::add);assertEquals(List.of(gun),delivered);t.advance(.02f,delivered::add);assertEquals(events,delivered);}
}
