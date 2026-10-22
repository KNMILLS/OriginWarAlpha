package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class WorkplaceLayoutTest {
 @Test void concealedGeometryAndRawObjectiveChangesCannotAlterExistingVisibleDressing(){
  var s=new RunEngine(0).saveState();var view=new WorkplaceLayout();view.update(s);var chosen=s.fixtures.stream().map(view::furniture).toList();var recesses=view.recesses();var changed=s.copy();for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)if(!s.visible[x][y]){changed.tiles[x][y]=Tile.HAZARD;changed.blueprintTiles[x][y]=Tile.WALL;}changed.switchX=changed.hatchX=0;changed.terminalX=0;view.update(changed);assertEquals(chosen,changed.fixtures.stream().map(view::furniture).toList());assertEquals(recesses,view.recesses());
 }
 @Test void decorationSurvivesWalkingAndResumeWithoutMovingOrRelabelingPhysicalFixtures(){
  var s=new RunEngine(0).saveState();var view=new WorkplaceLayout();view.update(s);assertTrue(view.roomCount()>=3);
  var original=s.fixtures.stream().map(f->f.kind+":"+f.x+":"+f.y+":"+f.width).toList();var chosen=s.fixtures.stream().map(view::furniture).toList();
  var next=s.copy();next.playerX=next.switchX;next.playerY=next.switchY;next.tiles[next.switchX][next.switchY]=Tile.FLOOR;view.update(next);assertEquals(chosen,next.fixtures.stream().map(view::furniture).toList());
  var resumed=new WorkplaceLayout();resumed.update(next);assertEquals(chosen,next.fixtures.stream().map(resumed::furniture).toList());assertEquals(original,s.fixtures.stream().map(f->f.kind+":"+f.x+":"+f.y+":"+f.width).toList());
 }
}
