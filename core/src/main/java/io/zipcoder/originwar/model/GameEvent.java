package io.zipcoder.originwar.model;
/** Ordered presentation facts; concealed sources are redacted by the engine. */
public record GameEvent(String type,int x,int y,String detail,String sourceId,int fromX,int fromY,int facing,int tick,long actionId,boolean visible,boolean audible,String marker){
 public GameEvent(String type,int x,int y,String detail){this(type,x,y,detail,"",x,y,0,0,0,true,true,"contact");}
 public GameEvent atTick(int occurrence){return new GameEvent(type,x,y,detail,sourceId,fromX,fromY,facing,occurrence,actionId,visible,audible,marker);}
 public GameEvent stamped(GameState s,boolean seen,boolean heard){return new GameEvent(type,seen?x:-1,seen?y:-1,detail,seen?sourceId:"",seen?fromX:-1,seen?fromY:-1,facing,tick==0?s.tick:tick,s.actionSerial,seen,heard,marker);}
}
