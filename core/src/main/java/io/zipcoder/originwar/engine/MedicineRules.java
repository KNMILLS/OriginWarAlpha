package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.DeterministicRandom;
import io.zipcoder.originwar.model.GameEvent;
import io.zipcoder.originwar.model.GameState;

import java.util.ArrayList;
import java.util.List;

final class MedicineRules {
    static final String TOXIN_FLAG = "medicine-toxin-active";
    static final List<String> EFFECTS = List.of("heal", "antidote", "stimulant", "toxin");
    private MedicineRules() {}

    static void initialize(GameState s) {
        if (!s.medicineEffects.isEmpty()) return;
        var effects = new ArrayList<>(EFFECTS);
        String[] prefixes = {"Vel", "Cald", "Oro", "Nex", "Tera", "Sero", "Zyl", "Phar"};
        String[] suffixes = {"orin", "ax", "ex", "odyne", "erol", "adine"};
        for (int i = 0; i < EFFECTS.size(); i++) {
            String name;
            do {
                s.medicineRng = DeterministicRandom.nextState(s.medicineRng);
                String prefix = prefixes[DeterministicRandom.bounded(s.medicineRng, prefixes.length)];
                s.medicineRng = DeterministicRandom.nextState(s.medicineRng);
                name = prefix + suffixes[DeterministicRandom.bounded(s.medicineRng, suffixes.length)];
            } while (s.medicineEffects.containsKey(name));
            s.medicineRng = DeterministicRandom.nextState(s.medicineRng);
            s.medicineEffects.put(name, effects.remove(DeterministicRandom.bounded(s.medicineRng, effects.size())));
        }
    }

    static int use(GameState s, GameState.Item item, List<GameEvent> events) {
        String effect = s.medicineEffects.get(item.name);
        if (effect == null) return 0;
        s.identifiedMedicine.add(item.name);
        switch (effect) {
            case "heal" -> s.hp = Math.min(s.maxHp, s.hp + 32);
            case "antidote" -> { s.poisonTicks = 0; s.flags.remove(TOXIN_FLAG); }
            case "stimulant" -> s.oxygenation = Math.min(100, s.oxygenation + 35);
            case "toxin" -> {
                s.poisonTicks = Math.max(s.poisonTicks, 4);
                // Experimentation has a limited, warned consequence; this medicine cannot itself kill.
                s.flags.add(TOXIN_FLAG);
            }
            default -> throw new IllegalStateException("Unknown medicine effect");
        }
        if (--item.quantity == 0) s.inventory.remove(item);
        events.add(new GameEvent("medicine", s.playerX, s.playerY, item.name + " identified: " + effect + "."
                + (effect.equals("toxin") ? " Mild toxicity: four ticks, minimum one HP. Seek an antidote." : "")));
        return 1;
    }

    static boolean identifyOne(GameState s, List<GameEvent> events) {
        for (var item : s.inventory) if (item.type == GameState.ItemType.MEDICINE
                && s.medicineEffects.containsKey(item.name) && s.identifiedMedicine.add(item.name)) {
            events.add(new GameEvent("medicine", s.terminalX, s.terminalY,
                    "Medical analyzer: " + item.name + " is " + s.medicineEffects.get(item.name) + ". Dose preserved."));
            return true;
        }
        return false;
    }
}
