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

/**
 * The main class of the game, constructed once in each of the platform-specific Launcher classes. Doesn't use any
 * platform-specific code.
 */
// In SquidSetup, squidlib-util is always a dependency, and squidlib (the display code that automatically includes
// libGDX) is checked by default. If you didn't change those dependencies, this class should run out of the box.
//
// If you didn't select squidlib as a dependency in SquidSetup, this class will be full of errors. If you don't depend
// on LibGDX, you'll need to figure out display on your own, and the setup of multiple platform projects is probably
// useless to you. But, if you do depend on LibGDX, you can make some use of this class. You can remove any imports or
// usages of classes in the squidpony.squidgrid.gui.gdx package, remove as much of create() as you  want (some of it
// doesn't use the display classes, so you might want the dungeon generation and such, otherwise just empty out the
// whole method), remove any SquidLib-specific code in render() and resize(), and probably remove putMap entirely.

// A main game class that uses LibGDX to display, which is the default for SquidLib, needs to extend ApplicationAdapter
// or something related, like Game. Game adds features that SquidLib doesn't currently use, so ApplicationAdapter is
// perfectly fine for these uses.
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
    /** In number of cells */
    private int gridWidth;
    /** In number of cells */
    private int gridHeight;
    /** The pixel width of a cell */
    private int cellWidth;
    /** The pixel height of a cell */
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
    public void create () {
        backgroundMusic = Gdx.audio.newSound(Gdx.files.internal("backgroundMusic.mp3"));
        backgroundMusic.loop();
        player = Player.getPlayer();
        victoryState = false;
        costMap= new HashMap<>();
        costMap.put('.', 1.0);
        costMap.put('~', 3.0);
        costMap.put('"', 0.1);
        costMap.put(',', 2.0);


        //These variables, corresponding to the screen's width and height in cells and a cell's width and height in
        //pixels, must match the size you specified in the launcher for input to behave.
        //This is one of the more common places a mistake can happen.
        //In our desktop launcher, we gave these arguments to the configuration:
        //	config.width = 80 * 11;
        //  config.height = (24 + 8) * 22;
        //Here, config.height refers to the total number of rows to be displayed on the screen.
        //We're displaying 24 rows of dungeon, then 8 more rows of text generation to show some tricks with language.
        //That adds up to 32 total rows of height.
        //gridHeight is 24 because that variable will be used for generating the dungeon and handling movement within
        //the upper 24 rows. Anything that refers to the full height, which happens rarely and usually for things like
        //screen resizes, just uses gridHeight + 8. Next to it is gridWidth, which is 50 because we want 50 grid spaces
        //across the whole screen. cellWidth and cellHeight are 11 and 22, and match the multipliers for config.width
        //and config.height, but in this case don't strictly need to because we soon use a "Stretchable" font. While
        //gridWidth and gridHeight are measured in spaces on the grid, cellWidth and cellHeight are the pixel dimensions
        //of an individual cell. The font will look more crisp if the cell dimensions match the config multipliers
        //exactly, and the stretchable fonts (technically, distance field fonts) can resize to non-square sizes and
        //still retain most of that crispness.
        gridWidth = 80;
        gridHeight = 24;
        cellWidth = 11;
        cellHeight = 22;
        // gotta have a random number generator. We can seed an RNG with any long we want, or even a String.
        rng = new RNG(System.currentTimeMillis() * (long)Math.PI);

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new SpriteBatch();
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = new Stage(new StretchViewport(gridWidth * cellWidth, (gridHeight + 8) * cellHeight), batch);

        // display is a SquidLayers object, and that class has a very large number of similar methods for placing text
        // on a grid, with an optional background color and lightness modifier per cell. It also handles animations and
        // other effects, but you don't need to use them at all. SquidLayers also automatically handles the stretchable
        // distance field fonts, which are a big improvement over fixed-size bitmap fonts and should probably be
        // preferred for new games. SquidLayers needs to know what the size of the grid is in columns and rows, how big
        // an individual cell is in pixel width and height, and lastly how to handle text, which can be a BitmapFont or
        // a TextCellFactory. Either way, it will use what is given to make its TextCellFactory, and that handles the
        // layout of text in a cell, among other things. DefaultResources stores pre-configured BitmapFont objects but
        // also some TextCellFactory objects for distance field fonts; either one can be passed to this constructor.
        // the font will try to load Inconsolata-LGC-Square as a bitmap font with a distance field effect.
        display = new SquidLayers(gridWidth, gridHeight + 8, cellWidth, cellHeight, DefaultResources.getStretchableFont());
        // a bit of a hack to increase the text height slightly without changing the size of the cells they're in.
        // this causes a tiny bit of overlap between cells, which gets rid of an annoying gap between vertical lines.
        // if you use '#' for walls instead of box drawing chars, you don't need this.
        display.setTextSize(cellWidth, cellHeight + 1);

        // this makes animations very fast, which is good for multi-cell movement but bad for attack animations.
        // 0.03 is in seconds, so each animation will be 0.03 seconds in duration.
        display.setAnimationDuration(0.03f);

        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
        display.setPosition(0, 0);



        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good "ruined" dungeons.
        dungeonGen = new DungeonGenerator(gridWidth, gridHeight, rng);

        dungeonGen.addDoors(25, true);
        //dungeonGen.generate(TilesetType.OPEN_AREAS);
        explored = new boolean[gridWidth][gridHeight];
        //uncomment this next line to randomly add water to the dungeon in pools.
        switch(levelCount){
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
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)
        decoDungeon = dungeonGen.generate(TilesetType.ROOMS_AND_CORRIDORS_B);
        decoDungeon = DungeonUtility.closeDoors(decoDungeon);
        costArray = DungeonUtility.generateCostMap(decoDungeon, costMap, 1.0);


        //There are lots of options for dungeon generation in SquidLib; you can pass a TilesetType enum to generate()
        //as shown on the following lines to change the style of dungeon generated from ruined areas, which are made
        //when no argument is passed to generate or when TilesetType.DEFAULT_DUNGEON is, to caves or other styles.
        //decoDungeon = dungeonGen.generate(TilesetType.REFERENCE_CAVES); // generate caves
        //decoDungeon = dungeonGen.generate(TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS); // generate large round rooms

        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.getBareDungeon();
        //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
        //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing character.
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon);
        // it's more efficient to get random floors from a packed set containing only (compressed) floor positions.
        // CoordPacker is a deep and involved class, but when other classes request packed data, you usually just need
        // to give them a short[] representing a region, as produced by CoordPacker.pack().
        short[] placement = CoordPacker.pack(bareDungeon, '.');
        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be huge, Coord is optimized for x and y values between -3 and 255, inclusive.
        cursor = Coord.get(-1, -1);
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player.
        player.setPosition((dungeonGen.utility.randomCell(placement)));
        while(lineDungeon[player.getPosition().getX()][player.getPosition().getY()] == '+'){
            player.setPosition(((dungeonGen.utility.randomCell(placement))));
        }
        stairsDown = null;
        stairSwitch = dungeonGen.stairsUp;

        validLevelSearch = new AStarSearch(costArray, AStarSearch.SearchType.EUCLIDEAN);
        Queue<Coord> validPathToExit = null;
        while(validPathToExit == null){
            validPathToExit = validLevelSearch.path(player.getPosition(), stairSwitch);
            if(validPathToExit == null){
                player.setPosition(dungeonGen.utility.randomCell(placement));
            }
        }
        fov = new FOV(FOV.RIPPLE_TIGHT);
        resMap = DungeonUtility.generateResistances(decoDungeon);
        for(int i = 0; i < resMap.length; i++){
            for(int j = 0; j < resMap[i].length; j++){
                if(decoDungeon[i][j] == '"'){
                    resMap[i][j] = 0.0;
                }
            }
        }
        fovmap = fov.calculateFOV(resMap, player.getPosition().getX(), player.getPosition().getY(), 8, Radius.CIRCLE);

        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<Coord>(100);
        awaitedMoves = new ArrayList<Coord>(100);
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        playerToCursor = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.EUCLIDEAN);
        playerToCursor = playerToCursor.initializeCost(costArray);

        unexploredSet = new LinkedHashSet<>();
        unexploredSet.addAll(Arrays.asList(CoordPacker.allPacked(placement)));
        bgColor = SColor.DARK_SLATE_GRAY;

        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, lightCounter);
        // DungeonUtility provides various ways to get default colors or other information from a dungeon char 2D array.
        fgColor = new Color[gridWidth][gridHeight];
        bgColorArr = new Color[gridWidth][gridHeight];
        colorIndices = DungeonUtility.generatePaletteIndices(decoDungeon);
        bgColorIndices = DungeonUtility.generateBGPaletteIndices(decoDungeon);

        for(int i = 0; i < gridWidth; i++){
            for(int j = 0; j < gridHeight; j++){
                int colorVal = colorIndices[i][j];
                if(colorVal <= 3){
                    fgColor[i][j] = display.getPalette().get(30);
                }else if(colorVal == 4){
                    fgColor[i][j] = display.getPalette().get(29);
                } else {
                    fgColor[i][j] = display.getPalette().get(colorVal);
                }

                bgColorArr[i][j] = display.getPalette().get(bgColorIndices[i][j]);
            }
        }

        // Here, we're preparing some 2D arrays so they don't get created during rendering.
        // Creating new arrays or objects during rendering can put lots of pressure on Java's garbage collector,
        // and Android's garbage collector can be very slow, especially when compared to desktop.
        // These methods are in GwtCompatibility not because GWT has some flaw with array creation,
        // but because Java in general is missing methods for dealing with 2D arrays.
        // So, you can think of the class more like "Gwt, and also Compatibility".
        // fill2D constructs a 2D array filled with one item. Other methods can insert a
        // 2D array into a differently-sized 2D array, or copy a 2D array of various types.
        spaces = GwtCompatibility.fill2D(' ', gridWidth, 6);
        languageBG = GwtCompatibility.fill2D(1, gridWidth, 6);
        languageFG = GwtCompatibility.fill2D(0, gridWidth, 6);
        foundSwitch = false;
        roomFinder = new RoomFinder(decoDungeon);
        foodList = addFood();
        foodEaten = 0;
        playerIsAlive = true;

        // this creates an array of sentences, where each imitates a different sort of language or mix of languages.
        // this serves to demonstrate the large amount of glyphs SquidLib supports.
        // FakeLanguageGen doesn't attempt to produce legible text, so any of the arguments given to these method calls
        // can be changed in a trial-and-error way to find the subjectively best output. The arguments to sentence()
        // are the minimum words, maximum words, between-word punctuation, sentence-ending punctuation, chance out of
        // 0.0 to 1.0 of putting between-word punctuation after a word, and lastly the max characters per sentence.
        // It is recommended that you don't increase the max characters per sentence much more, since it's already very
        // close to touching the edges of the message box it's in.
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


        // this is a big one.
        // SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
        // (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
        // keyHandler is meant to be able to handle complex, modified stairSwitch input, typically for games that distinguish
        // between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
        // implementation here handles hjkl keys (also called vi-keys), numpad, arrow keys, and wasd for 4-way movement.
        // Shifted letter keys produce capitalized chars when passed to KeyHandler.handle(), but we don't care about
        // that so we just use two case statements with the same body, i.e. one for 'A' and one for 'a'.
        // You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
        // path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
        // the event when someone clicks.
        baseInput = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key)
                {
                    case SquidInput.UP_ARROW:
                    case 'w':
                    case 'W':
                    {
                        //-1 is up on the screen
                        move(0, -1);
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
                    case 's':
                    case 'S':
                    {
                        //+1 is down on the screen
                        move(0, 1);
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
                    case 'a':
                    case 'A':
                    {
                        move(-1, 0);
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
                    case 'd':
                    case 'D':
                    {
                        move(1, 0);
                        break;
                    }
                    case 'H':
                    case 'h':
                    {
                        if(!helpOn){
                            helpOn = true;
                            normal = false;
                        }
                        else{
                            helpOn = false;
                            normal = true;
                        }
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                    case 't':
                    case 'T':
                    {
                        restart();
                    }
                    //skip a turn
                    case SquidInput.ENTER:
                    case 'z':
                    case 'Z':
                    {
                        move(0,0);
                        break;
                    }
                    case 'e':
                    case 'E':
                        if(!scoreScreenOn){
                            scoreScreenOn = true;
                            normal = false;
                        }
                        else{
                            scoreScreenOn = false;
                            normal = true;
                        }
                        break;
                    case '!':
                        if(debugMode) debugMode = false;
                        else debugMode = true;

                        break;
                    case 'p':
                    case 'P':
                        if(debugMode)player.setHealth(126);
                        break;
                    case 'o':
                    case 'O':
                        if(debugMode)player.setHealth(5);
                        break;
                    case 'i':
                    case 'I':
                        if(debugMode){
                            levelCount++;
                            create();
                        }
                }
            }
        },
                //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
                //input and converts it to grid coordinates (here, a cell is 12 wide and 24 tall, so clicking at the
                // pixel position 15,51 will pass screenX as 1 (since if you divide 15 by 12 and round down you get 1),
                // and screenY as 2 (since 51 divided by 24 rounded down is 2)).
                new SquidMouse(cellWidth, cellHeight, gridWidth, gridHeight, 0, 0, new InputAdapter() {

            // if the user clicks and there are no awaitedMoves queued up, generate toCursor if it
            // hasn't been generated already by mouseMoved, then copy it over to awaitedMoves.
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if(explored[screenX][screenY]) {
                        cursor = Coord.get(screenX, screenY);
                        //This uses DijkstraMap.findPath to get a possibly long path from the current player position
                        //to the position the user clicked on.
                        toCursor = playerToCursor.findPath(100, unexploredSet, null, player.getPosition(), cursor);

                    awaitedMoves = new ArrayList<Coord>(toCursor);
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return mouseMoved(screenX, screenY);
            }



            // causes the path to the mouse position to become highlighted (toCursor contains a list of points that
            // receive highlighting). Uses DijkstraMap.findPath() to find the path, which is surprisingly fast.
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if(!awaitedMoves.isEmpty())
                    return false;
                if(cursor.x == screenX && cursor.y == screenY)
                {
                    return false;
                }
                cursor = Coord.get(screenX, screenY);
                toCursor = playerToCursor.findPath(100, unexploredSet, null, player.getPosition(), cursor);
                return false;
            }
        }));
        input = new OriginInput(baseInput.getKeyHandler(), baseInput.getMouse());
        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        // and then add display, our one visual component, to the list of things that act in Stage.
        stage.addActor(display);

    }
    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private void move(int xmod, int ymod) {

        displayText = lang;
        if(player.getHealth() <= 0){
            playerIsAlive = false;
            input.drain();
            awaitedMoves.clear();
            return;
        }
        int newX = player.getPosition().x + xmod, newY = player.getPosition().y + ymod;
        if (newX >= 0 && newY >= 0 && newX < gridWidth && newY < gridHeight
                && bareDungeon[newX][newY] != '#')
        {
            player.setPosition(player.getPosition().translate(xmod, ymod));
            if(lineDungeon[newX][newY] == '+'){
                lineDungeon[newX][newY] = '/';
                decoDungeon[newX][newY] = '/';
                resMap = DungeonUtility.generateResistances(bareDungeon);
            }
            if(decoDungeon[newX][newY] == '~') {
                player.incrementTurn();
                player.incrementTurn();
            }
            if(decoDungeon[newX][newY] == ',') {
                player.incrementTurn();
            }
            if(decoDungeon[newX][newY] == '"') {
                if(player.getTurns() % 4 == 0)player.setHealth(player.getHealth() + 1);
                player.setTurns(player.getTurns()-1);
            }
            eatFood();
            fovmap = fov.calculateFOV(resMap, newX, newY, 8, Radius.CIRCLE);
        }
        if(player.getPosition() == stairSwitch){
            stairsDown = dungeonGen.stairsDown;
            foundSwitch = true;
        }
        if(player.getPosition() == stairsDown){
            levelCount++;
            player.setHealth(player.getHealth()+50-(levelCount*3));
            create();
        }
        scoreScreen[0] = "Player ID\t" + player.getId() + "\tCurrent Level:\t"  + levelCount + "\tTurns:\t" + player.getTurns();
        scoreScreen[1] = "Switch Found:\t" + foundSwitch + "\tFood remaining:\t" + Math.max(0,(10 - levelCount)-foodEaten);
        scoreScreen[2] = "Health Remaining:\t" + player.getHealth() + "\tFood Eaten:\t" + foodEaten;
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        boolean occupied;
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                occupied = player.getPosition().getX() == i && player.getPosition().getY() == j;
                if(fovmap[i][j] > 0.0){
                    explored[i][j] = true;
                    Coord toRemove = Coord.get(i, j);
                    unexploredSet.remove(toRemove);
                    if(occupied){
                        display.put(i, j, ' ');
                    } else {
                        display.put(i, j, lineDungeon[i][j], fgColor[i][j], bgColorArr[i][j],
                                lights[i][j] + (int) (320 * fovmap[i][j]));
                    }
                } else if(explored[i][j]){
                    display.put(i, j, lineDungeon[i][j], fgColor[i][j], bgColorArr[i][j], 40);
                }
            }
        }

        for (Coord pt : toCursor)
        {
            // use a brighter light to trace the path to the cursor, from 170 max lightness to 0 min.
            display.highlight(pt.x, pt.y, lights[pt.x][pt.y] + (int)(170 * fovmap[pt.x][pt.y]));
        }
        //places the player as an '@' at his position in orange (6 is an index into SColor.LIMITED_PALETTE).
        if((stairsDown != null) && levelCount < 10){
            display.put(stairsDown.x, stairsDown.y, '*', 12);
        }
        if((explored[stairSwitch.x][stairSwitch.y] && !foundSwitch) && levelCount < 10){
            display.put(stairSwitch.x, stairSwitch.y, '?', 12);
        }
        for(Food food : foodList){
            int x = food.getPosition().getX();
            int y = food.getPosition().getY();
            if(explored[x][y]){
                display.put(x, y, food.getSymbol(), 39);
            }
        }
        if(victoryState){
            lang[0] = "Submit a screenshot of your score screen to the devs.";
            lang[1] = "Top scores will be posted to the website.";
            lang[2] = "'H' for help screen\t'E' for score screen\t'Q' to quit\t'T' restart the game";
            displayText[3] = "A.L.I.C.E: I... I can't believe you did it.//";
            displayText[4] = "You actually escaped! 'THANKS FOR PLAYING!!!' -DEV TEAM";
            input.drain();
            awaitedMoves.clear();
        }
        else if(player.getHealth() >= 125){
            display.put(player.getPosition().x, player.getPosition().y, '∆', 27);
            displayText[3] = "A.L.I.C.E: Starvation is the enemy but no enemy is absolute...";
            displayText[4] = "No solution is either. You will be slower when gorged. Do keep this in mind.";
        }
        else if(player.getHealth()>50){
            display.put(player.getPosition().x, player.getPosition().y, '∆', 21);
            displayText[3] = "A.L.I.C.E: I would not go so far as to call you healthy but//";
            displayText[4] = "...you aren't dying. Continue that.";
        }
        else if(player.getHealth()>25){
            display.put(player.getPosition().x, player.getPosition().y, '∆', 18);
            displayText[3] = "A.L.I.C.E: I actually need to remind you to eat.";
            displayText[4] = "This is not inspiring.";
        }
        else if(playerIsAlive){
            display.put(player.getPosition().x, player.getPosition().y, '∆', 12);
            displayText[3] = "A.L.I.C.E: You are dying. Not to make this about me//";
            displayText[4] = "But you aren't terribly useful to me dead. Fix it. Please.";
        }
        else{
            display.put(player.getPosition().x, player.getPosition().y, '±', 2);
            playerIsAlive = false;
            lang[0] = "Submit a screenshot of your score screen to the devs.";
            lang[1] = "Top scores will be posted to the website.";
            lang[2] = "'H' for help screen\t'E' for score screen\t'Q' to quit\t'T' restart the game";
            displayText[3] = "A.L.I.C.E: You have died. What a waste...";
            displayText[4] = "You can try again if you really think you're worthy.";
        }
        // for clarity, you could replace the above line with the uncommented line below
        //display.put(player.x, player.y, '@', SColor.INTERNATIONAL_ORANGE);
        // since this is what 6 refers to, a color constant in a palette where 6 refers to this shade of orange.
        // You could experiment with different SColors; the JavaDocs for each color show a nice preview.
        // To view JavaDocs for a field, you can use Ctrl-Q in IntelliJ IDEA and Android Studio, or
        // just mouse over in Eclipse.
        // SColor extends Color, so you can use an SColor anywhere a Color is expected.

        // The arrays we produced in create() are used here to provide a blank area behind text
        display.put(0, gridHeight + 1, spaces, languageFG, languageBG);
        if(helpOn) displayText = helpText;
        if(scoreScreenOn) displayText = scoreScreen;
        else if(normal) displayText = lang;
        display.putString(2, gridHeight + 1, displayText[0], 0, 1);
        display.putString(2, gridHeight + 2, displayText[1], 0, 1);
        display.putString(2, gridHeight + 3, displayText[2], 0, 1);
        display.putString(2, gridHeight + 4, displayText[3], 0, 1);
        display.putString(2, gridHeight + 5, displayText[4], 0, 1);
