package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.*;
import java.util.*;

/** Stable decoration over occupied footprints. No collision, stock, route, RNG or save changes. */
public final class WorkplaceLayout {
 public enum Role { RECEIVING, SERVICE, SECURITY }
 private record Room(int x,int y,int w,int h){int area(){return w*h;}boolean contains(int px,int py){return px>=x&&py>=y&&px<x+w&&py<y+h;}}
 private final ArrayList<Room> rooms=new ArrayList<>();
 private GameState previous;private long seed;private int deck,fingerprint,receiving,security;private int[][] assigned;
 public void update(GameState state){
  if(state==previous)return;previous=state;
  Tile[][] source=state.blueprintTiles==null?state.tiles:state.blueprintTiles;
  int hash=Objects.hash(state.width,state.height,state.terminalX,state.terminalY,state.switchX,state.switchY);
  for(var column:source)hash=31*hash+Arrays.hashCode(column);
  if(assigned!=null&&seed==state.seed&&deck==state.deck&&fingerprint==hash)return;
  seed=state.seed;deck=state.deck;fingerprint=hash;rooms.clear();
  var candidates=new ArrayList<Room>();
  for(int y=1;y<state.height-3;y++)for(int x=1;x<state.width-4;x++){
   int width=state.width-x-1;
   for(int bottom=y;bottom<state.height-1;bottom++){
    int run=0;while(run<width&&interior(source[x+run][bottom]))run++;width=Math.min(width,run);if(width<4)break;
    int height=bottom-y+1;if(height>=3)candidates.add(new Room(x,y,width,height));
   }
  }
  candidates.sort(Comparator.comparingInt(Room::area).reversed().thenComparingInt(Room::y).thenComparingInt(Room::x));
  assigned=new int[state.width][state.height];for(var column:assigned)Arrays.fill(column,-1);
  for(var room:candidates){boolean overlap=false;for(int x=room.x;x<room.x+room.w;x++)for(int y=room.y;y<room.y+room.h;y++)if(assigned[x][y]>=0)overlap=true;
   if(overlap)continue;int id=rooms.size();rooms.add(room);for(int x=room.x;x<room.x+room.w;x++)for(int y=room.y;y<room.y+room.h;y++)assigned[x][y]=id;
  }
  // Multi-source structural distance attaches the narrow connectors without opening walls.
  var queue=new ArrayDeque<int[]>();for(int id=0;id<rooms.size();id++){var r=rooms.get(id);for(int y=r.y;y<r.y+r.h;y++)for(int x=r.x;x<r.x+r.w;x++)queue.add(new int[]{x,y});}
  while(!queue.isEmpty()){var p=queue.removeFirst();for(int[] d:new int[][]{{0,-1},{1,0},{0,1},{-1,0}}){int x=p[0]+d[0],y=p[1]+d[1];if(x<0||y<0||x>=state.width||y>=state.height||assigned[x][y]>=0||source[x][y]==Tile.WALL||source[x][y]==Tile.GLASS)continue;assigned[x][y]=assigned[p[0]][p[1]];queue.add(new int[]{x,y});}}
  receiving=roomAt(state.terminalX,state.terminalY);security=roomAt(state.switchX,state.switchY);
 }
 private boolean interior(Tile t){return t!=Tile.WALL&&t!=Tile.GLASS&&t!=Tile.DOOR_CLOSED&&t!=Tile.DOOR_OPEN&&t!=Tile.LOCKED_DOOR;}
 private int roomAt(int x,int y){return x>=0&&y>=0&&x<assigned.length&&y<assigned[0].length?assigned[x][y]:-1;}
 public Role role(int x,int y){int id=roomAt(x,y);return id>=0&&id==receiving?Role.RECEIVING:id>=0&&id==security?Role.SECURITY:Role.SERVICE;}
 public String furniture(GameState.Fixture fixture){
  if(!fixture.kind.equals("cargo")||fixture.width!=1||deck>2)return "";
  int variant=Math.floorMod(fixture.x*17+fixture.y*31+(int)seed,3);
  return switch(role(fixture.x,fixture.y)){
   case RECEIVING->new String[]{"prop.inspection-desk","prop.locker","prop.life-support"}[variant];
   case SERVICE->new String[]{"prop.maintenance-bench","prop.service-cart","prop.equipment-rack"}[variant];
   case SECURITY->new String[]{"prop.locker","prop.inspection-desk","prop.equipment-rack"}[variant];
  };
 }
 public String fallback(String region){return Set.of("prop.inspection-desk","prop.maintenance-bench").contains(region)?"prop.workbench":region;}
 public int roomCount(){return rooms.size();}
}
