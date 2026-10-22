package io.zipcoder.originwar.engine;
import io.zipcoder.originwar.model.*;
import java.util.*;
/** Simulation-owned light, connectivity and acoustics. No presentation clock or random stream. */
public final class StationSystems{
 private StationSystems(){}
 public static Tile knownTile(GameState s,int x,int y){return s.visible[x][y]?s.tiles[x][y]:s.rememberedTiles!=null?s.rememberedTiles[x][y]:null;}
 public static void copyServicePlan(GameState s){
  // The terminal contains a static public-service drawing, not current remote sensor data.
  var chart=s.copy();chart.tiles=chart.blueprintTiles;chart.playerX=s.terminalX;chart.playerY=s.terminalY;
  for(var c:StationGenerator.required(chart))for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){int x=c.x+dx,y=c.y+dy;if(!GridRoutes.inside(s,x,y))continue;s.explored[x][y]=true;if(s.rememberedTiles[x][y]==null)s.rememberedTiles[x][y]=s.blueprintTiles[x][y];}
  // A fixed supervisory panel is infrastructure on the drawing, not a live machine report.
  for(var fixture:s.fixtures)if(fixture.kind.equals("controller")){
   s.knowledge.add("supervisor-chart:"+fixture.x+":"+fixture.y);
   for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++){int x=fixture.x+dx,y=fixture.y+dy;if(!GridRoutes.inside(s,x,y))continue;s.explored[x][y]=true;if(s.rememberedTiles[x][y]==null)s.rememberedTiles[x][y]=s.blueprintTiles[x][y];}
  }
  s.knowledge.add("service-plan-d"+s.deck);
 }
 public static boolean powered(GameState s,int x,int y){return GridRoutes.inside(s,x,y)&&s.circuits!=null&&s.circuitPower[s.circuits[x][y]];}
 public static boolean braced(GameState s,int x,int y){return s.flags.contains("brace:"+s.deck+":"+x+":"+y);}
 public static boolean door(Tile t){return t==Tile.DOOR_OPEN||t==Tile.DOOR_CLOSED||t==Tile.LOCKED_DOOR;}
 public static float[][] illumination(GameState s){
  float[][] light=new float[s.width][s.height];
  for(var f:s.fixtures)if(f.kind.equals("light")&&s.circuitPower[f.circuit])
   for(int x=Math.max(0,f.x-4);x<=Math.min(s.width-1,f.x+4);x++)for(int y=Math.max(0,f.y-4);y<=Math.min(s.height-1,f.y+4);y++){
    int d=Math.abs(x-f.x)+Math.abs(y-f.y);
    if(d<=4&&CombatRules.clearLine(s,f.x,f.y,x,y,false))light[x][y]=Math.max(light[x][y],.60f-.09f*d);
   }
  if(s.lampOn)for(int x=Math.max(0,s.playerX-8);x<=Math.min(s.width-1,s.playerX+8);x++)for(int y=Math.max(0,s.playerY-8);y<=Math.min(s.height-1,s.playerY+8);y++){
   int dx=x-s.playerX,dy=y-s.playerY,d=Math.abs(dx)+Math.abs(dy);int[] dir=GridRoutes.DIRECTIONS[Math.floorMod(s.facing,4)];int along=dx*dir[0]+dy*dir[1],side=Math.abs(dx*dir[1]-dy*dir[0]);
   if(d<=8&&(d<=1||(along>0&&side<=along))&&CombatRules.clearLine(s,s.playerX,s.playerY,x,y,false))light[x][y]=Math.max(light[x][y],.95f-d*.07f);
  }
  return light;
 }
 public static boolean seesPlayer(GameState s,GameState.Actor a){
  int dx=s.playerX-a.x,dy=s.playerY-a.y,d=Math.abs(dx)+Math.abs(dy);
  if(!CombatRules.clearLine(s,a.x,a.y,s.playerX,s.playerY,false))return false;
  if(d<=1)return true;
  int range=a.kind==GameState.ActorKind.ADD?9:a.kind==GameState.ActorKind.CAMERA?9:7;
  int[] dir=GridRoutes.DIRECTIONS[Math.floorMod(a.facing,4)];int along=dx*dir[0]+dy*dir[1],side=Math.abs(dx*dir[1]-dy*dir[0]);
  return d<=range&&along>0&&side<=along&&s.light[s.playerX][s.playerY]>=.18f;
 }
 /** Weighted shortest acoustic paths; closed bulkheads strongly attenuate. */
 public static int[][] soundField(GameState s,int sx,int sy,int strength){
  int[][] dist=new int[s.width][s.height];for(var c:dist)Arrays.fill(c,Integer.MAX_VALUE);
  if(!GridRoutes.inside(s,sx,sy))return dist;
  var q=new PriorityQueue<int[]>(Comparator.comparingInt(a->a[2]));dist[sx][sy]=0;q.add(new int[]{sx,sy,0});
  while(!q.isEmpty()){int[] p=q.remove();if(p[2]!=dist[p[0]][p[1]])continue;
   for(var dir:GridRoutes.DIRECTIONS){int x=p[0]+dir[0],y=p[1]+dir[1];if(!GridRoutes.inside(s,x,y))continue;Tile t=s.tiles[x][y];
    if(t==Tile.WALL||t==Tile.GLASS||t==Tile.LOCKED_DOOR||braced(s,x,y))continue;
    int cost=p[2]+(t==Tile.DOOR_CLOSED?5:1);if(cost<=strength&&cost<dist[x][y]){dist[x][y]=cost;q.add(new int[]{x,y,cost});}
   }
  }return dist;
 }
 public static void noise(GameState s,int x,int y,int strength,String category,boolean player,List<GameEvent> events){
  if(strength<=0)return;int[][] field=soundField(s,x,y,strength);
  if(player)for(var a:s.actors)if(CombatRules.alive(a)&&!EnemyTurns.disabled(s,a)&&field[a.x][a.y]<=strength){
   a.lastX=x;a.lastY=y;a.alert=8;a.searchTicks=6;if(a.awareness!=GameState.Awareness.PURSUE)a.awareness=GameState.Awareness.INVESTIGATE;
  }
  else{} // player noises do not fabricate external contacts
  if(!player&&field[s.playerX][s.playerY]<=strength){
   String direction=direction(x-s.playerX,y-s.playerY);s.heard.add(new GameState.HeardContact(direction,category,s.tick));
   while(s.heard.size()>8)s.heard.removeFirst();
   if(!s.visible[x][y])events.add(new GameEvent("heard",-1,-1,category+" to the "+direction+"."));
  }
 }
 public static String direction(int dx,int dy){if(dx==0&&dy==0)return "nearby";return Math.abs(dx)>=Math.abs(dy)?(dx>0?"east":"west"):(dy>0?"south":"north");}
 public static int movementNoise(GameState s,Tile t,boolean careful){return (careful?1:3)+(t.cost>1?2:0)+EquipmentRules.movementNoise(s);}
}
