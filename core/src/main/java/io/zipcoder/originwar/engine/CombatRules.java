package io.zipcoder.originwar.engine;
import com.github.yellowstonegames.grid.BresenhamLine;
import io.zipcoder.originwar.model.*;
import java.util.*;
final class CombatRules{
 private CombatRules(){}
 static boolean hostile(GameState.Actor a){return true;}
 static boolean alive(GameState.Actor a){return !a.dead&&a.hp>0;}
 static GameState.Actor at(GameState s,int x,int y){return s.actors.stream().filter(a->alive(a)&&a.x==x&&a.y==y).findFirst().orElse(null);}
 static int distance(int ax,int ay,int bx,int by){return Math.abs(ax-bx)+Math.abs(ay-by);}
 static int roll(GameState s,int bound){s.combatRng=DeterministicRandom.nextState(s.combatRng);return DeterministicRandom.bounded(s.combatRng,bound);}
 static int clampChance(int chance){return Math.max(15,Math.min(95,chance));}
 static int facing(int ax,int ay,int bx,int by){int dx=bx-ax,dy=by-ay;return Math.abs(dx)>=Math.abs(dy)?(dx>=0?1:3):(dy>=0?2:0);}
 static boolean clearLine(GameState s,int ax,int ay,int bx,int by,boolean projectile){
  if(!GridRoutes.inside(s,ax,ay)||!GridRoutes.inside(s,bx,by))return false;
  var line=BresenhamLine.lineArray(ax,ay,bx,by);
  for(int i=1;i<line.length;i++){var c=line[i];Tile t=s.tiles[c.x][c.y];if(i<line.length-1&&(t.blocksVision||(projectile&&(t==Tile.GLASS||t==Tile.COVER))))return false;}
  return !projectile||s.tiles[bx][by]!=Tile.GLASS;
 }
 static boolean cover(GameState s,int ax,int ay,int bx,int by){var line=BresenhamLine.lineArray(ax,ay,bx,by);for(int i=1;i<line.length-1;i++)if(s.tiles[line[i].x][line[i].y]==Tile.COVER)return true;return false;}
 static int shoot(GameState s,int x,int y,List<GameEvent> e){
  var w=EquipmentRules.weapon(s.weapon);int distance=distance(s.playerX,s.playerY,x,y);
  if(s.weapon.equals("unarmed")||!GridRoutes.inside(s,x,y)||!s.visible[x][y]||distance==0||distance>w.range()||s.batteries<w.batteries()||!clearLine(s,s.playerX,s.playerY,x,y,true))return 0;
  s.batteries-=w.batteries();s.facing=facing(s.playerX,s.playerY,x,y);
  e.add(new GameEvent("gun",s.playerX,s.playerY,"Fired "+s.weapon+".","officer",s.playerX,s.playerY,s.facing,s.tick,s.actionSerial,true,true,"release"));
  int roll=roll(s,100);
  if(s.weapon.equals("scattergun")){
   int[] dir=GridRoutes.DIRECTIONS[s.facing];
   for(var target:List.copyOf(s.actors)){int dx=target.x-s.playerX,dy=target.y-s.playerY,d=Math.abs(dx)+Math.abs(dy),along=dx*dir[0]+dy*dir[1],side=Math.abs(dx*dir[1]-dy*dir[0]);
    if(alive(target)&&d<=w.range()&&along>0&&side<=1+along/2&&clearLine(s,s.playerX,s.playerY,target.x,target.y,true)&&roll<clampChance(w.accuracy()-d*3))damageActor(s,target,Math.max(5,w.damage()-d*2),false,e);
   }
  }else{var target=at(s,x,y);if(target!=null){int chance=w.accuracy()-distance*3-(target.kind==GameState.ActorKind.ROVER?18:0);if(roll<clampChance(chance))damageActor(s,target,w.damage()-(target.kind==GameState.ActorKind.ADD?3:0),false,e);else e.add(new GameEvent("impact",x,y,"Shot missed."));}}
  StationSystems.noise(s,s.playerX,s.playerY,s.weapon.equals("pistol")?10:14,"gunfire",true,e);return 1;
 }
 static int melee(GameState s,int x,int y,List<GameEvent> e){
  var a=at(s,x,y);if(a==null||distance(s.playerX,s.playerY,x,y)!=1)return 0;s.facing=facing(s.playerX,s.playerY,x,y);
  e.add(new GameEvent("melee",x,y,"Emergency strike."));if(roll(s,100)<75)damageActor(s,a,3,false,e);StationSystems.noise(s,s.playerX,s.playerY,5,"impact",true,e);return 1;
 }
 static void damageActor(GameState s,GameState.Actor a,int damage,boolean redirected,List<GameEvent> e){
  if(!alive(a))return;a.hp=Math.max(0,a.hp-Math.max(1,damage));
  if(s.visible[a.x][a.y])e.add(new GameEvent("hit",a.x,a.y,a.kind+" takes "+damage+" damage.",a.id,a.x,a.y,a.facing,s.tick,s.actionSerial,true,true,"contact"));
  if(a.hp>0){a.alert=8;a.lastX=s.playerX;a.lastY=s.playerY;a.awareness=GameState.Awareness.INVESTIGATE;return;}
  a.dead=true;
  if(a.kind==GameState.ActorKind.CONTROLLER||a.kind==GameState.ActorKind.CONSTRUCTOR)finishConstructor(s,redirected?"redirect":a.kind==GameState.ActorKind.CONTROLLER?"controller":"destruction",e);
  else if(a.kind==GameState.ActorKind.ROVER&&a.fuse>=0)explode(s,a,e);
 }
 static void damagePlayer(GameState s,int damage,List<GameEvent> e,String source){
  if(s.dead)return;int amount=Math.max(1,damage-EquipmentRules.protection(s.armor));s.hp=Math.max(0,s.hp-amount);s.memoryTicks.put("injury",s.tick);
  e.add(new GameEvent("hit",s.playerX,s.playerY,source+": "+amount+" damage.","officer",s.playerX,s.playerY,s.facing,s.tick,s.actionSerial,true,true,"contact"));
  if(s.hp==0){s.dead=true;e.add(new GameEvent("death",s.playerX,s.playerY,"Life signs lost. The expedition has ended.","officer",s.playerX,s.playerY,s.facing,s.tick,s.actionSerial,true,true,"contact"));}
 }
 static void explode(GameState s,GameState.Actor rover,List<GameEvent> e){
  rover.dead=true;rover.hp=0;
  if(s.visible[rover.x][rover.y])e.add(new GameEvent("explosion",rover.x,rover.y,"R.O.V.E.R. detonation. Blast radius one cell."));
  StationSystems.noise(s,rover.x,rover.y,16,"explosion",false,e);
  if(distance(rover.x,rover.y,s.playerX,s.playerY)<=1&&clearLine(s,rover.x,rover.y,s.playerX,s.playerY,false))damagePlayer(s,20,e,"Blast");
  for(var a:List.copyOf(s.actors))if(a!=rover&&alive(a)&&distance(rover.x,rover.y,a.x,a.y)<=1&&clearLine(s,rover.x,rover.y,a.x,a.y,false))damageActor(s,a,rover.redirected?60:20,rover.redirected,e);
 }
 static void finishConstructor(GameState s,String route,List<GameEvent> e){
  if(!s.flags.add("constructor-defeated"))return;s.constructorRoute=route;s.bossPhase="defeated";s.constructorStock=0;
  for(var a:s.actors)if(a.kind==GameState.ActorKind.CONSTRUCTOR||a.kind==GameState.ActorKind.CONTROLLER){s.flags.add("disabled:"+a.id);if(a.kind==GameState.ActorKind.CONTROLLER){a.hp=0;a.dead=true;}}
  s.journal.add("Constructor stopped via "+route+". Deployed machines retain independent power.");e.add(new GameEvent("hack",s.playerX,s.playerY,"Fabrication stopped. Deployed machines remain active."));
 }
}
