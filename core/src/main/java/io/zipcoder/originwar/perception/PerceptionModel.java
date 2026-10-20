package io.zipcoder.originwar.perception;

import io.zipcoder.originwar.model.DeterministicRandom;
import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.Tile;
import java.util.ArrayList;
import java.util.List;

/** A pure view of an unreliable experience. Nothing here writes the station, its actors or its RNGs. */
public final class PerceptionModel {
    private PerceptionModel() {}
    public enum Stage { CLEAR, WARNING, CRITICAL }
    public record Impression(int x, int y, int variant, float opacity) {}
    public record Frame(Stage stage, float intensity, float vignette, float pulse,
                        float driftX, float driftY, List<Impression> impressions,
                        String status, long cueKey) {
        public Frame { impressions = List.copyOf(impressions); }
    }

    /** phase is elapsed presentation seconds; it affects only slow pulses and bounded camera drift. */
    public static Frame sample(GameState s, double phase, boolean reducedDistortion) {
        int oxygen = Math.max(0, Math.min(100, s.oxygenation));
        if (oxygen >= 70 || s.dead || s.victory)
            return new Frame(Stage.CLEAR, 0, 0, 0, 0, 0, List.of(), "", -1);
        Stage stage = oxygen <= 30 ? Stage.CRITICAL : Stage.WARNING;
        float severity = (70 - oxygen) / 70f;
        // No strobe, no screen rotation and no manipulation of the pointer or reliable HUD.
        float pulse = (float)(.5 + .5 * Math.sin(phase * (stage == Stage.CRITICAL ? 3.2 : 2.1)));
        float drift = reducedDistortion ? 0 : 1.5f * severity;
        long window = s.tick / 6L;
        long key = DeterministicRandom.mix(s.perceptionRng ^ (window * 0x9E3779B97F4A7C15L) ^ s.deck);
        var impressions = new ArrayList<Impression>();
        // Stable during a small turn window. Only genuinely visible, passable, unoccupied cells qualify.
        if (stage == Stage.CRITICAL || (key & 3) != 0) {
            var candidates = new ArrayList<int[]>();
            for (int x = 1; x < s.width - 1; x++) for (int y = 1; y < s.height - 1; y++) {
                if (!s.visible[x][y] || s.tiles[x][y].blocksMovement ||
                        (x == s.playerX && y == s.playerY) ||
                        Math.abs(x - s.playerX) > 4 || Math.abs(y - s.playerY) > 2 ||
                        Math.abs(x - s.playerX) + Math.abs(y - s.playerY) < 3) continue;
                Tile tile = s.tiles[x][y];
                if (tile == Tile.SWITCH || tile == Tile.HATCH || tile == Tile.TERMINAL) continue;
                final int cx = x, cy = y;
                if (s.actors.stream().anyMatch(a -> !a.dead && a.x == cx && a.y == cy) ||
                        s.floorItems.stream().anyMatch(i -> i.x == cx && i.y == cy)) continue;
                candidates.add(new int[]{x,y});
            }
            int count = stage == Stage.CRITICAL ? 2 : 1;
            for (int n = 0; n < count && !candidates.isEmpty(); n++) {
                int index = (int)Math.floorMod(DeterministicRandom.mix(key + n), candidates.size());
                int[] cell = candidates.remove(index);
                impressions.add(new Impression(cell[0], cell[1], (int)((key >>> (n*4)) & 1), .24f + severity*.35f));
            }
        }
        return new Frame(stage, severity, .12f + .2f*severity + .05f*pulse, pulse,
                reducedDistortion ? 0 : drift*(float)Math.sin(phase*.8), reducedDistortion ? 0 : drift*(float)Math.cos(phase*.65), impressions,
                stage == Stage.CRITICAL ? "CRITICAL HYPOXIA / perceptions unreliable" : "HYPOXIA / verify with suit sensors",
                key);
    }
}
