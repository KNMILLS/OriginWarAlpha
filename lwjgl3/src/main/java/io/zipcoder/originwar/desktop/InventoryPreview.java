package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.EquipmentRules;
import io.zipcoder.originwar.model.*;

/** Known immediate handling only; no forecast of medicine effects or intervening enemies. */
final class InventoryPreview {
 private InventoryPreview(){}
 static String describe(GameState s,GameState.Item item,PlayerAction.Type type,ActionForecast forecast){
  String label=type==PlayerAction.Type.PICKUP?"COLLECT":type.name();
  if(!forecast.available())return label+" / unavailable";
  String detail="";int bulk=EquipmentRules.bulk(s),unit=EquipmentRules.bulk(item.type);
  if(type==PlayerAction.Type.PICKUP)detail="pack "+bulk+" > "+(bulk+unit)+" / 12";
  else if(type==PlayerAction.Type.DROP)detail="pack "+bulk+" > "+(bulk-(s.flags.contains("equipped-armor:"+item.id)?0:unit))+" / 12";
  else if(type==PlayerAction.Type.EQUIP)detail=!EquipmentRules.armorId(item.type).isEmpty()?"shell mount; previous shell retained or left":"pack "+bulk+" > "+(bulk+(item.x>=0?unit:0))+" / 12";
  else if(type==PlayerAction.Type.USE){
   if(item.type==GameState.ItemType.OXYGEN||item.type==GameState.ItemType.CELL){int free=item.type==GameState.ItemType.OXYGEN?s.maxReserve-s.reserve:12-s.batteries;int transfer=Math.min(free,item.power);detail="transfer "+transfer+"; container remainder "+(item.power-transfer);}
   else detail="consume one dose";
  }
  return label+" / "+forecast.ticks()+" tick"+(forecast.ticks()==1?"":"s")+" / "+detail+(forecast.warnings().isEmpty()?"":" / "+forecast.warnings().getFirst());
 }
}
