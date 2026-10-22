package io.zipcoder.originwar.desktop;
import io.zipcoder.originwar.model.Tile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class KnownArchitectureTest {
 @Test void hiddenActualTopologyCannotChooseAnUnobservedWallFace(){var s=ArtProofScenario.create();int x=30,y=30;s.visible[x+1][y]=false;s.rememberedTiles[x+1][y]=null;s.tiles[x+1][y]=Tile.FLOOR;assertEquals("wall.bulkhead",KnownArchitecture.wallRegion(s,x,y));s.tiles[x+1][y]=Tile.WALL;assertEquals("wall.bulkhead",KnownArchitecture.wallRegion(s,x,y));}
 @Test void rememberedPublicGeometryMayChooseSideFacingWithoutMutation(){var s=ArtProofScenario.create();int x=30,y=30;s.visible[x+1][y]=false;s.explored[x+1][y]=true;s.rememberedTiles[x+1][y]=Tile.FLOOR;long rng=s.combatRng,revision=s.revision;assertEquals("wall.side-left",KnownArchitecture.wallRegion(s,x,y));assertEquals(rng,s.combatRng);assertEquals(revision,s.revision);assertEquals(0,s.tick);}
}
