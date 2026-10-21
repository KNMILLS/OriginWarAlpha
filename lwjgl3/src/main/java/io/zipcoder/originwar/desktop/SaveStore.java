package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.utils.*;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.engine.RunEngine;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Local, bounded JSON persistence; no Java object deserialization or graphics dependency. */
public final class SaveStore {
    public static final class Envelope {
        public int version=1;
        public String runId="";
        public GameState state;
    }
    private final Path directory;
    private String runId="";
    private boolean usedBackup;
    public SaveStore(Path directory) { this.directory=directory.toAbsolutePath().normalize(); }
    private Json json() {
        var j=new Json();j.setOutputType(JsonWriter.OutputType.json);j.setUsePrototypes(false);j.setTypeName(null);
        j.setElementType(GameState.class,"actors",GameState.Actor.class);
        j.setElementType(GameState.class,"floorItems",GameState.Item.class);
        j.setElementType(GameState.class,"inventory",GameState.Item.class);
        j.setElementType(GameState.class,"medicineEffects",String.class);
        j.setElementType(GameState.class,"journal",String.class);j.setElementType(GameState.class,"log",String.class);
        j.setElementType(GameState.class,"flags",String.class);j.setElementType(GameState.class,"identifiedMedicine",String.class);
        return j;
    }
    public boolean exists() { return Files.isRegularFile(directory.resolve("active.json"))||Files.isRegularFile(directory.resolve("active.backup.json")); }
    public boolean usedBackup() { return usedBackup; }
    public void begin(GameState state) throws IOException {
        validate(state);Files.createDirectories(directory);
        var envelope=new Envelope();envelope.runId=UUID.randomUUID().toString();envelope.state=state;
        String text=json().toJson(envelope,Envelope.class);
        // Initialize BOTH candidates for this expedition. Never resurrect a previous run.
        atomicWrite(directory.resolve("active.backup.json"),text);
        atomicWrite(directory.resolve("active.json"),text);
        runId=envelope.runId;
    }
    public void save(GameState state) throws IOException {
        if(runId.isEmpty())throw new IOException("No active run. Start or load a run first.");
        validate(state);write(state,true);
    }
    private void write(GameState state, boolean backup) throws IOException {
        Files.createDirectories(directory);
        var env=new Envelope();env.runId=runId;env.state=state;
        Path primary=directory.resolve("active.json"),temp=directory.resolve("active.tmp");
        if(state.dead)atomicWrite(directory.resolve("death.marker"),runId);
        if(backup&&Files.isRegularFile(primary)) {
            // Never rotate malformed data over the last known-good backup.
            try { read(primary);Files.copy(primary,directory.resolve("active.backup.json"),StandardCopyOption.REPLACE_EXISTING); }
            catch(IOException ignored) { /* Keep the good backup. The new validated state still saves. */ }
        }
        Files.writeString(temp,json().toJson(env,Envelope.class),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
        moveAtomic(temp,primary);
    }
    public GameState load() throws IOException {
        usedBackup=false;IOException first;
        try { return accept(read(directory.resolve("active.json"))); }
        catch(EndedRunException ended){throw ended;}
        catch(IOException error){first=error;}
        try { var s=accept(read(directory.resolve("active.backup.json")));usedBackup=true;return s; }
        catch(IOException second){throw new IOException("Cannot resume primary or backup save: "+first.getMessage()+"; "+second.getMessage(),second);}
    }
    private GameState accept(Envelope env) throws IOException {
        Path marker=directory.resolve("death.marker");
        if(Files.isRegularFile(marker)&&Files.size(marker)<128&&Files.readString(marker).trim().equals(env.runId))throw new EndedRunException();
        runId=env.runId;return env.state.copy();
    }
    private Envelope read(Path path) throws IOException {
        if(!Files.isRegularFile(path)||Files.size(path)>4_000_000)throw new IOException("Missing or oversized save");
        try {
            var text=Files.readString(path,StandardCharsets.UTF_8);var tree=new JsonReader().parse(text);
            if(!tree.isObject()||tree.get("state")==null||!tree.get("state").isObject()||tree.get("runId")==null||tree.get("version")==null)throw new IOException("Incomplete save envelope");
            var env=json().fromJson(Envelope.class,text);
            if(env.version!=1||env.runId==null||!env.runId.matches("[0-9a-f-]{36}"))throw new IOException("Unsupported save envelope");
            validate(env.state);return env;
        } catch(SerializationException|IllegalArgumentException|NullPointerException e){throw new IOException("Malformed save: "+e.getMessage(),e);}
    }
    private void atomicWrite(Path path,String text)throws IOException {
        Path temporary=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(temporary,text,StandardCharsets.UTF_8);moveAtomic(temporary,path);
    }
    private void moveAtomic(Path source,Path target)throws IOException {
        // Windows scanners can briefly hold the previous file without delete sharing.
        // Keep the old primary/backup intact and bound the wait; persistent faults still surface.
        for(int attempt=0;;attempt++){
            try {
                try {Files.move(source,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(source,target,StandardCopyOption.REPLACE_EXISTING);}
                return;
            }catch(AccessDeniedException locked){
                if(attempt==4)throw locked;
                try {Thread.sleep(25L<<attempt);}
                catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Save replacement interrupted",interrupted);}
            }
        }
    }
    public static void validate(GameState s)throws IOException {
        try{RunEngine.validateState(s);}catch(RuntimeException error){throw new IOException("Invalid simulation state: "+error.getMessage(),error);}
        if(s==null||s.schemaVersion!=1||s.generationVersion!=1)throw new IOException("Unsupported save schema");
        if(s.width<8||s.height<8||s.width>128||s.height>128||s.deck<1||s.deck>10||s.tick<0)throw new IOException("Invalid map bounds/progression");
        if(s.tiles==null||s.visible==null||s.explored==null||s.light==null||s.tiles.length!=s.width||s.visible.length!=s.width||s.explored.length!=s.width||s.light.length!=s.width)throw new IOException("Incomplete map");
        for(int x=0;x<s.width;x++) {
            if(s.tiles[x]==null||s.visible[x]==null||s.explored[x]==null||s.light[x]==null||s.tiles[x].length!=s.height||s.visible[x].length!=s.height||s.explored[x].length!=s.height||s.light[x].length!=s.height)throw new IOException("Invalid map columns");
            for(int y=0;y<s.height;y++)if(s.tiles[x][y]==null||!Float.isFinite(s.light[x][y])||s.light[x][y]<0||s.light[x][y]>1)throw new IOException("Invalid terrain/light");
        }
        if(!inside(s,s.playerX,s.playerY)||s.tiles[s.playerX][s.playerY].blocksMovement)throw new IOException("Invalid player placement");
        if(s.maxHp<1||s.maxHp>1000||s.hp<0||s.hp>s.maxHp||(!s.dead&&s.hp==0)||s.str<1||s.str>50||s.intel<1||s.intel>50||s.dex<1||s.dex>50)throw new IOException("Invalid character stats");
        if(s.maxReserve<1||s.maxReserve>10000||s.reserve<0||s.reserve>s.maxReserve||s.oxygenation<0||s.oxygenation>100||s.batteries<0||s.batteries>10000||s.poisonTicks<0)throw new IOException("Invalid resources");
        if(s.actors==null||s.floorItems==null||s.inventory==null||s.flags==null||s.log==null||s.journal==null||s.medicineEffects==null||s.identifiedMedicine==null||s.inventory.size()>24||s.actors.size()>512||s.floorItems.size()>512||s.log.size()>1000||s.journal.size()>1000)throw new IOException("Invalid collections");
        var ids=new HashSet<String>();
        for(var actor:s.actors)if(actor==null||actor.kind==null||actor.id==null||actor.id.isBlank()||!ids.add(actor.id)||!inside(s,actor.x,actor.y)||actor.hp<0||actor.maxHp<1||actor.hp>actor.maxHp)throw new IOException("Invalid actor");
        ids.clear();for(var item:s.inventory)validateItem(item,ids);for(var item:s.floorItems){validateItem(item,ids);if(!inside(s,item.x,item.y))throw new IOException("Invalid pickup position");}
        if(s.weapon==null||s.armor==null||s.activeDialogue==null||s.bossPhase==null||s.constructorRoute==null)throw new IOException("Missing runtime state");
        for(var line:s.log)if(line==null||line.length()>4000)throw new IOException("Invalid log text");
        for(var line:s.journal)if(line==null||line.length()>8000)throw new IOException("Invalid journal text");
    }
    private static boolean inside(GameState s,int x,int y){return x>=0&&y>=0&&x<s.width&&y<s.height;}
    private static void validateItem(GameState.Item i,Set<String> ids)throws IOException {
        if(i==null||i.type==null||i.id==null||i.id.isBlank()||i.id.length()>200||!ids.add(i.id)||i.name==null||i.name.length()>200||i.quantity<1||i.quantity>10000)throw new IOException("Invalid item");
    }
    public static final class EndedRunException extends IOException {
        public EndedRunException(){super("This expedition has ended. Start a new game.");}
    }
}
