package io.zipcoder.originwar.desktop;
import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.PlayerAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ArtProofScenarioTest {
 @Test void stagedRoomUsesValidatedProductionRulesAndPhysicalEquipment(){var s=ArtProofScenario.create();RunEngine.validateState(s);assertTrue(s.flags.contains("art-proof-staging"));assertEquals(1,s.actors.size());var run=new RunEngine(s);assertTrue(run.submit(PlayerAction.move(1,0)).accepted());assertEquals(359,run.saveState().reserve);assertFalse(new RunEngine(2115).saveState().flags.contains("art-proof-staging"));}
 @Test void equipmentAndDarknessHaveRealCosts(){var run=new RunEngine(ArtProofScenario.create());assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.EQUIP,"proof-rifle")).accepted());assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.EQUIP,"proof-heavy")).accepted());assertEquals("rifle",run.saveState().weapon);assertEquals("heavy",run.saveState().armor);assertEquals(2,run.saveState().tick);assertTrue(run.submit(PlayerAction.of(PlayerAction.Type.LAMP)).accepted());assertFalse(run.saveState().lampOn);}
}
