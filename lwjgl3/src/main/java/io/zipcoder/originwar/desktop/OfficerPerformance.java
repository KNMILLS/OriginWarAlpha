package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.*;
import java.util.Set;

/** Presentation latch, derived only from an accepted command and the officer's own outcome. */
public final class OfficerPerformance {
 public record Pose(String action,float progress) {}
 private static final Set<PlayerAction.Type> WORK=Set.of(PlayerAction.Type.INTERACT,PlayerAction.Type.CIRCUIT,PlayerAction.Type.BRACE,PlayerAction.Type.HACK,PlayerAction.Type.FORCE,PlayerAction.Type.ANALYZE,PlayerAction.Type.PICKUP,PlayerAction.Type.DROP,PlayerAction.Type.EQUIP,PlayerAction.Type.USE);
 private String primary="idle",reaction="";private float primarySpan,reactionStart,reactionSpan;
 private float workSpan=.5f,workContact=1f/6;
 public static boolean workCommand(PlayerAction.Type type){return WORK.contains(type);}
 public static String workAction(PlayerAction.Type type){return switch(type){
  case PICKUP->"pickup";case DROP->"drop";case EQUIP->"equip";case USE->"supply";
  case CIRCUIT,BRACE,HACK,FORCE->"breaker";case INTERACT,ANALYZE->"terminal";default->"work";
 };}
 public void workTiming(float span,float contact){if(!Float.isFinite(span)||!Float.isFinite(contact)||span<=0||contact<0||contact>span)throw new IllegalArgumentException("Invalid work timing");workSpan=span;workContact=contact;}
 public void reset(){primary="idle";reaction="";primarySpan=reactionStart=reactionSpan=0;}
 public void accept(GameState before,GameState after,PlayerAction command,TurnResult result,float baseSpan,float hitSpan,float deathSpan){
  accept(before,after,command,result,baseSpan,hitSpan,deathSpan,baseSpan,baseSpan);
 }
 public void accept(GameState before,GameState after,PlayerAction command,TurnResult result,float baseSpan,float hitSpan,float deathSpan,float hitAt,float deathAt){
  reset();if(!result.accepted())return;
  boolean moved=before.deck==after.deck&&(before.playerX!=after.playerX||before.playerY!=after.playerY);
  boolean fired=result.events().stream().anyMatch(e->e.type().equals("gun")&&e.sourceId().equals("officer"));
  primary=moved?"walk":fired?"fire":command.type()==PlayerAction.Type.MELEE?"melee":before.facing!=after.facing&&command.type()==PlayerAction.Type.TURN?"turn_from_"+facing(before.facing):WORK.contains(command.type())&&result.cost()>0?workAction(command.type()):"idle";
  primarySpan=Math.max(0,baseSpan);
  // A nearby machine's hit/death does not cause a player reaction. Own HP is public truth.
  if(after.dead&&!before.dead){reaction="death";reactionSpan=deathSpan;reactionStart=deathAt;}
  else if(after.hp<before.hp){reaction="hit";reactionSpan=hitSpan;reactionStart=hitAt;}
 }
 private static String facing(int value){return new String[]{"north","east","south","west"}[value];}
 public Pose pose(float elapsed){
  if(!reaction.isEmpty()&&elapsed>=reactionStart&&(reaction.equals("death")||elapsed<reactionStart+reactionSpan))return new Pose(reaction,phase(elapsed-reactionStart,reactionSpan));
  if(Set.of("work","terminal","breaker","pickup","drop","equip","supply").contains(primary)){float hold=Math.max(0,primarySpan-workSpan),local=elapsed<=workContact?elapsed:elapsed<workContact+hold?workContact:elapsed-hold;return new Pose(primary,phase(local,workSpan));}
  return new Pose(primary,phase(elapsed,primarySpan));
 }
 private float phase(float time,float span){return span<=0?1:Math.max(0,Math.min(1,time/span));}
 public float seconds(){return Math.max(primarySpan,reactionStart+reactionSpan+(reaction.equals("death")?.35f:0));}
 public boolean deathPending(float elapsed){return reaction.equals("death")&&elapsed<seconds();}
}
