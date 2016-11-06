package io.zipcoder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

;

/**
 * Created by christophernobles on 11/4/16.
 */
public class PlayerTEST {
    @Test
    public void getTurnsTEST(){
        int expectedHealth = 0;
        int actualHealth = Player.getPlayer().getTurns();
        assertEquals("The value should be 0", actualHealth, expectedHealth);
    }

    @Test
    public void getHealthTEST(){
        int expectedHealth = 100;
        int actualHealth = Player.getPlayer().getHealth();
        assertEquals("The value should be 0", expectedHealth, actualHealth);
    }

    @Test
    public void setHealthTEST(){
        Player.getPlayer().setHealth(20);
        int expectedHealth = 20;
        int actualHealth = Player.getPlayer().getHealth();
        assertEquals("The value should be 0", expectedHealth, actualHealth);
    }

    @Test
    public void setTurnTEST(){
        Player.getPlayer().setHealth(14);
        int expectedTurn = 14;
        int actualTurn = Player.getPlayer().getHealth();
        assertEquals("The value should be 14", expectedTurn, actualTurn);
    }

    @Test
    public void incrementTurnTEST(){
        Player.getPlayer().incrementTurn();
        int expectedTurn = 1;
        int actualTurn = Player.getPlayer().getTurns();
        assertEquals("The value should be 1", expectedTurn, actualTurn);
    }

}
