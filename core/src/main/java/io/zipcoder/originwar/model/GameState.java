package io.zipcoder.originwar.model;
import java.util.*;
/** Version 2 survival DTO. RunEngine alone writes gameplay; consumers receive deep copies. */
public final class GameState {
 public int schemaVersion=2,generationVersion=2,rulesVersion=2;
 public long seed,combatRng,lootRng,medicineRng,perceptionRng,revision,actionSerial;
 public int width=64,height=40,deck=1,tick;
 public Tile[][] tiles,rememberedTiles,blueprintTiles; public boolean[][] visible,explored; public float[][] light; public int[][] circuits;
 public boolean[] circuitPower={true,true,true,true};
 public int[] routeCosts=new int[10];
 public int[] publicSupplyCells=new int[0];
 public int playerX,playerY,facing,hp=100,maxHp=100,reserve=360,maxReserve=360,oxygenation=100,batteries=12,poisonTicks;
 public int switchX,switchY,hatchX,hatchY,terminalX,terminalY,publicRouteCost,constructorStock=6;
 public boolean switchOn,dead,victory,lampOn=true,careful,radioConnected;
 public String weapon="pistol",armor="suit",activeDialogue="",constructorRoute="",bossPhase="inactive";
 public int scannedUntil; public long phantomUntil; public int phantomX=-1,phantomY=-1;
 public ArrayList<Actor> actors=new ArrayList<>();
 public ArrayList<Item> floorItems=new ArrayList<>(),inventory=new ArrayList<>();
 public ArrayList<String> journal=new ArrayList<>(),log=new ArrayList<>();
 /** flags are world facts; knowledge is the officer's evidence; ALICE's received facts are separate. */
 public HashSet<String> flags=new HashSet<>(),identifiedMedicine=new HashSet<>(),knowledge=new HashSet<>(),aliceKnowledge=new HashSet<>(),playedExchanges=new HashSet<>(),completedSpeech=new HashSet<>();
 public HashMap<String,String> medicineEffects=new HashMap<>();
 public ArrayList<HeardContact> heard=new ArrayList<>();
 public ArrayList<Fixture> fixtures=new ArrayList<>();
 public HashMap<String,Integer> memoryTicks=new HashMap<>();
 public long conversationMillis,lastExchangeActionSerial=-1000; public int lastExchangeTick=-1000; public String pendingExchange="";
 public enum ActorKind { SIRS,ADD,ROVER,CAMERA,TURRET,CONTROLLER,CONSTRUCTOR }
 public enum Awareness { PATROL,INVESTIGATE,PURSUE,SEARCH,RETURN }
 public static final class Actor {
  public String id=""; public ActorKind kind=ActorKind.SIRS; public Awareness awareness=Awareness.PATROL;
  public int x,y,hp=14,maxHp=14,alert,lastX,lastY,fuse=-1,facing,homeX,homeY,lastSeenTick=-1,nextActTick,searchTicks,doorWork,blockedTicks;
  public long armedAction=-1; public boolean redirected,dead,aiming;
  public Actor copy(){var a=new Actor();a.id=id;a.kind=kind;a.awareness=awareness;a.x=x;a.y=y;a.hp=hp;a.maxHp=maxHp;a.alert=alert;a.lastX=lastX;a.lastY=lastY;a.fuse=fuse;a.facing=facing;a.homeX=homeX;a.homeY=homeY;a.lastSeenTick=lastSeenTick;a.nextActTick=nextActTick;a.searchTicks=searchTicks;a.doorWork=doorWork;a.blockedTicks=blockedTicks;a.armedAction=armedAction;a.redirected=redirected;a.dead=dead;a.aiming=aiming;return a;}
 }
 public enum ItemType { OXYGEN,CELL,MEDICINE,PISTOL,RIFLE,SCATTERGUN,ARMOR_LIGHT,ARMOR_HEAVY,SERVICE_PROBE,DOOR_BRACE }
 public static final class Item {
  public String id="",name=""; public ItemType type=ItemType.OXYGEN; public int x=-1,y=-1,quantity=1,power;
  public Item copy(){var a=new Item();a.id=id;a.name=name;a.type=type;a.x=x;a.y=y;a.quantity=quantity;a.power=power;return a;}
 }
 public static final class HeardContact { public String direction="",category="";public int tick;public HeardContact(){}public HeardContact(String d,String c,int t){direction=d;category=c;tick=t;}public String direction(){return direction;}public String category(){return category;}public int tick(){return tick;} }
 public static final class Fixture { public String kind="light";public int x,y,circuit,width=1,height=1;public Fixture(){}public Fixture(String k,int x,int y,int c){kind=k;this.x=x;this.y=y;circuit=c;} }
 public GameState copy(){
  var s=new GameState();s.schemaVersion=schemaVersion;s.generationVersion=generationVersion;s.rulesVersion=rulesVersion;
  s.seed=seed;s.combatRng=combatRng;s.lootRng=lootRng;s.medicineRng=medicineRng;s.perceptionRng=perceptionRng;s.revision=revision;s.actionSerial=actionSerial;
  s.width=width;s.height=height;s.deck=deck;s.tick=tick;s.playerX=playerX;s.playerY=playerY;s.facing=facing;
  s.hp=hp;s.maxHp=maxHp;s.reserve=reserve;s.maxReserve=maxReserve;s.oxygenation=oxygenation;s.batteries=batteries;s.poisonTicks=poisonTicks;
  s.switchX=switchX;s.switchY=switchY;s.hatchX=hatchX;s.hatchY=hatchY;s.terminalX=terminalX;s.terminalY=terminalY;s.publicRouteCost=publicRouteCost;s.constructorStock=constructorStock;
  s.switchOn=switchOn;s.dead=dead;s.victory=victory;s.lampOn=lampOn;s.careful=careful;s.radioConnected=radioConnected;
  s.weapon=weapon;s.armor=armor;s.activeDialogue=activeDialogue;s.constructorRoute=constructorRoute;s.bossPhase=bossPhase;s.scannedUntil=scannedUntil;s.phantomUntil=phantomUntil;s.phantomX=phantomX;s.phantomY=phantomY;
  s.circuitPower=circuitPower.clone();s.routeCosts=routeCosts.clone();s.publicSupplyCells=publicSupplyCells.clone();
  if(tiles!=null){s.tiles=new Tile[width][];s.rememberedTiles=new Tile[width][];s.blueprintTiles=new Tile[width][];s.visible=new boolean[width][];s.explored=new boolean[width][];s.light=new float[width][];s.circuits=new int[width][];
   for(int x=0;x<width;x++){s.tiles[x]=tiles[x].clone();s.rememberedTiles[x]=rememberedTiles==null?new Tile[height]:rememberedTiles[x].clone();s.blueprintTiles[x]=blueprintTiles==null?tiles[x].clone():blueprintTiles[x].clone();s.visible[x]=visible[x].clone();s.explored[x]=explored[x].clone();s.light[x]=light[x].clone();s.circuits[x]=circuits==null?new int[height]:circuits[x].clone();}}
  for(var a:actors)s.actors.add(a.copy());for(var i:floorItems)s.floorItems.add(i.copy());for(var i:inventory)s.inventory.add(i.copy());
  s.journal.addAll(journal);s.log.addAll(log);s.flags.addAll(flags);s.identifiedMedicine.addAll(identifiedMedicine);s.knowledge.addAll(knowledge);s.aliceKnowledge.addAll(aliceKnowledge);s.playedExchanges.addAll(playedExchanges);s.completedSpeech.addAll(completedSpeech);s.medicineEffects.putAll(medicineEffects);s.heard.addAll(heard);s.memoryTicks.putAll(memoryTicks);
  for(var h:heard)s.heard.set(s.heard.indexOf(h),new HeardContact(h.direction,h.category,h.tick));for(var f:fixtures){var copy=new Fixture(f.kind,f.x,f.y,f.circuit);copy.width=f.width;copy.height=f.height;s.fixtures.add(copy);}
  s.conversationMillis=conversationMillis;s.lastExchangeTick=lastExchangeTick;s.lastExchangeActionSerial=lastExchangeActionSerial;s.pendingExchange=pendingExchange;return s;
 }
}
