package io.zipcoder.originwar.desktop;

import com.github.yellowstonegames.grid.*;
import io.zipcoder.originwar.model.*;
import java.util.ArrayList;
import java.util.List;

/** Separate SquidGrid lighting; never substitutes its FOV for the engine's visibility. */
final class StationLighting {
    record Fixture(int x,int y,int kind,float phase){}
    record Pulse(int x,int y,float range,int color){}
    private LightingManagerRgb manager;
    private long geometry=Long.MIN_VALUE;
    private int viewX=-1,viewY=-1;
    private final List<Fixture> fixtures=new ArrayList<>();
    private final List<Float> flickers=new ArrayList<>();
    private final java.util.HashSet<Integer> wallMounts=new java.util.HashSet<>();
    void update(GameState s,boolean reduced){
        long key=s.seed*31+s.deck+(s.switchOn?191:0);
        for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)key=key*31+s.tiles[x][y].ordinal();
        if(manager==null||manager.width!=s.width||manager.height!=s.height||key!=geometry){rebuild(s);geometry=key;viewX=-1;}
        if(viewX!=s.playerX||viewY!=s.playerY){manager.calculateFOV(s.playerX,s.playerY);viewX=s.playerX;viewY=s.playerY;}
        for(int n=0;n<manager.lights.size();n++)manager.lights.get(n).radiance.flicker=reduced?0:flickers.get(n);
        manager.update();
    }
    private void rebuild(GameState s){
        var resistances=new float[s.width][s.height];fixtures.clear();flickers.clear();wallMounts.clear();
        for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)resistances[x][y]=s.tiles[x][y].blocksVision?1:0;
        manager=new LightingManagerRgb(resistances,0x070b10ff,Radius.CIRCLE,10);
        for(int x=1;x<s.width-1;x++)for(int y=1;y<s.height-1;y++){
            Tile t=s.tiles[x][y];int kind=0,color=0;float radius=0;
            if(t==Tile.TERMINAL){kind=1;color=0x78b7bdff;radius=3.4f;}
            else if(t==Tile.SWITCH){kind=2;color=s.switchOn?0x8cbac0ff:0xca8952ff;radius=2.6f;}
            else if(t==Tile.HATCH){kind=3;color=s.switchOn?0xa9c4b7ff:0x9c6544ff;radius=2.2f;}
            else if(t==Tile.FLOOR&&s.tiles[x][y-1]==Tile.WALL){
                long hash=DeterministicRandom.mix(s.seed+s.deck*233L+x*479L+y*151L);
                if(Math.floorMod(hash,13)==0){kind=4;color=0xa4b9b4ff;radius=2.8f;}
                else if(Math.floorMod(hash,37)==0){kind=5;color=0xbb5938ff;radius=2.3f;}
            }
            if(kind==0)continue;
            float phase=Math.floorMod(DeterministicRandom.mix(s.seed+x*191L+y*811L),1000)/1000f;
            // Slow, low-amplitude power instability; reduced-distortion mode removes it.
            float flicker=kind==5?.16f:.055f;
            var radiance=new Radiance(radius,color,flicker,0,phase,0,(int)(s.seed+x*31L+y*97L));
            manager.addLight(x,y,radiance);fixtures.add(new Fixture(x,y,kind,phase));flickers.add(flicker);
            if(kind>=4)wallMounts.add((y-1)*s.width+x);
        }
    }
    List<Fixture> fixtures(){return fixtures;}
    boolean wallMount(int x,int y){return manager!=null&&wallMounts.contains(y*manager.width+x);}
    void pulse(GameState s,Pulse pulse){
        if(pulse.x()<0||pulse.y()<0||pulse.x()>=s.width||pulse.y()>=s.height||!s.visible[pulse.x()][pulse.y()])return;
        manager.updateUI(pulse.x(),pulse.y(),new Radiance(pulse.range(),pulse.color(),0,0,0,0,0));
    }
    /** RGB is a multiplier on materials; alpha is a strict authoritative current-vision mask. */
    int rgba(GameState s,int x,int y){
        if(!s.explored[x][y])return 0;
        if(!s.visible[x][y])return 0x181c2200;
        int color=manager.colorLighting[x][y];float strength=manager.lightingStrength[x][y]*.60f;
        int r=Math.min(255,(int)(37+strength*(color>>>24)));
        int g=Math.min(255,(int)(41+strength*((color>>>16)&255)));
        int b=Math.min(255,(int)(47+strength*((color>>>8)&255)));
        return r<<24|g<<16|b<<8|255;
    }
    /** Pure beam profile shared by shader-boundary tests. Direction and cells use screen-space y. */
    static float beam(float dx,float dy,float direction){
        float distance=(float)Math.hypot(dx,dy);if(distance<.001f)return .8f;
        float dot=(dx*(float)Math.cos(direction)+dy*(float)Math.sin(direction))/distance;
        float cone=smooth(.74f,.92f,dot),falloff=1-smooth(1.2f,8.5f,distance);
        float local=(1-smooth(.3f,2.1f,distance))*.72f;
        return cone*falloff*.95f+local;
    }
    private static float smooth(float low,float high,float value){float t=Math.max(0,Math.min(1,(value-low)/(high-low)));return t*t*(3-2*t);}
}
