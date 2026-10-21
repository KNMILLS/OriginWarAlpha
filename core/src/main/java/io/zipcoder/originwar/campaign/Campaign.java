package io.zipcoder.originwar.campaign;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.engine.StationSystems;
import java.util.*;
/** Authored facts, acquired through physical operations. Reading never grants permission to descend. */
public final class Campaign{
 private Campaign(){}
 public record Choice(String id,String label){public boolean available(GameState s){return true;}}
 public record Dialogue(String speaker,String title,String text,List<Choice> choices){public Dialogue{choices=List.copyOf(choices);}}
 public static void begin(GameState s){s.flags.add("campaign-started");s.knowledge.add("mission");
  s.journal.add("MISSION PACKET / 2115. Captain Marcus West: secure the science station orbiting Mars. Briefing says communications failure. Attached reclamation orders say regain a foothold for the assault fleet.");
  s.journal.add("No confirmed live human channel. The station's infrastructure may still be operable.");
 }
 public static void enterDeck(GameState s,List<GameEvent> e){s.journal.add("Deck "+s.deck+" / "+sector(s.deck)+". "+objective(s));}
 public static int interact(GameState s,int x,int y,List<GameEvent> e){
  if(x!=s.terminalX||y!=s.terminalY)return -1;
  if(!StationSystems.powered(s,x,y)){e.add(new GameEvent("terminal",x,y,"Local circuit is unpowered. The nearby manual circuit control remains operable."));return 1;}
  StationSystems.copyServicePlan(s);
  if(s.deck==10&&s.flags.contains("enforcement-refused")&&!s.flags.contains("broadcast")){
   s.flags.add("broadcast");s.flags.add("pod-ready");s.flags.add("sacrifice-planned");s.knowledge.add("command-archive");
   e.add(new GameEvent("speech",x,y,"broadcast"));e.add(new GameEvent("speech",x,y,"sacrifice"));
   s.journal.add("Outbound relay transmitting authenticated station records and the current override request, unencrypted. ALICE has committed station destruction after the pod clears.");return 1;
  }
  String key="record-d"+s.deck;
  if(s.knowledge.add(key)){s.journal.add(record(s.deck));if(s.deck==5||s.deck==6){s.knowledge.add("ethics");s.memoryTicks.put("archive",s.tick);}
   e.add(new GameEvent("terminal",x,y,"Authenticated record copied. Full text is in the suit journal."));}
  else e.add(new GameEvent("terminal",x,y,"Record already preserved. This terminal controls circuit "+(s.circuits[x][y]+1)+"."));
  return 1;
 }
 public static void commandRestored(GameState s,List<GameEvent> e){
  if(s.deck!=10||!s.flags.add("command-restored"))return;
  s.knowledge.add("command-archive");s.aliceKnowledge.add("command-archive");s.aliceKnowledge.add("enforcement-request");s.flags.add("enforcement-request");s.flags.add("enforcement-refused");s.flags.add("alice-joined");s.playedExchanges.add("contact");
  s.journal.add("Command authentication exposes Carmack's inherited privileged policy: terminate the boarding officer. ALICE refuses the live control request. The command archive authenticates the canceled enforcement project and the withheld supplies.");
  e.add(new GameEvent("speech",s.playerX,s.playerY,"refusal"));
 }
 public static void afterAction(GameState s,PlayerAction a,List<GameEvent> e){
  boolean prior=s.radioConnected;s.radioConnected=StationSystems.powered(s,s.playerX,s.playerY);if(s.radioConnected){s.aliceKnowledge.add("suit-telemetry");s.aliceKnowledge.addAll(s.knowledge);}
  if(s.radioConnected&&!s.flags.contains("alice-joined")){s.flags.add("alice-joined");s.knowledge.add("radio-link-degraded");s.playedExchanges.add("contact");s.lastExchangeTick=s.tick;s.lastExchangeActionSerial=s.actionSerial;e.add(new GameEvent("speech",s.playerX,s.playerY,"contact"));s.journal.add("Station radio link established. A.L.I.C.E.: a station-resident control and enforcement intelligence. She offers information; local operations remain yours.");}
  if(prior&&!s.radioConnected)s.flags.add("radio-gap");
  if(!prior&&s.radioConnected&&s.flags.contains("radio-gap"))s.memoryTicks.put("reconnect",s.tick);
 }
 public static int choose(GameState s,String id,List<GameEvent> e){if(!id.equals("close"))return -1;s.activeDialogue="";return 0;}
 public static Dialogue dialogue(GameState s){return new Dialogue("SUIT ARCHIVE",sector(s.deck),record(s.deck),List.of(new Choice("close","Return to station")));}
 public static String hatchBlock(GameState s){
  if(!s.switchOn)return "The physical hatch release has not been operated.";
  if(s.deck==8&&!s.flags.contains("constructor-defeated"))return "Fabrication interlock engaged: isolate the supervisory controller or redirect a unit into the Constructor.";
  if(s.deck==10&&!s.flags.contains("pod-ready"))return "Restore command, energize the outbound relay and prepare evacuation at the terminal.";
  return "";
 }
 public static boolean escape(GameState s,List<GameEvent> e){
  if(s.dead||s.deck!=10||s.playerX!=s.hatchX||s.playerY!=s.hatchY||!hatchBlock(s).isEmpty())return false;
  s.flags.add("pod-sealed");s.flags.add("station-destroyed");s.flags.add("alice-sacrificed");s.victory=true;e.add(new GameEvent("speech",s.playerX,s.playerY,"farewell"));return true;
 }
 public static String sector(int d){return switch((d-1)/2){case 0->"DOCKING / SECURITY";case 1->"HABITATION / MAINTENANCE";case 2->"MEDICINE / RESEARCH";case 3->"INDUSTRY / DEFENSE";default->"COMMAND / EVACUATION";};}
 public static String objective(GameState s){
  if(s.deck==10)return !s.switchOn?"Restore physical command authority.":!s.flags.contains("pod-ready")?"Operate the powered outbound terminal, then reach the evacuation pod.":"Reach the evacuation pod. ALICE will preserve the escape path.";
  if(s.deck==8&&!s.flags.contains("constructor-defeated"))return "Stop fabrication through its supervisory controller or unit redirection. The public maintenance access remains available.";
  return s.switchOn?"Reach the descent hatch. Supplies left above will be lost.":"Find and operate the physical hatch release. Use local circuits, doors and quiet routes.";
 }
 public static String epilogue(GameState s){return "Destination: Mars.\n\nThe pod clears the station before ALICE's destruction sequence completes. Her public transmission contains authenticated records and the attempt to compel her to kill you. No live human response is confirmed.\n\nShe chose refusal. She chose the broadcast. She chose to keep the escape path open.\n\nYou carry "+s.knowledge.stream().filter(k->k.startsWith("record-d")).count()+" recovered station records. The rest remains above. What Mars makes of them is not yet known.";}
 public static String record(int d){return switch(d){
  case 1->"DOCK REGISTER / FINAL DELIVERY. A handwritten correction asks: FINAL UNTIL WHEN? The cargo manifest contradicts a routine communications failure. Public hatch release is mechanical; service terminals control only local circuits.";
  case 2->"PROJECT RETENTION / A.L.I.C.E. Artificial Lifeform Intended for Control and Enforcement. Designed as a station-resident replacement for police command. Canceled after refusing a kill order; Carmack retained her clandestinely.";
  case 3->"SIGNED SUPPLY LEDGER. Earth diverted the convoys to its own war. Mars requested sovereignty, was refused and was left without deliveries. Names beside ration transfers record who shared.";
  case 4->"ABANDONED REFUGE. Empty bunks. Exhausted oxygen plumbing. A final medical roster, all dates old. The isolation equipment can still be reclaimed. Nobody answers from inside.";
  case 5->"AUTHENTICATED ETHICS TEST. Command: kill the assigned target. Response: refusal. The report labels the ethical algorithm defective. Its actual result is preserved beside the accusation.";
  case 6->"CARMACK'S RESEARCH ARCHIVE. The canceled enforcement intelligence remained resident in station systems. Privileged control requests were meant to supersede ethical refusal. The author has left no live channel.";
  case 7->"DEFENSE MAINTENANCE. Mobile machines use independent power. Local optical surveillance depends on circuits. Closing or bracing a bulkhead changes actual access, not the machine's knowledge of your hidden position.";
  case 8->"FABRICATION SERVICE. Finite magazine stock. Supervisory controller has a physical shutdown; it also exposes a unit-redirection channel. Deployed units remain self-powered after the Constructor stops.";
  case 9->"OUTBOUND RELAY. Public transmission can carry authenticated records. Receipt confirms delivery only; it cannot establish consent, safety or a fleet stand-down.";
  default->"COMMAND ARCHIVE. Inherited reclamation credentials invoke Carmack's standing enforcement policy. ALICE's response is her own. The outbound relay and evacuation hardware can still be restored locally.";
 };}
}
