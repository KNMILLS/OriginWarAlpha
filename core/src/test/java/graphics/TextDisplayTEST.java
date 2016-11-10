package graphics;

import io.zipcoder.graphics.TextDisplay;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by christophernobles on 11/10/16.
 */
public class TextDisplayTEST {
    @Test
    public void getAliceVictoryTextTEST(){
        TextDisplay testDisplay = new TextDisplay();
        testDisplay.setAliceVictoryText();
        String[] expectedArray = testDisplay.getAliceVictoryText();
        String[] actualArray = new String[]{
                "A.L.I.C.E: I... I can't believe you did it.//",
                "You actually escaped! 'THANKS FOR PLAYING!!!' -DEV TEAM"
        };
        assertEquals("The array should be", actualArray, expectedArray);
    }

    @Test
    public void getAliceDeathTextTEST(){
        TextDisplay testDisplay = new TextDisplay();
        testDisplay.setAliceVictoryText();
        String[] expectedArray = testDisplay.getAliceDeathText();
        String[] actualArray = new String[]{
                "A.L.I.C.E: You have died. What a waste...",
                "You can try again if you really think you're worthy."
        };
        assertEquals("The array should be", actualArray, expectedArray);
    }


}
