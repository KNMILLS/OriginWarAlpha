package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.GameState;
import java.util.*;

/** Reads generated inert art descriptors; never classifies concealed geometry or objectives. */
public final class WorkplaceLayout {
 public record Recess(int x,int y,String region) {}
 private final Map<Integer,String> furniture=new HashMap<>();private final ArrayList<Recess> recesses=new ArrayList<>();private GameState previous;private int roomCount,width;
 public void update(GameState state){
  if(state==previous)return;previous=state;furniture.clear();recesses.clear();roomCount=0;width=state.width;
  for(var f:state.fixtures){if(f.kind.startsWith("art-furniture:"))furniture.put(f.y*width+f.x,f.kind.substring("art-furniture:".length()));else if(f.kind.startsWith("art-recess:"))recesses.add(new Recess(f.x,f.y,f.kind.substring("art-recess:".length())));else if(f.kind.startsWith("art-room:"))roomCount++;}
 }
 public String furniture(GameState.Fixture fixture){return fixture.kind.equals("cargo")?furniture.getOrDefault(fixture.y*width+fixture.x,""):"";}
 public String fallback(String region){return Set.of("prop.inspection-desk","prop.maintenance-bench","prop.inspection-terminal").contains(region)?"prop.workbench":region.equals("prop.receiving-bulkhead")?"prop.equipment-rack":region.equals("prop.cargo-cradle")?"prop.cargo":region;}
 public int roomCount(){return roomCount;}
 public List<Recess> recesses(){return List.copyOf(recesses);}
}
