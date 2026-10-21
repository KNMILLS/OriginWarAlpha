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
        var b=a.copy();b.playerX++;t.accept(a,b,new TurnResult(true,1,List.of(),"",2,true));t.advance(.125f,e->{});assertEquals(.75f,t.gaitPhase(),.001);
    }
    @Test void concealedMachineOriginIsNeverInterpolated(){
        var s=new RunEngine(2016).saveState();var actor=s.actors.getFirst();actor.x=2;actor.y=2;s.visible[2][2]=false;var after=s.copy();after.actors.getFirst().x=3;after.visible[3][2]=true;
        var t=new SurvivalTimeline();t.accept(s,after,new TurnResult(true,1,List.of(),"",1,true));assertEquals(3,t.position(actor.id,3,2).x());assertFalse(t.moving(actor.id));
    }
    @Test void essentialEventsAtTheSameMarkerKeepTheirAuthoredOrder(){
        var s=new RunEngine(2016).saveState();var events=List.of(new GameEvent("speech",s.playerX,s.playerY,"broadcast"),new GameEvent("speech",s.playerX,s.playerY,"sacrifice"));var t=new SurvivalTimeline();var delivered=new ArrayList<GameEvent>();t.accept(s,s,new TurnResult(true,0,events,"",1,true));t.advance(0,delivered::add);assertEquals(events,delivered);
    }
}
