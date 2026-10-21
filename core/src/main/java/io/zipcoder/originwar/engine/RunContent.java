package io.zipcoder.originwar.engine;
import com.github.tommyettinger.random.WhiskerRandom;
import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;
import java.util.*;
final class RunContent{
 static final String CONSTRUCTOR_ID="constructor-machine",CONTROLLER_ID="constructor-controller";
 private RunContent(){}
 static void geometry(GameState s,WhiskerRandom random){
  // Structural service links are optional. Public connections are never removed.
  int shortcuts=0;
  for(int n=0;n<200&&shortcuts<2;n++){int x=2+random.nextInt(s.width-4),y=2+random.nextInt(s.height-4);if(s.tiles[x][y]!=Tile.WALL)continue;
   boolean h=!s.tiles[x-1][y].blocksMovement&&!s.tiles[x+1][y].blocksMovement,v=!s.tiles[x][y-1].blocksMovement&&!s.tiles[x][y+1].blocksMovement;
   if(h==v)continue;int ax=h?x-1:x,ay=h?y:y-1,bx=h?x+1:x,by=h?y:y+1;
   int len=GridRoutes.route(s,ax,ay,bx,by,false).size();if(len<8||len>40)continue;s.tiles[x][y]=Tile.LOCKED_DOOR;shortcuts++;
  }
 }
 static void populate(GameState s){
  MedicineRules.initialize(s);var cells=floors(s);var random=new WhiskerRandom(DeterministicRandom.mix(s.seed^s.deck*93487L));
  // Believable department stock; no automatic entry gifts.
  for(int i=0;i<2;i++){var c=loot(s,cells,random);add(s,GameState.ItemType.CELL,"cells-"+i,"Power cells",c.x,c.y,12);}
  var med=loot(s,cells,random);var names=s.medicineEffects.keySet().stream().sorted().toList();add(s,GameState.ItemType.MEDICINE,"medical",""+names.get(random.nextInt(names.size())),med.x,med.y,1);
  GameState.ItemType gear=switch(s.deck){case 1->GameState.ItemType.DOOR_BRACE;case 2->GameState.ItemType.SERVICE_PROBE;case 3,7->GameState.ItemType.RIFLE;case 4->GameState.ItemType.ARMOR_LIGHT;case 5,9->GameState.ItemType.SCATTERGUN;case 6->GameState.ItemType.ARMOR_HEAVY;default->GameState.ItemType.DOOR_BRACE;};
  var g=loot(s,cells,random);add(s,gear,"equipment",display(gear),g.x,g.y,1);
  int count=s.deck==1?1:2+s.deck/3;
  for(int i=0;i<count;i++){var kind=s.deck>=4&&i==count-1?GameState.ActorKind.ROVER:s.deck>=3&&i==count-2?GameState.ActorKind.ADD:GameState.ActorKind.SIRS;spawn(s,cells,random,kind,"security-"+i);}
  if(s.deck>=2)spawn(s,cells,random,GameState.ActorKind.CAMERA,"camera");
  if(s.deck>=5)spawn(s,cells,random,GameState.ActorKind.TURRET,"turret");
  if(s.deck==8){
   var publicCells=new HashSet<>(StationGenerator.required(s));var machineCells=cells.stream().filter(c->!publicCells.contains(c)).toList();var m=loot(s,machineCells.isEmpty()?cells:machineCells,random);s.actors.add(EnemyTurns.actor(GameState.ActorKind.CONSTRUCTOR,CONSTRUCTOR_ID,m.x,m.y));
   // Controller is a physical maintenance fixture on the guaranteed route, not a person.
   var route=GridRoutes.route(s,s.playerX,s.playerY,s.switchX,s.switchY,false);
   var controllerCells=route.stream().filter(c->!s.tiles[c.x][c.y].blocksMovement&&!special(s.tiles[c.x][c.y])).toList();var c=controllerCells.get(controllerCells.size()/2);s.actors.removeIf(a->a.x==c.x&&a.y==c.y);
   s.actors.add(EnemyTurns.actor(GameState.ActorKind.CONTROLLER,CONTROLLER_ID,c.x,c.y));s.fixtures.add(new GameState.Fixture("controller",c.x,c.y,s.circuits[c.x][c.y]));s.bossPhase="active";s.constructorStock=6;
  }
 }
 static String display(GameState.ItemType t){return switch(t){case SERVICE_PROBE->"Service probe";case DOOR_BRACE->"Door brace";case ARMOR_LIGHT->"Light protective shell";case ARMOR_HEAVY->"Heavy protective shell";default->t.name().toLowerCase(Locale.ROOT);};}
 private static Coord loot(GameState s,List<Coord> cells,WhiskerRandom random){var possible=cells.stream().filter(c->CombatRules.distance(c.x,c.y,s.playerX,s.playerY)>6&&!special(s.tiles[c.x][c.y])&&s.actors.stream().noneMatch(a->a.x==c.x&&a.y==c.y)).toList();return possible.isEmpty()?cells.getLast():possible.get(random.nextInt(possible.size()));}
 private static void spawn(GameState s,List<Coord> cells,WhiskerRandom random,GameState.ActorKind kind,String suffix){var publicCells=new HashSet<>(StationGenerator.required(s));boolean fixed=kind==GameState.ActorKind.CAMERA||kind==GameState.ActorKind.TURRET;var choices=cells.stream().filter(c->CombatRules.distance(c.x,c.y,s.playerX,s.playerY)>=12&&!special(s.tiles[c.x][c.y])&&(!fixed||!publicCells.contains(c))&&s.actors.stream().noneMatch(a->a.x==c.x&&a.y==c.y)).toList();if(choices.isEmpty())return;var c=choices.get(random.nextInt(choices.size()));var a=EnemyTurns.actor(kind,"d"+s.deck+"-"+suffix,c.x,c.y);a.facing=random.nextInt(4);s.actors.add(a);}
 static String medicineFor(GameState s,String effect){return s.medicineEffects.entrySet().stream().filter(e->e.getValue().equals(effect)).map(Map.Entry::getKey).findFirst().orElseThrow();}
 static GameState.Item add(GameState s,GameState.ItemType type,String suffix,String name,int x,int y,int power){var i=new GameState.Item();i.id="d"+s.deck+"-"+suffix;i.type=type;i.name=name;i.x=x;i.y=y;i.power=power;s.floorItems.add(i);return i;}
 static boolean special(Tile t){return t==Tile.SWITCH||t==Tile.HATCH||t==Tile.TERMINAL;}
 static List<Coord> floors(GameState s){var r=new ArrayList<Coord>();for(int x=1;x<s.width-1;x++)for(int y=1;y<s.height-1;y++)if(!s.tiles[x][y].blocksMovement)r.add(Coord.get(x,y));return r;}
}
