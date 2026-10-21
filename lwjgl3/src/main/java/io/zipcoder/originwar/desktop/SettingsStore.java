package io.zipcoder.originwar.desktop;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
public final class SettingsStore {
    public float master=.8f,music=.55f,sfx=.7f,voice=.7f,radio=.35f,uiScale=1;
    public boolean reducedDistortion,helmetLight=true;
    private final Path path;
    public SettingsStore(Path directory){
        path=directory.resolve("settings.properties");
        try{
            if(Files.exists(path)){
                var p=new Properties();try(var in=Files.newInputStream(path)){p.load(in);}
                master=value(p,"master",master,0,1);music=value(p,"music",music,0,1);
                sfx=value(p,"sfx",sfx,0,1);voice=value(p,"voice",sfx,0,1);radio=value(p,"radio",radio,0,1);uiScale=value(p,"uiScale",1,1,2);
                reducedDistortion=Boolean.parseBoolean(p.getProperty("reducedDistortion","false"));
                helmetLight=Boolean.parseBoolean(p.getProperty("helmetLight","true"));
            }
        }catch(IOException ignored){}
    }
    private float value(Properties p,String key,float fallback,float min,float max){
        try{float v=Float.parseFloat(p.getProperty(key,""));return Float.isFinite(v)?Math.max(min,Math.min(max,v)):fallback;}
        catch(NumberFormatException e){return fallback;}
    }
    public void save()throws IOException{
        Files.createDirectories(path.getParent());var p=new Properties();
        p.setProperty("master",""+master);p.setProperty("music",""+music);p.setProperty("sfx",""+sfx);
        p.setProperty("voice",""+voice);p.setProperty("radio",""+radio);
        p.setProperty("uiScale",""+uiScale);p.setProperty("reducedDistortion",""+reducedDistortion);
        p.setProperty("helmetLight",""+helmetLight);
        try(var out=Files.newOutputStream(path)){p.store(out,"Origin War audio and accessibility");}
    }
}
