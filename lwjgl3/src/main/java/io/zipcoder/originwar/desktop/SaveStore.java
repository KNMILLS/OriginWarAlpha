package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.utils.*;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.engine.RunEngine;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
/** Version2 per-expedition snapshots. Call from one background writer under ProfileLease. */
public class SaveStore{
 public static final class Envelope{public int version=2;public String runId="";public GameState state;}
 private final Path directory;private String runId="";private boolean usedBackup;
 public SaveStore(Path p){directory=p.toAbsolutePath().normalize();}
 private Json json(){
  var j=new Json();j.setOutputType(JsonWriter.OutputType.json);j.setUsePrototypes(false);j.setTypeName(null);
  j.setElementType(GameState.class,"actors",GameState.Actor.class);j.setElementType(GameState.class,"floorItems",GameState.Item.class);j.setElementType(GameState.class,"inventory",GameState.Item.class);
  j.setElementType(GameState.class,"fixtures",GameState.Fixture.class);j.setElementType(GameState.class,"heard",GameState.HeardContact.class);
  for(String field:List.of("flags","identifiedMedicine","knowledge","aliceKnowledge","playedExchanges","completedSpeech","journal","log","medicineEffects"))j.setElementType(GameState.class,field,String.class);
  j.setElementType(GameState.class,"memoryTicks",Integer.class);return j;
 }
 public boolean exists(){return Files.isRegularFile(directory.resolve("active.run"));}
 public boolean usedBackup(){return usedBackup;}
 public Path activeDirectory()throws IOException{return directory.resolve("runs").resolve(activeId());}
 private String activeId()throws IOException{
  Path p=directory.resolve("active.run");if(!Files.isRegularFile(p)||Files.size(p)>128)throw new IOException("No current survival expedition.");
  String id=Files.readString(p).trim();if(!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IOException("Malformed expedition pointer.");return id;
 }
 public void begin(GameState s)throws IOException{
  validate(s);String id=UUID.randomUUID().toString();Path folder=directory.resolve("runs").resolve(id);Files.createDirectories(folder);
  String text=encode(s,id);atomicWrite(folder.resolve("active.backup.json"),text);atomicWrite(folder.resolve("active.json"),text);
  beforeActivate(id);atomicWrite(directory.resolve("active.run"),id);runId=id;
 }
 /** Fault-injection seam: the previous pointer is still intact here. */
 protected void beforeActivate(String id)throws IOException{}
 private String encode(GameState s,String id){var env=new Envelope();env.runId=id;env.state=s;return json().toJson(env,Envelope.class);}
 public void save(GameState s)throws IOException{
  if(runId.isEmpty())throw new IOException("Load or begin an expedition first.");validate(s);Path folder=directory.resolve("runs").resolve(runId);Files.createDirectories(folder);
  if(s.dead)atomicWrite(folder.resolve("death.marker"),runId);
  Path primary=folder.resolve("active.json");
  if(Files.isRegularFile(primary)){try{read(primary,runId);atomicWrite(folder.resolve("active.backup.json"),Files.readString(primary));}catch(IOException invalid){/* retain last known good backup */}}
  atomicWrite(primary,encode(s,runId));
 }
 public GameState load()throws IOException{
  String id=activeId();Path folder=directory.resolve("runs").resolve(id);usedBackup=false;
  if(Files.isRegularFile(folder.resolve("death.marker")))throw new EndedRunException();
  IOException first;
  try{var env=read(folder.resolve("active.json"),id);runId=id;return env.state.copy();}
  catch(IOException e){first=e;}
  try{var env=read(folder.resolve("active.backup.json"),id);runId=id;usedBackup=true;return env.state.copy();}
  catch(IOException e){throw new IOException("Cannot resume primary or backup: "+first.getMessage()+"; "+e.getMessage(),e);}
 }
 private Envelope read(Path path,String expected)throws IOException{
  if(!Files.isRegularFile(path)||Files.size(path)>4_000_000)throw new IOException("Missing or oversized snapshot.");
  try{String text=Files.readString(path,StandardCharsets.UTF_8);var tree=new JsonReader().parse(text);if(!tree.isObject()||tree.get("state")==null||tree.get("version")==null||tree.get("runId")==null)throw new IOException("Incomplete snapshot.");
   var world=tree.get("state");requireFields(world,GameState.class);
   for(String group:List.of("actors","inventory","floorItems","fixtures","heard")){Class<?> dto=switch(group){case "actors"->GameState.Actor.class;case "fixtures"->GameState.Fixture.class;case "heard"->GameState.HeardContact.class;default->GameState.Item.class;};if(world.get(group)!=null)for(var value:world.get(group))requireFields(value,dto);}
   var env=json().fromJson(Envelope.class,text);
   if(env.version!=2||!expected.equals(env.runId))throw new IOException("Unsupported or mismatched expedition.");validate(env.state);return env;
  }catch(SerializationException|IllegalArgumentException|NullPointerException e){throw new IOException("Malformed survival snapshot: "+e.getMessage(),e);}
 }
 private static void requireFields(JsonValue object,Class<?> type)throws IOException{if(!object.isObject())throw new IOException("Expected complete "+type.getSimpleName()+" object.");for(var field:type.getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&object.get(field.getName())==null)throw new IOException("Missing saved field: "+field.getName());}
 protected void atomicWrite(Path path,String text)throws IOException{
  Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");byte[] data=text.getBytes(StandardCharsets.UTF_8);
  try(var channel=FileChannel.open(temp,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(data);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
  for(int attempt=0;;attempt++){try{try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}return;}
   catch(AccessDeniedException e){if(attempt==4)throw e;try{Thread.sleep(25L<<attempt);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Snapshot write interrupted.",interrupted);}}}
 }
 public static void validate(GameState s)throws IOException{try{RunEngine.validateState(s);}catch(RuntimeException e){throw new IOException("Invalid survival state: "+e.getMessage(),e);}}
 public static final class EndedRunException extends IOException{public EndedRunException(){super("This expedition has ended. Begin a new descent.");}}
}
