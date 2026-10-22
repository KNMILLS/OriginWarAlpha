package io.zipcoder.originwar.model;
import java.util.List;
/** Pure known-fact description, never a simulated RNG outcome. */
public record ActionForecast(boolean available,int ticks,int cells,int noise,String explanation,List<String> warnings){
 public ActionForecast{warnings=List.copyOf(warnings);}
}
