package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual all-AI ordinary commands through production file saves, including physical finale operations. */
class SurvivalCampaignPersistenceTest {
 @TempDir Path profiles;
 @Test void bothConstructorCampaignsMatchUninterruptedPlayThroughRealSaveResume()throws Exception{
  Path root=Files.isDirectory(Path.of("../tools"))?Path.of(".."):Path.of(".");
  for(String approach:List.of("controller","redirect")){
   var uninterrupted=new RunEngine(0);var resumed=new RunEngine(0);var store=new SaveStore(profiles.resolve(approach));store.begin(resumed.saveState());int count=0,checkpoints=0,lastDeck=1;boolean refusalSaved=false,broadcastSaved=false;
   for(String line:Files.readAllLines(root.resolve("tools/testing/survival/replays/seed0-"+approach+".tsv"))){
    if(line.isBlank()||line.startsWith("#"))continue;String[] p=line.split("\t",-1);var action=new PlayerAction(PlayerAction.Type.valueOf(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),p[3]);
    var expected=uninterrupted.submit(action);var actual=resumed.submit(action);assertTrue(actual.accepted(),approach+" command "+count+": "+actual.reason());assertEquals(expected,actual);count++;
    var state=resumed.saveState();boolean refusal=state.flags.contains("enforcement-refused"),broadcast=state.flags.contains("broadcast");
    if(state.deck!=lastDeck||count%200==0||refusal&&!refusalSaved||broadcast&&!broadcastSaved||state.victory){
     store.save(state);resumed=new RunEngine(new SaveStore(profiles.resolve(approach)).load());store=new SaveStore(profiles.resolve(approach));store.load();
     assertEquals(canonical(uninterrupted.saveState()),canonical(resumed.saveState()),approach+" checkpoint "+count);checkpoints++;lastDeck=state.deck;refusalSaved|=refusal;broadcastSaved|=broadcast;
    }
   }
   var end=resumed.saveState();assertTrue(end.victory);assertFalse(end.dead);assertEquals(approach,end.constructorRoute);assertTrue(end.flags.containsAll(Set.of("enforcement-refused","broadcast","pod-sealed","alice-sacrificed","station-destroyed")));assertTrue(refusalSaved&&broadcastSaved);assertTrue(checkpoints>=12);assertEquals(approach.equals("controller")?2434:2420,end.tick);System.out.println("File campaign "+approach+": commands="+count+" checkpoints="+checkpoints+" tick="+end.tick+" HP="+end.hp+" reserve="+end.reserve+"; all AI active.");
  }
 }
 static String canonical(Object value)throws Exception{
  if(value==null)return "null";if(value instanceof Number||value instanceof Boolean||value instanceof Enum<?>||value instanceof CharSequence)return value.getClass().getName()+":"+value;
  if(value instanceof Map<?,?> map){var entries=new ArrayList<String>();for(var e:map.entrySet())entries.add(canonical(e.getKey())+"="+canonical(e.getValue()));Collections.sort(entries);return entries.toString();}
  if(value instanceof Iterable<?> list){var entries=new ArrayList<String>();for(var e:list)entries.add(canonical(e));if(value instanceof Set<?>)Collections.sort(entries);return entries.toString();}
  if(value.getClass().isArray()){var entries=new ArrayList<String>();for(int i=0;i<Array.getLength(value);i++)entries.add(canonical(Array.get(value,i)));return entries.toString();}
  var entries=new TreeMap<String,String>();for(var f:value.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))entries.put(f.getName(),canonical(f.get(value)));return entries.toString();
 }
}
