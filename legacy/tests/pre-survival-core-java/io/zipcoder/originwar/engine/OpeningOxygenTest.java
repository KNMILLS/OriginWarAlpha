package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.GameState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpeningOxygenTest {
    @Test void newExpeditionsHaveExplorationHeadroomAndAccessibleSpareTanks() {
        for(long seed:new long[]{0,1,2016,2115,42,999}){
            var s=new RunEngine(seed).saveState();
            assertEquals(900,s.reserve);assertEquals(900,s.maxReserve);assertEquals(100,s.oxygenation);
            var tanks=s.inventory.stream().filter(i->i.type==GameState.ItemType.OXYGEN).toList();
            assertEquals(2,tanks.size());assertEquals(720,tanks.stream().mapToInt(i->i.power*i.quantity).sum());
            assertNotEquals(tanks.get(0).id,tanks.get(1).id);
            assertTrue(s.floorItems.stream().anyMatch(i->i.type==GameState.ItemType.OXYGEN&&i.x==s.playerX&&i.y==s.playerY&&i.power==360));
            assertEquals(225,RunEngine.lowReserveThreshold(s));
        }
    }
    @Test void restoringAnOlderSuitPreservesItsActualResourcesAndWarningBudget() {
        var s=new RunEngine(2016).saveState();s.maxReserve=360;s.reserve=103;
        s.inventory.removeIf(i->i.id.endsWith("spare"));
        var loaded=new RunEngine(s).saveState();
        assertEquals(360,loaded.maxReserve);assertEquals(103,loaded.reserve);
        assertEquals(s.inventory.size(),loaded.inventory.size());assertEquals(90,RunEngine.lowReserveThreshold(loaded));
        assertEquals(s.tick,loaded.tick);assertEquals(s.oxygenation,loaded.oxygenation);
    }
}
