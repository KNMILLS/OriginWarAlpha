package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.*;
import io.zipcoder.originwar.model.GameEvent;
import java.util.*;

/** Owns all audio objects; scene selection and gains never modify simulation state. */
public final class AudioDirector implements AutoCloseable {
    private final Map<String,Music> tracks=new HashMap<>();
    private final Map<String,Sound> sounds=new HashMap<>();
    private Music current,previous;
    private float fade=1,master=.8f,music=.55f,sfx=.7f;
    private boolean paused;
    private String selected="";
    private int playedSounds,failedSounds,musicLoops;
    private float lastPosition;
    private float elapsed;
    private final Map<String,Float> lastPlayed=new HashMap<>();
    public AudioDirector() {
        for(String id:List.of("menu","exploration","combat","danger","alice","ending")) {
            var file=Gdx.files.internal("audio/music/"+id+".ogg");if(file.exists()){var m=Gdx.audio.newMusic(file);m.setLooping(true);tracks.put(id,m);}
        }
        for(String id:List.of("step","step_lowgrav","door","lock","pickup","oxygen_refill","switch","hatch","warning","gun","impact","ui","terminal","robot_step","alarm","medicine","melee","rover","explosion","death","victory","hack","breath","pulse","hallucination","error","confirm","drop")) {
            var file=Gdx.files.internal("audio/sfx/"+id+".wav");if(file.exists())sounds.put(id,Gdx.audio.newSound(file));
        }
    }
    public void select(String id) {
        if(selected.equals(id))return;selected=id;
        Music next=tracks.get(id);if(next==null)next=tracks.get("exploration");if(next==current)return;
        if(previous!=null)previous.stop();previous=current;current=next;fade=0;
        lastPosition=0;
        if(current!=null){current.setVolume(0);if(!paused)current.play();}
    }
    public void update(float delta) {
        if(paused)return;elapsed+=delta;fade=Math.min(1,fade+delta/1.5f);
        if(current!=null){
            current.setVolume(master*music*fade);float position=current.getPosition();
            if(fade==1&&current.isPlaying()&&position+.2f<lastPosition){musicLoops++;Gdx.app.log("Audio","Stream loop completed: "+selected);}
            lastPosition=position;
        }
        if(previous!=null){previous.setVolume(master*music*(1-fade));if(fade==1){previous.stop();previous=null;}}
    }
    public void event(GameEvent event) {
        String id=switch(event.type()){
            case "oxygen"->"oxygen_refill";case "scan","broadcast","journal"->"terminal";
            case "dialogue","level"->"confirm";case "hit"->"impact";
            case "rover"->event.detail().contains("armed:")?"rover":event.detail().contains("moves")?"robot_step":"alarm";
            case "step"->event.detail().contains("GRAV")?"step_lowgrav":"step";default->event.type();
        };play(id);
    }
    public void play(String id){
        if(paused)return;var sound=sounds.get(id);if(sound==null)return;
        float interval=switch(id){case "rover","warning","alarm"->.6f;case "robot_step"->.12f;case "step","step_lowgrav"->.06f;default->.035f;};
        if(elapsed-lastPlayed.getOrDefault(id,-100f)<interval)return;lastPlayed.put(id,elapsed);
        float gain=switch(id){case "ui","confirm","error"->.55f;case "step","step_lowgrav"->.6f;case "breath","pulse","hallucination"->.6f;default->.85f;};
        long voice=sound.play(master*sfx*gain);if(voice>=0)playedSounds++;else{failedSounds++;Gdx.app.error("Audio","No playback voice for "+id);}
    }
    public void volumes(float master,float music,float sfx){this.master=Math.max(0,Math.min(1,master));this.music=Math.max(0,Math.min(1,music));this.sfx=Math.max(0,Math.min(1,sfx));}
    public void pause(boolean value){paused=value;if(value){if(current!=null)current.pause();if(previous!=null)previous.pause();for(var sound:sounds.values())sound.stop();}else{if(current!=null)current.play();if(previous!=null)previous.play();}}
    public int loadedMusic(){return tracks.size();}public int loadedSounds(){return sounds.size();}
    public String summary(){return "Playback summary: sounds="+playedSounds+", failed="+failedSounds+", musicLoops="+musicLoops;}
    @Override public void close(){Gdx.app.log("Audio",summary());for(var m:tracks.values())m.dispose();for(var s:sounds.values())s.dispose();}
}
