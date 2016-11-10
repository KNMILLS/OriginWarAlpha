package io.zipcoder.graphics;

import io.zipcoder.OriginWarAlpha;
import io.zipcoder.Player;

/**
 * Created by kenragonese on 11/10/16.
 */
public class TextDisplay {


    private String[] defaultText;
    private String[] displayText;
    private String[] scoreText;
    private String[] victoryText;
    private String[] helpText;
    private String[] deathText;
    private String[] aliceDisplayText;
    private String[] aliceVictoryText;
    private String[] aliceDeathText;

    public TextDisplay() {
        setDefaultText();
        setDisplayText(getDefaultText());
        setHelpText();
        setVictoryText();
        setDeathText();
        setAliceDisplayText();
    }


    public String[] getHelpText() {
        return helpText;
    }

    private void setHelpText() {
        this.helpText = new String[]{
                "You need to find the switch '?' to unlock the hatch '*' to the next level.",
                "There are rations '%' scattered about. Make use of them.",
                "Movement is modified by terrain: shallow water:2x, deep water:3x, grass:free.",
        };
    }

    public String[] getDefaultText() {
        return defaultText;
    }

    private void setDefaultText() {
        this.defaultText = new String[]{
                "Controls:\t WASD/Arrow Keys/Mouse to move.",
                "'H' for help screen.\t'E' for score screen",
                "'Q' to quit.\t'T' restart the game",
        };
    }

    public String[] getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String[] displayText) {
        this.displayText = displayText;
    }

    public String[] getScoreText() {
        return scoreText;
    }

    public void setScoreText(OriginWarAlpha game) {
        this.scoreText = new String[]{
                "Player ID\t" + Player.getPlayer().getId() + "\tCurrent Level:\t" + game.getLevelCount() + "\tTurns:\t" + Player.getPlayer().getTurns(),
                "Switch Found:\t" + game.isFoundSwitch() + "\tFood remaining:\t" + Math.max(0, (10 - game.getLevelCount()) - game.getFoodEaten()),
                "Health Remaining:\t" + Player.getPlayer().getHealth() + "\tFood Eaten:\t" + game.getFoodEaten(),
        };
    }

    public String[] getVictoryText() {
        return victoryText;
    }

    public void setVictoryText() {
        this.victoryText = new String[]{
                "Submit a screenshot of your score screen to the devs.",
                "Top scores will be posted to the website.",
                "'H' for help screen\t'E' for score screen\t'Q' to quit\t'T' restart the game",
        };
    }

    public String[] updateAliceDisplayByPlayerHealth(int playerhealth) {
        if (playerhealth >= 125) {
            this.aliceDisplayText[0] = "A.L.I.C.E: Starvation is the enemy but no enemy is absolute...";
            this.aliceDisplayText[1] = "No solution is either. You will be slower when gorged. Do keep this in mind.";
        } else if (playerhealth > 50) {
            this.aliceDisplayText[0] = "A.L.I.C.E: I would not go so far as to call you healthy but//";
            this.aliceDisplayText[1] = "...you aren't dying. Continue that.";
        } else if (playerhealth > 35) {
            this.aliceDisplayText[0] = "A.L.I.C.E: I actually need to remind you to eat.";
            this.aliceDisplayText[1] = "This is not inspiring.";
        } else if (playerhealth > 0) {
            this.aliceDisplayText[0] = "A.L.I.C.E: You are dying. Not to make this about me//";
            this.aliceDisplayText[1] = "But you aren't terribly useful to me dead. Fix it. Please.";
        }
        return aliceDisplayText;
    }

    public void setDefaultText(String[] defaultText) {
        this.defaultText = defaultText;
    }

    public void setScoreText(String[] scoreText) {
        this.scoreText = scoreText;
    }

    public void setAliceVictoryText() {
        this.aliceDisplayText[0] = "A.L.I.C.E: I... I can't believe you did it.//";
        this.aliceDisplayText[1] = "You actually escaped! 'THANKS FOR PLAYING!!!' -DEV TEAM";
    }

    public void setHelpText(String[] helpText) {
        this.helpText = helpText;
    }

    public String[] getDeathText() {
        return deathText;
    }

    public void setDeathText() {
        this.deathText = new String[]{
                "Submit a screenshot of your score screen to the devs.",
                "Top scores will be posted to the website.",
                "'H' for help screen\t'E' for score screen\t'Q' to quit\t'T' restart the game"
        };
    }

    public void setDeathText(String[] deathText) {
        this.deathText = deathText;
    }

    public String[] getAliceDisplayText() {
        return aliceDisplayText;
    }

    public void setAliceDisplayText(String[] aliceDisplayText) {
        this.aliceDisplayText = aliceDisplayText;
    }
    public void setAliceDisplayText() {
        this.aliceDisplayText = new String[]{
                "A.L.I.C.E: Hello, I am A.L.I.C.E//",
                "An Artificial Lifeform Intent on Control and Enforcement."
        };
    }


    public void setVictoryText(String[] victoryText) {
        this.victoryText = victoryText;
    }

    public String[] getAliceVictoryText() {
        return aliceVictoryText;
    }

    public void setAliceVictoryText(String[] aliceVictoryText) {
        this.aliceVictoryText = aliceVictoryText;
    }

    public String[] getAliceDeathText() {
        return aliceDeathText;
    }

    public void setAliceDeathText() {
        this.aliceDeathText[0] = "A.L.I.C.E: You have died. What a waste...";
        this.aliceDeathText[1] = "You can try again if you really think you're worthy.";
    }
}
