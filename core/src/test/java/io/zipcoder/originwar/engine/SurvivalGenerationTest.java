package io.zipcoder.originwar.engine;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.campaign.Campaign;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SurvivalGenerationTest{
 @Test void tenThousandSeededDecksHavePublicRoutesAndFixedFiniteBudget(){
  int seeds=Integer.parseInt(System.getenv().getOrDefault("ORIGINWAR_GENERATION_SEEDS","1000"));long tanks=0;int maxGap=0;
  for(int seed=0;seed<seeds;seed++){
   var s=new RunEngine(seed).saveState();int total=Arrays.stream(s.routeCosts).sum(),publicCount=0,optionalCount=0,lastSupply=0,cumulative=0;
   for(int deck=1;deck<=10;deck++){
    s.deck=deck;StationGenerator.generate(s);try{RunEngine.validateState(s);}catch(RuntimeException failure){fail("Invalid generated state seed="+seed+" deck="+deck,failure);}assertEquals(s.routeCosts[deck-1],s.publicRouteCost,"cost seed="+seed+" deck="+deck);
    var route=StationGenerator.required(s);assertFalse(route.isEmpty());assertFalse(GridRoutes.route(s,s.playerX,s.playerY,s.switchX,s.switchY,false).isEmpty());assertFalse(GridRoutes.route(s,s.switchX,s.switchY,s.hatchX,s.hatchY,false).isEmpty());
    var routeSet=new HashSet<>(route);for(var a:s.actors)if(a.kind==GameState.ActorKind.CAMERA||a.kind==GameState.ActorKind.TURRET||a.kind==GameState.ActorKind.CONSTRUCTOR)assertFalse(routeSet.contains(com.github.yellowstonegames.grid.Coord.get(a.x,a.y)),"fixed machine blocks public route seed="+seed+" deck="+deck);
    for(var item:s.floorItems)if(item.type==GameState.ItemType.OXYGEN){assertEquals(180,item.power);if(item.id.contains("public")){publicCount++;assertTrue(route.stream().anyMatch(c->c.x==item.x&&c.y==item.y));}else optionalCount++;}
    // Supply gap is measured along the fixed conservative itinerary, not Euclidean distance.
    var supplyIds=new HashSet<String>();int local=0;
    for(var c:route){local+=GridRoutes.entryCost(s.tiles[c.x][c.y]);for(var item:s.floorItems)if(item.id.contains("public")&&item.x==c.x&&item.y==c.y&&supplyIds.add(item.id)){maxGap=Math.max(maxGap,cumulative+local-lastSupply);lastSupply=cumulative+local;}}
    cumulative+=s.publicRouteCost;
   }
   assertEquals(Math.max(0,(int)Math.ceil((total*1.15+60-540)/180.0)),publicCount,"public oxygen seed="+seed);
   assertEquals((int)Math.ceil(total*.15/180),optionalCount,"optional oxygen seed="+seed);tanks+=publicCount+optionalCount;
  }
  assertTrue(maxGap<=300,"Public supplies exceed the300-tick bound: "+maxGap);
  System.out.println("Generated "+(seeds*10)+" decks; fixed oxygen canisters="+tanks+"; conservative first-visit gap="+maxGap);
 }
 @Test void seededOxygenDoesNotReactToCurrentPlayerResources(){var a=new RunEngine(17).saveState();var b=a.copy();a.deck=b.deck=5;a.reserve=0;a.hp=1;b.reserve=360;b.hp=100;StationGenerator.generate(a);StationGenerator.generate(b);assertEquals(a.floorItems.stream().map(i->i.id+":"+i.x+":"+i.y+":"+i.power).toList(),b.floorItems.stream().map(i->i.id+":"+i.x+":"+i.y+":"+i.power).toList());}
 @Test void actualPublicItinerarySurvivesWithoutReadingFightingOrOptionalTools(){for(long seed:new long[]{0,1,6,14,37}){
  var run=new RunEngine(seed);for(int deck=1;deck<=10;deck++){
   var s=run.saveState();s.actors.removeIf(a->a.kind!=GameState.ActorKind.CONTROLLER);run=new RunEngine(s);
   run=walk(run,run.saveState().switchX,run.saveState().switchY);
   assertTrue(run.submit(PlayerAction.at(PlayerAction.Type.INTERACT,run.saveState().switchX,run.saveState().switchY)).accepted());run=maintain(run);
   if(deck==10){run=walk(run,run.saveState().terminalX,run.saveState().terminalY);s=run.saveState();if(!StationSystems.powered(s,s.terminalX,s.terminalY))assertTrue(run.submit(PlayerAction.at(PlayerAction.Type.CIRCUIT,s.terminalX,s.terminalY)).accepted());assertTrue(run.submit(PlayerAction.at(PlayerAction.Type.INTERACT,s.terminalX,s.terminalY)).accepted());}
   run=walk(run,run.saveState().hatchX,run.saveState().hatchY);assertTrue(Campaign.hatchBlock(run.saveState()).isEmpty(),"seed="+seed+" deck="+deck+" "+Campaign.hatchBlock(run.saveState()));
   assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.ADVANCE,"confirm")).accepted());assertFalse(run.saveState().dead,"seed="+seed+" deck="+deck);
  }
  var end=run.saveState();assertTrue(end.victory);assertEquals(100,end.oxygenation);int remaining=end.reserve+end.inventory.stream().filter(i->i.type==GameState.ItemType.OXYGEN).mapToInt(i->i.power).sum();assertTrue(remaining>=60,"seed="+seed+" remaining="+remaining);System.out.println("Public itinerary seed="+seed+" ticks="+end.tick+" remainingOxygen="+remaining+"; geometry-aware, AI removed; not human difficulty evidence.");
 }}
 private static RunEngine walk(RunEngine run,int x,int y){int guard=0;while(run.saveState().playerX!=x||run.saveState().playerY!=y){assertTrue(guard++<3000);run=maintain(run);var s=run.saveState();var path=GridRoutes.route(s,s.playerX,s.playerY,x,y,false);assertFalse(path.isEmpty());var next=path.getFirst();var blocker=CombatRules.at(s,next.x,next.y);if(blocker!=null&&blocker.kind==GameState.ActorKind.CONTROLLER){assertTrue(run.submit(PlayerAction.at(PlayerAction.Type.INTERACT,next.x,next.y)).accepted());continue;}if(s.tiles[next.x][next.y]==Tile.DOOR_CLOSED){assertTrue(run.submit(PlayerAction.at(PlayerAction.Type.INTERACT,next.x,next.y)).accepted());continue;}assertTrue(run.submit(PlayerAction.move(next.x-s.playerX,next.y-s.playerY)).accepted());assertFalse(run.saveState().dead,"Public itinerary suffocated at tick "+run.saveState().tick);}return maintain(run);}
 private static RunEngine maintain(RunEngine run){var s=run.saveState();if(s.reserve<=180){var oxygen=s.inventory.stream().filter(i->i.type==GameState.ItemType.OXYGEN).findFirst();if(oxygen.isPresent())assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.USE,oxygen.get().id)).accepted());}s=run.saveState();for(var item:s.floorItems)if(item.type==GameState.ItemType.OXYGEN&&item.x==s.playerX&&item.y==s.playerY&&item.id.contains("public")){if(EquipmentRules.bulk(run.saveState())<=9)assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.PICKUP,item.id)).accepted());else if(run.saveState().reserve<=180)assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.USE,item.id)).accepted());}return run;}
}
