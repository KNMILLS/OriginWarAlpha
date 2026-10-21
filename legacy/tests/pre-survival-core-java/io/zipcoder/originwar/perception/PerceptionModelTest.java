package io.zipcoder.originwar.perception;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.GameState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PerceptionModelTest {
    @Test void oxygenControlsOnsetCriticalAndRecovery() {
        var s = new RunEngine(2016).saveState();
        assertEquals(PerceptionModel.Stage.CLEAR, PerceptionModel.sample(s, 1, false).stage());
        s.oxygenation = 50;
        var warned = PerceptionModel.sample(s, 1, false);
        assertEquals(PerceptionModel.Stage.WARNING, warned.stage());
        s.oxygenation = 15;
        var critical = PerceptionModel.sample(s, 1, false);
        assertEquals(PerceptionModel.Stage.CRITICAL, critical.stage());
        assertTrue(critical.intensity() > warned.intensity());
        assertFalse(critical.impressions().isEmpty());
        s.reserve = 100; // Refill alone does not instantly oxygenate the body.
        assertEquals(PerceptionModel.Stage.CRITICAL, PerceptionModel.sample(s, 1, false).stage());
        s.oxygenation = 70;
        assertTrue(PerceptionModel.sample(s, 1, false).impressions().isEmpty());
    }
    @Test void noWorldOrRandomStateIsChangedByAnyPresentation() {
        var s = new RunEngine(2115).saveState(); s.oxygenation = 10;
        GameState before = s.copy();
        for (int n=0;n<200;n++) PerceptionModel.sample(s,n*.1,n%2==0);
        assertEquals(before.tick,s.tick); assertEquals(before.hp,s.hp); assertEquals(before.reserve,s.reserve);
        assertEquals(before.oxygenation,s.oxygenation); assertEquals(before.combatRng,s.combatRng);
        assertEquals(before.lootRng,s.lootRng); assertEquals(before.medicineRng,s.medicineRng);
        assertEquals(before.perceptionRng,s.perceptionRng); assertEquals(before.actors.size(),s.actors.size());
        assertEquals(before.floorItems.size(),s.floorItems.size()); assertEquals(before.inventory.size(),s.inventory.size());
        assertEquals(before.flags,s.flags); assertEquals(before.journal,s.journal);
        for(int x=0;x<s.width;x++) {assertArrayEquals(before.tiles[x],s.tiles[x]);assertArrayEquals(before.visible[x],s.visible[x]);assertArrayEquals(before.explored[x],s.explored[x]);}
    }
    @Test void replayRestorationKeepsImpressionsAndOnlyUsesVisibleEmptyCells() {
        var s = new RunEngine(4026).saveState(); s.oxygenation=0;
        var a=PerceptionModel.sample(s,3,false); var b=PerceptionModel.sample(s.copy(),3,false);
        assertEquals(a,b);
        for(var p:a.impressions()) {
            assertTrue(s.visible[p.x()][p.y()]); assertFalse(s.tiles[p.x()][p.y()].blocksMovement);
            assertFalse(s.actors.stream().anyMatch(actor->!actor.dead&&actor.x==p.x()&&actor.y==p.y()));
        }
        var reduced=PerceptionModel.sample(s,3,true);
        assertEquals(0,reduced.driftX()); assertEquals(0,reduced.driftY());
        assertEquals(a.impressions(),reduced.impressions()); assertFalse(reduced.status().isEmpty());
        assertThrows(UnsupportedOperationException.class,()->a.impressions().clear());
    }
}
