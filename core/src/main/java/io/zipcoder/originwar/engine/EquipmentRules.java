package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.GameState;

/** Shared production equipment numbers; UI can describe these same public definitions. */
public final class EquipmentRules {
    public record Weapon(int range, int batteries, int damage, int accuracy) {}
    private EquipmentRules() {}
    public static Weapon weapon(String id) {
        return switch (id) {
            case "rifle" -> new Weapon(11, 2, 15, 81);
            case "scattergun" -> new Weapon(4, 3, 20, 72);
            default -> new Weapon(7, 1, 10, 74);
        };
    }
    public static int effectiveDex(GameState s) {
        return effectiveDex(s.dex,s.armor);
    }
    public static int effectiveDex(int dex,String armor){return Math.max(1,dex+(armor.equals("light")?1:armor.equals("heavy")?-2:0));}
    public static int protection(String armor) {
        return switch (armor) { case "light" -> 3; case "heavy" -> 5; default -> 1; };
    }
    static String weaponId(GameState.ItemType type) {
        return switch (type) { case PISTOL -> "pistol"; case RIFLE -> "rifle"; case SCATTERGUN -> "scattergun"; default -> ""; };
    }
    static String armorId(GameState.ItemType type) {
        return switch (type) { case ARMOR_LIGHT -> "light"; case ARMOR_HEAVY -> "heavy"; default -> ""; };
    }
}
