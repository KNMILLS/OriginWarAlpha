package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.ScreenUtils;
public final class OriginWarApp extends ApplicationAdapter {
    private SpriteBatch batch;private BitmapFont font;private int frame;private final String[] args;
    public OriginWarApp(String[] args){this.args=args;}
    @Override public void create(){batch=new SpriteBatch();font=new BitmapFont();}
    @Override public void render(){
        ScreenUtils.clear(.025f,.045f,.065f,1);batch.begin();font.getData().setScale(3);font.setColor(.6f,.9f,.9f,1);font.draw(batch,"ORIGIN WAR",90,Gdx.graphics.getHeight()-120);
        font.getData().setScale(1.3f);font.setColor(Color.LIGHT_GRAY);font.draw(batch,"2115  /  ORBITAL RESEARCH STATION",92,Gdx.graphics.getHeight()-175);font.draw(batch,"Modern runtime bootstrap - gameplay integration in progress",92,110);batch.end();
        frame++;for(String arg:args){if(arg.startsWith("--capture=")&&frame==60){var pix=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());PixmapIO.writePNG(Gdx.files.absolute(arg.substring(10)),pix,6,true);pix.dispose();}if(arg.startsWith("--frames=")&&frame>=Integer.parseInt(arg.substring(9)))Gdx.app.exit();}
    }
    @Override public void dispose(){batch.dispose();font.dispose();}
}
