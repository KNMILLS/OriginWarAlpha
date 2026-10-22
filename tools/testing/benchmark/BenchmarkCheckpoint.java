package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.PlayerAction;
import java.nio.file.*;

/** An ordinary-command prefix, never a teleport or an altered encounter. */
public final class BenchmarkCheckpoint {
 public static void main(String[] args)throws Exception{
  if(args.length!=3)throw new IllegalArgumentException("Fresh profile, ordinary replay, prefix command count required");
  Path profile=Path.of(args[0]).toAbsolutePath().normalize();if(Files.exists(profile))throw new IllegalArgumentException("Fresh profile required");
  int limit=Integer.parseInt(args[2]),count=0;var run=new RunEngine(0);
  for(String line:Files.readAllLines(Path.of(args[1]))){
   if(line.isBlank()||line.startsWith("#"))continue;if(count==limit)break;
   String[] p=line.split("\t",-1);var action=new PlayerAction(PlayerAction.Type.valueOf(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),p.length>3?p[3]:"");var result=run.submit(action);
   if(!result.accepted())throw new IllegalStateException("Prefix rejected at "+count+": "+line+" / "+result.reason());count++;
  }
  if(count!=limit)throw new IllegalArgumentException("Replay shorter than prefix");var s=run.saveState();new SaveStore(profile).begin(s);
  System.out.println("Ordinary seed-zero prefix accepted="+count+" deck="+s.deck+" tick="+s.tick+" HP="+s.hp+" reserve="+s.reserve+"; all AI/rules retained; no native or pacing claim.");
 }
}
