package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.math.Vector2;
import com.github.tommyettinger.digital.Interpolations;
import com.github.yellowstonegames.smooth.VectorGlider;
import io.zipcoder.originwar.model.GameState;
import java.util.HashMap;
import java.util.Map;

/** SquidSmooth tracks render positions only. Discrete engine coordinates remain authoritative. */
final class PresentationMotion {
    static final String PLAYER = "@player";
    static final float STEP_SECONDS = .20f;
    private static final class Track {
        VectorGlider glide;
        float age=STEP_SECONDS;
        int x,y;
        Track(int x,int y){this.x=x;this.y=y;glide=new VectorGlider(new Vector2(x,y),new Vector2(x,y));glide.setChange(1);}
        void move(int nx,int ny,boolean animate){
            if(nx==x&&ny==y)return;
            var start=animate?new Vector2(glide.getX(),glide.getY()):new Vector2(nx,ny);
            glide=new VectorGlider(start,new Vector2(nx,ny),Interpolations.smooth,null);
            x=nx;y=ny;age=animate?0:STEP_SECONDS;glide.setChange(age/STEP_SECONDS);
        }
    }
    private final Map<String,Track> tracks=new HashMap<>();
    void reset(GameState s){
        tracks.clear();tracks.put(PLAYER,new Track(s.playerX,s.playerY));
        for(var a:s.actors)tracks.put(a.id,new Track(a.x,a.y));
    }
    void accept(GameState before,GameState after){
        if(before==null||before.deck!=after.deck||before.seed!=after.seed){reset(after);return;}
        tracks.computeIfAbsent(PLAYER,k->new Track(before.playerX,before.playerY)).move(after.playerX,after.playerY,true);
        var existing=new HashMap<String,GameState.Actor>();for(var a:before.actors)existing.put(a.id,a);
        var live=new java.util.HashSet<String>();live.add(PLAYER);
        for(var a:after.actors){
            live.add(a.id);var old=existing.get(a.id);
            // Never animate from a hidden old position: entering vision discloses only the current cell.
            boolean safe=old!=null&&before.visible[old.x][old.y]&&after.visible[a.x][a.y];
            var t=tracks.computeIfAbsent(a.id,k->new Track(a.x,a.y));
            if(!safe){t=new Track(a.x,a.y);tracks.put(a.id,t);}
            else t.move(a.x,a.y,true);
        }
        tracks.keySet().retainAll(live);
    }
    void advance(float dt){
        if(!Float.isFinite(dt)||dt<=0)return;
        for(var t:tracks.values()){t.age=Math.min(STEP_SECONDS,t.age+dt);t.glide.setChange(t.age/STEP_SECONDS);}
    }
    float x(String id,int fallback){var t=tracks.get(id);return t==null?fallback:t.glide.getX();}
    float y(String id,int fallback){var t=tracks.get(id);return t==null?fallback:t.glide.getY();}
    boolean moving(String id){var t=tracks.get(id);return t!=null&&t.age<STEP_SECONDS;}
    float phase(String id){var t=tracks.get(id);return t==null?1:t.age/STEP_SECONDS;}
}
