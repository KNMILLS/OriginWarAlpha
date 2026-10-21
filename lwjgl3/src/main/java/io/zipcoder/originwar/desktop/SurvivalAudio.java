package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.*;
import com.badlogic.gdx.utils.*;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.campaign.ConversationCatalog;
import java.util.*;

/** Finite offline performances. Only authorized engine events create tactical sound cues. */
public final class SurvivalAudio implements AutoCloseable {
    private record Line(String speaker,String text,String path,String clear,float duration){}
    private record Exchange(String id,boolean essential,float gap,List<Line> lines){}
    private static final class Effect {final Sound sound;final long handle;float remaining;Effect(Sound sound,long handle,float remaining){this.sound=sound;this.handle=handle;this.remaining=remaining;}}
    private final Map<String,Exchange> exchanges=new LinkedHashMap<>();
    private final Map<String,Sound> sounds=new HashMap<>();
    private final Map<String,Music> loops=new HashMap<>();
    private final Map<String,Float> durations=new HashMap<>();private final ArrayList<Effect> effects=new ArrayList<>();
    private final ArrayDeque<Exchange> essentials=new ArrayDeque<>();
    private final ArrayDeque<String> completed=new ArrayDeque<>();
    private final HashSet<String> issued=new HashSet<>();
    private final ArrayDeque<String> transcript=new ArrayDeque<>();
    private Exchange active;private Music speech;private int line;private float lineElapsed,gap,duck;
    private boolean paused;private SettingsStore settings;private String caption="";private float scoreClock,scoreRemaining;
    private float captionRemaining;private long quietMillis=180_000,played,failed;private double quietFraction;
    public SurvivalAudio(){
        var runtime=Gdx.files.internal("audio/runtime.json");if(runtime.exists())for(var v:new JsonReader().parse(runtime).get("sfx"))durations.put(v.name,v.asFloat());
        var manifest=Gdx.files.internal("audio/survival/exchanges.json");if(manifest.exists())for(var ex:new JsonReader().parse(manifest).get("exchanges")){
            var lines=new ArrayList<Line>();for(var l:ex.get("lines"))lines.add(new Line(l.getString("speaker"),l.getString("text"),l.getString("path"),l.getString("clear_path",l.getString("path")),l.getFloat("duration")));
            exchanges.put(ex.getString("id"),new Exchange(ex.getString("id"),ex.getBoolean("essential"),ex.getFloat("line_gap_seconds",.32f),List.copyOf(lines)));
        }
        loop("powered","audio/survival/ambience/station_powered.ogg");loop("unpowered","audio/survival/ambience/station_unpowered.ogg");
        loop("breath","audio/ambience/breath_healthy.ogg");loop("warning","audio/ambience/breath_warning.ogg");loop("critical","audio/ambience/breath_critical.ogg");
        loop("radio","audio/survival/ambience/radio-static.ogg");
        loop("score","audio/music/exploration.ogg");
    }
    private void loop(String id,String path){try{if(Gdx.files.internal(path).exists()){var m=Gdx.audio.newMusic(Gdx.files.internal(path));m.setLooping(true);m.setVolume(0);m.play();loops.put(id,m);}}catch(RuntimeException e){failed++;Gdx.app.error("SurvivalAudio",path,e);}}
    public void settings(SettingsStore value){settings=value;}
    public void reset(){stopSpeech();for(var e:effects)e.sound.stop(e.handle);effects.clear();active=null;essentials.clear();issued.clear();completed.clear();transcript.clear();quietMillis=0;quietFraction=0;caption="";scoreClock=scoreRemaining=0;}
    public void event(GameEvent e,GameState state){
        if(e.type().equals("speech")){exchange(e.detail(),state);return;}
        if(!e.audible())return;
        boolean arming=e.type().equals("rover")&&e.detail().toLowerCase(Locale.ROOT).contains("armed");
        if(arming||Set.of("warning","hit","explosion","death","rover_arm").contains(e.type())){duck=1.2f;if(active!=null&&!active.essential){stopSpeech();active=null;} }
        String key=switch(e.type()){
            case "step"->state.careful?"step_careful":state.armor.equals("heavy")?"step_heavy":"step";
            case "robot_step"->e.detail().toLowerCase(Locale.ROOT).contains("rover")?"rover_step":e.detail().toLowerCase(Locale.ROOT).contains("add")?"add_step":"sirs_step";
            case "switch"->e.detail().contains("isolated")?"power_off":"power_on";
            case "hack"->"probe";case "oxygen"->"bank_transfer";case "radio"->"terminal";case "lamp"->"lamp";case "rover"->arming?"rover_arm":"rover";case "heard"->"robot_step";default->e.type();};
        play(key);
        if(settings!=null&&settings.soundCaptions&&(!Set.of("step","pickup").contains(e.type()))){caption=e.detail();captionRemaining=3;}
    }
    private void exchange(String id,GameState s){
        var ex=exchanges.get(id);if(ex==null||!issued.add(id))return;
        if(ex.essential){essentials.add(ex);if(active!=null&&!active.essential){stopSpeech();active=null;}}
        else if(active==null&&essentials.isEmpty()&&!danger(s)){start(ex,s);quietMillis=0;quietFraction=0;}
    }
    private void start(Exchange ex,GameState s){active=ex;line=0;gap=0;while(line<ex.lines.size()&&s.completedSpeech.contains(ex.id+":"+line))line++;if(line>=ex.lines.size()){active=null;return;}startLine();quietMillis=0;quietFraction=0;}
    private void startLine(){
        stopSpeech();lineElapsed=0;var l=active.lines.get(line);String path=settings!=null&&settings.clearSpeech?l.clear:l.path;
        transcript.addLast((l.speaker.equals("alice")?"A.L.I.C.E.":"OFFICER")+" / "+l.text);while(transcript.size()>60)transcript.removeFirst();
        try{speech=Gdx.audio.newMusic(Gdx.files.internal(path));speech.setVolume(voiceVolume());speech.play();played++;}catch(RuntimeException error){failed++;Gdx.app.error("SurvivalSpeech",path,error);speech=null;}
    }
    private float voiceVolume(){return settings==null?.43f:settings.master*settings.voice*.86f*(duck>0?.35f:1);}
    private boolean danger(GameState s){return s==null||s.dead||s.oxygenation<70||s.reserve<RunEngine.lowReserveThreshold(s)||s.actors.stream().anyMatch(a->!a.dead&&s.visible[a.x][a.y]&&(a.aiming||a.fuse>=0||a.awareness==GameState.Awareness.PURSUE));}
    public void update(float dt,GameState s,boolean freeze){
        setPaused(freeze);if(freeze)return;
        quietFraction+=dt*1000;long whole=(long)quietFraction;quietMillis+=whole;quietFraction-=whole;
        captionRemaining-=dt;duck=Math.max(0,duck-dt);
        for(var effect:effects){effect.remaining-=dt;if(effect.remaining<=0)effect.sound.stop(effect.handle);}effects.removeIf(e->e.remaining<=0);mixEffects();
        scoreClock+=dt;if(scoreClock>=420&&s!=null){scoreClock=0;scoreRemaining=18;}scoreRemaining=Math.max(0,scoreRemaining-dt);
        if(active!=null&&!active.essential&&!ConversationCatalog.contextValid(s,active.id)){stopSpeech();active=null;}
        if(active==null&&!essentials.isEmpty())start(essentials.removeFirst(),s);
        if(active!=null){
            if(gap>0){gap-=dt;if(gap<=0)startLine();}
            else{lineElapsed+=dt;if(speech!=null)speech.setVolume(voiceVolume());if(lineElapsed>=active.lines.get(line).duration){completed.add("complete:"+active.id+":"+line);stopSpeech();line++;if(line>=active.lines.size())active=null;else gap=active.gap;}}
        }
        if(settings==null)return;
        boolean powered=s!=null&&s.circuitPower[s.circuits[s.playerX][s.playerY]],present=s!=null&&!s.dead;
        for(var e:loops.entrySet()){
            float v=switch(e.getKey()){
                case "powered"->present&&powered?.12f:0;case "unpowered"->present&&!powered?.10f:0;
                case "breath"->present&&s.oxygenation>=70?.16f:0;case "warning"->present&&s.oxygenation<70&&s.oxygenation>30?.18f:0;case "critical"->present&&s.oxygenation<=30?.19f:0;
                case "radio"->active!=null&&!settings.clearSpeech?settings.radio*.06f:0;case "score"->active==null&&scoreRemaining>0?.035f*settings.music*Math.min(1,Math.min(scoreRemaining,18-scoreRemaining)/3):0;default->0;
            };e.getValue().setVolume(v*settings.master*settings.sfx*(active!=null?.60f:1));
        }
    }
    private void setPaused(boolean value){if(paused==value)return;paused=value;for(var m:loops.values())if(value)m.pause();else m.play();for(var effect:effects)if(value)effect.sound.pause(effect.handle);else effect.sound.resume(effect.handle);if(speech!=null){if(value)speech.pause();else speech.play();}}
    public boolean speaking(){return active!=null||!essentials.isEmpty();}
    public boolean aliceSpeaking(){return active!=null&&gap<=0&&active.lines.get(line).speaker.equals("alice");}
    public String subtitle(){return active!=null&&gap<=0?(active.lines.get(line).speaker.equals("alice")?"A.L.I.C.E.":"OFFICER")+"  /  "+active.lines.get(line).text:"";}
    public String caption(){return captionRemaining>0?caption:"";}
    public List<String> transcript(){return List.copyOf(transcript);}
    /** Saved completion identities reconstruct a durable information archive, not a fabricated chronology. */
    public List<String> history(GameState s){var history=new ArrayList<String>();for(var ex:exchanges.values())for(int i=0;i<ex.lines.size();i++)if(s.completedSpeech.contains(ex.id+":"+i)){var l=ex.lines.get(i);history.add(ex.id.toUpperCase(Locale.ROOT)+" / "+l.speaker.toUpperCase(Locale.ROOT)+" / "+l.text);}return List.copyOf(history);}
    public long quietMillis(){return quietMillis;}
    public String takeCompleted(){return completed.pollFirst();}
    public void returnCompleted(String token){if(token!=null)completed.addFirst(token);}
    private void play(String key){
        if(settings==null||settings.master*settings.sfx==0)return;
        try{var sound=sounds.get(key);if(sound==null){String path="audio/survival/sfx/"+key+".wav";if(!Gdx.files.internal(path).exists())path="audio/sfx/"+key+".wav";if(!Gdx.files.internal(path).exists())return;sound=Gdx.audio.newSound(Gdx.files.internal(path));sounds.put(key,sound);}if(effects.size()>=6){var oldest=effects.removeFirst();oldest.sound.stop(oldest.handle);}long handle=sound.play(0);effects.add(new Effect(sound,handle,durations.getOrDefault(key,2f)));mixEffects();played++;}catch(RuntimeException e){failed++;Gdx.app.error("SurvivalEffect",key,e);}
    }
    private void mixEffects(){if(settings==null)return;float gain=settings.master*settings.sfx*.65f/(float)Math.sqrt(Math.max(1,effects.size()));if(settings.reducedDynamicRange)gain*=.8f;for(var e:effects)e.sound.setVolume(e.handle,gain);}
    private void stopSpeech(){if(speech!=null){speech.stop();speech.dispose();speech=null;}}
    public String summary(){return "Survival audio: played="+played+" failed="+failed+" loadedEffects="+sounds.size()+" conversations="+exchanges.size();}
    public void close(){stopSpeech();for(var m:loops.values()){m.stop();m.dispose();}for(var s:sounds.values())s.dispose();loops.clear();sounds.clear();}
}
