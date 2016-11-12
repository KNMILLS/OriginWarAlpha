package io.zipcoder;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import io.zipcoder.Entities.Player;
import io.zipcoder.Items.Oxygen;
import io.zipcoder.Util.OriginInput;
import io.zipcoder.Util.SoundSingleton;
import io.zipcoder.graphics.TextDisplay;
import squidpony.GwtCompatibility;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.RoomFinder;
import squidpony.squidgrid.mapping.styled.TilesetType;
import squidpony.squidmath.AStarSearch;
import squidpony.squidmath.Coord;
import squidpony.squidmath.CoordPacker;
import squidpony.squidmath.RNG;
import java.util.*;
import java.util.Queue;

public class OriginWarAlpha extends ApplicationAdapter {
    private SpriteBatch batch;
    private RNG rng;
    private SquidLayers display;
    private DungeonGenerator dungeonGen;
    private char[][] decoDungeon, bareDungeon, lineDungeon, spaces, stairDungeon;
    private int[][] colorIndices, bgColorIndices, languageBG, languageFG, lights;
    private double[][] costArray;
    private boolean explored[][];
    private Color[][] fgColor, bgColorArr;
    private int gridWidth;
    private int gridHeight;
    private int cellWidth;
    private int cellHeight;
    private OriginInput input;
    private SquidInput baseInput;
    private Color bgColor;
    private Stage stage;
    private double lightCounter;
    private DijkstraMap playerToCursor;
    private Player player;
    private HashMap<Character, Double> costMap;
    private LinkedHashSet<Coord> unexploredSet;
    private Coord cursor, stairsDown, stairSwitch;
    private ArrayList<Coord> toCursor;
    private ArrayList<Coord> awaitedMoves;
    private float secondsWithoutMoves;
    private boolean helpOn;
    private int levelCount = 1;
    private boolean foundSwitch;
    private RoomFinder roomFinder;
    private ArrayList<Oxygen> oxygenList;
    private boolean debugMode;
    private int oxygenUsed;
    private boolean victoryState;
    private AStarSearch validLevelSearch;
    private TextDisplay textDisplay;
    private SoundSingleton soundSingleton;

    public void init(){
        soundSingleton = SoundSingleton.getSoundSingleton();
        soundSingleton.getBackgroundMusic().loop(.3f);
        soundSingleton.getHeartbeatSound().loop(.7f);
        //soundSingleton.getLaboredBreathing().loop();

    }

