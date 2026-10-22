package io.zipcoder.originwar.desktop;
import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
/** Stable atlas slots; presentation-only helpers never read or write simulation state. */
public final class GameArt implements AutoCloseable {
    private Texture atlas,white,actors,sectors,finale,attacks,humans,playerWalk;
    private TextureRegion[][] cells;
    private TextureRegion[][] actorCells,sectorCells,attackCells,humanCells,walkCells;
    private TextureRegion whiteRegion;
    private final TextureRegion doorLeft=new TextureRegion(),doorRight=new TextureRegion();
    public GameArt(){
        var pixel=new Pixmap(1,1,Pixmap.Format.RGBA8888);pixel.setColor(Color.WHITE);pixel.fill();white=new Texture(pixel);whiteRegion=new TextureRegion(white);pixel.dispose();
        var file=Gdx.files.internal("art/station.png");if(file.exists()){atlas=new Texture(file);atlas.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);cells=TextureRegion.split(atlas,atlas.getWidth()/8,atlas.getHeight()/8);}
        var actorFile=Gdx.files.internal("art/animation/actors.png");if(actorFile.exists()){actors=new Texture(actorFile);actors.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);actorCells=TextureRegion.split(actors,actors.getWidth()/8,actors.getHeight()/8);}
        var sectorFile=Gdx.files.internal("art/sectors/sectors.png");if(sectorFile.exists()){sectors=new Texture(sectorFile);sectors.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);sectorCells=TextureRegion.split(sectors,sectors.getWidth()/8,sectors.getHeight()/4);}
        var finaleFile=Gdx.files.internal("art/finale/finale.png");if(finaleFile.exists()){finale=new Texture(finaleFile);finale.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);}
        var attackFile=Gdx.files.internal("art/animation/directional-actions/attack.png");if(attackFile.exists()){attacks=new Texture(attackFile);attacks.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);attackCells=TextureRegion.split(attacks,attacks.getWidth()/8,attacks.getHeight()/3);}
        var humanFile=Gdx.files.internal("art/animation/humans/humans.png");if(humanFile.exists()){humans=new Texture(humanFile);humans.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);humanCells=TextureRegion.split(humans,humans.getWidth()/8,humans.getHeight());}
        var walkFile=Gdx.files.internal("art/atmosphere/player-walk.png");if(walkFile.exists()){playerWalk=new Texture(walkFile);playerWalk.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);walkCells=TextureRegion.split(playerWalk,playerWalk.getWidth()/4,playerWalk.getHeight()/4);}
    }
    public boolean ready(){return cells!=null;}
    public Texture white(){return white;}
    public TextureRegion whiteRegion(){return whiteRegion;}
    public void draw(SpriteBatch batch,int slot,float x,float y,float size){if(cells!=null)batch.draw(cells[slot/8][slot%8],x,y,size,size);}
    public void actor(SpriteBatch batch,int family,int facing,int frame,float x,float y,float size){
        if(actorCells!=null)batch.draw(actorCells[family][Math.floorMod(facing,4)*2+Math.floorMod(frame,2)],x,y,size,size);
        else draw(batch,32+family*4+Math.floorMod(frame,4),x,y,size);
    }
    /** Progress is one full gait cycle supplied by the renderer; this method owns no clock. */
    public void playerWalking(SpriteBatch batch,int facing,float progress,float x,float y,float size){
        float phase=Float.isFinite(progress)?progress-(float)Math.floor(progress):0f;
        if(walkCells!=null)batch.draw(walkCells[Math.floorMod(facing,4)][Math.min(3,(int)(phase*4))],x,y,size,size);
        else actor(batch,0,facing,(int)(phase*2),x,y,size);
    }
    /** A physical split-panel door; the caller owns its visual transition and locked state. */
    public void door(SpriteBatch batch,boolean locked,float openFraction,float x,float y,float size){
        if(cells==null)return;
        float f=Float.isFinite(openFraction)?Math.max(0f,Math.min(1f,openFraction)):0f;
        TextureRegion closed=cells[1][locked?2:0];
        if(f<=0f){batch.draw(closed,x,y,size,size);return;}
        batch.draw(cells[1][1],x,y,size,size);
        if(f>=1f)return;
        // Only the inner panel travels; clip it behind the stationary open-door jambs.
        float remaining=.36f*(1f-f);
        crop(doorLeft,closed,.14f+.36f*f,.50f,.13f,.87f);
        crop(doorRight,closed,.50f,.86f-.36f*f,.13f,.87f);
        batch.draw(doorLeft,x+.14f*size,y+.13f*size,remaining*size,.74f*size);
        batch.draw(doorRight,x+(.50f+.36f*f)*size,y+.13f*size,remaining*size,.74f*size);
    }
    private static void crop(TextureRegion out,TextureRegion source,float x0,float x1,float y0,float y1){
        out.setTexture(source.getTexture());
        float du=source.getU2()-source.getU(),dv=source.getV2()-source.getV();
        out.setRegion(source.getU()+du*x0,source.getV()+dv*(1f-y1),source.getU()+du*x1,source.getV()+dv*(1f-y0));
    }
    /** Tiny native-pixel indicator cores only. Call after world darkening, on visible fixtures.
     * Kinds:0 terminal,1 switch,2 hatch jambs,3 wall strip. Phase is normalized by caller. */
    public void emissive(SpriteBatch batch,int kind,float phase,float x,float y,float size){
        Color before=batch.getColor();float r=before.r,g=before.g,b=before.b,a=before.a;
        float p=Float.isFinite(phase)?phase-(float)Math.floor(phase):0f;
        float pulse=p<.10f?1f:.35f;
        switch(kind){
            case 0 -> {
                batch.setColor(.36f,.72f,.72f,a*(.45f+.25f*pulse));
                core(batch,x,y,size,24,49,14,1);core(batch,x,y,size,24,47,9,1);
                batch.setColor(.95f,.56f,.18f,a*pulse);core(batch,x,y,size,40,40,2,2);
            }
            case 1 -> {batch.setColor(.95f,.58f,.20f,a*pulse);core(batch,x,y,size,35,42,2,2);}
            case 2 -> {
                batch.setColor(.62f,.76f,.69f,a*(.45f+.30f*pulse));
                core(batch,x,y,size,3,31,2,2);core(batch,x,y,size,58,31,2,2);
            }
            case 3 -> {batch.setColor(.74f,.78f,.70f,a*(.42f+.25f*pulse));core(batch,x,y,size,24,53,16,1);}
            default -> { }
        }
        batch.setColor(r,g,b,a);
    }
    private void core(SpriteBatch batch,float x,float y,float size,int px,int py,int w,int h){
        batch.draw(whiteRegion,x+px*size/64f,y+py*size/64f,w*size/64f,h*size/64f);
    }
    public void pose(SpriteBatch batch,int slot,float x,float y,float size){if(actorCells!=null)batch.draw(actorCells[slot/8][slot%8],x,y,size,size);else draw(batch,slot==54||slot==55?58:57,x,y,size);}
    public void attack(SpriteBatch batch,int family,int facing,int frame,float x,float y,float size){if(attackCells!=null)batch.draw(attackCells[family][Math.floorMod(facing,4)*2+Math.floorMod(frame,2)],x,y,size,size);else pose(batch,new int[]{40,44,48}[family]+Math.floorMod(frame,2),x,y,size);}
    public void sector(SpriteBatch batch,int slot,float x,float y,float size){if(sectorCells!=null)batch.draw(sectorCells[slot/8][slot%8],x,y,size,size);else if(slot<5)draw(batch,slot,x,y,size);else if(slot<10)draw(batch,5,x,y,size);}
    public boolean completeActors(){return actorCells!=null;}public boolean completeSectors(){return sectorCells!=null;}
    public boolean completeHumans(){return humanCells!=null;}
    public void human(SpriteBatch batch,int family,int frame,float x,float y,float size){if(humanCells!=null)batch.draw(humanCells[0][family*2+Math.floorMod(frame,2)],x,y,size,size);else if(family<3)draw(batch,52+family,x,y,size);else pose(batch,60+Math.floorMod(frame,2),x,y,size);}
    public boolean finaleReady(){return finale!=null;}
    public void finale(SpriteBatch batch,float width,float height){if(finale!=null){float scale=Math.max(width/finale.getWidth(),height/finale.getHeight());float w=finale.getWidth()*scale,h=finale.getHeight()*scale;batch.draw(finale,(width-w)/2,(height-h)/2,w,h);}}
    @Override public void close(){if(atlas!=null)atlas.dispose();if(actors!=null)actors.dispose();if(sectors!=null)sectors.dispose();if(finale!=null)finale.dispose();if(attacks!=null)attacks.dispose();if(humans!=null)humans.dispose();if(playerWalk!=null)playerWalk.dispose();white.dispose();}
}
