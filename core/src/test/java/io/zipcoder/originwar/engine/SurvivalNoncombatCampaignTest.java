package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** An ordinary-action feasibility witness. Its author knew geometry; this is not a human playtest. */
class SurvivalNoncombatCampaignTest {
    @Test void publicDescentCanFinishWithoutAttacksOrOptionalEquipment() throws Exception {
        var run=new RunEngine(0);
        var allowed=EnumSet.of(PlayerAction.Type.MOVE,PlayerAction.Type.ADVANCE,PlayerAction.Type.CIRCUIT,
                PlayerAction.Type.INTERACT,PlayerAction.Type.LAMP,PlayerAction.Type.PICKUP,
                PlayerAction.Type.USE,PlayerAction.Type.WAIT);
        String replay;
        try(var input=getClass().getResourceAsStream("/survival/seed0-noncombat.tsv")){
            assertNotNull(input);replay=new String(input.readAllBytes(),StandardCharsets.UTF_8);
        }
        int actions=0;
        for(String line:replay.split("\\R")){
            if(line.isBlank()||line.startsWith("#"))continue;
            String[] fields=line.split("\t",-1);
            var type=PlayerAction.Type.valueOf(fields[0]);
            assertTrue(allowed.contains(type),"This witness must not attack, read, force, probe or brace");
            var before=run.saveState();
            if(type==PlayerAction.Type.PICKUP){
                var item=before.floorItems.stream().filter(i->i.x==before.playerX&&i.y==before.playerY
                        &&(fields[3].isEmpty()||i.id.equals(fields[3]))).findFirst().orElseThrow();
                assertEquals(GameState.ItemType.OXYGEN,item.type,"No optional equipment acquisition");
            }
            var result=run.submit(new PlayerAction(type,Integer.parseInt(fields[1]),Integer.parseInt(fields[2]),fields[3]));
            assertTrue(result.accepted(),"Rejected action "+actions+": "+result.reason());
            assertFalse(run.saveState().dead,"Died at action "+actions);actions++;
        }
        var end=run.saveState();
        assertTrue(end.victory);assertEquals(10,end.deck);assertEquals("controller",end.constructorRoute);
        assertTrue(end.flags.containsAll(Set.of("enforcement-refused","broadcast","alice-sacrificed","pod-sealed","station-destroyed")));
        assertEquals(12,end.batteries,"No gun or powered access expenditure");
        assertEquals(2372,actions);
    }
}
