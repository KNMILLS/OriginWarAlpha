package io.zipcoder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by christophernobles on 11/6/16.
 */
public class RobotTEST {
    @Test
    public void getHealthTEST(){
        Robot robotto = new Robot();
        int expectedHealth = 100;
        int actualHealth = robotto.getHealth();
        assertEquals("The value should be 100", expectedHealth, actualHealth);
    }
}
