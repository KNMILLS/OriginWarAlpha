import io.zipcoder.Player;
import org.junit.Test;

;import static org.junit.Assert.assertEquals;

/**
 * Created by christophernobles on 11/4/16.
 */
public class PlayerTEST {
    @Test
    public void getTurnsTEST(){
        int expected = 0;
        int actual = Player.getPlayer().getTurns();
        assertEquals("The value should be 0", actual, expected);
    }

    @Test
    public void getHealthTEST(){
        int expected = 100;
        int actual = Player.getPlayer().getHealth();
        assertEquals("The value should be 0", expected, actual);
    }

    @Test
    public void setHealthTEST(){
        Player.getPlayer().setHealth(20);
        int expected = 20;
        int actual = Player.getPlayer().getHealth();
        assertEquals("The value should be 0", expected, actual);
    }

    @Test
    public void incrementTurnTEST(){
        Player.getPlayer().incrementTurn();
        int expected = 1;
        int actual = Player.getPlayer().getTurns();
        assertEquals("The value should be 1", expected, actual);
    }

}
