package io.zipcoder.originwar.desktop;
import io.zipcoder.originwar.engine.StationSystems;
import io.zipcoder.originwar.model.GameState;
/** Cosmetic wall faces derive exclusively from observed or remembered terrain. */
final class KnownArchitecture {
 private KnownArchitecture() {}
 static String wallRegion(GameState state,int x,int y){boolean east=open(state,x+1,y),west=open(state,x-1,y);return east&&!west?"wall.side-left":west&&!east?"wall.side-right":"wall.bulkhead";}
 private static boolean open(GameState state,int x,int y){if(x<0||y<0||x>=state.width||y>=state.height)return false;var tile=StationSystems.knownTile(state,x,y);return tile!=null&&!tile.blocksMovement;}
}
