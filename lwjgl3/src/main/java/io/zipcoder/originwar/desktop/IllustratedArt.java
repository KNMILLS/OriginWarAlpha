package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.utils.*;
import java.util.*;

/** Named illustrated regions retain author-defined bounds and planted foot anchors. */
public final class IllustratedArt implements AutoCloseable {
 public record Region(TextureRegion texture,float width,float height,float pivotX,float pivotY,String layer) {}
 public record Motion(float cellSeconds,float cycleCells,float startSeconds,float stopSeconds,List<Float> contacts) {}
 private final Map<String,Region> regions=new HashMap<>();
 private final Map<String,Texture> textures=new HashMap<>();
 private final Map<String,AnimationClip> clips=new HashMap<>();
 private final Map<String,String> proofBindings=new HashMap<>();
 private final Set<String> proofStyles=new HashSet<>();
 private final Map<String,Motion> motionReferences=new HashMap<>();
 private final Map<String,Float> durationReferences=new HashMap<>();
 private final Texture white,shadow;
 private boolean proofLoaded;
 public IllustratedArt(){
  var p=new Pixmap(1,1,Pixmap.Format.RGBA8888);p.setColor(Color.WHITE);p.fill();white=new Texture(p);p.dispose();
  var s=new Pixmap(64,24,Pixmap.Format.RGBA8888);s.setBlending(Pixmap.Blending.None);
  for(int y=0;y<24;y++)for(int x=0;x<64;x++){float d=(float)Math.sqrt(Math.pow((x-31.5f)/32,2)+Math.pow((y-11.5f)/12,2));s.setColor(0,0,0,Math.max(0,1-d)*.65f);s.drawPixel(x,y);}shadow=new Texture(s);shadow.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);s.dispose();
  var file=Gdx.files.internal("art/survival/regions.json");if(!file.exists())throw new GdxRuntimeException("Missing illustrated region manifest");
  loadRegions(new JsonReader().parse(file),"");
  loadRegions(new JsonReader().parse(Gdx.files.internal("art/survival/equipment-motion/regions.json")),"");
  var motion=Gdx.files.internal("art/survival/motion/clips.json");if(motion.exists())loadMotion(new JsonReader().parse(motion),"");
  loadMotion(new JsonReader().parse(Gdx.files.internal("art/survival/equipment-motion/clips.json")),"");
 }
 private Texture texture(String path){return textures.computeIfAbsent(path,key->{var t=new Texture(Gdx.files.internal(key));t.setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);return t;});}
 private Region region(JsonValue value){var t=texture(value.getString("path"));int x=value.getInt("x",0),y=value.getInt("y",0),w=value.getInt("pixelWidth",t.getWidth()),h=value.getInt("pixelHeight",t.getHeight());return new Region(new TextureRegion(t,x,y,w,h),value.getFloat("widthCells"),value.getFloat("heightCells"),value.getFloat("pivotX",.5f),value.getFloat("pivotY",0),value.getString("layer","world"));}
 private void loadRegions(JsonValue root,String prefix){var values=root.get("regions");if(values!=null)for(var v:values)regions.put(prefix+v.getString("name"),region(v));}
 private void loadMotion(JsonValue root,String prefix){
  loadRegions(root,prefix);var definitions=root.get("clips");if(definitions==null)return;
  for(var clip:definitions){var frames=new ArrayList<AnimationClip.Frame>();var starts=new ArrayList<Float>();String id=prefix+clip.getString("id");float elapsed=0,step=clip.getFloat("frameSeconds",1f/24);int index=0;
   for(var frame:clip.get("frames")){String name=prefix+frame.getString("region",clip.getString("id")+"."+index++);if(frame.has("path"))regions.put(name,region(frame));if(!regions.containsKey(name))throw new GdxRuntimeException("Missing animation region "+name);float span=frame.getFloat("seconds",frame.getFloat("durationSeconds",step))*frame.getInt("hold",1);starts.add(elapsed);elapsed+=span;frames.add(new AnimationClip.Frame(name,span));}
   var marks=new ArrayList<AnimationClip.Marker>();var entries=clip.get("markers");if(entries!=null)for(var marker:entries){int frame=marker.getInt("frame",0);if(frame<0||frame>starts.size())throw new GdxRuntimeException("Invalid marker frame "+id);marks.add(new AnimationClip.Marker(marker.getString("kind"),marker.getFloat("seconds",frame==starts.size()?elapsed:starts.get(frame))));}
   clips.put(id,new AnimationClip(frames,marks,clip.getBoolean("loop",false),clip.getFloat("cycleCells",0)));
  }
 }
 /** Load just the selected candidate. Switching frees its predecessor's GPU pages. */
 public void loadProof(String style){
  if(proofStyles.contains(style))return;
  if(!proofLoaded){proofLoaded=true;var environment=Gdx.files.internal("art/proof/environment/atlas.json");if(environment.exists())loadRegions(new JsonReader().parse(environment),"proof.environment.");}
  String folder=style.equals("illustrated")?"art/production/officer/":"art/proof/"+style+"/";
  var pack=Gdx.files.internal(folder+"atlas.json");if(!pack.exists())throw new GdxRuntimeException("Missing candidate atlas "+pack);
  for(String old:List.copyOf(proofStyles)){
   String prefix="proof."+old+".",oldFolder=old.equals("illustrated")?"art/production/officer/":"art/proof/"+old+"/";
   regions.keySet().removeIf(k->k.startsWith(prefix));clips.keySet().removeIf(k->k.startsWith(prefix));proofBindings.keySet().removeIf(k->k.startsWith(old+":"));
   for(String path:List.copyOf(textures.keySet()))if(path.startsWith(oldFolder))textures.remove(path).dispose();proofStyles.remove(old);
  }
  var root=new JsonReader().parse(pack);String prefix="proof."+style+".";loadMotion(root,prefix);proofStyles.add(style);var bindings=root.get("bindings");if(bindings!=null)for(var b:bindings){String clip=prefix+b.getString("clip");if(!clips.containsKey(clip))throw new GdxRuntimeException("Unbound proof animation "+clip);String key=proofKey(style,b.getString("weapon","pistol"),b.getString("armor","suit"),b.getString("action"),b.getString("facing"));if(proofBindings.putIfAbsent(key,clip)!=null)throw new GdxRuntimeException("Duplicate proof binding "+key);}
  if(style.equals("illustrated")&&textureBytes()>192L*1024*1024)throw new GdxRuntimeException("Active art texture budget exceeded; revise atlas residency before campaign expansion");
  if(bindings!=null)for(var b:bindings)durationReferences.put(proofKey(style,b.getString("weapon","pistol"),b.getString("armor","suit"),b.getString("action"),b.getString("facing")),clipSeconds(prefix+b.getString("clip"),0));
  if(bindings!=null)for(var b:bindings)if(b.getString("action").equals("walk")){
   String weapon=b.getString("weapon","pistol"),armor=b.getString("armor","suit"),face=b.getString("facing"),walk=prefix+b.getString("clip");float seconds=clipSeconds(walk,.5f),cells=cycleCells(walk,1);
   motionReferences.put(proofKey(style,weapon,armor,"walk",face),new Motion(seconds/cells,cells,clipSeconds(proofClip(style,weapon,armor,"start",face),0),clipSeconds(proofClip(style,weapon,armor,"stop",face),0),markers(walk).stream().filter(m->m.kind().equals("contact")).map(m->m.seconds()/seconds).toList()));
  }
 }
 public Motion proofMotion(String style,String weapon,String armor,String face){return motionReferences.getOrDefault(proofKey(style,weapon,armor,"walk",face),new Motion(.5f,1,0,0,List.of()));}
 public float proofReferenceSeconds(String style,String weapon,String armor,String action,String face,float fallback){return durationReferences.getOrDefault(proofKey(style,weapon,armor,action,face),fallback);}
 private String proofKey(String style,String weapon,String armor,String action,String facing){return String.join(":",style,weapon,armor,action,facing);}
 public String proofClip(String style,String weapon,String armor,String action,String facing){String prefix="proof."+style+".";String bound=proofBindings.get(proofKey(style,weapon,armor,action,facing));if(bound!=null)return bound;String full=prefix+"officer."+weapon+"."+armor+"."+action+"."+facing;if(hasClip(full))return full;if(armor.equals("suit")){String equipment=prefix+"officer."+weapon+"."+action+"."+facing;if(hasClip(equipment))return equipment;if(weapon.equals("pistol")){String old=prefix+"officer."+action+"."+facing;if(hasClip(old))return old;}}return "";}
 public boolean proofAvailable(String style){return proofStyles.contains(style);}
 public String environment(String name,boolean active){String replacement="proof.environment."+name;return active&&has(replacement)?replacement:name;}
 public boolean has(String name){return regions.containsKey(name);}
 public String frame(String clip,float progress,String fallback){var c=clips.get(clip);return c==null?fallback:c.phase(progress);}
 public String timedFrame(String clip,float seconds,String fallback){var c=clips.get(clip);return c==null?fallback:c.at(seconds);}
 public String proofFireFrame(String clip,float seconds){var c=clips.get(clip);return c==null?"":c.retimedFire(seconds,.5f,.14f);}
 public boolean hasClip(String id){return clips.containsKey(id);}
 public float clipSeconds(String id,float fallback){var c=clips.get(id);return c==null?fallback:c.seconds();}
 public float cycleCells(String id,float fallback){var c=clips.get(id);return c==null||c.cycleCells()<=0?fallback:c.cycleCells();}
 public float markerSeconds(String clip,String marker,float fallback){var c=clips.get(clip);return c==null?fallback:c.marker(marker,fallback);}
 public List<AnimationClip.Marker> markers(String clip){var c=clips.get(clip);return c==null?List.of():c.markers();}
 public void draw(SpriteBatch batch,String name,float x,float y,float cell,Color tint){var r=regions.get(name);if(r==null)return;float w=r.width*cell,h=r.height*cell;batch.setColor(tint);batch.draw(r.texture,x-r.pivotX*w,y-r.pivotY*h,w,h);batch.setColor(Color.WHITE);}
 public void contactShadow(SpriteBatch batch,float x,float y,float cell,float alpha){batch.setColor(1,1,1,alpha);batch.draw(shadow,x-cell*.30f,y-cell*.065f,cell*.60f,cell*.18f);batch.setColor(Color.WHITE);}
 public void litGround(SpriteBatch batch,String name,float x,float y,float cell,float bl,float tl,float tr,float br){var r=regions.get(name);if(r==null)return;float u=r.texture.getU(),v=r.texture.getV2(),u2=r.texture.getU2(),v2=r.texture.getV();float[] vertices={x,y,packed(bl),u,v,x,y+cell,packed(tl),u,v2,x+cell,y+cell,packed(tr),u2,v2,x+cell,y,packed(br),u2,v};batch.draw(r.texture.getTexture(),vertices,0,20);}
 private float packed(float light){return Color.toFloatBits(Math.min(1,light*.95f),Math.min(1,light),Math.min(1,light*1.02f),1);}
 public void rect(SpriteBatch b,float x,float y,float w,float h,Color c){b.setColor(c);b.draw(white,x,y,w,h);b.setColor(Color.WHITE);}
 public int count(){return regions.size();}
 public int textureCount(){return textures.size();}
 public long textureBytes(){long size=0;for(var t:textures.values())size+=(long)t.getWidth()*t.getHeight()*4;return size;}
 public void close(){for(var t:textures.values())t.dispose();white.dispose();shadow.dispose();}
}
