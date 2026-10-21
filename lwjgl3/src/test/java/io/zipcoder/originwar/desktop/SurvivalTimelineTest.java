package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class SurvivalTimelineTest {
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
