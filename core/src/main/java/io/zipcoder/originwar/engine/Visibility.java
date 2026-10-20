package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.FOV;
import com.github.yellowstonegames.grid.Radius;
import io.zipcoder.originwar.model.GameState;

/** Vision and current light never disclose cells through opaque station bulkheads. */
final class Visibility {
    static final float VISION_RADIUS = 10f;
    private Visibility() {}

    static void refresh(GameState s) {
        float[][] resistance = new float[s.width][s.height];
        for (int x = 0; x < s.width; x++) for (int y = 0; y < s.height; y++)
            resistance[x][y] = s.tiles[x][y].blocksVision ? 1f : 0f;
        FOV.reuseFOV(resistance, s.light, s.playerX, s.playerY, VISION_RADIUS, Radius.CIRCLE);
        for (int x = 0; x < s.width; x++) for (int y = 0; y < s.height; y++) {
            s.visible[x][y] = s.light[x][y] > 0.001f;
            if (s.visible[x][y]) s.explored[x][y] = true;
        }
        s.visible[s.playerX][s.playerY] = true;
        s.explored[s.playerX][s.playerY] = true;
    }
}
