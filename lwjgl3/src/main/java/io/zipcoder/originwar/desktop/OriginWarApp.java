package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.engine.EquipmentRules;
import io.zipcoder.originwar.campaign.Campaign;
import io.zipcoder.originwar.perception.PerceptionModel;
import io.zipcoder.originwar.model.*;
import java.nio.file.Path;
import java.util.*;

public final class OriginWarApp extends ApplicationAdapter {
    private enum Screen { TITLE, PLAY, PAUSE, INVENTORY, JOURNAL, STATS, MAP, RECORD, SETTINGS, HELP, CREDITS, DEAD, VICTORY }
    private record Button(Rectangle box,Runnable action){}
    private record Slider(Rectangle box,int channel){}
    private record Effect(String type,int x,int y,int toX,int toY,float until){}
    private final String[] args;
    private final ArrayList<Button> buttons=new ArrayList<>();
    private final ArrayList<Slider> sliders=new ArrayList<>();
    private final ArrayDeque<PlayerAction> walking=new ArrayDeque<>();
    private final ArrayDeque<String> replay=new ArrayDeque<>();
    private final ArrayList<Effect> effects=new ArrayList<>();
    private final GlyphLayout layout=new GlyphLayout();
    private SpriteBatch batch;
    private BitmapFont font;
    private GameArt art;
    private AudioDirector audio;
    private SaveStore saves;
    private SettingsStore settings;
    private RunEngine engine;
    private GameState state;
    private Screen screen=Screen.TITLE,returnScreen=Screen.TITLE;
    private String seedText="2016",notice="",capture="";
    private String diagnostics="";
    private int saveFailures;
    private int frame,maxFrames,selectedItem,targetX=-1,targetY=-1,dragging=-1;
    private int captureFrame=90;
    private final ArrayDeque<Float> frameIntervals=new ArrayDeque<>();
    private float cameraX,cameraY,moveDelay,clock,noticeTime;
    private float nextBreath,nextPulse,nextPhantom;
    private float endingStarted=-1;
    private boolean endingPanel=true;
    private long lastPerceptionCue=Long.MIN_VALUE;
    private PerceptionModel.Frame perception;
    private int journalEntry,journalPage,dialoguePage,lastJournalEntry=-1;
    private int prosePage,itemPage,lastSelectedItem=-1;
    private Screen proseScreen;
    private String lastDialogue="";
    private float mapX,mapY,mapW,mapH,originX,originY,tile=64,worldZoom=64;
    private boolean focused=true,seedEditing,captureRequested,noticeInDialogue=true;
    private int replayDelay=6;
    private Path screenshotDirectory;
    private final Color ink=new Color(.90f,.94f,.95f,1),cyan=new Color(.34f,.83f,.83f,1),amber=new Color(.96f,.68f,.3f,1);

