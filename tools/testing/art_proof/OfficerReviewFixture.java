package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import java.nio.file.*;

/** Explicit synthetic presentation probes, never a generated-campaign/pacing witness. */
public final class OfficerReviewFixture {
 public static void main(String[] args)throws Exception{
  if(args.length<2||args.length>3)throw new IllegalArgumentException("Fresh profile directory and injury|fatal|early-hit required");
  Path profile=Path.of(args[0]).toAbsolutePath().normalize();if(Files.exists(profile))throw new IllegalArgumentException("Fixture requires a fresh directory; existing profiles are preserved");
  String mode=args[1];if(!java.util.Set.of("injury","fatal","early-hit").contains(mode))throw new IllegalArgumentException("Unknown fixture mode");
  var state=ArtProofScenario.create();state.hp=mode.equals("fatal")?1:100;if(args.length==3)state.facing=Integer.parseInt(args[2]);
  state.flags.add("officer-review-fixture:"+mode);
  var machine=state.actors.getFirst();machine.x=machine.homeX=10;machine.y=machine.homeY=9;machine.facing=3;machine.aiming=true;machine.nextActTick=0;
  PlayerAction command=args.length==3?PlayerAction.of(PlayerAction.Type.WAIT):new PlayerAction(mode.equals("early-hit")?PlayerAction.Type.CAREFUL_MOVE:PlayerAction.Type.MOVE,1,0,"");
  boolean found=false;for(long seed=1;seed<100;seed++){
   state.combatRng=seed;var trial=new RunEngine(state);var result=trial.submit(command);
   if(result.accepted()&&trial.saveState().hp<state.hp){found=true;break;}
  }
  if(!found)throw new IllegalStateException("No deterministic injury fixture found");
  state.log.add("SYNTHETIC OFFICER PRESENTATION FIXTURE / "+mode+". Not campaign difficulty or ordinary placement evidence.");
  new SaveStore(profile).begin(new RunEngine(state).saveState());
  System.out.println("Created synthetic "+mode+" fixture at "+profile+"; hp="+state.hp+"; combatRng="+state.combatRng+"; commit="+command.type());
 }
}
