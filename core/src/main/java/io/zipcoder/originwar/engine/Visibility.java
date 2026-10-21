package io.zipcoder.originwar.engine;
import com.github.yellowstonegames.grid.*;
import io.zipcoder.originwar.model.GameState;
final class Visibility{
 static final float VISION_RADIUS=10f;
 static void refresh(GameState s){
  if(s.rememberedTiles==null)s.rememberedTiles=new io.zipcoder.originwar.model.Tile[s.width][s.height];
  if(s.blueprintTiles==null){s.blueprintTiles=new io.zipcoder.originwar.model.Tile[s.width][];for(int x=0;x<s.width;x++)s.blueprintTiles[x]=s.tiles[x].clone();}
  for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)if(s.explored[x][y]&&s.rememberedTiles[x][y]==null)s.rememberedTiles[x][y]=s.blueprintTiles[x][y];
  float[][] resistance=new float[s.width][s.height],fov=new float[s.width][s.height];s.light=StationSystems.illumination(s);
  for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)resistance[x][y]=s.tiles[x][y].blocksVision?1:0;
  FOV.reuseFOV(resistance,fov,s.playerX,s.playerY,VISION_RADIUS,Radius.CIRCLE);
  for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++){int d=Math.abs(x-s.playerX)+Math.abs(y-s.playerY);s.visible[x][y]=fov[x][y]>.001f&&(s.light[x][y]>=.12f||d<=1);if(s.visible[x][y]){s.explored[x][y]=true;s.rememberedTiles[x][y]=s.tiles[x][y];}}
  s.visible[s.playerX][s.playerY]=true;s.explored[s.playerX][s.playerY]=true;
 }
}
