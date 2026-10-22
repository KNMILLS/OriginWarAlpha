package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class WorkplaceDressingTest {
 @Test void generatedOpeningHasThreeWorkplaceRolesAndOnlyBlockedRecessFootprints(){
  for(long seed:new long[]{0,1,37,420})for(int deck=1;deck<=2;deck++){
   var s=new RunEngine(seed).saveState();s.deck=deck;StationGenerator.generate(s);RunEngine.validateState(s);
   for(String role:List.of("receiving","service","security"))assertTrue(s.fixtures.stream().anyMatch(f->f.kind.equals("art-room:"+role)),"seed="+seed+" deck="+deck+" role="+role);
   for(var f:s.fixtures)if(f.kind.startsWith("art-recess:"))assertEquals(Tile.WALL,s.tiles[f.x][f.y]);else if(f.kind.startsWith("art-furniture:"))assertEquals(Tile.COVER,s.tiles[f.x][f.y]);
  }
 }
 @Test void removingEveryArtDescriptorLeavesActualCommandsEventsAndEveryOtherStateFieldIdentical()throws Exception{
  var decorated=new RunEngine(0);var plainState=decorated.saveState();plainState.fixtures.removeIf(f->WorkplaceDressing.annotation(f.kind));var plain=new RunEngine(plainState);
  String stream;try(var input=getClass().getResourceAsStream("/survival/seed0-noncombat.tsv")){assertNotNull(input);stream=new String(input.readAllBytes(),StandardCharsets.UTF_8);}
  int tested=0;for(String line:stream.split("\\R")){
   if(line.isBlank()||line.startsWith("#"))continue;var fields=line.split("\\t",-1);var command=new PlayerAction(PlayerAction.Type.valueOf(fields[0]),Integer.parseInt(fields[1]),Integer.parseInt(fields[2]),fields.length>3?fields[3]:"");
   assertEquals(decorated.submit(command),plain.submit(command));var state=decorated.saveState();state.fixtures.removeIf(f->WorkplaceDressing.annotation(f.kind));assertEquals(SurvivalObservedPilotTest.hash(state),SurvivalObservedPilotTest.hash(plain.saveState()),"command="+tested);if(++tested==80)break;
  }
  assertEquals(80,tested);
 }
}
