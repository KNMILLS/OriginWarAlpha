package io.zipcoder.originwar.campaign;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.engine.RunEngine;
import java.util.*;
/** Eligibility is factual; performed audio is a separate presentation binding. */
public final class ConversationCatalog{
 private ConversationCatalog(){}
 public static final List<String> OPTIONAL=List.of("reconnect","recovery","archive","injury","packdrop","force","listen","careful","lamp","descent","quiet");
 public static boolean eligible(GameState s,String id){
  return !s.playedExchanges.contains(id)&&contextValid(s,id);
 }
 public static boolean contextValid(GameState s,String id){
  if(!s.radioConnected||s.dead||s.victory||s.oxygenation<70||s.reserve<RunEngine.lowReserveThreshold(s))return false;
  if(s.actors.stream().anyMatch(a->!a.dead&&s.visible[a.x][a.y]&&(a.aiming||a.fuse>=0||a.awareness==GameState.Awareness.PURSUE)))return false;
  int age=s.tick-s.memoryTicks.getOrDefault(id,-10000);
  return switch(id){
   case "archive"->s.knowledge.contains("ethics")&&s.aliceKnowledge.contains("ethics");
   case "injury"->s.hp<80&&age<12;
   case "lamp"->!s.lampOn;
   case "descent"->s.playerX==s.hatchX&&s.playerY==s.hatchY&&s.switchOn;
   case "quiet"->s.tick>16;
   default->age>=0&&age<16;
  };
 }
 public static String next(GameState s,long quietMillis,int frequency){
  if(frequency<=0||quietMillis<(frequency==1?360_000:180_000)||s.actionSerial-s.lastExchangeActionSerial<8)return "";
  return OPTIONAL.stream().filter(id->eligible(s,id)).findFirst().orElse("");
 }
}
