package io.zipcoder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

;

/**
 * Created by christophernobles on 11/4/16.
 */
public class PlayerTEST {
    @Test
    public void setTurnsTEST(){
        Player.getPlayer().setTurns(5);
        int expectedTurn = 5;
        int actualTurn = Player.getPlayer().getTurns();
        assertEquals("The value should be 5", expectedTurn, actualTurn);
    }

    @Test
    public void getTurnsTEST(){
        Player.getPlayer().setTurns(0);
        int expectedTurn = 0;
        int actualTurn = Player.getPlayer().getTurns();
        assertEquals("The value should be 0", actualTurn, expectedTurn);
    }

    @Test
    public void setHealthTEST(){
        Player.getPlayer().setHealth(20);
        int expectedHealth = 20;
        int actualHealth = Player.getPlayer().getHealth();
        assertEquals("The value should be 0", expectedHealth, actualHealth);
    }

    @Test
    public void getHealthTEST(){
        Player.getPlayer().setHealth(100);
        int expectedHealth = 100;
        int actualHealth = Player.getPlayer().getHealth();
        assertEquals("The value should be 0", expectedHealth, actualHealth);
    }

    @Test
    public void incrementTurnTEST(){
        Player.getPlayer().incrementTurn();
        //Player.getPlayer().incrementTurn();
        int expectedTurn = 1;
        int actualTurn = Player.getPlayer().getTurns();
        assertEquals("The value should be 1", expectedTurn, actualTurn);
    }

    @Test
    public void setAliveTEST(){
        Player.getPlayer().setAlive(true);
        Boolean expectedBoolean = true;
        Boolean actualBoolean = Player.getPlayer().isAlive();
        assertEquals("The player should be alive.", expectedBoolean, actualBoolean);
    }

    @Test
    public void isAliveTEST(){
        Player.getPlayer().setAlive(false);
        Boolean expectedBoolean = false;
        Boolean actualBoolean = Player.getPlayer().isAlive();
        assertEquals("The player should be dead.", expectedBoolean, actualBoolean);
    }

}
