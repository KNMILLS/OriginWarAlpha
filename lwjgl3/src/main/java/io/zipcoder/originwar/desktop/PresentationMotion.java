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
        final java.util.ArrayDeque<Vector2> waypoints=new java.util.ArrayDeque<>();
        Track(int x,int y){this.x=x;this.y=y;glide=new VectorGlider(new Vector2(x,y),new Vector2(x,y));glide.setChange(1);}
        void move(int nx,int ny,boolean animate){
            if(nx==x&&ny==y)return;
            if(!animate||Math.abs(nx-x)+Math.abs(ny-y)>1){snap(nx,ny);return;}
            float dx=glide.getEnd().x-glide.getStart().x,dy=glide.getEnd().y-glide.getStart().y;
            int stepX=nx-x,stepY=ny-y;x=nx;y=ny;
            boolean bend=Math.abs(dx*stepY-dy*stepX)>.001f||dx*stepX+dy*stepY<0;
            if(age<STEP_SECONDS&&(bend||!waypoints.isEmpty())){
                waypoints.addLast(new Vector2(nx,ny));
                if(waypoints.size()>2)snap(nx,ny);
            }else if(Math.hypot(nx-glide.getX(),ny-glide.getY())>2.1f)snap(nx,ny);
            else begin(new Vector2(nx,ny));
        }
        private void snap(int nx,int ny){
            x=nx;y=ny;waypoints.clear();age=STEP_SECONDS;
            glide=new VectorGlider(new Vector2(nx,ny),new Vector2(nx,ny));glide.setChange(1);
        }
        private void begin(Vector2 destination){
            glide=new VectorGlider(new Vector2(glide.getX(),glide.getY()),destination,Interpolations.smooth,null);age=0;
        }
        void advance(float dt){
            while(dt>0){
                float used=Math.min(dt,STEP_SECONDS-age);age=Math.min(STEP_SECONDS,age+used);glide.setChange(age/STEP_SECONDS);dt-=used;
                if(age>=STEP_SECONDS&&!waypoints.isEmpty())begin(waypoints.removeFirst());else break;
            }
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
            boolean safe=old!=null&&before.visible[old.x][old.y]&&after.visible[old.x][old.y]&&after.visible[a.x][a.y];
            var t=tracks.computeIfAbsent(a.id,k->new Track(a.x,a.y));
            if(!safe){t=new Track(a.x,a.y);tracks.put(a.id,t);}
            else t.move(a.x,a.y,true);
        }
        tracks.keySet().retainAll(live);
    }
    void advance(float dt){
        if(!Float.isFinite(dt)||dt<=0)return;
        for(var t:tracks.values())t.advance(dt);
    }
    float x(String id,int fallback){var t=tracks.get(id);return t==null?fallback:t.glide.getX();}
    float y(String id,int fallback){var t=tracks.get(id);return t==null?fallback:t.glide.getY();}
    boolean moving(String id){var t=tracks.get(id);return t!=null&&t.age<STEP_SECONDS;}
    float phase(String id){var t=tracks.get(id);return t==null?1:t.age/STEP_SECONDS;}
}
