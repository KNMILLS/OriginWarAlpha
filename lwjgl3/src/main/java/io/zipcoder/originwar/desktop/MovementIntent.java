package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.PlayerAction;
import java.util.LinkedHashMap;
import java.util.HashSet;

/** One manual intent, never a route or a speculative simulation commitment. */
final class MovementIntent {
 private final LinkedHashMap<Integer,PlayerAction> held=new LinkedHashMap<>();
 private final HashSet<Integer> pressed=new HashSet<>();
 private PlayerAction pending;private float until;
 boolean press(int key,PlayerAction action,float now){
  if(!pressed.add(key))return false; // OS keyboard repeat cannot rebuild a canceled hold.
  held.put(key,action);pending=action;until=now+.18f;return true;
 }
 void release(int key){held.remove(key);pressed.remove(key);}
 PlayerAction peek(float now){return held.isEmpty()?(now<=until?pending:null):held.values().stream().reduce((a,b)->b).orElse(null);}
 void consumed(){pending=null;}
 void clear(){held.clear();pending=null;until=0;}
}
