package io.zipcoder.originwar.engine;
import io.zipcoder.originwar.model.*;
import java.util.*;
/** Tick-driven imperfect machines. Perception targets and timers survive saving. */
final class EnemyTurns{
 private EnemyTurns(){}
 static boolean disabled(GameState s,GameState.Actor a){return s.flags.contains("disabled:"+a.id)||((a.kind==GameState.ActorKind.CAMERA||a.kind==GameState.ActorKind.TURRET||a.kind==GameState.ActorKind.CONSTRUCTOR||a.kind==GameState.ActorKind.CONTROLLER)&&!StationSystems.powered(s,a.x,a.y));}
 static void hear(GameState s,int x,int y,int radius){StationSystems.noise(s,x,y,radius,"mechanical noise",true,new ArrayList<>());}
 static void advance(GameState s,List<GameEvent> e){
  Visibility.refresh(s);
  for(var a:List.copyOf(s.actors)){
   if(s.dead)break;if(!CombatRules.alive(a)||disabled(s,a))continue;
   if(a.kind==GameState.ActorKind.CONSTRUCTOR){constructor(s,a,e);continue;}
   if(a.kind==GameState.ActorKind.CONTROLLER)continue;
   boolean sees=StationSystems.seesPlayer(s,a);
   if(sees){a.awareness=GameState.Awareness.PURSUE;a.lastX=s.playerX;a.lastY=s.playerY;a.lastSeenTick=s.tick;a.alert=8;
    if(a.kind==GameState.ActorKind.CAMERA&&StationSystems.powered(s,a.x,a.y))for(var b:s.actors)if(b!=a&&CombatRules.alive(b)&&StationSystems.powered(s,b.x,b.y)&&s.circuits[b.x][b.y]==s.circuits[a.x][a.y]){b.lastX=a.lastX;b.lastY=a.lastY;b.lastSeenTick=s.tick;b.awareness=GameState.Awareness.INVESTIGATE;b.alert=8;}
   }else if(a.awareness==GameState.Awareness.PURSUE){a.awareness=GameState.Awareness.SEARCH;a.searchTicks=6;a.aiming=false;}
   if(a.kind==GameState.ActorKind.CAMERA){if(!sees&&s.tick%3==0)a.facing=(a.facing+1)%4;continue;}
   if(a.kind==GameState.ActorKind.ROVER){rover(s,a,sees,e);continue;}
   if(s.tick<a.nextActTick)continue;a.nextActTick=s.tick+(a.kind==GameState.ActorKind.TURRET?3:2);
   int d=CombatRules.distance(a.x,a.y,s.playerX,s.playerY);
   if(sees&&d<=(a.kind==GameState.ActorKind.ADD?7:a.kind==GameState.ActorKind.TURRET?7:4)&&CombatRules.clearLine(s,a.x,a.y,s.playerX,s.playerY,true)){
    if(a.kind==GameState.ActorKind.ADD&&d<3&&retreat(s,a,e)){a.aiming=false;continue;}
    if(!a.aiming){a.aiming=true;event(s,a,"aim","Weapon alignment. "+a.kind+" is aiming.",a.x,a.y,e);continue;}
    attack(s,a,e);a.aiming=false;continue;
   }
   a.aiming=false;if(a.kind==GameState.ActorKind.TURRET)continue;
   switch(a.awareness){
    case INVESTIGATE,PURSUE,SEARCH->{
     if(a.x!=a.lastX||a.y!=a.lastY){if(pursue(s,a,a.lastX,a.lastY,e))a.blockedTicks=0;else if(++a.blockedTicks>=3){a.awareness=GameState.Awareness.SEARCH;a.facing=(a.facing+1)%4;if(--a.searchTicks<=0){a.awareness=GameState.Awareness.RETURN;a.alert=0;a.blockedTicks=0;}}}
     else{a.awareness=GameState.Awareness.SEARCH;a.facing=(a.facing+1)%4;if(--a.searchTicks<=0){a.awareness=GameState.Awareness.RETURN;a.alert=0;}}
    }
    case RETURN->{if(a.x==a.homeX&&a.y==a.homeY)a.awareness=GameState.Awareness.PATROL;else if(pursue(s,a,a.homeX,a.homeY,e))a.blockedTicks=0;else if(++a.blockedTicks>=3){a.awareness=GameState.Awareness.PATROL;a.blockedTicks=0;}}
    case PATROL->{a.nextActTick=s.tick+4;var dir=GridRoutes.DIRECTIONS[Math.floorMod(a.facing,4)];if(canEnter(s,a,a.x+dir[0],a.y+dir[1]))move(s,a,a.x+dir[0],a.y+dir[1],e);else a.facing=(a.facing+1)%4;}
   }
  }
 }
 private static void attack(GameState s,GameState.Actor a,List<GameEvent> e){
  if(!CombatRules.clearLine(s,a.x,a.y,s.playerX,s.playerY,true))return;
  a.facing=CombatRules.facing(a.x,a.y,s.playerX,s.playerY);int chance=70-(CombatRules.cover(s,a.x,a.y,s.playerX,s.playerY)?25:0);
  event(s,a,"gun",a.kind+" fires.",a.x,a.y,e);StationSystems.noise(s,a.x,a.y,12,"gunfire",false,e);
  if(CombatRules.roll(s,100)<chance)CombatRules.damagePlayer(s,a.kind==GameState.ActorKind.ADD?11:8,e,s.visible[a.x][a.y]?a.kind.toString():"Incoming fire");
 }
 private static void constructor(GameState s,GameState.Actor a,List<GameEvent> e){
  if(s.flags.contains("constructor-defeated")||s.constructorStock<=0||s.tick<a.nextActTick)return;
  a.nextActTick=s.tick+12;s.bossPhase="active";
  for(var dir:GridRoutes.DIRECTIONS){int x=a.x+dir[0],y=a.y+dir[1];if(!canEnter(s,a,x,y))continue;
   var r=actor(GameState.ActorKind.ROVER,"fabricated-"+s.deck+"-"+s.constructorStock,x,y);s.constructorStock--;s.actors.add(r);
   event(s,a,"rover","Constructor releases a unit from finite stock.",a.x,a.y,e);StationSystems.noise(s,a.x,a.y,12,"heavy fabrication",false,e);return;
  }
 }
 private static void rover(GameState s,GameState.Actor a,boolean sees,List<GameEvent> e){
  var target=a.redirected?s.actors.stream().filter(b->b.kind==GameState.ActorKind.CONSTRUCTOR&&CombatRules.alive(b)).findFirst().orElse(null):null;
  if(a.fuse>=0&&a.armedAction!=s.actionSerial){if(--a.fuse<=0){CombatRules.explode(s,a,e);return;}}
  boolean near=a.redirected&&target!=null?CombatRules.distance(a.x,a.y,target.x,target.y)<=1:sees&&CombatRules.distance(a.x,a.y,s.playerX,s.playerY)<=1;
  if(near&&a.fuse<0){a.fuse=2;a.armedAction=s.actionSerial;event(s,a,"rover","R.O.V.E.R. armed: two ticks after your next commitment.",a.x,a.y,e);StationSystems.noise(s,a.x,a.y,7,"arming mechanism",false,e);return;}
  if(a.fuse>=0)return;
  if(a.redirected&&target!=null)pursue(s,a,target.x,target.y,e);
  else if(a.awareness==GameState.Awareness.RETURN){if(a.x==a.homeX&&a.y==a.homeY){a.awareness=GameState.Awareness.PATROL;a.blockedTicks=0;}else if(pursue(s,a,a.homeX,a.homeY,e))a.blockedTicks=0;else if(++a.blockedTicks>=3){a.awareness=GameState.Awareness.PATROL;a.blockedTicks=0;}}
  else if(a.awareness!=GameState.Awareness.PATROL){if(a.x==a.lastX&&a.y==a.lastY||!pursue(s,a,a.lastX,a.lastY,e)&&++a.blockedTicks>=3){a.awareness=GameState.Awareness.SEARCH;a.facing=(a.facing+1)%4;if(--a.searchTicks<=0){a.awareness=GameState.Awareness.RETURN;a.alert=0;a.blockedTicks=0;}}}
 }
 static GameState.Actor actor(GameState.ActorKind kind,String id,int x,int y){
  var a=new GameState.Actor();a.kind=kind;a.id=id;a.x=a.homeX=a.lastX=x;a.y=a.homeY=a.lastY=y;a.searchTicks=6;
  a.maxHp=switch(kind){case ADD->28;case ROVER,CAMERA->8;case TURRET->18;case CONTROLLER->24;case CONSTRUCTOR->52;default->14;};a.hp=a.maxHp;return a;
 }
 private static boolean pursue(GameState s,GameState.Actor a,int tx,int ty,List<GameEvent> e){
  int[][] dist=new int[s.width][s.height];for(var c:dist)Arrays.fill(c,-1);var q=new ArrayDeque<int[]>();if(!GridRoutes.inside(s,tx,ty))return false;dist[tx][ty]=0;q.add(new int[]{tx,ty});
  while(!q.isEmpty()){var p=q.remove();for(var dir:GridRoutes.DIRECTIONS){int x=p[0]+dir[0],y=p[1]+dir[1];if(!GridRoutes.inside(s,x,y)||dist[x][y]>=0)continue;
   Tile t=s.tiles[x][y];boolean closed=t==Tile.DOOR_CLOSED;
   if(t.blocksMovement&&!closed)continue;if(closed&&(a.kind==GameState.ActorKind.ROVER||StationSystems.braced(s,x,y)||(a.kind!=GameState.ActorKind.SIRS&&!StationSystems.powered(s,x,y))))continue;
   if(!(x==a.x&&y==a.y)&&s.actors.stream().anyMatch(b->b!=a&&CombatRules.alive(b)&&b.x==x&&b.y==y))continue;
   dist[x][y]=dist[p[0]][p[1]]+1;q.add(new int[]{x,y});
  }}
  int best=Integer.MAX_VALUE,bx=a.x,by=a.y;for(var dir:GridRoutes.DIRECTIONS){int x=a.x+dir[0],y=a.y+dir[1];if(GridRoutes.inside(s,x,y)&&dist[x][y]>=0&&dist[x][y]<best){best=dist[x][y];bx=x;by=y;}}
  if(bx==a.x&&by==a.y)return false;
  if(s.tiles[bx][by]==Tile.DOOR_CLOSED){if(StationSystems.powered(s,bx,by)||a.kind==GameState.ActorKind.SIRS){s.tiles[bx][by]=Tile.DOOR_OPEN;event(s,a,"door",a.kind+" operates the bulkhead.",bx,by,e);StationSystems.noise(s,bx,by,5,"bulkhead",false,e);return true;}return false;}
  if(canEnter(s,a,bx,by)){move(s,a,bx,by,e);return true;}return false;
 }
 private static boolean retreat(GameState s,GameState.Actor a,List<GameEvent> e){int best=CombatRules.distance(a.x,a.y,s.playerX,s.playerY),x=a.x,y=a.y;for(var d:GridRoutes.DIRECTIONS){int nx=a.x+d[0],ny=a.y+d[1],n=CombatRules.distance(nx,ny,s.playerX,s.playerY);if(n>best&&canEnter(s,a,nx,ny)){best=n;x=nx;y=ny;}}if(x==a.x&&y==a.y)return false;move(s,a,x,y,e);return true;}
 private static boolean canEnter(GameState s,GameState.Actor self,int x,int y){return GridRoutes.inside(s,x,y)&&!s.tiles[x][y].blocksMovement&&!(x==s.playerX&&y==s.playerY)&&s.actors.stream().noneMatch(a->a!=self&&CombatRules.alive(a)&&a.x==x&&a.y==y);}
 private static void move(GameState s,GameState.Actor a,int x,int y,List<GameEvent> e){int ox=a.x,oy=a.y;a.facing=CombatRules.facing(ox,oy,x,y);a.x=x;a.y=y;event(s,a,"robot_step",a.kind+" moves.",ox,oy,e);StationSystems.noise(s,x,y,a.kind==GameState.ActorKind.ADD?5:3,"machine movement",false,e);}
 private static void event(GameState s,GameState.Actor a,String type,String text,int ox,int oy,List<GameEvent> e){boolean seen=s.visible[a.x][a.y];if(seen)e.add(new GameEvent(type,a.x,a.y,text,a.id,ox,oy,a.facing,s.tick,s.actionSerial,true,true,"contact"));}
}
