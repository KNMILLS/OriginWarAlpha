package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import com.badlogic.gdx.utils.Json;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PresentationTest {
    private static GameState room(){
        var s=new GameState();s.width=13;s.height=11;s.playerX=4;s.playerY=5;s.activeDialogue="";
        s.tiles=new Tile[s.width][s.height];s.visible=new boolean[s.width][s.height];s.explored=new boolean[s.width][s.height];s.light=new float[s.width][s.height];
        for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++){
            s.tiles[x][y]=x==0||y==0||x==s.width-1||y==s.height-1?Tile.WALL:Tile.FLOOR;
            s.visible[x][y]=s.explored[x][y]=true;
        }
        return s;
    }
    @Test void softBeamHasDirectionalFalloffAndUsefulPeripheralWash(){
        assertTrue(StationLighting.beam(4,0,0)>.5f);
        assertEquals(0,StationLighting.beam(-4,0,0),.001f);
        assertTrue(StationLighting.beam(0,1,0)>.3f);
        assertEquals(0,StationLighting.beam(10,0,0),.001f);
        assertTrue(StationLighting.beam(4,1,0)>StationLighting.beam(4,4,0));
    }
    @Test void fixtureLightStopsAtClosedBulkheadAndRebuildsWhenOpened(){
        var s=room();for(int y=0;y<s.height;y++)s.tiles[6][y]=Tile.WALL;
        s.tiles[6][5]=Tile.DOOR_CLOSED;s.tiles[5][5]=Tile.TERMINAL;
        var lighting=new StationLighting();lighting.update(s,true);
        int closed=lighting.rgba(s,7,5);assertTrue((closed>>>24)<50,"No direct fixture illumination behind opaque door");
        s.tiles[6][5]=Tile.DOOR_OPEN;lighting.update(s,true);
        int opened=lighting.rgba(s,7,5);assertTrue((opened>>>24)>(closed>>>24)+10,"Opening a real door updates the lighting resistance field");
    }
    @Test void lightingCannotPromoteHiddenOrUnexploredCellsToVision(){
        var s=room();s.tiles[5][5]=Tile.TERMINAL;s.visible[5][5]=false;s.explored[8][8]=false;
        String before=new Json().toJson(s);var lighting=new StationLighting();
        lighting.update(s,false);assertEquals(0,lighting.rgba(s,5,5)&255);assertEquals(0,lighting.rgba(s,8,8));
        lighting.update(s,true);assertEquals(before,new Json().toJson(s));
    }
    @Test void continuousMotionRetargetsWithoutTeleportAndNeverWritesSimulation(){
        var before=room();var next=before.copy();next.playerX++;
        var motion=new PresentationMotion();motion.reset(before);motion.accept(before,next);
        assertEquals(4,motion.x(PresentationMotion.PLAYER,-1),.001f);motion.advance(.08f);
        float intermediate=motion.x(PresentationMotion.PLAYER,-1);assertTrue(intermediate>4&&intermediate<5);
        var third=next.copy();third.playerX++;motion.accept(next,third);
        assertEquals(intermediate,motion.x(PresentationMotion.PLAYER,-1),.001f);
        motion.advance(1);assertEquals(6,motion.x(PresentationMotion.PLAYER,-1),.001f);
        assertEquals(4,before.playerX);assertEquals(5,next.playerX);assertEquals(6,third.playerX);assertEquals(0,third.tick);
    }
    @Test void enteringVisionDoesNotAnimateFromHiddenOldEnemyCoordinates(){
        var before=room();var a=new GameState.Actor();a.id="test-sirs";a.x=7;a.y=5;before.actors.add(a);before.visible[7][5]=false;
        var next=before.copy();next.actors.getFirst().x=6;
        var motion=new PresentationMotion();motion.reset(before);motion.accept(before,next);
        assertEquals(6,motion.x(a.id,-1),.001f);assertFalse(motion.moving(a.id));
    }
    @Test void presentationAcrossActualWorldTurnsPreservesCompleteEngineAndRng(){
        var run=new RunEngine(2115);var motion=new PresentationMotion();var lighting=new StationLighting();
        var state=run.snapshot().world();motion.reset(state);
        var json=new Json();String save=json.toJson(run.saveState());
        for(int n=0;n<100;n++){motion.advance(.016f);lighting.update(state,n%2==0);}
        assertEquals(save,json.toJson(run.saveState()));
        var result=run.submit(PlayerAction.item(PlayerAction.Type.CHOOSE,"briefing-orders"));assertTrue(result.accepted());
        assertTrue(run.submit(PlayerAction.item(PlayerAction.Type.CHOOSE,"west-duty")).accepted());
        assertTrue(run.submit(PlayerAction.of(PlayerAction.Type.WAIT)).accepted());
        var next=run.snapshot().world();assertTrue(next.tick>0);motion.accept(state,next);save=json.toJson(run.saveState());
        for(int n=0;n<60;n++){motion.advance(.016f);lighting.update(next,false);}
        assertEquals(save,json.toJson(run.saveState()));
    }
}