    @Override
    public void create() {
        if(levelCount==1){
            init();
        }
        textDisplay = new TextDisplay();
        textDisplay.setDefaultText(this);
        player = Player.getPlayer();
        costMap = initializeCostMap();
        gridWidth = 80;
        gridHeight = 24;
        cellWidth = 11;
        cellHeight = 22;
        rng = new RNG(System.currentTimeMillis() * (long) Math.PI);
        batch = new SpriteBatch();
        stage = new Stage(new StretchViewport(gridWidth * cellWidth, (gridHeight + 8) * cellHeight), batch);
        display = new SquidLayers(gridWidth, gridHeight + 8, cellWidth, cellHeight, DefaultResources.getLargeNarrowFont());
        display.setTextSize(cellWidth, cellHeight + 1);
        display.setAnimationDuration(0.03f);
        display.setPosition(0, 0);
        display.extendPalette(Color.CLEAR);
        dungeonGen = new DungeonGenerator(gridWidth, gridHeight, rng);
        stairDungeon = dungeonGen.generate(TilesetType.ROOMS_AND_CORRIDORS_B);
        dungeonGen.addDoors(25, true);
        explored = new boolean[gridWidth][gridHeight];
        switch (levelCount) {
            case 1:
                dungeonGen.addGrass(35);
                break;
            case 2:
                dungeonGen.addGrass(25);
                break;
            case 3:
                dungeonGen.addGrass(15);
                break;
            case 4:
                dungeonGen.addGrass(10);
                break;
            case 5:
                dungeonGen.addGrass(10);
                dungeonGen.addWater(10);
                break;
            case 6:
                dungeonGen.addGrass(5);
                dungeonGen.addWater(35);
                break;
            case 7:
                dungeonGen.addWater(35);
                break;
            case 8:
                dungeonGen.addWater(60);
                break;
            case 9:
                dungeonGen.addWater(75);
                break;
            case 10:
                victoryState = true;
            default:
                dungeonGen.addGrass(100);
        }
        decoDungeon = dungeonGen.generate(stairDungeon);
        decoDungeon = DungeonUtility.closeDoors(decoDungeon);
        costArray = DungeonUtility.generateCostMap(decoDungeon, costMap, 1.0);
        bareDungeon = dungeonGen.getBareDungeon();
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon);
        short[] placement = CoordPacker.pack(bareDungeon, '.');
        cursor = Coord.get(-1, -1);
        player.setPosition((dungeonGen.utility.randomCell(placement)));
        while (lineDungeon[player.getPosition().getX()][player.getPosition().getY()] == '+') {
            player.setPosition(((dungeonGen.utility.randomCell(placement))));
        }
        stairsDown = null;
        stairSwitch = dungeonGen.stairsUp;
        validLevelSearch = new AStarSearch(costArray, AStarSearch.SearchType.EUCLIDEAN);
        Queue<Coord> validPathToExit = null;
        //TODO This isn't working yet
        while(validPathToExit == null || validPathToExit.size() == 0){
            validPathToExit = validLevelSearch.path(player.getPosition(), stairSwitch);
            if (validPathToExit.size() == 0) {
                player.setPosition(dungeonGen.utility.randomCell(placement));
            }
        }
        player.initFOV(decoDungeon);
        toCursor = new ArrayList<Coord>(100);
        awaitedMoves = new ArrayList<Coord>(100);
        playerToCursor = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.EUCLIDEAN);
        playerToCursor = playerToCursor.initializeCost(costArray);
        unexploredSet = new LinkedHashSet<>();
        unexploredSet.addAll(Arrays.asList(CoordPacker.allPacked(placement)));
        bgColor = SColor.DARK_SLATE_GRAY;
        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, lightCounter);
        fgColor = new Color[gridWidth][gridHeight];
        bgColorArr = new Color[gridWidth][gridHeight];
        colorIndices = DungeonUtility.generatePaletteIndices(decoDungeon);
        bgColorIndices = DungeonUtility.generateBGPaletteIndices(decoDungeon);

        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                int colorVal = colorIndices[i][j];
                if (colorVal <= 3) {
                    fgColor[i][j] = display.getPalette().get(30);
                } else if (colorVal == 4) {
                    fgColor[i][j] = display.getPalette().get(29);
                } else {
                    fgColor[i][j] = display.getPalette().get(colorVal);
                }

                bgColorArr[i][j] = display.getPalette().get(bgColorIndices[i][j]);
            }
        }

        spaces = GwtCompatibility.fill2D(' ', gridWidth, 6);
        languageBG = GwtCompatibility.fill2D(40, gridWidth, 6);
        languageFG = GwtCompatibility.fill2D(40, gridWidth, 6);
        foundSwitch = false;
        roomFinder = new RoomFinder(decoDungeon);
        oxygenList = addOxygen();
        oxygenUsed = 0;
        player.setAlive(true);

        baseInput = inputConfig();
        input = new OriginInput(baseInput.getKeyHandler(), baseInput.getMouse());
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        stage.addActor(display);

    }

    private void move(int xmod, int ymod) {
        int newX = player.getPosition().x + xmod, newY = player.getPosition().y + ymod;
        textDisplay.setDefaultText(this);
        textDisplay.setDisplayText(textDisplay.getDefaultText());
        textDisplay.setAliceDisplayText(textDisplay.updateAliceDisplayByPlayerHealth(player.getHealth()));
        if (!player.isAlive()) {
            input.drain();
            awaitedMoves.clear();
            return;
        }
        else {
            if (newX >= 0 && newY >= 0 && newX < gridWidth && newY < gridHeight
                    && bareDungeon[newX][newY] != '#') {
                player.setPosition(player.getPosition().translate(xmod, ymod));
                if (lineDungeon[newX][newY] == '+') {
                    lineDungeon[newX][newY] = '/';
                    decoDungeon[newX][newY] = '/';
                    soundSingleton.getDoorSound().play(.2f);
                    player.getResMap()[newX][newY] = .15;
                }
                if (decoDungeon[newX][newY] == '~') {
                    player.incrementTurn();
                    player.incrementTurn();
                    soundSingleton.getWaterStep().play(.15f);
                }
                if (decoDungeon[newX][newY] == ',') {
                    player.incrementTurn();
                    soundSingleton.getWaterStep().play(.1f);
                }
                if (decoDungeon[newX][newY] == '"') {
                    if (player.getTurns() % 4 == 0) player.setHealth(player.getHealth() + 1);
                    player.setTurns(player.getTurns() - 1);
                }
                else{
                    soundSingleton.getFootStep().play(.08f);
                }
                refillOxygen();
                player.updateFOVMap();

            }
        }
        if (player.getPosition() == stairSwitch) {
            stairsDown = dungeonGen.stairsDown;
            if(!foundSwitch){
                switch(levelCount){
                    case 1:
                        soundSingleton.getWhatAreYouDoingHereSound().play(.2f);
                        break;
                    case 2:
                        soundSingleton.getMonsterSound().play(.3f);
                        break;
                    default:
                        soundSingleton.getCardLockSound().play(.2f);
                }
            }
            foundSwitch = true;

        }
        if (player.getPosition() == stairsDown) {
            levelCount++;
            if(player.getHealth()<100){
                player.setHealth(Math.min(100, player.getHealth() + (10*(10-levelCount))));
            }
            create();
            soundSingleton.getStairSound().play(.5f);
        }

    }

    private void putMap() {
        boolean occupied;
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                occupied = player.getPosition().getX() == i && player.getPosition().getY() == j;
                if (player.getFovMap()[i][j] > 0.0) {
                    explored[i][j] = true;
                    Coord toRemove = Coord.get(i, j);
                    unexploredSet.remove(toRemove);
                    if (occupied) {
                        display.put(i, j, ' ');
                    } else {
                        display.put(i, j, lineDungeon[i][j], fgColor[i][j], bgColorArr[i][j],
                                lights[i][j] + (int) (-200 + 320 * player.getFovMap()[i][j]));
                    }
                }
                else if (explored[i][j]) {
                    display.put(i, j, lineDungeon[i][j], fgColor[i][j], bgColorArr[i][j], -300);
                }
            }
        }

        for (Coord pt : toCursor) {
            display.highlight(pt.x, pt.y, lights[pt.x][pt.y] + (int) (170 * player.getFovMap()[pt.x][pt.y]));
        }
        if ((stairsDown != null) && levelCount < 10) {
            display.put(stairsDown.x, stairsDown.y, '*', 12);
        }
        if ((explored[stairSwitch.x][stairSwitch.y] && !foundSwitch) && levelCount < 10) {
            display.put(stairSwitch.x, stairSwitch.y, '?', 12);
        }
        for (Oxygen oxygen : oxygenList) {
            int x = oxygen.getPosition().getX();
            int y = oxygen.getPosition().getY();
            if (explored[x][y]) {
                display.put(x, y, oxygen.getSymbol(), 12);
            }
        }
