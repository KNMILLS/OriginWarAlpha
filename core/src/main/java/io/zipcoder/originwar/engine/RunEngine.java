package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.campaign.*;
import java.util.*;

/** Sole simulation writer. Action descriptions are pure; effects complete at their committed tick. */
public final class RunEngine {
 public static final int LAST_DECK=10,LOW_RESERVE=90,MAX_INVENTORY=12,MAX_INVENTORY_RECORDS=64,MAX_FLOOR_ITEMS=512;
 public static final int OXYGENATION_LOSS=5,OXYGENATION_RECOVERY=4,SUFFOCATION_DAMAGE=4,STARTING_RESERVE=360,STARTER_TANK=180;
 private final GameState state;
 public RunEngine(long seed){
  state=new GameState();state.seed=seed;state.combatRng=DeterministicRandom.mix(seed^0x14A76CB1L);state.lootRng=DeterministicRandom.mix(seed^0x3B719A62L);state.medicineRng=DeterministicRandom.mix(seed^0x5CB41F83L);state.perceptionRng=DeterministicRandom.mix(seed^0x7DA65294L);
  MedicineRules.initialize(state);state.inventory.add(StationGenerator.tank(0,-1,-1,180,"starter"));
  starter(GameState.ItemType.PISTOL,"pistol","Service pistol",1);starter(GameState.ItemType.CELL,"cells","Power cells",12);starter(GameState.ItemType.MEDICINE,"heal",RunContent.medicineFor(state,"heal"),1);
  state.flags.add("equipped-weapon:starter-pistol");state.identifiedMedicine.add(RunContent.medicineFor(state,"heal"));
  StationGenerator.prepareCampaign(state);StationGenerator.generate(state);Campaign.begin(state);Campaign.enterDeck(state,new ArrayList<>());Visibility.refresh(state);
  state.log.add("Boarded. Oxygen is finite. Observe light, listen, and plan your movement.");
 }
 private void starter(GameState.ItemType type,String id,String name,int power){var i=new GameState.Item();i.type=type;i.id="starter-"+id;i.name=name;i.power=power;state.inventory.add(i);}
 public RunEngine(GameState restored){validateState(restored);state=restored.copy();Visibility.refresh(state);}
 public WorldSnapshot snapshot(){return new WorldSnapshot(state);}
 public GameState saveState(){return state.copy();}
 public static int lowReserveThreshold(GameState s){return Math.max(LOW_RESERVE,s.maxReserve/4);}
 public static boolean isThreat(GameState s,GameState.Actor a){return CombatRules.alive(a)&&!EnemyTurns.disabled(s,a);}
 public List<PlayerAction> pathTo(int x,int y){
  if(state.dead||state.victory)return List.of();var result=new ArrayList<PlayerAction>();int px=state.playerX,py=state.playerY;
  for(Coord c:GridRoutes.route(state,px,py,x,y,true)){if(StationSystems.knownTile(state,c.x,c.y)==Tile.DOOR_CLOSED)result.add(PlayerAction.at(PlayerAction.Type.INTERACT,c.x,c.y));result.add(PlayerAction.move(c.x-px,c.y-py));px=c.x;py=c.y;}
  return List.copyOf(result);
 }
 public ActionForecast forecast(PlayerAction a){
  if(a==null||a.type()==null||a.id()==null)return unavailable("Invalid action.");
  boolean speechReceipt=a.type()==PlayerAction.Type.EXCHANGE&&a.id().startsWith("complete:");
  if((state.dead||state.victory)&&!speechReceipt)return unavailable("No active expedition.");
  var warnings=new ArrayList<String>();int cost=1,cells=0,noise=0;String text="One committed action.";boolean ok=true;int x=a.x(),y=a.y();
  switch(a.type()){
   case SCAN,CHOOSE,EXCHANGE->{cost=0;text="No world time.";}
   case MOVE,CAREFUL_MOVE->{x+=state.playerX;y+=state.playerY;
    if(Math.abs(a.x())+Math.abs(a.y())!=1||!GridRoutes.inside(state,x,y))return unavailable("Choose one cardinal step.");
    if(state.visible[x][y]&&(state.tiles[x][y].blocksMovement||CombatRules.at(state,x,y)!=null))return unavailable("Blocked. Operate a door or choose an explicit attack.");
    Tile known=StationSystems.knownTile(state,x,y);
    cost=(a.type()==PlayerAction.Type.CAREFUL_MOVE?2:1)+(known!=null&&known.cost>1?1:0);
    noise=StationSystems.movementNoise(state,known==null?Tile.FLOOR:known,a.type()==PlayerAction.Type.CAREFUL_MOVE);text="Crossing completes on the final tick.";
   }
   case TURN->{ok=x>=0&&x<4;text="Turn the body and helmet beam.";}
   case LAMP->text=state.lampOn?"Switch helmet light off.":"Switch helmet light on; optical sensors may see the beam.";
   case WAIT->text="Wait one tick.";
   case LISTEN->text="Listen for current audible contacts; machines also act.";
   case PICKUP->{var i=floor(a.id());ok=i!=null&&EquipmentRules.bulk(state)+EquipmentRules.bulk(i.type)<=MAX_INVENTORY;text=ok?"Take one physical item.":"No fitting item underfoot.";}
   case USE->{var i=item(a.id(),true);ok=i!=null;if(ok){ok=switch(i.type){case OXYGEN->state.reserve<state.maxReserve;case CELL->state.batteries<12;case MEDICINE->true;default->false;};}text="Transfer only what fits; preserve the remainder.";}
   case DROP->{ok=carried(a.id())!=null&&state.floorItems.size()<MAX_FLOOR_ITEMS;text="Leave this physical item here.";}
   case EQUIP->{var i=item(a.id(),false);ok=i!=null&&(!EquipmentRules.weaponId(i.type).isEmpty()||!EquipmentRules.armorId(i.type).isEmpty());if(ok&&!EquipmentRules.weaponId(i.type).isEmpty()&&i.x>=0)ok=EquipmentRules.bulk(state)+EquipmentRules.bulk(i.type)<=12;if(ok&&i.x<0&&!EquipmentRules.armorId(i.type).isEmpty()&&equippedBulk(i)>12)ok=state.floorItems.size()<MAX_FLOOR_ITEMS;text="Equip selected gear; leave the previous shell here if the pack cannot hold it.";}
   case SHOOT->{var w=EquipmentRules.weapon(state.weapon);cells=w.batteries();noise=state.weapon.equals("pistol")?10:14;ok=!state.weapon.equals("unarmed")&&GridRoutes.inside(state,x,y)&&state.visible[x][y]&&CombatRules.distance(state.playerX,state.playerY,x,y)>0&&CombatRules.distance(state.playerX,state.playerY,x,y)<=w.range()&&state.batteries>=cells&&CombatRules.clearLine(state,state.playerX,state.playerY,x,y,true);text="Finite ammunition. A hit is uncertain.";}
   case MELEE->{ok=GridRoutes.inside(state,x,y)&&state.visible[x][y]&&CombatRules.distance(state.playerX,state.playerY,x,y)==1&&CombatRules.at(state,x,y)!=null;noise=5;text="Emergency strike: low damage, close exposure.";}
   case FORCE->{int[] p=target(a,false);x=p[0];y=p[1];ok=adjacent(x,y)&&state.tiles[x][y]==Tile.LOCKED_DOOR;cost=4;noise=9;text="Four ticks of loud mechanical work.";}
   case HACK->{int[] p=target(a,true);x=p[0];y=p[1];var actor=GridRoutes.inside(state,x,y)?CombatRules.at(state,x,y):null;
    ok=adjacent(x,y)&&((state.tiles[x][y]==Tile.LOCKED_DOOR&&has(GameState.ItemType.SERVICE_PROBE))||(actor!=null&&actor.kind==GameState.ActorKind.CONTROLLER&&StationSystems.powered(state,x,y)&&!EnemyTurns.disabled(state,actor)));cells=2;cost=3;ok&=state.batteries>=cells;text="Local service access: three ticks and two cells. No deck-wide override.";
   }
   case CIRCUIT->{int[] p=target(a,false);x=p[0];y=p[1];ok=near(x,y)&&controlAt(x,y);cost=StationSystems.powered(state,x,y)?1:3;noise=3;text="Toggle this local circuit: lights, fixed security and access change together.";}
   case BRACE->{int[] p=target(a,false);x=p[0];y=p[1];ok=adjacent(x,y)&&state.tiles[x][y]==Tile.DOOR_CLOSED&&(StationSystems.braced(state,x,y)?EquipmentRules.bulk(state)<12:has(GameState.ItemType.DOOR_BRACE));text="Install or recover a physical door brace.";}
   case ANALYZE->{ok=near(state.terminalX,state.terminalY)&&StationSystems.powered(state,state.terminalX,state.terminalY)&&(state.deck==5||state.deck==6)&&state.inventory.stream().anyMatch(i->i.type==GameState.ItemType.MEDICINE&&!state.identifiedMedicine.contains(i.name));text="Analyze one compound at a powered medical workstation.";}
   case RADIO->{ok=StationSystems.powered(state,state.playerX,state.playerY);text="Query the station link.";}
   case ADVANCE->{ok=atHatch()&&Campaign.hatchBlock(state).isEmpty();text=state.deck==10?"Board and seal the evacuation pod.":"Irreversible descent. Supplies above will be lost.";}
   case INTERACT->{int[] p=target(a,false);x=p[0];y=p[1];ok=near(x,y)&&interactable(x,y);text="Operate the selected physical mechanism.";if(ok&&(state.tiles[x][y]==Tile.DOOR_CLOSED||state.tiles[x][y]==Tile.DOOR_OPEN))noise=4;if(ok&&state.tiles[x][y]==Tile.SWITCH&&!StationSystems.powered(state,x,y))cost=3;if(ok&&state.tiles[x][y]==Tile.DOOR_OPEN&&state.flags.contains("damaged-door:"+state.deck+":"+x+":"+y))return unavailable("The forced release is damaged; this bulkhead cannot reseal.");if(ok&&state.tiles[x][y]==Tile.HATCH){ok=atHatch()&&Campaign.hatchBlock(state).isEmpty();text="Confirm irreversible departure.";}}
  }
  for(var enemy:state.actors)if(!enemy.dead&&state.visible[enemy.x][enemy.y]){
   if(enemy.fuse>=0&&enemy.fuse<=cost&&CombatRules.distance(state.playerX,state.playerY,enemy.x,enemy.y)<=cost+1)warnings.add("Known R.O.V.E.R. fuse may expire before your next decision.");
   if(enemy.aiming)warnings.add("A visible machine has aligned its weapon.");
  }
  if(state.reserve<cost)warnings.add("Reserve will run out during this commitment.");
  return new ActionForecast(ok,cost,cells,noise,text,warnings);
 }
 private ActionForecast unavailable(String message){return new ActionForecast(false,0,0,0,message,List.of());}
 public TurnResult submit(PlayerAction a){
  var f=forecast(a);if(!f.available())return TurnResult.rejected(f.explanation());
  if(a.type()==PlayerAction.Type.SCAN)return new TurnResult(true,0,List.of(new GameEvent("scan",state.playerX,state.playerY,"Reserve "+state.reserve+"/360; oxygenation "+state.oxygenation+"; HP "+state.hp+"; bulk "+EquipmentRules.bulk(state)+"/12.")),"",state.revision,false);
  if(a.type()==PlayerAction.Type.EXCHANGE){if(a.id().startsWith("complete:")){String[] token=a.id().split(":");if(token.length!=3||!state.playedExchanges.contains(token[1])||!List.of("0","1").contains(token[2]))return TurnResult.rejected("Unknown speech segment.");boolean changed=state.completedSpeech.add(token[1]+":"+token[2]);if(changed)state.revision++;return new TurnResult(true,0,List.of(),"",state.revision,changed);}if(!ConversationCatalog.eligible(state,a.id())||state.actionSerial-state.lastExchangeActionSerial<8)return TurnResult.rejected("No current exchange.");state.playedExchanges.add(a.id());state.pendingExchange=a.id();state.lastExchangeTick=state.tick;state.lastExchangeActionSerial=state.actionSerial;state.revision++;return new TurnResult(true,0,List.of(new GameEvent("speech",state.playerX,state.playerY,a.id())),"",state.revision,true);}
  if(a.type()==PlayerAction.Type.CHOOSE){boolean changed=!state.activeDialogue.isEmpty();state.activeDialogue="";if(changed)state.revision++;return new TurnResult(true,0,List.of(),"",state.revision,changed);}
  boolean departing=a.type()==PlayerAction.Type.ADVANCE||(a.type()==PlayerAction.Type.INTERACT&&atHatch()&&target(a,false)[0]==state.hatchX&&target(a,false)[1]==state.hatchY);
  if(departing&&!a.id().equals("confirm"))return TurnResult.rejected("Departure is irreversible. Confirm to leave supplies above.");
  state.actionSerial++;var events=new ArrayList<GameEvent>();int oldDeck=state.deck;
  if(a.type()==PlayerAction.Type.FORCE){StationSystems.noise(state,state.playerX,state.playerY,9,"mechanical forcing",true,events);events.add(new GameEvent("door_force",state.playerX,state.playerY,"Mechanical release work.").atTick(state.tick+1));}
  for(int i=0;i<f.ticks()&&!state.dead;i++){
   if(i==f.ticks()-1){int first=events.size();complete(a,events);stampOccurrence(events,first,state.tick+1);}
   int first=events.size();tick(events);stampOccurrence(events,first,state.tick);
  }
  if(!state.dead){Campaign.afterAction(state,a,events);if(departing){if(Campaign.hatchBlock(state).isEmpty()){if(oldDeck==10)Campaign.escape(state,events);else{state.deck++;StationGenerator.generate(state);Campaign.enterDeck(state,events);events.add(new GameEvent("hatch",state.playerX,state.playerY,"Entered deck "+state.deck+". Supplies above are inaccessible."));}}}}
  for(var event:events)if(event.type().equals("speech"))state.playedExchanges.add(event.detail());
  Visibility.refresh(state);state.revision++;
  for(var e:events)if(!Set.of("step","robot_step","speech").contains(e.type()))state.log.add(e.detail());while(state.log.size()>60)state.log.removeFirst();
  return new TurnResult(true,f.ticks(),events.stream().map(e->e.stamped(state,e.visible(),e.audible())).toList(),"",state.revision,true);
 }
 private static void stampOccurrence(List<GameEvent> events,int first,int tick){for(int n=first;n<events.size();n++)events.set(n,events.get(n).atTick(tick));}
 private void complete(PlayerAction a,List<GameEvent> e){
  switch(a.type()){
   case MOVE,CAREFUL_MOVE->{int x=state.playerX+a.x(),y=state.playerY+a.y();state.facing=CombatRules.facing(state.playerX,state.playerY,x,y);state.careful=a.type()==PlayerAction.Type.CAREFUL_MOVE;
    if(state.tiles[x][y].blocksMovement||CombatRules.at(state,x,y)!=null){e.add(new GameEvent("blocked",state.playerX,state.playerY,"Crossing obstructed. You remain at the source."));break;}
    int ox=state.playerX,oy=state.playerY;state.playerX=x;state.playerY=y;Visibility.refresh(state);
    e.add(new GameEvent("step",x,y,state.tiles[x][y].name(),"officer",ox,oy,state.facing,state.tick,state.actionSerial,true,true,"contact"));StationSystems.noise(state,x,y,StationSystems.movementNoise(state,state.tiles[x][y],state.careful),"footstep",true,e);if(state.careful)state.memoryTicks.put("careful",state.tick+1);
   }
   case TURN->state.facing=a.x();
   case LAMP->{state.lampOn=!state.lampOn;if(!state.lampOn)state.memoryTicks.put("lamp",state.tick+1);e.add(new GameEvent("lamp",state.playerX,state.playerY,"Helmet light "+(state.lampOn?"on.":"off.")));}
   case WAIT,ADVANCE->{}
   case LISTEN->{Visibility.refresh(state);for(var machine:state.actors)if(!machine.dead&&!EnemyTurns.disabled(state,machine)&&machine.kind!=GameState.ActorKind.CAMERA)StationSystems.noise(state,machine.x,machine.y,4,"mechanical contact",false,e);if(!state.heard.isEmpty())state.memoryTicks.put("listen",state.tick+1);}
   case PICKUP->{var i=floor(a.id());if(i!=null&&EquipmentRules.bulk(state)+EquipmentRules.bulk(i.type)<=12){state.floorItems.remove(i);i.x=i.y=-1;state.inventory.add(i);e.add(new GameEvent("pickup",state.playerX,state.playerY,"Packed "+i.name+"."));}}
   case USE->use(a.id(),e);
   case DROP->{var i=carried(a.id());if(i!=null){boolean constrained=EquipmentRules.bulk(state)>9;state.inventory.remove(i);unequip(i);i.x=state.playerX;i.y=state.playerY;state.floorItems.add(i);if(constrained)state.memoryTicks.put("packdrop",state.tick+1);e.add(new GameEvent("drop",i.x,i.y,"Left "+i.name+" here."));}}
   case EQUIP->equip(a.id(),e);
   case SHOOT->CombatRules.shoot(state,a.x(),a.y(),e);
   case MELEE->CombatRules.melee(state,a.x(),a.y(),e);
   case FORCE->{var p=target(a,false);if(state.tiles[p[0]][p[1]]==Tile.LOCKED_DOOR){state.tiles[p[0]][p[1]]=Tile.DOOR_OPEN;state.flags.add("damaged-door:"+state.deck+":"+p[0]+":"+p[1]);state.memoryTicks.put("force",state.tick+1);e.add(new GameEvent("door",p[0],p[1],"Mechanical release forced open."));}}
   case HACK->{var p=target(a,true);var machine=CombatRules.at(state,p[0],p[1]);state.batteries-=2;
    if(machine!=null&&machine.kind==GameState.ActorKind.CONTROLLER){var rover=state.actors.stream().filter(r->r.kind==GameState.ActorKind.ROVER&&!r.dead&&!r.redirected).min(Comparator.comparing(r->r.id)).orElse(null);if(rover!=null){rover.redirected=true;rover.fuse=-1;rover.awareness=GameState.Awareness.PURSUE;e.add(new GameEvent("hack",p[0],p[1],"Controller accepted a unit-redirection command."));}else e.add(new GameEvent("hack",p[0],p[1],"No active unit available. Physical shutdown remains accessible."));}
    else if(state.tiles[p[0]][p[1]]==Tile.LOCKED_DOOR){state.tiles[p[0]][p[1]]=Tile.DOOR_OPEN;state.flags.add("service-panel:"+state.deck+":"+p[0]+":"+p[1]);e.add(new GameEvent("hack",p[0],p[1],"Local probe bypass complete."));}
   }
   case CIRCUIT->{var p=target(a,false);int c=state.circuits[p[0]][p[1]];state.circuitPower[c]=!state.circuitPower[c];e.add(new GameEvent("switch",p[0],p[1],"Circuit "+(c+1)+" "+(state.circuitPower[c]?"energized.":"isolated. Lights, fixed security and station links follow its power.")));StationSystems.noise(state,p[0],p[1],3,"breaker",true,e);}
   case BRACE->brace(a,e);
   case ANALYZE->MedicineRules.identifyOne(state,e);
   case RADIO->e.add(new GameEvent("radio",state.playerX,state.playerY,"Emergency station link: degraded but usable. No live human channels."));
   case INTERACT->interact(a,e);
   default->{}
  }
 }
 private void interact(PlayerAction a,List<GameEvent> e){var p=target(a,false);int x=p[0],y=p[1];var actor=CombatRules.at(state,x,y);
  if(actor!=null&&actor.kind==GameState.ActorKind.CONTROLLER){CombatRules.finishConstructor(state,"controller",e);return;}
  switch(state.tiles[x][y]){
   case SWITCH->{if(!state.switchOn){state.switchOn=true;e.add(new GameEvent("switch",x,y,"Physical hatch release operated."));Campaign.commandRestored(state,e);}}
   case TERMINAL->Campaign.interact(state,x,y,e);
   case DOOR_CLOSED->{if(!StationSystems.braced(state,x,y)){state.tiles[x][y]=Tile.DOOR_OPEN;e.add(new GameEvent("door",x,y,"Bulkhead opened."));StationSystems.noise(state,x,y,4,"bulkhead",true,e);}}
   case DOOR_OPEN->{if(!(x==state.playerX&&y==state.playerY)&&CombatRules.at(state,x,y)==null){state.tiles[x][y]=Tile.DOOR_CLOSED;e.add(new GameEvent("door",x,y,"Bulkhead closed."));StationSystems.noise(state,x,y,4,"bulkhead",true,e);}}
   default->{}
  }
 }
 private void use(String id,List<GameEvent> e){var i=item(id,true);if(i==null)return;
  if(i.type==GameState.ItemType.MEDICINE){boolean onFloor=state.floorItems.remove(i);if(onFloor)state.inventory.add(i);MedicineRules.use(state,i,e);return;}
  int transfer=i.type==GameState.ItemType.CELL?Math.min(12-state.batteries,i.power):Math.min(state.maxReserve-state.reserve,i.power);
  if(i.type==GameState.ItemType.CELL)state.batteries+=transfer;else{state.reserve+=transfer;if(state.oxygenation<70)state.memoryTicks.put("recovery",state.tick+1);}i.power-=transfer;
  if(i.power==0){state.inventory.remove(i);state.floorItems.remove(i);}e.add(new GameEvent(i.type==GameState.ItemType.CELL?"pickup":"oxygen",state.playerX,state.playerY,"Transferred "+transfer+". Remainder "+i.power+"."));
 }
 private void equip(String id,List<GameEvent> e){var i=item(id,false);if(i==null)return;boolean fromFloor=state.floorItems.remove(i);if(fromFloor){i.x=i.y=-1;state.inventory.add(i);}String weapon=EquipmentRules.weaponId(i.type),armor=EquipmentRules.armorId(i.type);
  if(!weapon.isEmpty()){state.flags.removeIf(f->f.startsWith("equipped-weapon:"));state.flags.add("equipped-weapon:"+i.id);state.weapon=weapon;}
  else{var old=state.inventory.stream().filter(j->state.flags.contains("equipped-armor:"+j.id)).findFirst().orElse(null);state.flags.removeIf(f->f.startsWith("equipped-armor:"));state.flags.add("equipped-armor:"+i.id);state.armor=armor;
   if(old!=null&&EquipmentRules.bulk(state)>12){state.inventory.remove(old);old.x=state.playerX;old.y=state.playerY;state.floorItems.add(old);}}
  e.add(new GameEvent("pickup",state.playerX,state.playerY,"Equipped "+i.name+"."));
 }
 private void unequip(GameState.Item i){if(state.flags.remove("equipped-weapon:"+i.id))state.weapon="unarmed";if(state.flags.remove("equipped-armor:"+i.id))state.armor="suit";}
 private int equippedBulk(GameState.Item item){int n=EquipmentRules.bulk(state)-EquipmentRules.bulk(item.type);for(var i:state.inventory)if(state.flags.contains("equipped-armor:"+i.id))n+=EquipmentRules.bulk(i.type);return n;}
 private void brace(PlayerAction a,List<GameEvent> e){var p=target(a,false);int x=p[0],y=p[1];String key="brace:"+state.deck+":"+x+":"+y;
  if(state.flags.remove(key)){var i=state.floorItems.stream().filter(j->j.type==GameState.ItemType.DOOR_BRACE&&j.x==x&&j.y==y&&state.flags.contains("installed:"+j.id)).findFirst().orElseThrow();state.flags.remove("installed:"+i.id);state.floorItems.remove(i);i.x=i.y=-1;state.inventory.add(i);}
  else{var i=state.inventory.stream().filter(j->j.type==GameState.ItemType.DOOR_BRACE).findFirst().orElseThrow();state.inventory.remove(i);i.x=x;i.y=y;state.floorItems.add(i);state.flags.add(key);state.flags.add("installed:"+i.id);}e.add(new GameEvent(state.flags.contains(key)?"brace_place":"brace_recover",x,y,"Door brace "+(state.flags.contains(key)?"installed.":"recovered.")));
 }
 private void tick(List<GameEvent> e){
  state.tick++;if(state.dead)return;int old=state.oxygenation,prior=state.reserve;
  if(state.reserve>0){state.reserve--;state.oxygenation=Math.min(100,state.oxygenation+4);}else{state.oxygenation=Math.max(0,state.oxygenation-5);if(state.oxygenation==0)state.hp=Math.max(0,state.hp-4);}
  if(state.poisonTicks>0){state.poisonTicks--;state.hp=Math.max(state.flags.contains(MedicineRules.TOXIN_FLAG)?1:0,state.hp-1);if(state.poisonTicks==0)state.flags.remove(MedicineRules.TOXIN_FLAG);}
  if(old<70&&state.oxygenation>=70)state.memoryTicks.put("recovery",state.tick);
  if(prior>90&&state.reserve<=90)e.add(new GameEvent("warning",state.playerX,state.playerY,"Oxygen reserve low."));
  if(prior>0&&state.reserve==0)e.add(new GameEvent("warning",state.playerX,state.playerY,"Reserve empty. Oxygenation is falling."));
  if(old>=70&&state.oxygenation<70)e.add(new GameEvent("warning",state.playerX,state.playerY,"Hypoxia. Verify perceptions with suit telemetry."));
  if(state.hp==0){state.dead=true;e.add(new GameEvent("death",state.playerX,state.playerY,"Life signs lost."));return;}
  EnemyTurns.advance(state,e);
 }
 private boolean near(int x,int y){return GridRoutes.inside(state,x,y)&&CombatRules.distance(x,y,state.playerX,state.playerY)<=1;}
 private boolean adjacent(int x,int y){return near(x,y)&&!(x==state.playerX&&y==state.playerY);}
 private boolean atHatch(){return state.playerX==state.hatchX&&state.playerY==state.hatchY;}
 private boolean has(GameState.ItemType type){return state.inventory.stream().anyMatch(i->i.type==type);}
 private GameState.Item carried(String id){return state.inventory.stream().filter(i->i.id.equals(id)).findFirst().orElse(null);}
 private GameState.Item floor(String id){return state.floorItems.stream().filter(i->i.x==state.playerX&&i.y==state.playerY&&(id==null||id.isEmpty()||i.id.equals(id))).findFirst().orElse(null);}
 private GameState.Item item(String id,boolean oxygenDefault){if((id==null||id.isEmpty())&&oxygenDefault)return state.inventory.stream().filter(i->i.type==GameState.ItemType.OXYGEN).findFirst().orElse(null);var i=carried(id);return i==null?floor(id):i;}
 private boolean controlAt(int x,int y){return state.tiles[x][y]==Tile.TERMINAL||state.fixtures.stream().anyMatch(f->(f.kind.equals("breaker")||f.kind.equals("controller"))&&f.x==x&&f.y==y);}
 private boolean interactable(int x,int y){var t=state.tiles[x][y];var a=CombatRules.at(state,x,y);return t==Tile.SWITCH||t==Tile.HATCH||t==Tile.TERMINAL||t==Tile.DOOR_CLOSED||t==Tile.DOOR_OPEN||(a!=null&&a.kind==GameState.ActorKind.CONTROLLER);}
 private int[] target(PlayerAction a,boolean hack){if(a.x()!=0||a.y()!=0)return new int[]{a.x(),a.y()};if(state.tiles[state.playerX][state.playerY]==Tile.TERMINAL||atHatch())return new int[]{state.playerX,state.playerY};
  for(var d:GridRoutes.DIRECTIONS){int x=state.playerX+d[0],y=state.playerY+d[1];if(!GridRoutes.inside(state,x,y))continue;var t=state.tiles[x][y];if(hack?(t==Tile.LOCKED_DOOR||CombatRules.at(state,x,y)!=null):a.type()==PlayerAction.Type.CIRCUIT?controlAt(x,y):a.type()==PlayerAction.Type.FORCE?t==Tile.LOCKED_DOOR:a.type()==PlayerAction.Type.BRACE?t==Tile.DOOR_CLOSED:interactable(x,y))return new int[]{x,y};}return new int[]{-1,-1};}
 public static void validateState(GameState s){
  Objects.requireNonNull(s,"Saved state");if(s.schemaVersion!=2||s.generationVersion!=2||s.rulesVersion!=2||s.width<5||s.height<5||s.width>128||s.height>128||s.deck<1||s.deck>10||s.tick<0||s.actionSerial<0||s.revision<0)throw new IllegalArgumentException("Unsupported or invalid survival state");
  if(s.tiles==null||s.visible==null||s.explored==null||s.light==null||s.circuits==null||s.tiles.length!=s.width||s.visible.length!=s.width||s.explored.length!=s.width||s.light.length!=s.width||s.circuits.length!=s.width||s.circuitPower==null||s.circuitPower.length!=4||s.routeCosts==null||s.routeCosts.length!=10)throw new IllegalArgumentException("Missing station arrays");
  if(s.rememberedTiles==null||s.blueprintTiles==null||s.rememberedTiles.length!=s.width||s.blueprintTiles.length!=s.width||s.publicSupplyCells==null||s.publicSupplyCells.length>512)throw new IllegalArgumentException("Missing station knowledge arrays");
  for(int x=0;x<s.width;x++){
   if(s.rememberedTiles[x]==null||s.blueprintTiles[x]==null||s.rememberedTiles[x].length!=s.height||s.blueprintTiles[x].length!=s.height)throw new IllegalArgumentException("Invalid station knowledge dimensions");
   for(int y=0;y<s.height;y++)if(s.blueprintTiles[x][y]==null)throw new IllegalArgumentException("Incomplete service blueprint");
  }
  var supplyCells=new HashSet<Integer>();for(int cell:s.publicSupplyCells)if(cell<0||cell>=10*s.width*s.height||!supplyCells.add(cell))throw new IllegalArgumentException("Invalid fixed supply placement");
  for(int cost:s.routeCosts)if(cost<0||cost>100000)throw new IllegalArgumentException("Invalid fixed route budget");
  for(int x=0;x<s.width;x++){if(s.tiles[x]==null||s.visible[x]==null||s.explored[x]==null||s.light[x]==null||s.circuits[x]==null||s.tiles[x].length!=s.height||s.visible[x].length!=s.height||s.explored[x].length!=s.height||s.light[x].length!=s.height||s.circuits[x].length!=s.height)throw new IllegalArgumentException("Invalid map dimensions");for(int y=0;y<s.height;y++)if(s.tiles[x][y]==null||!Float.isFinite(s.light[x][y])||s.light[x][y]<0||s.light[x][y]>1||s.circuits[x][y]<0||s.circuits[x][y]>3)throw new IllegalArgumentException("Invalid terrain, light or circuit");}
  for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)if(s.explored[x][y]&&s.rememberedTiles[x][y]==null)throw new IllegalArgumentException("Explored terrain has no remembered observation");
  if(!GridRoutes.inside(s,s.playerX,s.playerY)||s.tiles[s.playerX][s.playerY].blocksMovement||!GridRoutes.inside(s,s.switchX,s.switchY)||!GridRoutes.inside(s,s.hatchX,s.hatchY)||!GridRoutes.inside(s,s.terminalX,s.terminalY)||s.facing<0||s.facing>3)throw new IllegalArgumentException("Invalid player/objective placement");
  if(s.maxHp!=100||s.hp<0||s.hp>s.maxHp||(s.hp==0)!=s.dead||s.maxReserve!=360||s.reserve<0||s.reserve>360||s.oxygenation<0||s.oxygenation>100||s.batteries<0||s.batteries>12||s.poisonTicks<0||s.poisonTicks>1000||s.constructorStock<0||s.constructorStock>6)throw new IllegalArgumentException("Invalid survival resources");
  if(s.actors==null||s.inventory==null||s.floorItems==null||s.fixtures==null||s.heard==null||s.flags==null||s.knowledge==null||s.aliceKnowledge==null||s.playedExchanges==null||s.memoryTicks==null||s.log==null||s.journal==null||s.identifiedMedicine==null||s.medicineEffects==null||s.pendingExchange==null)throw new IllegalArgumentException("Missing collections");
  if(s.inventory.size()>64||s.floorItems.size()>512||s.actors.size()>128||s.fixtures.size()>512||s.heard.size()>8||s.log.size()>1000||s.journal.size()>1000||s.flags.size()>1000||s.knowledge.size()>128||s.aliceKnowledge.size()>128||s.playedExchanges.size()>128||s.memoryTicks.size()>128)throw new IllegalArgumentException("Unbounded saved collections");
  if(!List.of("pistol","rifle","scattergun","unarmed").contains(s.weapon)||!List.of("suit","light","heavy").contains(s.armor)||s.bossPhase==null||s.constructorRoute==null||s.activeDialogue==null)throw new IllegalArgumentException("Invalid equipment or campaign state");
  var ids=new HashSet<String>();for(var i:s.inventory)validateItem(s,i,ids,false);for(var i:s.floorItems)validateItem(s,i,ids,true);if(EquipmentRules.bulk(s)>12)throw new IllegalArgumentException("Pack exceeds physical bulk");
  ids.clear();for(var a:s.actors)if(a==null||a.id==null||a.id.isBlank()||a.id.length()>200||!ids.add(a.id)||a.kind==null||a.awareness==null||!GridRoutes.inside(s,a.x,a.y)||s.tiles[a.x][a.y].blocksMovement||a.maxHp<1||a.maxHp>1000||a.hp<0||a.hp>a.maxHp||(a.hp==0)!=a.dead||a.fuse< -1||a.fuse>1000||a.facing<0||a.facing>3)throw new IllegalArgumentException("Invalid machine");
  for(var f:s.fixtures)if(f==null||f.kind==null||!GridRoutes.inside(s,f.x,f.y)||f.width<1||f.width>3||f.height<1||f.height>2||!GridRoutes.inside(s,f.x+f.width-1,f.y+f.height-1)||f.circuit<0||f.circuit>3)throw new IllegalArgumentException("Invalid fixture");
  for(var h:s.heard)if(h==null||h.direction==null||h.category==null||h.tick<0||h.tick>s.tick)throw new IllegalArgumentException("Invalid sound contact");
  for(var set:List.of(s.flags,s.knowledge,s.aliceKnowledge,s.playedExchanges,s.identifiedMedicine))for(var value:set)if(value==null||value.length()>1000)throw new IllegalArgumentException("Invalid saved identity");
  for(String fact:s.knowledge)if(fact.startsWith("supervisor-chart:")){if(!fact.matches("supervisor-chart:[0-9]{1,3}:[0-9]{1,3}"))throw new IllegalArgumentException("Malformed service chart label");String[] parts=fact.split(":");if(!GridRoutes.inside(s,Integer.parseInt(parts[1]),Integer.parseInt(parts[2])))throw new IllegalArgumentException("Invalid service chart label");}
  for(var line:s.log)if(line==null||line.length()>4000)throw new IllegalArgumentException("Invalid log");for(var line:s.journal)if(line==null||line.length()>8000)throw new IllegalArgumentException("Invalid journal");
  for(var entry:s.medicineEffects.entrySet())if(entry.getKey()==null||entry.getKey().length()>200||!MedicineRules.EFFECTS.contains(entry.getValue()))throw new IllegalArgumentException("Invalid compound map");if(!s.medicineEffects.keySet().containsAll(s.identifiedMedicine))throw new IllegalArgumentException("Unknown identified compound");
  if(s.completedSpeech==null||s.completedSpeech.size()>256)throw new IllegalArgumentException("Invalid completed speech history");for(var token:s.completedSpeech)if(token==null||!token.matches("[a-z]+:[01]"))throw new IllegalArgumentException("Invalid speech segment");
  for(var entry:s.memoryTicks.entrySet())if(entry.getKey()==null||entry.getKey().length()>100||entry.getValue()==null||entry.getValue()<0||entry.getValue()>s.tick)throw new IllegalArgumentException("Invalid remembered event");
  long gunFlags=s.flags.stream().filter(f->f.startsWith("equipped-weapon:")).count(),armorFlags=s.flags.stream().filter(f->f.startsWith("equipped-armor:")).count();
  if((s.weapon.equals("unarmed")?gunFlags!=0:gunFlags!=1||s.inventory.stream().noneMatch(i->s.flags.contains("equipped-weapon:"+i.id)&&EquipmentRules.weaponId(i.type).equals(s.weapon)))||(s.armor.equals("suit")?armorFlags!=0:armorFlags!=1||s.inventory.stream().noneMatch(i->s.flags.contains("equipped-armor:"+i.id)&&EquipmentRules.armorId(i.type).equals(s.armor))))throw new IllegalArgumentException("Equipped identity is not physical gear");
 }
 private static void validateItem(GameState s,GameState.Item i,Set<String> ids,boolean floor){if(i==null||i.id==null||i.id.isBlank()||i.id.length()>200||!ids.add(i.id)||i.name==null||i.name.length()>200||i.type==null||i.quantity!=1||((i.type==GameState.ItemType.OXYGEN||i.type==GameState.ItemType.CELL)&&(i.power<1||i.power>(i.type==GameState.ItemType.OXYGEN?180:12)))||(i.type==GameState.ItemType.MEDICINE&&!s.medicineEffects.containsKey(i.name))||(!floor&&(i.x!=-1||i.y!=-1))||(floor&&(!GridRoutes.inside(s,i.x,i.y)||!GridRoutes.passable(s.tiles[i.x][i.y]))))throw new IllegalArgumentException("Invalid physical item");}
}
