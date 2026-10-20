package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
/** Lead-owned stable atlas slots; generated source and exports are preserved under assets/art. */
public final class GameArt implements AutoCloseable {
    private Texture atlas,white,actors,sectors,finale;
    private TextureRegion[][] cells;
    private TextureRegion[][] actorCells,sectorCells;
    private TextureRegion whiteRegion;
    public GameArt(){
        var pixel=new Pixmap(1,1,Pixmap.Format.RGBA8888);pixel.setColor(Color.WHITE);pixel.fill();white=new Texture(pixel);whiteRegion=new TextureRegion(white);pixel.dispose();
        var file=Gdx.files.internal("art/station.png");if(file.exists()){atlas=new Texture(file);atlas.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);cells=TextureRegion.split(atlas,atlas.getWidth()/8,atlas.getHeight()/8);}
        var actorFile=Gdx.files.internal("art/animation/actors.png");if(actorFile.exists()){actors=new Texture(actorFile);actors.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);actorCells=TextureRegion.split(actors,actors.getWidth()/8,actors.getHeight()/8);}
        var sectorFile=Gdx.files.internal("art/sectors/sectors.png");if(sectorFile.exists()){sectors=new Texture(sectorFile);sectors.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);sectorCells=TextureRegion.split(sectors,sectors.getWidth()/8,sectors.getHeight()/4);}
        var finaleFile=Gdx.files.internal("art/finale/finale.png");if(finaleFile.exists()){finale=new Texture(finaleFile);finale.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);}
    }
    public boolean ready(){return cells!=null;}
    public Texture white(){return white;}
    public TextureRegion whiteRegion(){return whiteRegion;}
    public void draw(SpriteBatch batch,int slot,float x,float y,float size){if(cells!=null)batch.draw(cells[slot/8][slot%8],x,y,size,size);}
    public void actor(SpriteBatch batch,int family,int facing,int frame,float x,float y,float size){
        if(actorCells!=null)batch.draw(actorCells[family][Math.floorMod(facing,4)*2+Math.floorMod(frame,2)],x,y,size,size);
        else draw(batch,32+family*4+Math.floorMod(frame,4),x,y,size);
    }
    public void pose(SpriteBatch batch,int slot,float x,float y,float size){if(actorCells!=null)batch.draw(actorCells[slot/8][slot%8],x,y,size,size);else draw(batch,slot==54||slot==55?58:57,x,y,size);}
    public void sector(SpriteBatch batch,int slot,float x,float y,float size){if(sectorCells!=null)batch.draw(sectorCells[slot/8][slot%8],x,y,size,size);else if(slot<5)draw(batch,slot,x,y,size);else if(slot<10)draw(batch,5,x,y,size);}
    public boolean completeActors(){return actorCells!=null;}public boolean completeSectors(){return sectorCells!=null;}
    public boolean finaleReady(){return finale!=null;}
    public void finale(SpriteBatch batch,float width,float height){if(finale!=null){float scale=Math.max(width/finale.getWidth(),height/finale.getHeight());float w=finale.getWidth()*scale,h=finale.getHeight()*scale;batch.draw(finale,(width-w)/2,(height-h)/2,w,h);}}
    @Override public void close(){if(atlas!=null)atlas.dispose();if(actors!=null)actors.dispose();if(sectors!=null)sectors.dispose();if(finale!=null)finale.dispose();white.dispose();}
}
