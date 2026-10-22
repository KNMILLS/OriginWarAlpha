package io.zipcoder.originwar.model;
import java.util.List;
public record TurnResult(boolean accepted,int cost,List<GameEvent> events,String reason,long revision,boolean stateChanged){
 public TurnResult{events=List.copyOf(events);}
 public TurnResult(boolean accepted,int cost,List<GameEvent> events){this(accepted,cost,events,"",0,accepted&&cost>0);}
 public static TurnResult rejected(){return rejected("That action is unavailable.");}
 public static TurnResult rejected(String reason){return new TurnResult(false,0,List.of(),reason,0,false);}
}
