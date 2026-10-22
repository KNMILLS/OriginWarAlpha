package io.zipcoder.originwar.desktop;

import java.util.*;

/** Authored presentation exposures and ordered cues; no simulation state or clock. */
public record AnimationClip(List<Frame> frames,List<Marker> markers,boolean loop,float cycleCells) {
 public record Frame(String region,float seconds) {}
 public record Marker(String kind,float seconds) {}
 public AnimationClip {
  frames=List.copyOf(frames);markers=markers.stream().sorted(Comparator.comparingDouble(Marker::seconds)).toList();
  if(frames.isEmpty()||frames.stream().anyMatch(f->f.region()==null||f.region().isBlank()||!Float.isFinite(f.seconds())||f.seconds()<=0))throw new IllegalArgumentException("Invalid animation exposures");
  float length=0;for(var f:frames)length+=f.seconds();
  final float span=length;
  if(markers.stream().anyMatch(m->m.kind()==null||m.kind().isBlank()||!Float.isFinite(m.seconds())||m.seconds()<0||m.seconds()>span+.001f))throw new IllegalArgumentException("Invalid animation marker");
  if(!Float.isFinite(cycleCells)||cycleCells<0)throw new IllegalArgumentException("Invalid cycle distance");
 }
 public float seconds(){float result=0;for(var f:frames)result+=f.seconds();return result;}
 public String at(float time){float t=Math.max(0,time);if(loop&&t>=seconds())t%=seconds();for(var f:frames){if(t<f.seconds())return f.region();t-=f.seconds();}return frames.getLast().region();}
 public String phase(float phase){return at(Math.max(0,Math.min(.999999f,phase))*seconds());}
 public String retimedFire(float time,float span,float release){float sourceRelease=marker("release",seconds()*.5f);float t=Math.max(0,Math.min(span,time));float sourceTime=t<=release?sourceRelease*t/release:sourceRelease+(seconds()-sourceRelease)*(t-release)/(span-release);return at(sourceTime);}
 /** An already aimed shot retains its guard before release and after the authored recoil. */
 public String aimedFire(float time,float span,float release,String aimed){float sourceReturn=marker("aim_return",-1);if(sourceReturn<0||aimed.isBlank())return retimedFire(time,span,release);float sourceRelease=marker("release",seconds()*.5f);float t=Math.max(0,Math.min(span,time));float sourceTime=t<=release?sourceRelease:sourceRelease+(seconds()-sourceRelease)*(t-release)/(span-release);return t<release||sourceTime>=sourceReturn?aimed:at(sourceTime);}
 public float marker(String kind,float fallback){return markers.stream().filter(m->m.kind().equals(kind)).map(Marker::seconds).findFirst().orElse(fallback);}
 /** Repeated contacts remain distinct; interval excludes its start to prevent replay. */
 public List<Marker> between(float previous,float now){return markers.stream().filter(m->m.seconds()>previous&&m.seconds()<=now).toList();}
}
