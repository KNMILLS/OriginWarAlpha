package io.zipcoder.originwar.model;
public record PlayerAction(Type type,int x,int y,String id){
 public enum Type { MOVE,CAREFUL_MOVE,WAIT,LISTEN,TURN,LAMP,INTERACT,SHOOT,MELEE,PICKUP,USE,DROP,EQUIP,HACK,FORCE,SCAN,ADVANCE,CHOOSE,CIRCUIT,BRACE,ANALYZE,RADIO,EXCHANGE }
 public static PlayerAction of(Type type){return new PlayerAction(type,0,0,"");}
 public static PlayerAction move(int dx,int dy){return new PlayerAction(Type.MOVE,dx,dy,"");}
 public static PlayerAction at(Type type,int x,int y){return new PlayerAction(type,x,y,"");}
 public static PlayerAction item(Type type,String id){return new PlayerAction(type,0,0,id);}
}
