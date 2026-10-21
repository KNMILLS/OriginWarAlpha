package io.zipcoder.originwar.engine;
import com.github.tommyettinger.random.WhiskerRandom;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import com.github.yellowstonegames.place.ConnectingMapGenerator;
import io.zipcoder.originwar.model.*;
import java.util.*;
/** Seed-fixed industrial geometry and campaign-wide oxygen allocations, independent of live resources. */
final class StationGenerator{
 private StationGenerator(){}
 static void prepareCampaign(GameState s){
  var candidates=new ArrayList<int[]>();int cumulative=0;
  for(int deck=1;deck<=10;deck++){var trial=new GameState();trial.seed=s.seed;trial.deck=deck;trial.width=s.width;trial.height=s.height;build(trial);s.routeCosts[deck-1]=trial.publicRouteCost;
   var visited=new HashSet<Coord>();int local=0;for(var c:required(trial)){local+=GridRoutes.entryCost(trial.tiles[c.x][c.y]);if(visited.add(c))candidates.add(new int[]{cumulative+local,(deck-1)*s.width*s.height+c.x*s.height+c.y});}cumulative+=trial.publicRouteCost;
  }
  int count=Math.max(0,(int)Math.ceil((cumulative*1.15+60-540)/180.0));s.publicSupplyCells=new int[count];int previous=-1,lastTime=0;
  for(int i=0;i<count;i++){double ideal=count==1?cumulative/2.0:240.0+i*Math.max(0,cumulative-300.0)/(count-1);int best=-1;double score=Double.MAX_VALUE;
   for(int n=previous+1;n<candidates.size()-(count-i-1);n++){var c=candidates.get(n);if(c[0]-lastTime>280)break;double delta=Math.abs(c[0]-ideal);if(delta<score){score=delta;best=n;}}
   if(best<0)throw new IllegalStateException("No bounded public supply placement");s.publicSupplyCells[i]=candidates.get(best)[1];lastTime=candidates.get(best)[0];previous=best;
  }
  if(count>0&&cumulative-lastTime>300)throw new IllegalStateException("Unbounded final supply gap");
 }
 static void generate(GameState s){
  build(s);supplies(s);RunContent.populate(s);s.blueprintTiles=new Tile[s.width][];for(int x=0;x<s.width;x++)s.blueprintTiles[x]=s.tiles[x].clone();Visibility.refresh(s);
 }
 private static void build(GameState s){
  var rng=new WhiskerRandom(DeterministicRandom.mix(s.seed^(s.deck*0xD1B54A32D192ED03L)));
  int zone=(s.deck-1)/2;int rw=5+rng.nextInt(4),rh=4+rng.nextInt(3);
  char[][] map=new ConnectingMapGenerator(s.width,s.height,rw,rh,rng,1,.24).generate();
  s.tiles=new Tile[s.width][s.height];s.rememberedTiles=new Tile[s.width][s.height];s.blueprintTiles=null;s.visible=new boolean[s.width][s.height];s.explored=new boolean[s.width][s.height];s.light=new float[s.width][s.height];s.circuits=new int[s.width][s.height];s.circuitPower=new boolean[]{true,true,true,true};
  s.floorItems.clear();s.actors.clear();s.fixtures.clear();s.heard.clear();s.switchOn=false;s.radioConnected=false;s.bossPhase="inactive";
  var floors=new ArrayList<Coord>();
  for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++){s.tiles[x][y]=map[x][y]=='#'?Tile.WALL:Tile.FLOOR;s.circuits[x][y]=(x>=s.width/2?1:0)+(y>=s.height/2?2:0);if(s.tiles[x][y]==Tile.FLOOR)floors.add(Coord.get(x,y));}
  if(floors.isEmpty())throw new IllegalStateException("Empty station");
  var start=floors.get(rng.nextInt(floors.size()));s.playerX=start.x;s.playerY=start.y;
  var activation=farthest(s,start.x,start.y,null);s.switchX=activation.x;s.switchY=activation.y;
  var hatch=farthest(s,activation.x,activation.y,start);s.hatchX=hatch.x;s.hatchY=hatch.y;
  s.tiles[activation.x][activation.y]=Tile.SWITCH;s.tiles[hatch.x][hatch.y]=Tile.HATCH;
  var initial=GridRoutes.route(s,start.x,start.y,activation.x,activation.y,false);
  var terminal=initial.getFirst();s.terminalX=terminal.x;s.terminalY=terminal.y;s.tiles[terminal.x][terminal.y]=Tile.TERMINAL;
  for(var c:floors){int x=c.x,y=c.y;if(s.tiles[x][y]!=Tile.FLOOR||CombatRules.distance(x,y,start.x,start.y)<5)continue;
   boolean narrow=(s.tiles[x-1][y]==Tile.WALL&&s.tiles[x+1][y]==Tile.WALL)||(s.tiles[x][y-1]==Tile.WALL&&s.tiles[x][y+1]==Tile.WALL);
   if(narrow&&rng.nextInt(10)==0)s.tiles[x][y]=Tile.DOOR_CLOSED;
   else{int roll=rng.nextInt(100);if(roll<2+zone)s.tiles[x][y]=Tile.LOW_GRAV;else if(roll<3+zone)s.tiles[x][y]=Tile.NO_GRAV;else if(roll<4+zone)s.tiles[x][y]=Tile.HAZARD;}
  }
  RunContent.geometry(s,rng);s.tiles[start.x][start.y]=Tile.FLOOR;
  if(s.deck>1)s.circuitPower[Math.floorMod((int)DeterministicRandom.mix(s.seed+s.deck),4)]=false;
  var route=required(s);int base=(StationSystems.powered(s,s.switchX,s.switchY)?1:3)+1+(s.deck==8?1:0)+(s.deck==10?1+(StationSystems.powered(s,s.terminalX,s.terminalY)?0:3):0);for(var c:route)base+=GridRoutes.entryCost(s.tiles[c.x][c.y]);
  s.publicRouteCost=base+2*((base+179)/180); // pickup/refill handling is included in the fixed budget.
  var used=new HashSet<Coord>();
  for(int x=4;x<s.width-3;x+=8)for(int y=4;y<s.height-3;y+=6){
   final int fx=x,fy=y;var c=floors.stream().filter(p->!s.tiles[p.x][p.y].blocksMovement).min(Comparator.comparingInt(p->Math.abs(p.x-fx)+Math.abs(p.y-fy))).orElseThrow();
   if(used.add(c))s.fixtures.add(new GameState.Fixture("light",c.x,c.y,s.circuits[c.x][c.y]));
  }
  // Equipment follows room purpose; bulky props do not block the required public route.
  var requiredSet=new HashSet<>(route);
  int furnished=0;
  for(int n=0;n<1000&&furnished<Math.max(12,floors.size()/28);n++){var c=floors.get(rng.nextInt(floors.size()));int width=zone>=2&&n%2==0?2:1;if(c.x<2||c.x+width>=s.width-1||c.y<2||c.y>=s.height-2||CombatRules.distance(c.x,c.y,start.x,start.y)<3)continue;
   boolean fits=true;for(int dx=0;dx<width;dx++)if(s.tiles[c.x+dx][c.y]!=Tile.FLOOR||s.tiles[c.x+dx][c.y-1]!=Tile.WALL||requiredSet.contains(Coord.get(c.x+dx,c.y)))fits=false;
   for(int dx=-1;dx<=width;dx++)if(s.tiles[c.x+dx][c.y+1].blocksMovement)fits=false;
   if(s.tiles[c.x-1][c.y].blocksMovement||s.tiles[c.x+width][c.y].blocksMovement)fits=false;
   if(!fits)continue;for(int dx=0;dx<width;dx++)s.tiles[c.x+dx][c.y]=Tile.COVER;
   var fixture=new GameState.Fixture(width==2?(zone==2?"medical":"workbench"):"cargo",c.x,c.y,s.circuits[c.x][c.y]);fixture.width=width;s.fixtures.add(fixture);furnished++;
  }
  // A seed-fixed outage gives light/power decisions without adapting to the player.
  for(int circuit=0;circuit<4;circuit++){final int cId=circuit;int cx=(circuit%2==0?s.width/4:3*s.width/4),cy=(circuit<2?s.height/4:3*s.height/4);var point=floors.stream().filter(p->s.circuits[p.x][p.y]==cId&&!s.tiles[p.x][p.y].blocksMovement&&!RunContent.special(s.tiles[p.x][p.y])).min(Comparator.comparingInt(p->Math.abs(p.x-cx)+Math.abs(p.y-cy))).orElse(null);if(point!=null)s.fixtures.add(new GameState.Fixture("breaker",point.x,point.y,circuit));}
 }
 static List<Coord> required(GameState s){var r=new ArrayList<Coord>();r.add(Coord.get(s.playerX,s.playerY));r.addAll(GridRoutes.route(s,s.playerX,s.playerY,s.switchX,s.switchY,false));if(s.deck==10){r.addAll(GridRoutes.route(s,s.switchX,s.switchY,s.terminalX,s.terminalY,false));r.addAll(GridRoutes.route(s,s.terminalX,s.terminalY,s.hatchX,s.hatchY,false));}else r.addAll(GridRoutes.route(s,s.switchX,s.switchY,s.hatchX,s.hatchY,false));return r;}
 private static void supplies(GameState s){
  int total=Arrays.stream(s.routeCosts).sum();if(total==0){total=s.publicRouteCost*10;Arrays.fill(s.routeCosts,s.publicRouteCost);}
  int worldTanks=Math.max(0,(int)Math.ceil((total*1.15+60-540)/180.0));int optional=Math.max(0,(int)Math.ceil(total*.15/180));
  int before=0;for(int i=0;i<s.deck-1;i++)before+=s.routeCosts[i];int after=before+s.routeCosts[s.deck-1];
  for(int i=0;i<s.publicSupplyCells.length;i++){int encoded=s.publicSupplyCells[i],deck=encoded/(s.width*s.height)+1;if(deck!=s.deck)continue;int cell=encoded%(s.width*s.height);s.floorItems.add(tank(s.deck,cell/s.height,cell%s.height,180,"public-"+i));}
  var rng=new WhiskerRandom(DeterministicRandom.mix(s.seed^0x75AC12L));var cells=RunContent.floors(s);
  for(int i=0;i<optional;i++){int deck=1+rng.nextInt(10);long draw=rng.nextLong();if(deck!=s.deck)continue;var c=cells.get(Math.floorMod((int)draw,cells.size()));s.floorItems.add(tank(s.deck,c.x,c.y,180,"optional-"+i));}
 }
 private static Coord farthest(GameState s,int x,int y,Coord excluded){
  var d=GridRoutes.distances(s,x,y,false);Coord best=null;float longest=-1;
  for(int nx=1;nx<s.width-1;nx++)for(int ny=1;ny<s.height-1;ny++){float v=d.gradientMap[nx][ny];if(GridRoutes.passable(s.tiles[nx][ny])&&v<DijkstraMap.FLOOR&&v>longest&&(excluded==null||excluded.x!=nx||excluded.y!=ny)){best=Coord.get(nx,ny);longest=v;}}
  if(best==null)throw new IllegalStateException("Disconnected station");return best;
 }
 static GameState.Item tank(int deck,int x,int y,int power,String suffix){var i=new GameState.Item();i.id="oxygen-d"+deck+"-"+suffix;i.name="Oxygen canister";i.type=GameState.ItemType.OXYGEN;i.x=x;i.y=y;i.power=power;return i;}
}
