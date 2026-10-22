package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.backends.lwjgl3.*;
public final class DesktopLauncher {
    public static void main(String[] args) {
        int width=1280,height=720;
        String title="Origin War";
        for(String arg:args){if(arg.startsWith("--width="))width=Math.max(1024,Integer.parseInt(arg.substring(8)));if(arg.startsWith("--height="))height=Math.max(720,Integer.parseInt(arg.substring(9)));}
        for(String arg:args)if(arg.startsWith("--window-title="))title=arg.substring(15);
        var config=new Lwjgl3ApplicationConfiguration();config.setTitle(title);config.setWindowedMode(width,height);config.setWindowSizeLimits(1024,720,-1,-1);config.setPauseWhenLostFocus(true);config.useVsync(true);config.setForegroundFPS(60);config.setAudioConfig(64,512,9);
        var app=new OriginWarApp(args);
        config.setWindowListener(new Lwjgl3WindowAdapter(){
            @Override public boolean closeRequested(){return app.requestClose();}
            @Override public void focusLost(){app.pause();}
            @Override public void focusGained(){app.resume();}
        });
        new Lwjgl3Application(app,config);
    }
}
