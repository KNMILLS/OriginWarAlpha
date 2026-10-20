package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.backends.lwjgl3.*;
public final class DesktopLauncher {
    public static void main(String[] args) {
        var config=new Lwjgl3ApplicationConfiguration();config.setTitle("Origin War");config.setWindowedMode(1280,720);config.useVsync(true);config.setForegroundFPS(60);
        new Lwjgl3Application(new OriginWarApp(args),config);
    }
}