//        if(decoDungeon[player.getPosition().getX()][player.getPosition().getY()] == '~'){
//            display.setAnimationDuration(0.9f);
//        } else if(decoDungeon[player.getPosition().getX()][player.getPosition().getY()] == ','){
//            display.setAnimationDuration(0.6f);
//        } else if(decoDungeon[player.getPosition().getX()][player.getPosition().getY()] == '"'){
//            display.setAnimationDuration(2000.1f);
//        } else {
//            display.setAnimationDuration(0.3f);
//        }
        if (victoryState) {
            player.setHealth(0);
            player.setAlive(false);
            display.put(player.getPosition().x, player.getPosition().y, '±', Color.GOLD);
            textDisplay.setDisplayText(textDisplay.getEndGameText());
            textDisplay.setAliceDisplayText(textDisplay.getAliceVictoryText());
        } else if (player.getHealth() >= 125) {
            player.setHpColor(27);
            display.put(player.getPosition().x, player.getPosition().y, '∆', player.getHpColor());
        } else if (player.getHealth() > 50) {
            player.setHpColor(24);
            display.put(player.getPosition().x, player.getPosition().y, '∆', player.getHpColor());
            //soundSingleton.getBreathSound().loop(.1f);
            //soundSingleton.getLaboredBreathing().loop();
        } else if (player.getHealth() > 25) {
            player.setHpColor(18);
            display.put(player.getPosition().x, player.getPosition().y, '∆', player.getHpColor());
        } else if (player.getHealth() > 0) {
            player.setHpColor(12);
            display.put(player.getPosition().x, player.getPosition().y, '∆', player.getHpColor());

        } else {
            player.setHpColor(2);
            display.put(player.getPosition().x, player.getPosition().y, '±', player.getHpColor());
            player.setAlive(false);
            textDisplay.setDisplayText(textDisplay.getEndGameText());
            textDisplay.setAliceDisplayText(textDisplay.getAliceDeathText());
        }
        display.put(0, gridHeight + 1, spaces, languageFG, languageBG);
        if (helpOn) textDisplay.setDisplayText(textDisplay.getHelpText());
        else textDisplay.setDisplayText(textDisplay.getDefaultText());
        display.putString(1, gridHeight + 1, textDisplay.getDisplayText()[0], player.getHpColor(), 40);
        display.putString(1, gridHeight + 2, textDisplay.getDisplayText()[1], player.getHpColor(), 40);
        display.putString(1, gridHeight + 3, textDisplay.getDisplayText()[2], player.getHpColor(), 40);
        display.putString(1, gridHeight + 4, textDisplay.getAliceDisplayText()[0], player.getHpColor(), 40);
        display.putString(1, gridHeight + 5, textDisplay.getAliceDisplayText()[1], player.getHpColor(), 40);
        display.putString(1, gridHeight + 6, textDisplay.getControlsBanner()[0], player.getHpColor(), 40);

    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        lightCounter += Gdx.graphics.getDeltaTime() * 15;
        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, lightCounter);
        putMap();
        if (!awaitedMoves.isEmpty()) {
            if (this.input.hasNext()) {
                awaitedMoves.clear();
                playerToCursor.clearGoals();
            }
            double moveValue = costMap.get(decoDungeon[player.getPosition().getX()][player.getPosition().getY()]);
            if(moveValue<1)moveValue=.5;
            secondsWithoutMoves += Gdx.graphics.getDeltaTime();
            if (secondsWithoutMoves >= 0.1 * moveValue) {
                secondsWithoutMoves = 0;
                Coord m = null;
                if(!awaitedMoves.isEmpty()){m = awaitedMoves.remove(0);}
                if(!toCursor.isEmpty())toCursor.remove(0);
                if(m == null)m = player.getPosition();
                move(m.x - player.getPosition().x, m.y - player.getPosition().y);
            }
        } else if (input.hasNext()) {
            input.next();
        }
        stage.draw();
        stage.act();
        if (player.getHealth() <= 0) {
            // still need to display the map, then write over it with a message.
            putMap();
            display.putBoxedString(gridWidth / 2 - 18, gridHeight / 2 - 8, "       THANKS FOR PLAYING!          ");
            display.putBoxedString(gridWidth / 2 - 18, gridHeight / 2 - 5, "            -DEV TEAM               ");
            display.putBoxedString(gridWidth / 2 - 18, gridHeight / 2 + 5, "             q to quit.             ");

            // because we return early, we still need to draw.
            stage.draw();
            // q still needs to quit.
            if (input.hasNext())
                input.next();
            return;
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        input.getMouse().reinitialize((float) width / this.gridWidth, (float) height / (this.gridHeight + 8), this.gridWidth, this.gridHeight, 0, 0);
    }

    private void refillOxygen() {
        for (Oxygen oxygen : oxygenList) {
            if (oxygen.getPosition().equals(player.getPosition())) {
                oxygenList.remove(oxygen);
                oxygenUsed++;
                soundSingleton.getOxygenSound().play(.2f);
                player.setHealth(player.getHealth() + 10);
                //display.putBoxedString(gridWidth / 2 - 18, gridHeight / 2 + 5, "             YUM!             ");
                break;
            }
        }
    }

    private ArrayList<Oxygen> addOxygen() {
        int oxygenToAdd = 8 - levelCount;
        ArrayList<Oxygen> toReturn = new ArrayList<>();
        DungeonUtility dungeonUtility = new DungeonUtility(rng);
        while (oxygenToAdd > 0) {
            ArrayList<char[][]> rooms = this.roomFinder.findRooms();
            for(char[][] room : rooms){
                boolean notDuplicate = true;
                double chance = rng.nextDouble(1.0);
                if (chance > 0.66) {
                    Coord position = dungeonUtility.randomFloor(room);
                    if(position == null || position == player.getPosition()) continue;
                    for(Oxygen oxygen : toReturn){
                        if (oxygen.getPosition().equals(position)){
                            notDuplicate = false;
                            break;
                        }
                    }
                    if(notDuplicate){
                        toReturn.add(new Oxygen(position));
                        oxygenToAdd--;
                        continue;
                    }

                }

            }
        }
        return toReturn;

    }

    private void restart() {
        player.setAlive(true);
        player.setHealth(101);
        player.setTurns(-1);
        levelCount = 1;
        oxygenUsed = 0;
        create();
    }
    public int getLevelCount() {
        return levelCount;
    }
    public int getOxygenUsed() {
        return oxygenUsed;
    }
    public boolean isFoundSwitch() {
        return foundSwitch;
    }

    private void revealMap(){
        for(int i = 0; i < player.getResMap().length; i++ ){
            for(int j = 0; j < player.getResMap()[0].length; j++ ){
                player.getResMap()[i][j] = 0.0;
                explored[i][j] = true;
                unexploredSet.clear();
            }
        }
    }

    private SquidInput inputConfig(){
        SquidInput toReturn = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key) {
                    case SquidInput.UP_ARROW:
                    case 'w':
                    case 'W': {
                        move(0, -1);
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
                    case 's':
                    case 'S': {
                        move(0, 1);
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
                    case 'a':
                    case 'A': {
                        move(-1, 0);
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
                    case 'd':
                    case 'D': {
                        move(1, 0);
                        break;
                    }
                    case 'H':
                    case 'h': {
                        if (!helpOn) {
                            helpOn = true;
                        } else {
                            helpOn = false;
                        }
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE: {
                        Gdx.app.exit();
                        break;
                    }
                    case 't':
                    case 'T': {
                        restart();
                    }
                    case SquidInput.ENTER:
                    case 'z':
                    case 'Z': {
                        move(0, 0);
                        break;
                    }
                    case '!':
                        if (debugMode) debugMode = false;
                        else debugMode = true;

                        break;
                    case 'p':
                    case 'P':
                        if (debugMode) player.setHealth(126);
                        break;
                    case 'o':
                    case 'O':
                        if (debugMode) player.setHealth(5);
                        break;
                    case 'i':
                    case 'I':
                        if (debugMode) {
                            levelCount++;
                            create();
                        }
                        break;
                    case 'u':
                    case 'U':
                        if(debugMode)revealMap();
                        break;
                }
            }
        },
                new SquidMouse(cellWidth, cellHeight, gridWidth, gridHeight, 0, 0, new InputAdapter() {

                    @Override
                    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                        if (explored[screenX][screenY]) {
                            cursor = Coord.get(screenX, screenY);
                            toCursor = playerToCursor.findPath(100, unexploredSet, null, player.getPosition(), cursor);
                            awaitedMoves = new ArrayList<Coord>(toCursor);
                        }
                        return false;
                    }

                    @Override
                    public boolean touchDragged(int screenX, int screenY, int pointer) {
                        return mouseMoved(screenX, screenY);
                    }

                    @Override
                    public boolean mouseMoved(int screenX, int screenY) {
                        if (!awaitedMoves.isEmpty())
                            return false;
                        if (cursor.x == screenX && cursor.y == screenY) {
                            return false;
                        }
                        cursor = Coord.get(screenX, screenY);
                        toCursor = playerToCursor.findPath(100, unexploredSet, null, player.getPosition(), cursor);
                        return false;
                    }
                }));
        return toReturn;
    }

    private HashMap<Character, Double> initializeCostMap(){
        HashMap<Character, Double> toReturn = new HashMap<>();
        toReturn.put('.', 1.0);
        toReturn.put('~', 3.0);
        toReturn.put('"', 0.1);
        toReturn.put(',', 2.0);
        toReturn.put('/',1.0);
        toReturn.put('+', 1.0);
        return toReturn;
    }
}