    public OriginWarApp(String[] args){this.args=args;}
    @Override public void create(){
        batch=new SpriteBatch();font=new BitmapFont(Gdx.files.internal("ui/ui.fnt"));art=new GameArt();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear,Texture.TextureFilter.Linear);
        String local=System.getenv("LOCALAPPDATA");
        Path data=Path.of(local==null?System.getProperty("user.home"):local,"OriginWar");
        for(String arg:args){
            if(arg.startsWith("--user-data="))data=Path.of(arg.substring(12));
            if(arg.startsWith("--seed="))seedText=arg.substring(7);
            if(arg.startsWith("--capture="))capture=arg.substring(10);
            if(arg.startsWith("--diagnostics="))diagnostics=arg.substring(14);
            if(arg.startsWith("--capture-frame="))captureFrame=Integer.parseInt(arg.substring(16));
            if(arg.startsWith("--frames="))maxFrames=Integer.parseInt(arg.substring(9));
            if(arg.startsWith("--replay="))for(String line:Gdx.files.absolute(arg.substring(9)).readString("UTF-8").split("\\R"))if(!line.isBlank()&&!line.startsWith("#"))replay.add(line);
        }
        screenshotDirectory=data.resolve("screenshots");
        settings=new SettingsStore(data);saves=new SaveStore(data);audio=new AudioDirector();applyVolumes();audio.select("menu");
        Gdx.input.setInputProcessor(new InputAdapter(){
            @Override public boolean keyDown(int key){return key(key);}
            @Override public boolean keyTyped(char c){
                if(screen!=Screen.TITLE||!seedEditing)return false;
                if(c=='\b'&&!seedText.isEmpty())seedText=seedText.substring(0,seedText.length()-1);
                else if((Character.isDigit(c)||c=='-')&&seedText.length()<19)seedText+=c;
                return true;
            }
            @Override public boolean touchDown(int x,int y,int pointer,int button){return click(x,Gdx.graphics.getHeight()-y,button);}
            @Override public boolean touchDragged(int x,int y,int pointer){if(dragging<0)return false;setSlider(dragging,x);return true;}
            @Override public boolean touchUp(int x,int y,int pointer,int button){if(dragging<0)return false;dragging=-1;saveSettings();return true;}
            @Override public boolean scrolled(float x,float y){
                if(screen==Screen.INVENTORY&&state!=null)selectedItem=Math.max(0,Math.min(state.inventory.size()-1,selectedItem+(int)y));
                if(screen==Screen.JOURNAL&&state!=null)journalEntry=Math.max(0,Math.min(state.journal.size()-1,journalEntry+(int)y));return true;
            }
        });
        for(String arg:args)if(arg.equals("--play"))newGame();
        for(String arg:args)if(arg.equals("--resume"))resumeGame();
        if(state!=null)for(String arg:args){if(arg.startsWith("--view="))try{
            Screen view=Screen.valueOf(arg.substring(7).toUpperCase(Locale.ROOT));
            if(!EnumSet.of(Screen.MAP,Screen.RECORD,Screen.INVENTORY,Screen.STATS,Screen.JOURNAL,Screen.SETTINGS,Screen.HELP,Screen.PAUSE,Screen.CREDITS).contains(view))throw new IllegalArgumentException("Not a reading screen");
            open(view);
        }catch(IllegalArgumentException error){throw new IllegalArgumentException("Unknown reading view: "+arg,error);}}
        Gdx.app.log("OriginWar","Assets: atlas="+art.ready()+", music="+audio.loadedMusic()+", SFX="+audio.loadedSounds());
        if(!replay.isEmpty())Gdx.app.log("Replay","Ordinary action replay; no state injection. Commands="+replay.size());
    }
    private void newGame(){
        try{
            long seed=Long.parseLong(seedText);var candidate=new RunEngine(seed);saves.begin(candidate.saveState());
            engine=candidate;state=engine.snapshot().world();
            cameraX=state.playerX;cameraY=state.playerY;targetX=-1;targetY=-1;selectedItem=0;journalEntry=0;journalPage=0;endingStarted=-1;effects.clear();walking.clear();screen=Screen.PLAY;
            hint("Expedition "+seed+". Find the security override and exit hatch.");
        }catch(Exception e){notice("Cannot start expedition: "+e.getMessage());Gdx.app.error("NewGame",e.getMessage(),e);}
    }
    private void resumeGame(){
        try{
            engine=new RunEngine(saves.load());state=engine.snapshot().world();cameraX=state.playerX;cameraY=state.playerY;
            walking.clear();targetX=-1;targetY=-1;selectedItem=0;journalEntry=Math.max(0,state.journal.size()-1);endingStarted=-1;endingPanel=true;screen=state.dead?Screen.DEAD:state.victory?Screen.VICTORY:Screen.PLAY;
            hint(saves.usedBackup()?"Recovered the last good backup.":"Expedition resumed.");
        }catch(Exception e){notice(e.getMessage());}
    }
    private boolean act(PlayerAction action){
        if(engine==null||state==null||state.dead||state.victory||!focused)return false;
        int oldDeck=state.deck;String oldDialogue=state.activeDialogue;var result=engine.submit(action);
        if(!result.accepted()){
            if(action.type()!=PlayerAction.Type.MOVE)notice("Action unavailable. Check range, supplies or equipment.");
            return false;
        }
        state=engine.snapshot().world();for(var event:result.events()){
            audio.event(event);
            float duration=event.type().equals("explosion")?.8f:Set.of("hit","impact").contains(event.type())?.16f:.32f;
            if(Set.of("gun","melee","hit","impact","explosion","death").contains(event.type()))effects.add(new Effect(event.type(),event.x(),event.y(),event.type().equals("gun")&&event.x()==state.playerX&&event.y()==state.playerY?action.x():state.playerX,event.type().equals("gun")&&event.x()==state.playerX&&event.y()==state.playerY?action.y():state.playerY,clock+duration));
        }
        if(oldDialogue.equals("west-truth")&&state.activeDialogue.isEmpty())hint("G: collect supplies. R: refill oxygen.");
        if(state.deck!=oldDeck){walking.clear();effects.clear();targetX=-1;targetY=-1;cameraX=state.playerX;cameraY=state.playerY;}
        try{saves.save(engine.saveState());}catch(Exception e){saveFailures++;notice("Save failed: "+e.getMessage());Gdx.app.error("Save",e.getMessage(),e);}
        if(state.dead){walking.clear();screen=Screen.DEAD;}else if(state.victory){walking.clear();screen=Screen.VICTORY;}
        moveDelay=.13f;
        return true;
    }
    private boolean key(int key){
        if(key==Input.Keys.F12){captureRequested=true;return true;}
        if(key==Input.Keys.F11){fullscreen();return true;}
        if(key==Input.Keys.ESCAPE){
            walking.clear();seedEditing=false;
            if(screen==Screen.PLAY)screen=Screen.PAUSE;else back();
            audio.pause(screen==Screen.PAUSE);return true;
        }
        if(screen==Screen.TITLE){if(key==Input.Keys.ENTER)newGame();return false;}
        if(screen==Screen.PLAY&&state!=null&&!state.activeDialogue.isEmpty()){
            var choices=Campaign.dialogue(state).choices();
            int index=key==Input.Keys.NUM_1?0:key==Input.Keys.NUM_2?1:key==Input.Keys.NUM_3?2:-1;
            if(index>=0&&index<choices.size())act(PlayerAction.item(PlayerAction.Type.CHOOSE,choices.get(index).id()));
            return true;
        }
        if(screen==Screen.INVENTORY){
            if(key==Input.Keys.UP)selectedItem=Math.max(0,selectedItem-1);
            else if(key==Input.Keys.DOWN)selectedItem=Math.min(state.inventory.size()-1,selectedItem+1);
            else if(key==Input.Keys.U||key==Input.Keys.ENTER)inventoryAction(PlayerAction.Type.USE);
            else if(key==Input.Keys.X)inventoryAction(PlayerAction.Type.DROP);
            else if(key==Input.Keys.Q)inventoryAction(PlayerAction.Type.EQUIP);
            else if(key==Input.Keys.TAB)screen=Screen.PLAY;return true;
        }
        if(screen!=Screen.PLAY)return false;
        walking.clear();
        switch(key){
            case Input.Keys.UP,Input.Keys.W->act(PlayerAction.move(0,-1));
            case Input.Keys.DOWN,Input.Keys.S->act(PlayerAction.move(0,1));
            case Input.Keys.LEFT,Input.Keys.A->act(PlayerAction.move(-1,0));
            case Input.Keys.RIGHT,Input.Keys.D->act(PlayerAction.move(1,0));
            case Input.Keys.SPACE,Input.Keys.PERIOD->act(PlayerAction.of(PlayerAction.Type.WAIT));
            case Input.Keys.E,Input.Keys.ENTER->act(PlayerAction.of(PlayerAction.Type.INTERACT));
            case Input.Keys.G->act(PlayerAction.of(PlayerAction.Type.PICKUP));
            case Input.Keys.R->act(PlayerAction.of(PlayerAction.Type.USE));
            case Input.Keys.TAB,Input.Keys.I->screen=Screen.INVENTORY;
            case Input.Keys.J->screen=Screen.JOURNAL;
            case Input.Keys.K->open(Screen.MAP);
            case Input.Keys.C->screen=Screen.STATS;
            case Input.Keys.F->{if(targetX>=0)act(PlayerAction.at(PlayerAction.Type.SHOOT,targetX,targetY));else notice("Right-click a visible target first.");}
            case Input.Keys.H->{if(targetX>=0)act(PlayerAction.at(PlayerAction.Type.HACK,targetX,targetY));else act(PlayerAction.of(PlayerAction.Type.HACK));}
            case Input.Keys.B->{if(targetX>=0)act(PlayerAction.at(PlayerAction.Type.FORCE,targetX,targetY));else act(PlayerAction.of(PlayerAction.Type.FORCE));}
            case Input.Keys.M->act(PlayerAction.of(PlayerAction.Type.MELEE));
            case Input.Keys.V->act(PlayerAction.of(PlayerAction.Type.SCAN));
            case Input.Keys.Z->worldZoom=worldZoom==64?32:64;
            case Input.Keys.F1->open(Screen.HELP);
            default->{return false;}
        }return true;
    }
    private boolean click(float x,float y,int button){
        for(var slider:sliders)if(slider.box.contains(x,y)){dragging=slider.channel;setSlider(dragging,x);return true;}
        for(var b:List.copyOf(buttons))if(b.box.contains(x,y)){audio.play("ui");b.action.run();return true;}
        seedEditing=false;
        if(screen!=Screen.PLAY||!state.activeDialogue.isEmpty()||!new Rectangle(mapX,mapY,mapW,mapH).contains(x,y))return false;
        int gx=(int)Math.floor((x-originX)/tile),gy=(int)Math.floor((originY+tile-y)/tile);
        if(gx<0||gy<0||gx>=state.width||gy>=state.height)return true;
        if(button==Input.Buttons.RIGHT){targetX=gx;targetY=gy;notice(describe(gx,gy));}
        else{
            walking.clear();walking.addAll(engine.pathTo(gx,gy));
            if(walking.isEmpty()&&Math.abs(gx-state.playerX)+Math.abs(gy-state.playerY)<=1)act(PlayerAction.at(PlayerAction.Type.INTERACT,gx,gy));
        }return true;
    }
    private void inventoryAction(PlayerAction.Type type){
        if(selectedItem<0||selectedItem>=state.inventory.size())return;
        boolean committed=act(PlayerAction.item(type,state.inventory.get(selectedItem).id));
        selectedItem=Math.min(selectedItem,state.inventory.size()-1);
        if(committed&&!state.dead&&!state.victory){walking.clear();screen=Screen.PLAY;}
    }
    private String describe(int x,int y){
        if(!state.explored[x][y])return "Unexplored. Move closer to inspect.";
        if(!state.visible[x][y])return "Remembered terrain. Current objects and threats are outside sensor range.";
        for(var a:state.actors)if(!a.dead&&a.x==x&&a.y==y)return a.kind+"  HP "+a.hp+"/"+a.maxHp;
        for(var i:state.floorItems)if(i.x==x&&i.y==y)return i.name+"  [G to pick up when standing here]";
        return state.tiles[x][y].name().replace('_',' ');
    }
    @Override public void render(){
        if(frame>120&&focused&&screen==Screen.PLAY){if(frameIntervals.size()==8192)frameIntervals.removeFirst();frameIntervals.addLast(Gdx.graphics.getDeltaTime()*1000);}
        float dt=Math.min(.05f,Gdx.graphics.getDeltaTime());clock+=dt;moveDelay-=dt;noticeTime-=dt;
        effects.removeIf(effect->effect.until()<clock);
        if(!replay.isEmpty()&&engine!=null&&focused&&(screen==Screen.PLAY||replay.peekFirst().startsWith("CAPTURE\t"))&&--replayDelay<=0){
            String line=replay.removeFirst();String[] parts=line.split("\\t",-1);
            if(parts[0].equals("CAPTURE")){
                var file=Gdx.files.absolute(screenshotDirectory.resolve(parts[1]+".png").toString());file.parent().mkdirs();
                var pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());PixmapIO.writePNG(file,pixels,6,true);pixels.dispose();
                Gdx.app.log("Replay","Captured "+parts[1]+" at tick="+state.tick+", reserve="+state.reserve+", oxygenation="+state.oxygenation);
            }else{
                var action=new PlayerAction(PlayerAction.Type.valueOf(parts[0]),parts.length>1&&!parts[1].isEmpty()?Integer.parseInt(parts[1]):0,parts.length>2&&!parts[2].isEmpty()?Integer.parseInt(parts[2]):0,parts.length>3?parts[3]:"");
                if(!act(action))throw new IllegalStateException("Replay rejected "+line+" at tick "+state.tick);
            }
            replayDelay=3;
        }
        if(state!=null)perception=PerceptionModel.sample(state,clock,settings.reducedDistortion);
        if(screen==Screen.PLAY&&state!=null&&!state.activeDialogue.isEmpty())walking.clear();
        if(screen==Screen.PLAY&&focused&&moveDelay<=0&&!walking.isEmpty()){
            if(!act(walking.removeFirst()))walking.clear();
            for(var a:state.actors)if(RunEngine.isThreat(state,a)&&state.visible[a.x][a.y])walking.clear();
        }
        audio.select(musicRole());audio.update(dt);perceptionAudio();
        ScreenUtils.clear(.025f,.039f,.052f,1);
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight()));
        buttons.clear();sliders.clear();batch.begin();
        if(state!=null&&screen!=Screen.TITLE&&screen!=Screen.CREDITS&&screen!=Screen.VICTORY){world(dt);if(screen==Screen.PLAY&&state.activeDialogue.isEmpty())hud();}
        switch(screen){
            case TITLE->title();case PLAY->{if(state!=null&&!state.activeDialogue.isEmpty())dialogue();}case PAUSE->pauseMenu();case INVENTORY->inventory();
            case JOURNAL->journal();case STATS->stats();case MAP->stationMap();case RECORD->runRecord();case SETTINGS->settings();case HELP->help();
            case CREDITS->credits();case DEAD->ending(false);case VICTORY->ending(true);
        }
        if(noticeTime>0&&!notice.isEmpty()&&(state==null||state.activeDialogue.isEmpty()||noticeInDialogue)){
            float width=Gdx.graphics.getWidth()-72;String label=notice;
            fontScale(1);layout.setText(font,label);
            while(layout.width>width&&label.length()>4){label=label.substring(0,label.length()-4)+"...";layout.setText(font,label);}
            rect(24,0,Gdx.graphics.getWidth()-48,Math.min(40,layout.height+12),new Color(.05f,.09f,.12f,1));
            text(label,36,6+layout.height,1,amber);
        }
        batch.end();frame++;
        if(captureRequested){
            var file=Gdx.files.absolute(screenshotDirectory.resolve(String.format(Locale.ROOT,"%06d-%s.png",frame,screen.name().toLowerCase(Locale.ROOT))).toString());
            file.parent().mkdirs();var pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            PixmapIO.writePNG(file,pixels,6,true);pixels.dispose();captureRequested=false;
        }
        if(!capture.isEmpty()&&frame==captureFrame){
            Gdx.files.absolute(capture).parent().mkdirs();
            var pixels=Pixmap.createFromFrameBuffer(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
            PixmapIO.writePNG(Gdx.files.absolute(capture),pixels,6,true);pixels.dispose();
        }
        if(maxFrames>0&&frame>=maxFrames)Gdx.app.exit();
    }
    private void world(float dt){
        float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();mapX=24;mapY=154;mapW=w-(settings.uiScale>1.25f?424:354);mapH=h-258;tile=worldZoom;
        cameraX=MathUtils.lerp(cameraX,state.playerX,Math.min(1,dt*14));cameraY=MathUtils.lerp(cameraY,state.playerY,Math.min(1,dt*14));
        originX=Math.round(mapX+mapW/2-(cameraX+.5f)*tile+(perception==null?0:perception.driftX()));originY=Math.round(mapY+mapH/2+(cameraY-.5f)*tile+(perception==null?0:perception.driftY()));
        rect(mapX-2,mapY-2,mapW+4,mapH+4,new Color(.12f,.24f,.27f,1));
        batch.flush();Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);Gdx.gl.glScissor((int)mapX,(int)mapY,(int)mapW,(int)mapH);
        rect(mapX,mapY,mapW,mapH,new Color(.008f,.014f,.021f,1));
        int zone=Math.min(4,(state.deck-1)/2);
        for(int x=0;x<state.width;x++)for(int y=0;y<state.height;y++){
            if(!state.explored[x][y])continue;
            float px=originX+x*tile,py=originY-y*tile;
            if(px+tile<mapX||px>mapX+mapW||py+tile<mapY||py>mapY+mapH)continue;
            float light=state.visible[x][y]?Math.max(.36f,state.light[x][y]):.16f;
            batch.setColor(light,light,light,1);
            if(art.ready()){
                art.sector(batch,zone,px,py,tile);int slot=tileSlot(state.tiles[x][y]);
                if(state.tiles[x][y]==Tile.WALL){art.sector(batch,5+zone,px,py,tile);if(Math.floorMod(DeterministicRandom.mix(state.seed+x*191L+y*811L+state.deck),17)==0)art.sector(batch,15+zone,px,py,tile);}
                else if(slot>=0)art.draw(batch,slot,px,py,tile);
                if(state.tiles[x][y]==Tile.COVER)art.sector(batch,10+zone,px,py,tile);
                if(state.tiles[x][y]==Tile.TERMINAL)art.sector(batch,25+zone,px,py,tile);
                if(state.tiles[x][y]==Tile.FLOOR&&Math.floorMod(DeterministicRandom.mix(state.seed+x*479L+y*151L+state.deck),47)==0)art.sector(batch,20+zone,px,py,tile);
                if(state.deck==10&&state.tiles[x][y]==Tile.HATCH)art.sector(batch,30,px,py,tile);
            }else{
                Color c=state.tiles[x][y].blocksMovement?new Color(.19f,.27f,.30f,1):new Color(.09f,.14f,.17f,1);
                rect(px,py,tile-1,tile-1,c.mul(light));
            }
        }
        for(var i:state.floorItems)if(state.visible[i.x][i.y])drawSprite(itemSlot(i.type),i.x,i.y,Color.CYAN);
        for(var a:state.actors)if(state.visible[a.x][a.y]){
            float px=originX+a.x*tile,py=originY-a.y*tile;batch.setColor(Color.WHITE);
            if(a.dead){if(a.kind==GameState.ActorKind.SIRS)art.pose(batch,47,px,py,tile);else if(a.kind==GameState.ActorKind.ADD)art.pose(batch,51,px,py,tile);else if(a.kind==GameState.ActorKind.ROVER)art.draw(batch,30,px,py,tile);continue;}
            int family=switch(a.kind){case ALICE->1;case SIRS->2;case ADD->3;case ROVER->4;default->-1;};
            int human=switch(a.kind){case WEST->0;case CARMACK->1;case SURVIVOR->2;case OPERATOR->3;default->-1;};
            boolean attacking=effects.stream().anyMatch(effect->effect.type().equals("gun")&&effect.x()==a.x&&effect.y()==a.y);
            if(human>=0&&art.completeHumans())art.human(batch,human,(int)(clock*.8f+Math.floorMod(a.id.hashCode(),5))%2,px,py,tile);
            else if(family>=0){
                if(attacking&&a.kind==GameState.ActorKind.SIRS)art.attack(batch,1,a.facing,(int)(clock*16)%2,px,py,tile);
                else if(attacking&&a.kind==GameState.ActorKind.ADD)art.attack(batch,2,a.facing,(int)(clock*16)%2,px,py,tile);
                else if(a.kind==GameState.ActorKind.ROVER&&a.fuse>=0&&a.fuse<=2)art.pose(batch,52+(int)(clock*8)%2,px,py,tile);
                else art.actor(batch,family,a.facing,(int)(clock*5)%2,px,py,tile);
            }else{
            int machine=switch(a.kind){case CAMERA->56;case TURRET->58;case OPERATOR->60;case CONSTRUCTOR->62;default->-1;};
            if(machine>=0&&art.completeActors())art.pose(batch,machine+(int)(clock*4)%2,px,py,tile);else{
            int slot=switch(a.kind){
                case ALICE->36;case SIRS->40;case ADD->44;case ROVER->48;case WEST->52;
                case CARMACK->53;case OPERATOR,SURVIVOR->54;case CONSTRUCTOR->55;case TURRET->29;case CAMERA->19;
            };
            if(slot>=36&&slot<52)slot+=(int)(clock*5)%4;
            drawSprite(slot,a.x,a.y,a.kind==GameState.ActorKind.ALICE?Color.CYAN:Color.SCARLET);
            }
            }
            if(!a.id.startsWith("story-")){
                bar(px+8,py+3,tile-16,4,(float)a.hp/a.maxHp,a.redirected?Color.CYAN:Color.SCARLET);
                if(a.kind==GameState.ActorKind.ROVER&&a.fuse>=0)text(""+a.fuse,px+tile*.38f,py+tile-5,.9f,amber);
            }
        }
        batch.setColor(Color.WHITE);
        boolean striking=effects.stream().anyMatch(effect->effect.type().equals("melee")||(effect.type().equals("gun")&&effect.x()==state.playerX&&effect.y()==state.playerY));
        if(state.dead)art.pose(batch,43,originX+state.playerX*tile,originY-state.playerY*tile,tile);
        else if(striking)art.attack(batch,0,state.facing,(int)(clock*16)%2,originX+state.playerX*tile,originY-state.playerY*tile,tile);
        else art.actor(batch,0,state.facing,moveDelay>0?(int)(clock*16)%2:0,originX+state.playerX*tile,originY-state.playerY*tile,tile);
        for(var effect:effects)if(effect.x()>=0&&effect.x()<state.width&&effect.y()>=0&&effect.y()<state.height&&state.visible[effect.x()][effect.y()]){
            float px=originX+effect.x()*tile,py=originY-effect.y()*tile;
            if(effect.type().equals("gun")){
                if(effect.toX()>=0&&effect.toX()<state.width&&effect.toY()>=0&&effect.toY()<state.height&&state.visible[effect.toX()][effect.toY()]){
                    float endX=originX+(effect.toX()+.5f)*tile,endY=originY+(.5f-effect.toY())*tile,dx=endX-px-tile/2,dy=endY-py-tile/2;
                    batch.setColor(.5f,.9f,1,.7f);batch.draw(art.whiteRegion(),px+tile/2,py+tile/2,0,0,(float)Math.hypot(dx,dy),2,1,1,MathUtils.atan2Deg(dy,dx));batch.setColor(Color.WHITE);
                }
            }else if(effect.type().equals("explosion")){art.pose(batch,54+(effect.until()-clock<.4f?1:0),px-tile*.3f,py-tile*.3f,tile*1.6f);}
            else if(effect.type().equals("hit")||effect.type().equals("impact"))art.draw(batch,57,px+tile*.2f,py+tile*.2f,tile*.6f);
        }
        if(perception!=null)for(var impression:perception.impressions()){
            batch.setColor(.65f,.7f,.95f,impression.opacity());
            if(art.ready())art.draw(batch,impression.variant()==0?59:40,originX+impression.x()*tile,originY-impression.y()*tile,tile);
            batch.setColor(Color.WHITE);
        }
        if(targetX>=0&&targetX<state.width&&targetY>=0&&targetY<state.height&&state.visible[targetX][targetY]){
            float x=originX+targetX*tile,y=originY-targetY*tile;
            rect(x,y,tile,2,amber);rect(x,y+tile-2,tile,2,amber);rect(x,y,2,tile,amber);rect(x+tile-2,y,2,tile,amber);
        }
        if(perception!=null&&perception.intensity()>0){
            float edge=20+perception.intensity()*34;Color shade=new Color(.15f,.015f,.09f,perception.vignette());
            rect(mapX,mapY,mapW,edge,shade);rect(mapX,mapY+mapH-edge,mapW,edge,shade);
            rect(mapX,mapY,edge,mapH,shade);rect(mapX+mapW-edge,mapY,edge,mapH,shade);
        }
        batch.flush();Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);batch.setColor(Color.WHITE);
    }
    private int tileSlot(Tile t){
        return switch(t){
            case WALL->5;case DOOR_CLOSED->8;case DOOR_OPEN->9;case LOCKED_DOOR->10;
            case LOW_GRAV->11;case NO_GRAV->12;case HAZARD->13;case COVER->14;case GLASS->7;
            case SWITCH->state.switchOn?16:15;case HATCH->state.switchOn?18:17;case TERMINAL->19;default->-1;
        };
    }
    private void drawSprite(int slot,int x,int y,Color fallback){
        batch.setColor(Color.WHITE);
        if(art.ready())art.draw(batch,slot,originX+x*tile,originY-y*tile,tile);
        else rect(originX+x*tile+8,originY-y*tile+8,tile-16,tile-16,fallback);
    }
    private int itemSlot(GameState.ItemType type){
        return switch(type){case OXYGEN->20;case CELL->21;case MEDICINE->22;case PISTOL->23;case RIFLE->24;case SCATTERGUN->25;case ARMOR_LIGHT->26;case ARMOR_HEAVY->27;};
    }
    private void hud(){
        if(settings.uiScale>1.25f){largeHud();return;}
        float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight(),x=w-310;
        text("ORIGIN WAR",24,h-30,1.4f,cyan);text(zoneName()+" / DECK "+state.deck,24,h-70,.95f,ink);
        text("SEED "+state.seed+"   TICK "+state.tick,w-310,h-32,.9f,ink);
        rect(x,mapY,286,mapH,new Color(.035f,.065f,.083f,1));text("SUIT TELEMETRY",x+16,h-126,1.1f,cyan);
        bar(x+16,h-162,254,14,(float)state.hp/state.maxHp,new Color(.8f,.29f,.32f,1));
        text("HP "+state.hp+" / "+state.maxHp,x+16,h-179,1,ink);
        bar(x+16,h-216,254,14,(float)state.reserve/state.maxReserve,state.reserve<=RunEngine.lowReserveThreshold(state)?amber:cyan);
        text("OXYGEN "+state.reserve+" / "+state.maxReserve,x+16,h-233,1,ink);
        text("Oxygenation "+state.oxygenation+"%",x+16,h-260,1,state.oxygenation<65?amber:ink);
        text("STR "+state.str+"    INT "+state.intel+"    DEX "+state.dex,x+16,h-303,1,ink);
        text("Lv "+state.level+"   Battery cells "+state.batteries,x+16,h-329,1,ink);
        text(state.weapon+" / "+state.armor+" / Tanks "+carriedTanks(),x+16,h-358,.95f,ink);
        hudObjective(x+16,h-390,250);
        perceptionBanner();
        button("Inventory [Tab]",x+16,mapY+77,254,30,()->open(Screen.INVENTORY));
        button("Journal [J]",x+16,mapY+41,254,30,()->open(Screen.JOURNAL));
        button("Pause [Esc]",x+16,mapY+5,254,30,()->open(Screen.PAUSE));
        text("WASD move  E interact  G pick up  R refill  Tab inventory  K map",24,132,.95f,ink);
        int start=Math.max(0,state.log.size()-3);
        float logY=103,logBottom=noticeTime>0&&!notice.isEmpty()?64:24;for(int n=state.log.size()-1;n>=start;n--){
            var message=state.log.get(n);var lines=pages(message,w-48,Math.max(20,logY-logBottom),.95f);
            fontScale(.95f);layout.setText(font,lines.getFirst(),ink,w-48,Align.left,true);if(logY-layout.height<logBottom)break;
            float used=paragraph(lines.getFirst(),24,logY,w-48,.95f,n==state.log.size()-1?cyan:ink);logY-=used+9;
        }
    }
    private void largeHud(){
        float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight(),x=w-380;
        text("ORIGIN WAR",24,h-32,1.05f,cyan);paragraph("DECK "+state.deck+" / "+zoneName(),24,h-72,w-48,.95f,ink);
        rect(x,mapY,356,mapH,new Color(.035f,.065f,.083f,1));
        bar(x+14,h-136,328,14,(float)state.hp/state.maxHp,new Color(.8f,.29f,.32f,1));text("HP "+state.hp+" / "+state.maxHp,x+14,h-151,.95f,ink);
        bar(x+14,h-210,328,14,(float)state.reserve/state.maxReserve,state.reserve<=RunEngine.lowReserveThreshold(state)?amber:cyan);fittedText("O2 "+state.reserve+" / "+state.maxReserve+" | Tanks "+carriedTanks(),x+14,h-226,328,.95f,ink);
        text("Oxygenation "+state.oxygenation+"%",x+14,h-276,.85f,state.oxygenation<70?amber:ink);
        hudObjective(x+14,h-322,328);
        button("Inventory [Tab]",x+14,mapY+77,328,30,()->open(Screen.INVENTORY));button("Journal [J]",x+14,mapY+41,328,30,()->open(Screen.JOURNAL));button("Pause [Esc]",x+14,mapY+5,328,30,()->open(Screen.PAUSE));
        if(!state.log.isEmpty())paragraph(state.log.getLast(),24,128,w-48,.95f,ink);
        perceptionBanner();
    }
    private int carriedTanks(){return state.inventory.stream().filter(i->i.type==GameState.ItemType.OXYGEN&&i.power>0).mapToInt(i->i.quantity).sum();}
    private void perceptionBanner(){
        if(perception==null||perception.intensity()==0)return;
        rect(mapX,mapY+mapH-44,mapW,44,new Color(.025f,.02f,.03f,1));
        text(state.oxygenation<=30?"CRITICAL HYPOXIA":"LOW OXYGENATION",mapX+10,mapY+mapH-10,.95f,amber);
    }
    private void hudObjective(float x,float top,float width){
        float height=Math.max(30,top-(mapY+120));var objective=pages(Campaign.objective(state),width,height,.95f);
        String label=objective.getFirst();
        if(objective.size()>1){
            while(!label.isEmpty()){
                fontScale(.95f);layout.setText(font,label+"... [J]",amber,width,Align.left,true);
                if(layout.height<=height)break;
                int split=label.lastIndexOf(' ');label=split<0?"":label.substring(0,split).stripTrailing();
            }
            label+="... [J]";
        }
        paragraph(label,x,top,width,.95f,amber);
    }
    private String zoneName(){
        return new String[]{"DOCKING / SECURITY","HABITATION / MAINTENANCE","MEDICAL / RESEARCH","AUTOMATED DEFENSE","COMMAND / EVACUATION"}[Math.min(4,(state.deck-1)/2)];
    }
    private String musicRole(){
        if(screen==Screen.TITLE||screen==Screen.CREDITS)return "menu";
        if(screen==Screen.VICTORY||(state!=null&&state.victory&&screen!=Screen.TITLE&&screen!=Screen.CREDITS))return "ending";
        if(state==null)return "menu";
        if(!state.activeDialogue.isEmpty()&&(state.activeDialogue.startsWith("alice")||state.deck==10||state.activeDialogue.equals("refusal")||state.activeDialogue.equals("sacrifice-plan")))return "alice";
        if(state.oxygenation<70||state.reserve==0)return "danger";
        if(state.actors.stream().anyMatch(a->RunEngine.isThreat(state,a)&&a.alert>0&&state.visible[a.x][a.y]))return "combat";
        return "exploration";
    }
    private void perceptionAudio(){
        if(screen!=Screen.PLAY||!focused||state==null||!state.activeDialogue.isEmpty()||perception==null||perception.intensity()==0)return;
        if(clock>=nextBreath){audio.play("breath");nextBreath=clock+4.2f-perception.intensity();}
        if(clock>=nextPulse){audio.play("pulse");nextPulse=clock+2.3f-perception.intensity();}
        if(clock>=nextPhantom&&lastPerceptionCue!=perception.cueKey()&&!perception.impressions().isEmpty()){
            audio.play("hallucination");nextPhantom=clock+9;lastPerceptionCue=perception.cueKey();
        }
    }
    private void dialogue(){
        var node=Campaign.dialogue(state);buttons.clear();sliders.clear();
        if(!lastDialogue.equals(state.activeDialogue)){dialoguePage=0;lastDialogue=state.activeDialogue;}
        float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
        rect(0,0,w,h,new Color(.008f,.018f,.027f,1));rect(36,52,w-72,h-104,new Color(.018f,.04f,.055f,1));
        int speaker=node.speaker().contains("WEST")?0:node.speaker().equals("DR. JAMES CARMACK")?1:node.speaker().startsWith("STATION MEDIC")?2:-1;
        boolean aliceSpeaker=node.speaker().equals("A.L.I.C.E.");
        boolean portrait=speaker>=0&&art.completeHumans()||aliceSpeaker&&art.completeActors();
        text(node.speaker(),66,h-87,1.15f,amber);
        fontScale(1.7f);layout.setText(font,node.title());
        float titleScale=1.7f*Math.min(1,(w-132-(portrait?128:0))/Math.max(1,layout.width));
        text(node.title(),66,h-124,titleScale,cyan);fontScale(titleScale);layout.setText(font,node.title());float titleHeight=layout.height;
        if(portrait){float ax=w-148,ay=h-156;rect(ax-4,ay-4,72,72,new Color(.035f,.075f,.09f,1));if(speaker>=0)art.human(batch,speaker,(int)(clock*.8f)%2,ax,ay,64);else art.actor(batch,1,2,(int)(clock*.8f)%2,ax,ay,64);}
        float choicesHeight=0;
        var choiceLabels=new ArrayList<String>();var choiceHeights=new ArrayList<Float>();int ordinal=0;
        for(var choice:node.choices()){
            String label=(++ordinal)+". "+choice.label()+(choice.available(state)?"":" [unavailable]");
            fontScale(.95f);layout.setText(font,label,ink,w-156,Align.left,true);
            float height=Math.max(36,layout.height+20);choiceLabels.add(label);choiceHeights.add(height);choicesHeight+=height+8;
        }
        float top=Math.min(h-180,h-124-titleHeight-26),bottom=82+choicesHeight+40;
        // Paragraph paging keeps all authored prose and choices reachable at enlarged text sizes.
        var pages=pages(node.text(),w-132,Math.max(60,top-bottom),1.05f);
        dialoguePage=Math.max(0,Math.min(dialoguePage,pages.size()-1));
        paragraph(pages.get(dialoguePage),66,top,w-132,1.05f,ink);
        if(pages.size()>1){
            button("Previous",66,bottom-34,160,32,()->dialoguePage=Math.max(0,dialoguePage-1));
            button("Next",238,bottom-34,140,32,()->dialoguePage=Math.min(pages.size()-1,dialoguePage+1));
            text((dialoguePage+1)+" / "+pages.size(),402,bottom-12,.9f,ink);
        }
        float y=74+choicesHeight;int index=0;
        for(var choice:node.choices()){
            final var selected=choice;float height=choiceHeights.get(index);y-=height+8;
            wrappedButton(choiceLabels.get(index++),66,y,w-132,height,()->{
                if(selected.available(state))act(PlayerAction.item(PlayerAction.Type.CHOOSE,selected.id()));else{notice("Requires INT "+selected.intelligence()+" and reserve "+selected.reserve()+".");audio.play("error");}
            });
        }
    }
    private List<String> pages(String value,float width,float height,float scale){
        var result=new ArrayList<String>();String current="";
        for(String word:value.split("(?<=\\s)")){
            fontScale(scale);layout.setText(font,current+word,ink,width,Align.left,true);
            if(layout.height>height&&!current.isEmpty()){result.add(current.strip());current=word.stripLeading();}else current+=word;
        }
        if(!current.isEmpty())result.add(current.strip());if(result.isEmpty())result.add("");return result;
    }
    private void title(){
        float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight(),x=w*.62f,y=h-160;
        fontScale(3/settings.uiScale);font.setColor(cyan);font.draw(batch,"ORIGIN",w*.11f,h-90);font.setColor(ink);font.draw(batch,"WAR",w*.11f,h-165);
        float leftWidth=x-w*.11f-40;
        paragraph("2115 / THE ORBITAL STATION",w*.11f,h-240,leftWidth,1.05f,amber);
        paragraph("Your orders promise a routine assignment. The station tells another story.",w*.11f,h-330,leftWidth,1.05f,ink);
        button("New expedition [Enter]",x,y,330,48,this::newGame);
        button("Resume expedition",x,y-60,330,48,this::resumeGame);
        button("How to play",x,y-120,330,48,()->open(Screen.HELP));
        button("Settings",x,y-180,330,48,()->open(Screen.SETTINGS));
        button("Credits",x,y-240,330,48,()->open(Screen.CREDITS));
        button("Quit",x,y-300,330,48,()->Gdx.app.exit());
        button("Seed: "+seedText+(seedEditing?"_":""),w*.11f,160,380,38,()->seedEditing=true);
        button("Randomize",w*.11f,110,180,36,()->seedText=""+System.currentTimeMillis());
        if(noticeTime<=0||notice.isEmpty())paragraph("Death ends an expedition. Suspend and resume between decisions.",w*.11f,80,w*.78f,.95f,ink);
    }
    private void overlay(String title){
        buttons.clear();sliders.clear();
        float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
        rect(0,0,w,h,new Color(.008f,.018f,.027f,1));
        fontScale(2);layout.setText(font,title);
        text(title,80,h-70,2*Math.min(1,Math.min((w-160)/Math.max(1,layout.width),40/Math.max(1,layout.height))),cyan);
        float backWidth=settings.uiScale>1.25f?250:160;button("Back [Esc]",w-80-backWidth,44,backWidth,36,this::back);
    }
    private void pauseMenu(){
        overlay("EXPEDITION PAUSED");float x=100,y=Gdx.graphics.getHeight()-160;
        float width=settings.uiScale>1.25f?380:300;
        button("Resume",x,y,width,44,()->{screen=Screen.PLAY;audio.pause(false);});
        button("Settings",x,y-60,width,44,()->open(Screen.SETTINGS));
        button("How to play",x,y-120,width,44,()->open(Screen.HELP));
        button("Save and return to title",x,y-180,width,44,()->{screen=Screen.TITLE;audio.pause(false);});
        button("Station map [K]",x+width+40,y,width,44,()->open(Screen.MAP));
        button("Expedition record",x+width+40,y-60,width,44,()->open(Screen.RECORD));
    }
    private void stationMap(){
        overlay("STATION MAP / DECK "+state.deck);float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
        float legendHeight=paragraph("Explored terrain. Cyan: you. Amber: objectives. Red: visible threat.",80,h-120,w-160,.95f,ink);
        float top=h-120-legendHeight-26,size=Math.min((w-160)/state.width,(top-130)/state.height),left=(w-size*state.width)/2;
        for(int x=0;x<state.width;x++)for(int y=0;y<state.height;y++)if(state.explored[x][y]){
            Tile t=state.tiles[x][y];Color color=t.blocksMovement?new Color(.19f,.25f,.28f,1):state.visible[x][y]?new Color(.42f,.56f,.59f,1):new Color(.16f,.25f,.28f,1);
            if(t==Tile.HATCH||t==Tile.SWITCH||t==Tile.TERMINAL)color=amber;
            rect(left+x*size,top-(y+1)*size,size-1,size-1,color);
        }
        for(var actor:state.actors)if(state.visible[actor.x][actor.y]&&RunEngine.isThreat(state,actor))rect(left+actor.x*size,top-(actor.y+1)*size,size-1,size-1,Color.SCARLET);
        rect(left+state.playerX*size,top-(state.playerY+1)*size,size-1,size-1,cyan);
    }
    private void runRecord(){
        overlay("EXPEDITION RECORD");
        String outcome=state.victory?"Escaped toward Mars":state.dead?"Expedition lost":"Expedition in progress";
        String body=outcome+"\n\nSeed "+state.seed+" / Deck "+state.deck+" / World ticks "+state.tick
            +"\n\nLevel "+state.level+" / XP "+state.xp+" / STR "+state.str+" / INT "+state.intel+" / DEX "+state.dex
            +"\n\nHP "+state.hp+" / "+state.maxHp+" / Oxygen reserve "+state.reserve+" / "+state.maxReserve+" / Oxygenation "+state.oxygenation+"%"
            +"\n\nWeapon "+state.weapon+" / Armor "+state.armor+" / Battery cells "+state.batteries
            +"\n\nIdentified medicines "+state.identifiedMedicine.size()+" / Journal entries "+state.journal.size()
            +"\n\nConstructor route: "+(state.constructorRoute.isEmpty()?"not completed":state.constructorRoute)
            +"\n\nCrew: "+(state.flags.contains("crew-safe")?"rescued":state.flags.contains("crew-decided")?"left behind":"undecided")
            +" / Fleet: "+(state.flags.contains("fleet-standdown")?"assault held":state.flags.contains("fleet-records")?"records received":"orders active");
        pagedProse(body,90,Gdx.graphics.getHeight()-135,Gdx.graphics.getWidth()-180,130,1.05f);
    }
    private void inventory(){
        overlay("INVENTORY / EQUIPMENT");float h=Gdx.graphics.getHeight(),w=Gdx.graphics.getWidth(),detailX=w*.53f,listWidth=detailX-120;
        selectedItem=Math.max(0,Math.min(selectedItem,state.inventory.size()-1));
        text("Weapon "+state.weapon+" / Armor "+state.armor+" / Cells "+state.batteries,90,h-115,.95f,ink);
        if(state.inventory.isEmpty()){text("Your pack is empty.",90,h-170,1,ink);return;}
        var labels=new ArrayList<String>();var heights=new ArrayList<Float>();
        for(int n=0;n<state.inventory.size();n++){
            var item=state.inventory.get(n);String label=(n==selectedItem?"> ":"  ")+item.name+" x"+item.quantity;
            fontScale(.95f);layout.setText(font,label,ink,listWidth-24,Align.left,true);
            labels.add(label);heights.add(Math.max(40,layout.height+20));
        }
        float available=h-290,used=heights.get(selectedItem);int first=selectedItem;
        while(first>0&&used+heights.get(first-1)+6<=available){first--;used+=heights.get(first)+6;}
        float rowTop=h-150;
        for(int n=first;n<state.inventory.size();n++){
            float height=heights.get(n);if(rowTop-height<140)break;rowTop-=height;
            final int index=n;
            wrappedButton(labels.get(n),90,rowTop,listWidth,height,()->selectedItem=index);rowTop-=6;
        }
        button("Up",90,88,80,36,()->selectedItem=Math.max(0,selectedItem-1));
        button("Down",182,88,100,36,()->selectedItem=Math.min(state.inventory.size()-1,selectedItem+1));
        fittedText("Item "+(selectedItem+1)+"/"+state.inventory.size(),294,113,listWidth-204,.95f,ink);
        if(selectedItem<0||selectedItem>=state.inventory.size())return;
        var item=state.inventory.get(selectedItem);float titleHeight=paragraph(item.name,detailX,h-170,w-detailX-70,1.2f,cyan);
        if(lastSelectedItem!=selectedItem){itemPage=0;lastSelectedItem=selectedItem;}
        float top=h-190-titleHeight;var pages=pages(itemDescription(item)+equipmentComparison(item),w-detailX-70,Math.max(60,top-280),1.05f);
        itemPage=Math.max(0,Math.min(itemPage,pages.size()-1));paragraph(pages.get(itemPage),detailX,top,w-detailX-70,1.05f,ink);
        if(pages.size()>1){button("<",detailX,235,60,32,()->itemPage=Math.max(0,itemPage-1));button(">",detailX+72,235,60,32,()->itemPage=Math.min(pages.size()-1,itemPage+1));text((itemPage+1)+" / "+pages.size(),detailX+150,257,.95f,ink);}
        if(item.type==GameState.ItemType.OXYGEN||item.type==GameState.ItemType.MEDICINE||item.type==GameState.ItemType.CELL)
            button("Use [U]",detailX,175,180,40,()->inventoryAction(PlayerAction.Type.USE));
        else button("Equip [Q]",detailX,175,180,40,()->inventoryAction(PlayerAction.Type.EQUIP));
        button("Drop [X]",detailX,123,180,40,()->inventoryAction(PlayerAction.Type.DROP));
    }
    private String itemDescription(GameState.Item i){
        return switch(i.type){
            case OXYGEN->"Finite oxygen tank. Restores "+i.power+" reserve, up to suit capacity. One turn to use; refill happens before breathing.";
            case CELL->"Battery cells power ranged weapons and station equipment.";
            case MEDICINE->state.identifiedMedicine.contains(i.name)?"Identified: "+state.medicineEffects.getOrDefault(i.name,"recorded compound"):"Unidentified pharmaceutical. Using or identifying this compound reveals its effect for this expedition.";
            case ARMOR_LIGHT->"Light protection: modest mitigation and good mobility.";
            case ARMOR_HEAVY->"Heavy protection trades mobility for damage mitigation.";
            case PISTOL->"Efficient security sidearm. Consumes battery cells.";
            case RIFLE->"Long-range precision weapon. Higher battery demand.";
            case SCATTERGUN->"Close-range energy scattergun. Strong nearby; inefficient at distance.";
        };
    }
    private String equipmentComparison(GameState.Item i){
        String weapon=switch(i.type){case PISTOL->"pistol";case RIFLE->"rifle";case SCATTERGUN->"scattergun";default->"";};
        if(!weapon.isEmpty()){
            var next=EquipmentRules.weapon(weapon);var now=EquipmentRules.weapon(state.weapon);
            return "\n\nCompared with "+state.weapon+":\nDamage "+now.damage()+" -> "+next.damage()+"\nRange "+now.range()+" -> "+next.range()+"\nCells per shot "+now.batteries()+" -> "+next.batteries()+"\nBase accuracy "+now.accuracy()+" -> "+next.accuracy()+"\nDEX, distance, target and cover affect the actual hit chance.";
        }
        String armor=switch(i.type){case ARMOR_LIGHT->"light";case ARMOR_HEAVY->"heavy";default->"";};
        if(!armor.isEmpty())return "\n\nCompared with "+state.armor+":\nProtection "+EquipmentRules.protection(state.armor)+" -> "+EquipmentRules.protection(armor)+"\nEffective DEX "+EquipmentRules.effectiveDex(state)+" -> "+EquipmentRules.effectiveDex(state.dex,armor);
        return "";
    }
    private void journal(){
        overlay("EXPEDITION JOURNAL");float y=Gdx.graphics.getHeight()-125,w=Gdx.graphics.getWidth();
        if(state.journal.isEmpty()){paragraph(Campaign.objective(state),90,y,w-190,1.1f,ink);return;}
        journalEntry=Math.max(0,Math.min(journalEntry,state.journal.size()-1));
        if(lastJournalEntry!=journalEntry){journalPage=0;lastJournalEntry=journalEntry;}
        var pages=pages(state.journal.get(journalEntry),w-190,y-190,1);
        journalPage=Math.max(0,Math.min(journalPage,pages.size()-1));paragraph(pages.get(journalPage),90,y,w-190,1,ink);
        button("Previous entry",90,96,220,36,()->journalEntry=Math.max(0,journalEntry-1));button("Next entry",326,96,220,36,()->journalEntry=Math.min(state.journal.size()-1,journalEntry+1));
        button("Page <",90,145,150,34,()->journalPage=Math.max(0,journalPage-1));button("Page >",254,145,150,34,()->journalPage=Math.min(pages.size()-1,journalPage+1));
        fittedText("Entry "+(journalEntry+1)+"/"+state.journal.size()+" / Page "+(journalPage+1)+"/"+pages.size(),430,170,w-520,.95f,ink);
    }
    private void stats(){
        overlay("CHARACTER / ADVANCEMENT");float y=Gdx.graphics.getHeight()-150;
        float w=Gdx.graphics.getWidth();
        pagedProse("HP "+state.hp+" / "+state.maxHp+"\n\nSTR "+state.str+": melee and forcing doors\n\nINT "+state.intel+": hacking and persuasion\n\nDEX "+state.dex+": ranged accuracy and evasion\n\nLevel "+state.level+" / XP "+state.xp+"\nAvailable increases "+state.attributePoints,90,y,w-500,140,1.1f);
        for(int n=0;n<3;n++){String id=new String[]{"str","int","dex"}[n];button("Increase "+id.toUpperCase(Locale.ROOT),w-350,y-60-n*60,250,40,()->act(PlayerAction.item(PlayerAction.Type.ALLOCATE,id)));}
    }
    private void settings(){
        overlay("SETTINGS / ACCESSIBILITY");float y=Gdx.graphics.getHeight()-150;
        String[] names={"Master volume","Music volume","Effects volume","UI scale"};
        float[] values={settings.master,settings.music,settings.sfx,settings.uiScale-1};
        for(int n=0;n<4;n++){
            fittedText(names[n],100,y-n*65,270,1.15f,ink);Rectangle box=new Rectangle(400,y-n*65-18,420,22);
            rect(box.x,box.y,box.width,box.height,new Color(.1f,.19f,.23f,1));rect(box.x,box.y,box.width*values[n],box.height,cyan);
            sliders.add(new Slider(box,n));text(n==3?Math.round(settings.uiScale*100)+"%":Math.round(values[n]*100)+"%",850,y-n*65,1,ink);
        }
        button("Reduced distortion: "+(settings.reducedDistortion?"ON":"OFF"),100,y-310,420,42,()->{settings.reducedDistortion=!settings.reducedDistortion;saveSettings();});
        button("Toggle fullscreen [F11]",100,y-365,420,42,this::fullscreen);
        button("Mute / restore master",570,y-310,300,42,()->{settings.master=settings.master>0?0:.8f;applyVolumes();saveSettings();});
    }
    private void setSlider(int channel,float x){
        for(var slider:sliders)if(slider.channel==channel){
            float value=MathUtils.clamp((x-slider.box.x)/slider.box.width,0,1);
            switch(channel){case 0->settings.master=value;case 1->settings.music=value;case 2->settings.sfx=value;case 3->settings.uiScale=1+value;}
            applyVolumes();
        }
    }
    private void applyVolumes(){audio.volumes(settings.master,settings.music,settings.sfx);}
    private void saveSettings(){try{settings.save();}catch(Exception e){notice("Settings could not be saved: "+e.getMessage());}}
    private void help(){
        overlay("HOW TO PLAY");
        pagedProse("Explore the station one decision at a time. Time advances only when you commit a world action. Menus, aiming and inspection pause the simulation.\n\nWASD / arrows: move. Bump closed doors to open them; bump hostile robots to melee. Left-click explored terrain to walk; visible threats interrupt the route. Right-click: inspect / select target.\n\nE / Enter: interact. G: pick up underfoot. R: oxygen tank. Space / period: wait. F: fire at selected target. M: adjacent melee. H: hack. B: force a locked door. V: inspect true suit status.\n\nTab / I: inventory. U / Enter: use. Q: equip. X: drop. J: journal. K: explored station map. C: character / earned attribute points. Esc: back / pause. F11: fullscreen. Z: world zoom. Dialogue choices: mouse or1/2/3.\n\nFind security overrides and follow the journal's story objectives. Low gravity and hazards cost extra time. Heavy armor lowers effective DEX. Observe real R.O.V.E.R. countdowns. Guns consume finite battery cells. Medicines are unidentified separately in each run.\n\nWatch reserve and oxygenation: an empty tank causes hypoxia. Visual impressions can be unreliable; suit telemetry and right-click sensor inspection remain true within legitimate vision. Refill and allow real turns for oxygenation to recover. Reduced distortion is available in Settings.\n\nDeath ends the expedition. Save and resume between decisions. Different seeds produce different stations.",90,Gdx.graphics.getHeight()-125,Gdx.graphics.getWidth()-190,130,1.04f);
    }
    private void credits(){
        overlay("ORIGIN WAR / CREDITS");
        pagedProse("Original 2016 design\nKen Ragonese and Evan Hitchings\n\nRemake direction\nOrigin War Revision 2, 20 October 2026\n\nTechnology\nlibGDX / LWJGL3, SquidSquad and their credited dependencies\n\nArtwork and audio\nOriginal remake production. Included provenance records identify sources, tools and verification status.\n\nUnverified legacy music and artwork are excluded from the modern runtime.",90,Gdx.graphics.getHeight()-130,Gdx.graphics.getWidth()-190,130,1.15f);
    }
    private void ending(boolean won){
        if(won&&art.finaleReady()){
            buttons.clear();sliders.clear();float w=Gdx.graphics.getWidth(),h=Gdx.graphics.getHeight();
            if(endingStarted<0){endingStarted=clock;endingPanel=true;}art.finale(batch,w,h);
            float elapsed=clock-endingStarted;String caption=elapsed<2?"BROADCAST / UNENCRYPTED":elapsed<4?"EVACUATION POD / SEALED":"TOWARD MARS";
            rect(0,h-90,w,90,new Color(.008f,.02f,.04f,.72f));text(caption,32,h-34,1.4f,cyan);
            if(elapsed<4)return;
            button("Run record",w-290,h-75,250,40,()->open(Screen.RECORD));
            if(!endingPanel){button("Read epilogue",40,48,280,44,()->endingPanel=true);return;}
            float panel=settings.uiScale>1.25f?420:340;rect(0,0,w,panel,new Color(.008f,.018f,.027f,1));
            pagedProse(Campaign.epilogue(state),40,panel-26,w-80,94,1.02f);
            button("New expedition",40,48,280,44,this::newGame);button("Title",340,48,180,44,()->screen=Screen.TITLE);button("Credits",540,48,180,44,()->open(Screen.CREDITS));button("View scene",w-290,48,250,44,()->endingPanel=false);return;
        }
        overlay(won?"TOWARD MARS":"EXPEDITION LOST");
        String body=won?Campaign.epilogue(state)
            :"The station keeps its silence.\n\nSeed "+state.seed+"   Deck "+state.deck+"   Turns "+state.tick+"\n\nThis expedition has ended. Equipment and discoveries belonged to this run.";
        pagedProse(body,100,Gdx.graphics.getHeight()-155,Gdx.graphics.getWidth()-200,210,1.15f);
        button("New expedition",100,130,280,44,this::newGame);button("Title",410,130,180,44,()->screen=Screen.TITLE);
        button("Run record",620,130,280,44,()->open(Screen.RECORD));
    }
    private void open(Screen next){returnScreen=screen;screen=next;walking.clear();if(next==Screen.INVENTORY&&state!=null)selectedItem=Math.max(0,Math.min(selectedItem,state.inventory.size()-1));audio.pause(next==Screen.PAUSE);}
    private void back(){
        walking.clear();
        if(screen==Screen.SETTINGS||screen==Screen.HELP||screen==Screen.CREDITS||screen==Screen.MAP||screen==Screen.RECORD)screen=returnScreen;
        else if(screen==Screen.DEAD||screen==Screen.VICTORY)screen=Screen.TITLE;
        else screen=state==null?Screen.TITLE:state.dead?Screen.DEAD:state.victory?Screen.VICTORY:Screen.PLAY;
        audio.pause(screen==Screen.PAUSE);
    }
    private void fullscreen(){if(Gdx.graphics.isFullscreen())Gdx.graphics.setWindowedMode(1280,720);else Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());}
    private void notice(String message){notice=message==null?"Unknown error":message;noticeTime=7;noticeInDialogue=true;}
    private void hint(String message){notice(message);noticeInDialogue=false;}
    private void rect(float x,float y,float w,float h,Color color){batch.setColor(color);batch.draw(art.white(),x,y,w,h);batch.setColor(Color.WHITE);}
    private void fontScale(float scale){font.getData().setScale(scale*settings.uiScale/3f);}
    private void text(String value,float x,float y,float scale,Color color){fontScale(scale);font.setColor(color);font.draw(batch,value,x,y);}
    private void fittedText(String value,float x,float y,float width,float scale,Color color){fontScale(scale);layout.setText(font,value);text(value,x,y,scale*Math.min(1,width/Math.max(1,layout.width)),color);}
    private float paragraph(String value,float x,float y,float width,float scale,Color color){
        fontScale(scale);layout.setText(font,value,color,width,Align.left,true);font.draw(batch,layout,x,y);return layout.height;
    }
    private void button(String label,float x,float y,float w,float h,Runnable action){
        rect(x,y,w,h,new Color(.075f,.14f,.17f,1));rect(x,y,w,2,new Color(.2f,.43f,.47f,1));
        fontScale(.95f);layout.setText(font,label);float fit=Math.min(1,Math.min((w-24)/Math.max(1,layout.width),(h-10)/Math.max(1,layout.height)));
        fontScale(.95f*fit);layout.setText(font,label);font.setColor(ink);font.draw(batch,label,Math.round(x+12),Math.round(y+(h+layout.height)/2));buttons.add(new Button(new Rectangle(x,y,w,h),action));
    }
    private void wrappedButton(String label,float x,float y,float w,float h,Runnable action){
        rect(x,y,w,h,new Color(.075f,.14f,.17f,1));rect(x,y,w,2,new Color(.2f,.43f,.47f,1));
        paragraph(label,x+12,y+h-10,w-24,.95f,ink);buttons.add(new Button(new Rectangle(x,y,w,h),action));
    }
    private void pagedProse(String value,float x,float top,float width,float controlsY,float scale){
        if(proseScreen!=screen){prosePage=0;proseScreen=screen;}
        var pages=pages(value,width,top-controlsY-65,scale);prosePage=Math.max(0,Math.min(prosePage,pages.size()-1));
        paragraph(pages.get(prosePage),x,top,width,scale,ink);
        if(pages.size()>1){button("Page <",x,controlsY,150,36,()->prosePage=Math.max(0,prosePage-1));button("Page >",x+164,controlsY,150,36,()->prosePage=Math.min(pages.size()-1,prosePage+1));text((prosePage+1)+" / "+pages.size(),x+340,controlsY+26,.95f,ink);}
    }
    private void bar(float x,float y,float w,float h,float fraction,Color color){
        rect(x,y,w,h,new Color(.12f,.16f,.18f,1));rect(x,y,w*MathUtils.clamp(fraction,0,1),h,color);
    }
    @Override public void pause(){focused=false;if(audio!=null)audio.pause(true);walking.clear();}
    @Override public void resume(){focused=true;if(audio!=null)audio.pause(screen==Screen.PAUSE);}
    @Override public void dispose(){
        var report=new StringBuilder("Origin War runtime diagnostics\nframes="+frame+" replayRemaining="+replay.size()+" saveFailures="+saveFailures+"\n");
        if(state!=null)report.append("seed=").append(state.seed).append(" deck=").append(state.deck).append(" tick=").append(state.tick).append(" hp=").append(state.hp).append(" reserve=").append(state.reserve).append(" oxygenation=").append(state.oxygenation).append(" dialogue=").append(state.activeDialogue).append(" victory=").append(state.victory).append(" dead=").append(state.dead).append('\n');
        if(!frameIntervals.isEmpty()){var samples=new ArrayList<>(frameIntervals);samples.sort(Float::compare);double average=samples.stream().mapToDouble(Float::doubleValue).average().orElse(0);String performance=String.format(Locale.ROOT,"Active foreground frame intervals: n=%d mean=%.2fms p95=%.2fms max=%.2fms heap=%.1fMiB",samples.size(),average,samples.get((int)((samples.size()-1)*.95)),samples.getLast(),(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1048576.0);Gdx.app.log("Performance",performance);report.append(performance).append('\n');}
        if(audio!=null)report.append(audio.summary()).append('\n');
        if(!diagnostics.isEmpty())try{var file=Gdx.files.absolute(diagnostics);file.parent().mkdirs();file.writeString(report.toString(),false,"UTF-8");}catch(GdxRuntimeException error){Gdx.app.error("Diagnostics","Could not write optional report",error);}
        if(audio!=null)audio.close();if(art!=null)art.close();if(batch!=null)batch.dispose();if(font!=null)font.dispose();
    }
}
