package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.*;
import io.zipcoder.originwar.model.GameEvent;
import java.util.*;

/** Owns all audio objects; scene selection and gains never modify simulation state. */
public final class AudioDirector implements AutoCloseable {
    interface Resources {
        boolean exists(String path); Music music(String path); Sound sound(String path); String text(String path);
        default void log(String message) {} default void error(String message) {}
    }
    private static final List<String> SOUND_IDS=List.of("step","step_lowgrav","door","lock","pickup","oxygen_refill","switch","hatch","warning","gun","impact","ui","terminal","robot_step","alarm","medicine","melee","rover","explosion","death","victory","hack","breath","pulse","hallucination","error","confirm","drop","sirs_step","add_step","rover_step","sirs_gun","add_gun","rover_tick");
    private final Resources resources;
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
    private final ArrayList<Playing> voices=new ArrayList<>();
    private final Map<String,Float> durations=new HashMap<>();
    private record Playing(Sound sound,long handle,float until,float gain,float pan,int priority) {}
    private AliceVoiceCatalog catalog;
    private Music speech,breath,heart,oldBreath,oldHeart;
    private String dialogueKey="";
    private boolean present,closed,speechFinished;
    private int listenerX,listenerY,bodyLevel=-1,spokenClips,discardedCues;
    private float presence,bodyFade=1,duck,duckGain=1;
    private HelmetAudioPolicy.Body body=HelmetAudioPolicy.body(100);
    public AudioDirector() {
        this(new Resources() {
            public boolean exists(String path){return Gdx.files.internal(path).exists();}
            public Music music(String path){return Gdx.audio.newMusic(Gdx.files.internal(path));}
            public Sound sound(String path){return Gdx.audio.newSound(Gdx.files.internal(path));}
            public String text(String path){return Gdx.files.internal(path).readString("UTF-8");}
            public void log(String message){Gdx.app.log("Audio",message);}
            public void error(String message){Gdx.app.error("Audio",message);}
        });
    }
    AudioDirector(Resources resources) {
        this.resources=resources;
        for(String id:List.of("menu","exploration","combat","danger","alice","ending")) {
            String path="audio/music/"+id+".ogg";if(resources.exists(path)){var m=resources.music(path);m.setLooping(true);tracks.put(id,m);}
        }
        for(String id:SOUND_IDS) {
            String path="audio/sfx/"+id+".wav";if(resources.exists(path))sounds.put(id,resources.sound(path));
        }
        if(resources.exists("audio/runtime.json")) {
            var effects=new com.badlogic.gdx.utils.JsonReader().parse(resources.text("audio/runtime.json")).get("sfx");
            for(var item=effects.child;item!=null;item=item.next)durations.put(item.name,item.asFloat());
        }
        if(resources.exists("audio/voice/index.json"))catalog=new AliceVoiceCatalog(resources.text("audio/voice/index.json"));
    }
    public void select(String id) {
        if(closed||selected.equals(id))return;selected=id;
        Music next=tracks.get(id);if(next==null)next=tracks.get("exploration");if(next==current)return;
        if(previous!=null)previous.stop();previous=current;current=next;fade=0;
        lastPosition=0;
        if(current!=null){current.setVolume(0);if(!paused&&master*music>0)current.play();}
    }
    /** Pass only true presentation state. Reading views retain the suit; title/end do not. */
    public void context(int oxygenation,boolean inWorld,int playerX,int playerY) {
        if(closed)return;
        boolean returning=inWorld&&!present;
        present=inWorld;listenerX=playerX;listenerY=playerY;body=HelmetAudioPolicy.body(oxygenation);
        if(inWorld&&body.level()!=bodyLevel) {
            dispose(oldBreath);dispose(oldHeart);oldBreath=breath;oldHeart=heart;
            bodyLevel=body.level();bodyFade=0;
            String level=List.of("healthy","warning","critical").get(bodyLevel);
            breath=loop("audio/ambience/breath_"+level+".ogg");heart=loop("audio/ambience/heartbeat_"+level+".ogg");
        } else if(returning&&!paused&&master*sfx>0) {
            toggle(breath,true);toggle(heart,true);
        }
    }
    private Music loop(String path) {
        if(!resources.exists(path))return null;
        Music m=resources.music(path);m.setLooping(true);m.setVolume(0);
        if(!paused&&master*sfx>0)m.play();return m;
    }
    /** A changed/missing/cleared full text interrupts speech; identical frames never restart it. */
    public void dialogue(String id,String fullText) {
        if(closed)return;
        String key=id==null||id.isEmpty()?"":id+":"+AliceVoiceCatalog.hash(fullText==null?"":fullText);
        if(dialogueKey.equals(key))return;dialogueKey=key;dispose(speech);speech=null;
        if(key.isEmpty()||catalog==null)return;
        var clip=catalog.find(id,fullText==null?"":fullText);
        if(clip==null||!resources.exists(clip.path()))return;
        speech=resources.music(clip.path());speechFinished=false;speech.setLooping(false);speech.setVolume(outputMaster()*sfx*.86f);
        speech.setOnCompletionListener(m->speechFinished=true);
        if(!paused&&master*sfx>0)speech.play();spokenClips++;
    }
    public void update(float delta) {
        if(paused||closed)return;
        delta=Float.isFinite(delta)?Math.max(0,Math.min(delta,.25f)):0;
        elapsed+=delta;fade=Math.min(1,fade+delta/1.5f);bodyFade=Math.min(1,bodyFade+delta/.6f);
        presence=Math.max(0,Math.min(1,presence+(present?1:-1)*delta/.6f));duck=Math.max(0,duck-delta);
        expire();
        if(speech!=null&&speech.isPlaying())duck=Math.max(duck,.12f);
        float targetDuck=duck>0?.40f:1;
        duckGain+=Math.signum(targetDuck-duckGain)*Math.min(Math.abs(targetDuck-duckGain),delta/(duck>0?.12f:1.2f));
        float scoreGain=outputMaster()*music*.68f*duckGain;
        gain(speech,outputMaster()*sfx*.86f);
        if(current!=null){
            current.setVolume(scoreGain*fade);float position=current.getPosition();
            if(fade==1&&current.isPlaying()&&position+.2f<lastPosition){musicLoops++;resources.log("Stream loop completed: "+selected);}
            lastPosition=position;
        }
        if(previous!=null){previous.setVolume(scoreGain*(1-fade));if(fade==1){previous.stop();previous=null;}}
        float bodyGain=outputMaster()*sfx*presence*(speech!=null&&speech.isPlaying()?.60f:1);
        gain(breath,bodyGain*body.breath()*bodyFade);gain(heart,bodyGain*body.heart()*bodyFade);
        gain(oldBreath,bodyGain*body.breath()*(1-bodyFade));gain(oldHeart,bodyGain*body.heart()*(1-bodyFade));
        if(bodyFade==1){dispose(oldBreath);dispose(oldHeart);oldBreath=oldHeart=null;}
        if(!present&&presence==0){stop(breath);stop(heart);}
    }
    /** Compatibility is deliberately local-only; caller must explicitly authorize visible/heard world events. */
    public void event(GameEvent event) {
        event(event,event.x()==listenerX&&event.y()==listenerY);
    }
    public void event(GameEvent event,boolean legitimatelyHeard) {
        var cue=HelmetAudioPolicy.cue(event,listenerX,listenerY,legitimatelyHeard);
        if(cue!=null)play(cue);
    }
    public void play(String id) {
        // Continuous body is solely context-owned; old render scheduling is harmless.
        if(id.equals("breath")||id.equals("pulse"))return;
        var cue=HelmetAudioPolicy.cue(new GameEvent(id,listenerX,listenerY,""),listenerX,listenerY,true);
        if(cue!=null)play(cue);
    }
    private void play(HelmetAudioPolicy.Cue cue) {
        if(paused||closed||master*sfx==0)return;Sound sound=sounds.get(cue.id());if(sound==null)return;
        if(elapsed-lastPlayed.getOrDefault(cue.id(),-100f)<cue.cooldown())return;
        expire();
        if(voices.size()>=6) {
            var oldest=voices.stream().min(Comparator.comparingInt(Playing::priority).thenComparingDouble(Playing::until)).orElseThrow();
            if(oldest.priority()>cue.priority()){discardedCues++;return;}
            oldest.sound().stop(oldest.handle());voices.remove(oldest);
        }
        lastPlayed.put(cue.id(),elapsed);
        long handle=sound.play(outputMaster()*sfx*cue.gain()*.65f/(float)Math.sqrt(voices.size()+1),1,cue.pan());
        if(handle>=0){playedSounds++;voices.add(new Playing(sound,handle,elapsed+durations.getOrDefault(cue.id(),3f)+.04f,cue.gain(),cue.pan(),cue.priority()));refreshOneShots();if(cue.priority()>=2)duck=Math.max(duck,1.1f);}
        else{failedSounds++;resources.error("No playback voice for "+cue.id());}
    }
    private float outputMaster(){return master*.70f;}
    private void refreshOneShots(){float group=outputMaster()*sfx*.65f/(float)Math.sqrt(Math.max(1,voices.size()));for(var v:voices)v.sound().setPan(v.handle(),v.pan(),group*v.gain());}
    private void expire(){boolean changed=false;for(var iterator=voices.iterator();iterator.hasNext();){var v=iterator.next();if(elapsed>=v.until()){v.sound().stop(v.handle());iterator.remove();changed=true;}}if(changed)refreshOneShots();}
    public void volumes(float master,float music,float sfx) {
        if(closed)return;
        this.master=HelmetAudioPolicy.unit(master);this.music=HelmetAudioPolicy.unit(music);this.sfx=HelmetAudioPolicy.unit(sfx);
        refreshOneShots();
        if(!paused){toggle(current,this.master*this.music>0);toggle(previous,this.master*this.music>0);toggle(speech,!speechFinished&&this.master*this.sfx>0);toggle(breath,present&&this.master*this.sfx>0);toggle(heart,present&&this.master*this.sfx>0);toggle(oldBreath,present&&this.master*this.sfx>0);toggle(oldHeart,present&&this.master*this.sfx>0);update(0);}
    }
    public void pause(boolean value) {
        if(closed||paused==value)return;paused=value;
        for(Music m:streams())if(value)m.pause();
        if(value){for(var s:sounds.values())s.stop();voices.clear();}
        else volumes(master,music,sfx);
    }
    private List<Music> streams(){var all=new ArrayList<Music>();for(Music m:new Music[]{current,previous,speech,breath,heart,oldBreath,oldHeart})if(m!=null)all.add(m);return all;}
    private static void toggle(Music m,boolean play){if(m!=null){if(play)m.play();else m.pause();}}
    private static void gain(Music m,float gain){if(m!=null)m.setVolume(gain);}
    private static void stop(Music m){if(m!=null)m.stop();}
    private static void dispose(Music m){if(m!=null){m.stop();m.dispose();}}
    public int loadedMusic(){return tracks.size();}public int loadedSounds(){return sounds.size();}
    public String summary(){return "Playback summary: sounds="+playedSounds+", failed="+failedSounds+", musicLoops="+musicLoops+", ALICEclips="+spokenClips+", boundedDrops="+discardedCues+", activeSFX="+voices.size();}
    @Override public void close(){if(closed)return;closed=true;resources.log(summary());for(var m:tracks.values())dispose(m);for(var s:sounds.values())s.dispose();for(Music m:new Music[]{speech,breath,heart,oldBreath,oldHeart})dispose(m);voices.clear();}
}
