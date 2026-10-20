package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
/** Lead-owned stable atlas slots; generated source and exports are preserved under assets/art. */
public final class GameArt implements AutoCloseable {
    private Texture atlas,white;
    private TextureRegion[][] cells;
    public GameArt(){
        var pixel=new Pixmap(1,1,Pixmap.Format.RGBA8888);pixel.setColor(Color.WHITE);pixel.fill();white=new Texture(pixel);pixel.dispose();
        var file=Gdx.files.internal("art/station.png");if(file.exists()){atlas=new Texture(file);atlas.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);cells=TextureRegion.split(atlas,atlas.getWidth()/8,atlas.getHeight()/8);}
    }
    public boolean ready(){return cells!=null;}
    public Texture white(){return white;}
    public void draw(SpriteBatch batch,int slot,float x,float y,float size){if(cells!=null)batch.draw(cells[slot/8][slot%8],x,y,size,size);}
    @Override public void close(){if(atlas!=null)atlas.dispose();white.dispose();}
}
