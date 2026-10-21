package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;

/** Explicit staged art room. Never used by StationGenerator or a normal New Game. */
final class ArtProofScenario {
 private ArtProofScenario() {}
 static GameState create(){
  var s=new RunEngine(2115).saveState();s.actors.clear();s.floorItems.clear();s.fixtures.clear();s.journal.clear();s.log.clear();s.heard.clear();s.knowledge.clear();s.aliceKnowledge.clear();s.playedExchanges.clear();s.completedSpeech.clear();s.memoryTicks.clear();s.pendingExchange="";s.flags.removeIf(f->!f.startsWith("equipped-weapon:"));s.flags.add("art-proof-staging");
  for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++){s.tiles[x][y]=Tile.WALL;s.blueprintTiles[x][y]=Tile.WALL;s.rememberedTiles[x][y]=null;s.visible[x][y]=false;s.explored[x][y]=false;s.light[x][y]=0;s.circuits[x][y]=x<12?0:y<13?1:2;}
  room(s,3,4,11,12);room(s,13,4,21,12);room(s,13,14,21,20);s.tiles[12][8]=Tile.DOOR_CLOSED;s.tiles[17][13]=Tile.DOOR_CLOSED;
  s.playerX=7;s.playerY=9;s.facing=2;s.switchX=18;s.switchY=17;s.hatchX=20;s.hatchY=19;s.terminalX=8;s.terminalY=6;s.switchOn=false;
  s.tiles[18][17]=Tile.SWITCH;s.tiles[20][19]=Tile.HATCH;s.tiles[8][6]=Tile.TERMINAL;
  s.tiles[4][6]=s.tiles[5][6]=Tile.COVER;var bench=new GameState.Fixture("workbench",4,6,0);bench.width=2;s.fixtures.add(bench);
  s.tiles[19][6]=Tile.COVER;s.fixtures.add(new GameState.Fixture("cargo",19,6,1));
  cover(s,"locker",10,5,1);cover(s,"service-cart",9,11,2);cover(s,"life-support",3,10,2);cover(s,"equipment-rack",3,8,2);
  s.fixtures.add(new GameState.Fixture("light",5,7,0));s.fixtures.add(new GameState.Fixture("light",10,10,0));s.fixtures.add(new GameState.Fixture("light",16,7,1));s.fixtures.add(new GameState.Fixture("light",18,17,2));s.fixtures.add(new GameState.Fixture("breaker",8,6,0));
  for(int x=0;x<s.width;x++)s.blueprintTiles[x]=s.tiles[x].clone();
  gear(s,GameState.ItemType.RIFLE,"proof-rifle","Service rifle",7,9);gear(s,GameState.ItemType.ARMOR_HEAVY,"proof-heavy","Heavy pressure shell",7,9);
  var machine=new GameState.Actor();machine.id="proof-sirs";machine.kind=GameState.ActorKind.SIRS;machine.x=machine.homeX=18;machine.y=machine.homeY=16;machine.facing=2;s.actors.add(machine);
  s.log.add("STAGED ART COMPARISON. Authored test geometry; ordinary RunEngine rules, finite resources and live machine. This is not a campaign pacing test.");
  s.journal.add("ART REVIEW: F6 switches painted/rendered appearance without a world action. Walk, turn, aim and fire. Rifle and heavy armor are underfoot; equipping them uses the ordinary physical pack. The far room contains a live S.I.R.S. ESC pauses. Unavailable candidate states are explicitly labeled.");
  return new RunEngine(s).saveState();
 }
 private static void room(GameState s,int left,int top,int right,int bottom){for(int x=left;x<=right;x++)for(int y=top;y<=bottom;y++)s.tiles[x][y]=Tile.FLOOR;}
 private static void cover(GameState s,String kind,int x,int y,int width){var f=new GameState.Fixture(kind,x,y,0);f.width=width;s.fixtures.add(f);for(int dx=0;dx<width;dx++)s.tiles[x+dx][y]=Tile.COVER;}
 private static void gear(GameState s,GameState.ItemType type,String id,String name,int x,int y){var i=new GameState.Item();i.id=id;i.type=type;i.name=name;i.x=x;i.y=y;i.power=1;s.floorItems.add(i);}
}
