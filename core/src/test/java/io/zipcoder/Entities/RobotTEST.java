package io.zipcoder.Entities;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by christophernobles on 11/6/16.
 */
public class RobotTEST {
    @Test
    public void getHealthTEST(){
        Robot robotto = new Robot();
        int expectedHealth = 10;
        int actualHealth = robotto.getHealth();
        assertEquals("The value should be 10", expectedHealth, actualHealth);
    }
}
