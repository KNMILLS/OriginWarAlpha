package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import java.util.*;

/** Immutable visual descriptors added during generation. They never affect rules or RNG. */
final class WorkplaceDressing {
 private record Room(int x,int y,int w,int h){int area(){return w*h;}}
 private WorkplaceDressing(){}
 static boolean annotation(String kind){return kind.startsWith("art-recess:")||kind.startsWith("art-furniture:")||kind.startsWith("art-room:");}
 static void apply(GameState state){
  state.fixtures.removeIf(f->annotation(f.kind));if(state.deck>2)return;
  var source=state.tiles;var rooms=new ArrayList<Room>();var candidates=new ArrayList<Room>();
  for(int y=1;y<state.height-3;y++)for(int x=1;x<state.width-4;x++){
   int width=state.width-x-1;for(int bottom=y;bottom<state.height-1;bottom++){
    int run=0;while(run<width&&interior(source[x+run][bottom]))run++;width=Math.min(width,run);if(width<4)break;int height=bottom-y+1;if(height>=3)candidates.add(new Room(x,y,width,height));
   }
  }
  candidates.sort(Comparator.comparingInt(Room::area).reversed().thenComparingInt(Room::y).thenComparingInt(Room::x));
  int[][] assigned=new int[state.width][state.height];for(var column:assigned)Arrays.fill(column,-1);
  for(var room:candidates){boolean overlap=false;for(int x=room.x;x<room.x+room.w;x++)for(int y=room.y;y<room.y+room.h;y++)if(assigned[x][y]>=0)overlap=true;
   if(overlap)continue;int id=rooms.size();rooms.add(room);for(int x=room.x;x<room.x+room.w;x++)for(int y=room.y;y<room.y+room.h;y++)assigned[x][y]=id;
  }
  var queue=new ArrayDeque<int[]>();for(var r:rooms)for(int y=r.y;y<r.y+r.h;y++)for(int x=r.x;x<r.x+r.w;x++)queue.add(new int[]{x,y});
  while(!queue.isEmpty()){var p=queue.removeFirst();for(int[] d:GridRoutes.DIRECTIONS){int x=p[0]+d[0],y=p[1]+d[1];if(x<0||y<0||x>=state.width||y>=state.height||assigned[x][y]>=0||source[x][y]==Tile.WALL||source[x][y]==Tile.GLASS)continue;assigned[x][y]=assigned[p[0]][p[1]];queue.add(new int[]{x,y});}}
  int receiving=assigned[state.terminalX][state.terminalY],security=assigned[state.switchX][state.switchY];
  for(var fixture:List.copyOf(state.fixtures))if(fixture.kind.equals("cargo")&&fixture.width==1){int variant=Math.floorMod(fixture.x*17+fixture.y*31+(int)state.seed,3),id=assigned[fixture.x][fixture.y];String region=id>=0&&id==receiving?new String[]{"prop.inspection-desk","prop.locker","prop.life-support"}[variant]:id>=0&&id==security?new String[]{"prop.locker","prop.inspection-desk","prop.equipment-rack"}[variant]:new String[]{"prop.maintenance-bench","prop.service-cart","prop.equipment-rack"}[variant];add(state,"art-furniture:"+region,fixture.x,fixture.y);}
  for(int id=0;id<rooms.size();id++){
   var room=rooms.get(id);String role=id==receiving?"receiving":id==security?"security":"service";add(state,"art-room:"+role,room.x+room.w/2,room.y+room.h/2);
   var slots=new ArrayList<Integer>();for(int x=room.x;x<room.x+room.w;x++)if(source[x][room.y-1]==Tile.WALL&&(x==0||!door(source[x-1][room.y-1]))&&(x==state.width-1||!door(source[x+1][room.y-1])))slots.add(x);
   if(slots.isEmpty())continue;String[] kit=id==receiving?new String[]{"prop.locker","prop.inspection-desk","prop.life-support"}:id==security?new String[]{"prop.equipment-rack","prop.inspection-desk","prop.locker"}:new String[]{"prop.maintenance-bench","prop.equipment-rack"};
   int count=Math.min(kit.length,slots.size());for(int n=0;n<count;n++){int index=count==1?slots.size()/2:n*(slots.size()-1)/(count-1);add(state,"art-recess:"+kit[n],slots.get(index),room.y-1);}
  }
 }
 private static void add(GameState s,String kind,int x,int y){if(s.fixtures.size()<480)s.fixtures.add(new GameState.Fixture(kind,x,y,s.circuits[x][y]));}
 private static boolean interior(Tile t){return t!=Tile.WALL&&t!=Tile.GLASS&&!door(t);}
 private static boolean door(Tile t){return t==Tile.DOOR_CLOSED||t==Tile.DOOR_OPEN||t==Tile.LOCKED_DOOR;}
}
