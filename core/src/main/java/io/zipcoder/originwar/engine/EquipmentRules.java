package io.zipcoder.originwar.engine;
import io.zipcoder.originwar.model.GameState;
public final class EquipmentRules{
 public record Weapon(int range,int batteries,int damage,int accuracy){}
 private EquipmentRules(){}
 public static Weapon weapon(String id){return switch(id){case "pistol"->new Weapon(7,1,10,86);case "rifle"->new Weapon(11,2,15,93);case "scattergun"->new Weapon(4,3,20,84);default->new Weapon(1,0,0,0);};}
 public static int protection(String armor){return switch(armor){case "light"->3;case "heavy"->5;default->1;};}
 public static int movementNoise(GameState s){return s.armor.equals("heavy")?2:0;}
 public static String weaponId(GameState.ItemType type){return switch(type){case PISTOL->"pistol";case RIFLE->"rifle";case SCATTERGUN->"scattergun";default->"";};}
 public static String armorId(GameState.ItemType type){return switch(type){case ARMOR_LIGHT->"light";case ARMOR_HEAVY->"heavy";default->"";};}
 public static int bulk(GameState.ItemType type){return switch(type){case PISTOL,SERVICE_PROBE->2;case RIFLE,SCATTERGUN,ARMOR_LIGHT->4;case OXYGEN->3;case ARMOR_HEAVY->6;default->1;};}
 public static int bulk(GameState s){int n=0;for(var i:s.inventory)if(!s.flags.contains("equipped-armor:"+i.id))n=Math.addExact(n,Math.multiplyExact(i.quantity,bulk(i.type)));return n;}
}
