package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.backends.lwjgl3.*;
public final class DesktopLauncher {
    public static void main(String[] args) {
        int width=1280,height=720;
        for(String arg:args){if(arg.startsWith("--width="))width=Math.max(1024,Integer.parseInt(arg.substring(8)));if(arg.startsWith("--height="))height=Math.max(720,Integer.parseInt(arg.substring(9)));}
        var config=new Lwjgl3ApplicationConfiguration();config.setTitle("Origin War");config.setWindowedMode(width,height);config.setWindowSizeLimits(1024,720,-1,-1);config.setPauseWhenLostFocus(true);config.useVsync(true);config.setForegroundFPS(60);
        new Lwjgl3Application(new OriginWarApp(args),config);
    }
}
