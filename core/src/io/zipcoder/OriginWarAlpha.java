package io.zipcoder;

import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.*;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.GwtCompatibility;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
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
    SpriteBatch batch;
    private RNG rng;
    private SquidLayers display;
    private DungeonGenerator dungeonGen;
    private char[][] decoDungeon, bareDungeon, lineDungeon, spaces;
    private int[][] colorIndices, bgColorIndices, languageBG, languageFG, lights;
    private double[][] fovmap, resMap, costArray;
    private boolean explored[][];
    private Color[][] fgColor, bgColorArr;
    private FOV fov;
    /**
     * In number of cells
     */
    private int gridWidth;
    /**
     * In number of cells
     */
    private int gridHeight;
    /**
     * The pixel width of a cell
     */
    private int cellWidth;
    /**
     * The pixel height of a cell
     */
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
    private int langIndex = 0;
    private boolean helpOn;
    private boolean normal;
    private String[] lang;
    private String[] helpText;
    private String[] displayText;
    private String[] victoryText;
    private String[] scoreScreen;
    private int levelCount = 1;
    private boolean foundSwitch;
    private RoomFinder roomFinder;
    private ArrayList<Food> foodList;
    private boolean scoreScreenOn;
    private boolean debugMode;
    private int foodEaten;
    private boolean playerIsAlive;
    private boolean victoryState;
    private AStarSearch validLevelSearch;
    private Sound backgroundMusic;

    @Override
    public void create() {
//        backgroundMusic = Gdx.audio.newSound(Gdx.files.internal("backgroundMusic.mp3"));
//        backgroundMusic.loop();
        player = Player.getPlayer();
        victoryState = false;
        costMap = new HashMap<>();
        costMap.put('.', 1.0);
        costMap.put('~', 3.0);
        costMap.put('"', 0.1);
        costMap.put(',', 2.0);
        gridWidth = 80;
        gridHeight = 24;
        cellWidth = 11;
        cellHeight = 22;
        rng = new RNG(System.currentTimeMillis() * (long) Math.PI);
        batch = new SpriteBatch();
        stage = new Stage(new StretchViewport(gridWidth * cellWidth, (gridHeight + 8) * cellHeight), batch);
        display = new SquidLayers(gridWidth, gridHeight + 8, cellWidth, cellHeight, DefaultResources.getStretchableFont());
        display.setTextSize(cellWidth, cellHeight + 1);
        display.setAnimationDuration(0.03f);
        display.setPosition(0, 0);
        dungeonGen = new DungeonGenerator(gridWidth, gridHeight, rng);
        dungeonGen.addDoors(25, true);
        explored = new boolean[gridWidth][gridHeight];
        switch (levelCount) {
            case 1:
                dungeonGen.addGrass(75);
                break;
            case 2:
                dungeonGen.addGrass(60);
                break;
            case 3:
                dungeonGen.addGrass(35);
                break;
            case 4:
                dungeonGen.addGrass(10);
                break;
            case 5:
                dungeonGen.addGrass(25);
                dungeonGen.addWater(10);
                break;
            case 6:
                dungeonGen.addGrass(10);
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
        decoDungeon = dungeonGen.generate(TilesetType.ROOMS_AND_CORRIDORS_B);
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
        //This isn't working yet
        while(validPathToExit == null){
            validPathToExit = validLevelSearch.path(player.getPosition(), stairSwitch);
            if (validPathToExit == null) {
                player.setPosition(dungeonGen.utility.randomCell(placement));
            }
        }
        fov = new FOV(FOV.RIPPLE_TIGHT);
        resMap = DungeonUtility.generateResistances(decoDungeon);
        for (int i = 0; i < resMap.length; i++) {
            for (int j = 0; j < resMap[i].length; j++) {
                if (decoDungeon[i][j] == '"') {
                    resMap[i][j] = 0.0;
                }
            }
        }
        fovmap = fov.calculateFOV(resMap, player.getPosition().getX(), player.getPosition().getY(), 8, Radius.CIRCLE);
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
        languageBG = GwtCompatibility.fill2D(1, gridWidth, 6);
        languageFG = GwtCompatibility.fill2D(0, gridWidth, 6);
        foundSwitch = false;
        roomFinder = new RoomFinder(decoDungeon);
        foodList = addFood();
        foodEaten = 0;
        playerIsAlive = true;

        lang = new String[]{
                "Controls:\t WASD/Arrow Keys/Mouse to move.",
                "'H' for help screen.\t'E' for score screen",
                "'Q' to quit.\t'T' restart the game",
                "",
                "",
        };
        helpText = new String[]{
                "You need to find the switch '?' to unlock the hatch '*' to the next level.",
                "There are rations '%' scattered about. Make use of them.",
                "Movement is modified by terrain: shallow water:2x, deep water:3x, grass:free.",
                "",
                "",
        };
        displayText = new String[]{
                "",
                "",
                "",
                "",
                "",
        };
        scoreScreen = new String[]{
                "",
                "",
                "",
                "",
                "",
        };
        victoryText = new String[]{
                "",
                "",
                "",
                "",
                "",
        };
        displayText = lang;

        baseInput = new SquidInput(new SquidInput.KeyHandler() {
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
                            normal = false;
                        } else {
                            helpOn = false;
                            normal = true;
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
                    case 'e':
                    case 'E':
                        if (!scoreScreenOn) {
                            scoreScreenOn = true;
                            normal = false;
                        } else {
                            scoreScreenOn = false;
                            normal = true;
                        }
                        break;
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
        input = new OriginInput(baseInput.getKeyHandler(), baseInput.getMouse());
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        stage.addActor(display);

    }

    private void move(int xmod, int ymod) {

        displayText = lang;
        if (player.getHealth() <= 0) {
            playerIsAlive = false;
            input.drain();
            awaitedMoves.clear();
            return;
        }
        int newX = player.getPosition().x + xmod, newY = player.getPosition().y + ymod;
        if (newX >= 0 && newY >= 0 && newX < gridWidth && newY < gridHeight
                && bareDungeon[newX][newY] != '#') {
            player.setPosition(player.getPosition().translate(xmod, ymod));
            if (lineDungeon[newX][newY] == '+') {
                lineDungeon[newX][newY] = '/';
                decoDungeon[newX][newY] = '/';
                resMap = DungeonUtility.generateResistances(bareDungeon);
            }
            if (decoDungeon[newX][newY] == '~') {
                player.incrementTurn();
                player.incrementTurn();
            }
            if (decoDungeon[newX][newY] == ',') {
                player.incrementTurn();
            }
            if (decoDungeon[newX][newY] == '"') {
                if (player.getTurns() % 4 == 0) player.setHealth(player.getHealth() + 1);
                player.setTurns(player.getTurns() - 1);
            }
            eatFood();
            fovmap = fov.calculateFOV(resMap, newX, newY, 8, Radius.CIRCLE);
        }
        if (player.getPosition() == stairSwitch) {
            stairsDown = dungeonGen.stairsDown;
            foundSwitch = true;
        }
        if (player.getPosition() == stairsDown) {
            levelCount++;
            player.setHealth(player.getHealth() + 50 - (levelCount * 3));
            create();
        }
        scoreScreen[0] = "Player ID\t" + player.getId() + "\tCurrent Level:\t" + levelCount + "\tTurns:\t" + player.getTurns();
        scoreScreen[1] = "Switch Found:\t" + foundSwitch + "\tFood remaining:\t" + Math.max(0, (10 - levelCount) - foodEaten);
        scoreScreen[2] = "Health Remaining:\t" + player.getHealth() + "\tFood Eaten:\t" + foodEaten;
    }

    public void putMap() {
        boolean occupied;
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                occupied = player.getPosition().getX() == i && player.getPosition().getY() == j;
                if (fovmap[i][j] > 0.0) {
                    explored[i][j] = true;
                    Coord toRemove = Coord.get(i, j);
                    unexploredSet.remove(toRemove);
                    if (occupied) {
                        display.put(i, j, ' ');
                    } else {
                        display.put(i, j, lineDungeon[i][j], fgColor[i][j], bgColorArr[i][j],
                                lights[i][j] + (int) (320 * fovmap[i][j]));
                    }
                } else if (explored[i][j]) {
                    display.put(i, j, lineDungeon[i][j], fgColor[i][j], bgColorArr[i][j], 40);
                }
            }
        }

        for (Coord pt : toCursor) {
            display.highlight(pt.x, pt.y, lights[pt.x][pt.y] + (int) (170 * fovmap[pt.x][pt.y]));
        }
        if ((stairsDown != null) && levelCount < 10) {
            display.put(stairsDown.x, stairsDown.y, '*', 12);
        }
        if ((explored[stairSwitch.x][stairSwitch.y] && !foundSwitch) && levelCount < 10) {
            display.put(stairSwitch.x, stairSwitch.y, '?', 12);
        }
        for (Food food : foodList) {
            int x = food.getPosition().getX();
            int y = food.getPosition().getY();
            if (explored[x][y]) {
                display.put(x, y, food.getSymbol(), 39);
            }
        }
        if (victoryState) {
            lang[0] = "Submit a screenshot of your score screen to the devs.";
            lang[1] = "Top scores will be posted to the website.";
            lang[2] = "'H' for help screen\t'E' for score screen\t'Q' to quit\t'T' restart the game";
            displayText[3] = "A.L.I.C.E: I... I can't believe you did it.//";
            displayText[4] = "You actually escaped! 'THANKS FOR PLAYING!!!' -DEV TEAM";
            input.drain();
            awaitedMoves.clear();
        } else if (player.getHealth() >= 125) {
            display.put(player.getPosition().x, player.getPosition().y, '∆', 27);
            displayText[3] = "A.L.I.C.E: Starvation is the enemy but no enemy is absolute...";
            displayText[4] = "No solution is either. You will be slower when gorged. Do keep this in mind.";
        } else if (player.getHealth() > 50) {
            display.put(player.getPosition().x, player.getPosition().y, '∆', 21);
            displayText[3] = "A.L.I.C.E: I would not go so far as to call you healthy but//";
            displayText[4] = "...you aren't dying. Continue that.";
        } else if (player.getHealth() > 25) {
            display.put(player.getPosition().x, player.getPosition().y, '∆', 18);
            displayText[3] = "A.L.I.C.E: I actually need to remind you to eat.";
            displayText[4] = "This is not inspiring.";
        } else if (playerIsAlive) {
            display.put(player.getPosition().x, player.getPosition().y, '∆', 12);
            displayText[3] = "A.L.I.C.E: You are dying. Not to make this about me//";
            displayText[4] = "But you aren't terribly useful to me dead. Fix it. Please.";
        } else {
            display.put(player.getPosition().x, player.getPosition().y, '±', 2);
            playerIsAlive = false;
            lang[0] = "Submit a screenshot of your score screen to the devs.";
            lang[1] = "Top scores will be posted to the website.";
            lang[2] = "'H' for help screen\t'E' for score screen\t'Q' to quit\t'T' restart the game";
            displayText[3] = "A.L.I.C.E: You have died. What a waste...";
            displayText[4] = "You can try again if you really think you're worthy.";
        }
        display.put(0, gridHeight + 1, spaces, languageFG, languageBG);
        if (helpOn) displayText = helpText;
        if (scoreScreenOn) displayText = scoreScreen;
        else if (normal) displayText = lang;
        display.putString(2, gridHeight + 1, displayText[0], 0, 1);
        display.putString(2, gridHeight + 2, displayText[1], 0, 1);
        display.putString(2, gridHeight + 3, displayText[2], 0, 1);
        display.putString(2, gridHeight + 4, displayText[3], 0, 1);
        display.putString(2, gridHeight + 5, displayText[4], 0, 1);

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
            secondsWithoutMoves += Gdx.graphics.getDeltaTime();
            if (secondsWithoutMoves >= 0.1) {
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
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        input.getMouse().reinitialize((float) width / this.gridWidth, (float) height / (this.gridHeight + 8), this.gridWidth, this.gridHeight, 0, 0);
    }

    public void eatFood() {
        for (Food food : foodList) {
            if (food.getPosition().equals(player.getPosition())) {
                foodList.remove(food);
                foodEaten++;
                player.setHealth(player.getHealth() + 10);
                break;
            }
        }
    }

    public ArrayList<Food> addFood() {
        int foodToAdd = 10 - levelCount;
        ArrayList<Food> toReturn = new ArrayList<>();
        while (foodToAdd > 0) {
            foodToAdd--;
            for(char[][] room : this.roomFinder.findRooms()){
                DungeonUtility dungeonUtility = new DungeonUtility();
                double chance = Math.random();
                if (chance >= .5) {
                    Coord position = dungeonUtility.randomFloor(room);
                    if(position == null) continue;
                    toReturn.add(new Food(position));
                    foodToAdd--;
                    continue;
                }

            }
        }
        return toReturn;

    }

    public void restart() {
        playerIsAlive = true;
        player.setHealth(101);
        player.setTurns(-1);
        levelCount = 1;
        foodEaten = 0;
        create();
    }
}