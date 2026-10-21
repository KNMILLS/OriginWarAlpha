package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.*;
import java.util.*;

/** Named illustrated regions retain author-defined sizes and planted foot anchors. */
public final class IllustratedArt implements AutoCloseable {
    public record Region(TextureRegion texture,float width,float height,float pivotX,float pivotY,String layer) {}
    private final Map<String,Region> regions=new HashMap<>();
    private final Map<String,Texture> textures=new HashMap<>();
    private final Texture white;
    public IllustratedArt(){
        var p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fill();white=new Texture(p);p.dispose();
        var file=Gdx.files.internal("art/survival/regions.json");
        if(!file.exists())throw new GdxRuntimeException("Missing illustrated region manifest");
        var root=new JsonReader().parse(file);
        for(var v:root.get("regions")){
            var texture=textures.computeIfAbsent(v.getString("path"),path->{var t=new Texture(Gdx.files.internal(path));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t;});
            regions.put(v.getString("name"),new Region(new TextureRegion(texture),v.getFloat("widthCells"),v.getFloat("heightCells"),v.getFloat("pivotX"),v.getFloat("pivotY"),v.getString("layer","world")));
        }
        // Optional motion manifest has the same region contract plus named clips.
        var motion=Gdx.files.internal("art/survival/motion/clips.json");
        if(motion.exists())loadMotion(new JsonReader().parse(motion));
    }
    private final Map<String,List<String>> clips=new HashMap<>();
    private void loadMotion(JsonValue root){
        var entries=root.get("regions");if(entries!=null)for(var v:entries){
            var texture=textures.computeIfAbsent(v.getString("path"),path->{var t=new Texture(Gdx.files.internal(path));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t;});
            int x=v.getInt("x",0),y=v.getInt("y",0),w=v.getInt("pixelWidth",texture.getWidth()),h=v.getInt("pixelHeight",texture.getHeight());
            regions.put(v.getString("name"),new Region(new TextureRegion(texture,x,y,w,h),v.getFloat("widthCells"),v.getFloat("heightCells"),v.getFloat("pivotX",.5f),v.getFloat("pivotY",0),v.getString("layer","world")));
        }
        var definitions=root.get("clips");if(definitions!=null)for(var clip:definitions){var frames=new ArrayList<String>();String id=clip.getString("id");int index=0;for(var frame:clip.get("frames")){
            String name=id+"."+index++,path=frame.getString("path");
            var texture=textures.computeIfAbsent(path,p->{var t=new Texture(Gdx.files.internal(p));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t;});
            regions.put(name,new Region(new TextureRegion(texture),frame.getFloat("widthCells"),frame.getFloat("heightCells"),frame.getFloat("pivotX",.5f),frame.getFloat("pivotY",0),"world"));frames.add(name);
        }clips.put(id,List.copyOf(frames));}
    }
    public boolean has(String name){return regions.containsKey(name);}
    public String frame(String clip,float progress,String fallback){var frames=clips.get(clip);return frames==null||frames.isEmpty()?fallback:frames.get(Math.min(frames.size()-1,(int)(Math.max(0,progress)*frames.size())));}
    public void draw(SpriteBatch batch,String name,float x,float y,float cell,Color tint){var r=regions.get(name);if(r==null)return;float w=r.width*cell,h=r.height*cell;batch.setColor(tint);batch.draw(r.texture,x-r.pivotX*w,y-r.pivotY*h,w,h);batch.setColor(Color.WHITE);}
    public void rect(SpriteBatch b,float x,float y,float w,float h,Color c){b.setColor(c);b.draw(white,x,y,w,h);b.setColor(Color.WHITE);}
    public int count(){return regions.size();}
    public void close(){for(var t:textures.values())t.dispose();white.dispose();}
}