//        for (int i = 0; i < 6; i++) {
//            display.putString(2, gridHeight + i + 1, lang[(langIndex + i) % lang.length], 0, 1);
//        }

    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        lightCounter += Gdx.graphics.getDeltaTime() * 15;
        // this does the standard lighting for walls, floors, etc. but also uses counter to do the Simplex noise thing.
        lights = DungeonUtility.generateLightnessModifiers(decoDungeon, lightCounter);
        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        // if the user clicked, we have a list of moves to perform.
        if(!awaitedMoves.isEmpty())
        {
            // this doesn't check for input, but instead processes and removes Points from awaitedMoves.
            if(this.input.hasNext()){
                awaitedMoves.clear();
                playerToCursor.clearGoals();
            }
            secondsWithoutMoves += Gdx.graphics.getDeltaTime();
            if (secondsWithoutMoves >= 0.1) {
                secondsWithoutMoves = 0;
                Coord m = awaitedMoves.remove(0);
                toCursor.remove(0);
                move(m.x - player.getPosition().x, m.y - player.getPosition().y);
            }
        }
        // if we are waiting for the player's input and get input, process it.
        else if(input.hasNext()) {
            input.next();
        }
        //stage has its own batch and must be explicitly told to draw().
        stage.draw();
        //you may need to explicitly tell stage to act() if input isn't working.
        //there shouldn't be a problem from calling act(), but
        //you can comment out the next line to test if input has problems.
        stage.act();
    }

    @Override
	public void resize(int width, int height) {
		super.resize(width, height);
        //very important to have the mouse behave correctly if the user fullscreens or resizes the game!
        // this gets the mouse's understanding of the screen updated for the newly resized grid.
        // you may need to change this if your game has multiple sections, like a SquidMessageBox or other widget below
        // the main grid of the game. If a game uses VisualInput, then that way of handling input will need to be told
        // about the changes to the screen as well.
		input.getMouse().reinitialize((float) width / this.gridWidth, (float)height / (this.gridHeight + 8), this.gridWidth, this.gridHeight, 0, 0);
	}

	public void eatFood(){
        for(Food food : foodList){
            if(food.getPosition().equals(player.getPosition())){
                foodList.remove(food);
                foodEaten++;
                player.setHealth(player.getHealth() + 10);
                break;
            }
        }
    }

	public ArrayList<Food> addFood(){
	    int foodToAdd = 10 - levelCount;
        ArrayList<Food> toReturn = new ArrayList<>();
        while(foodToAdd > 0){
            foodToAdd--;
            for(char[][] room : this.roomFinder.findRooms()){
                DungeonUtility dungeonUtility = new DungeonUtility(rng);
                double chance = Math.random();
                if(chance >= .5){
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

    public void restart(){
        playerIsAlive = true;
        player.setHealth(101);
        player.setTurns(-1);
        levelCount = 1;
        foodEaten = 0;
        create();
    }
}
