package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.BresenhamLine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static io.zipcoder.originwar.model.PlayerAction.Type.*;

/** A deliberately bounded ordinary-action player. The policy never receives a GameState or engine. */
class SurvivalObservedPilotTest {
    enum Approach { CONTROLLER, REDIRECT }
    record Cell(int x,int y) { int distance(Cell b){return Math.abs(x-b.x)+Math.abs(y-b.y);} }
    record Gear(String id,GameState.ItemType type,String name,int power,boolean equipped) {}
    record Contact(String id,GameState.ActorKind kind,Cell cell,int hp,int fuse,boolean aiming,boolean active,boolean redirected) {}
    record Control(Cell cell,boolean powered) {}
    record Observation(int deck,int tick,long action,Cell position,int hp,int reserve,int oxygenation,int cells,
                       int bulk,String weapon,String armor,boolean lamp,boolean release,boolean constructorStopped,
                       boolean podReady,boolean dead,boolean victory,Tile[][] known,boolean[][] visible,
                       List<Gear> pack,List<GearAt> floor,List<Contact> actors,List<Control> controls,
                       Set<String> knowledge,Map<String,String> identified,List<String> messages) {
        Tile tile(Cell c){return inside(c)?known[c.x][c.y]:null;}
        boolean inside(Cell c){return c.x>=0&&c.y>=0&&c.x<known.length&&c.y<known[0].length;}
        Contact actor(Cell c){return actors.stream().filter(a->a.cell.equals(c)).findFirst().orElse(null);}
        List<Cell> locations(Tile t){var r=new ArrayList<Cell>();for(int x=0;x<known.length;x++)for(int y=0;y<known[x].length;y++)if(known[x][y]==t)r.add(new Cell(x,y));return r;}
    }
    record GearAt(Gear item,Cell cell) {}

    /** The only information boundary. Full snapshots are used outside this adapter solely for audit/replay. */
    static Observation observe(GameState s,List<String> messages){
        var known=new Tile[s.width][s.height];var visible=new boolean[s.width][s.height];
        for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++){visible[x][y]=s.visible[x][y];if(s.explored[x][y])known[x][y]=StationSystems.knownTile(s,x,y);}
        var pack=s.inventory.stream().map(i->gear(i,s.flags)).toList();
        var floor=s.floorItems.stream().filter(i->s.visible[i.x][i.y]).map(i->new GearAt(gear(i,s.flags),new Cell(i.x,i.y))).toList();
        var actors=s.actors.stream().filter(a->!a.dead&&s.visible[a.x][a.y]).map(a->new Contact(a.id,a.kind,new Cell(a.x,a.y),a.hp,a.fuse,a.aiming,
                (a.kind!=GameState.ActorKind.CAMERA&&a.kind!=GameState.ActorKind.TURRET&&a.kind!=GameState.ActorKind.CONSTRUCTOR&&a.kind!=GameState.ActorKind.CONTROLLER)||StationSystems.powered(s,a.x,a.y),a.redirected)).toList();
        var controls=new ArrayList<Control>();for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)if(s.visible[x][y]&&s.tiles[x][y]==Tile.TERMINAL)controls.add(new Control(new Cell(x,y),StationSystems.powered(s,x,y)));
        for(var f:s.fixtures)if((f.kind.equals("breaker")||f.kind.equals("controller"))&&s.visible[f.x][f.y])controls.add(new Control(new Cell(f.x,f.y),StationSystems.powered(s,f.x,f.y)));
        var identified=new TreeMap<String,String>();for(String name:s.identifiedMedicine)identified.put(name,s.medicineEffects.get(name));
        return new Observation(s.deck,s.tick,s.actionSerial,new Cell(s.playerX,s.playerY),s.hp,s.reserve,s.oxygenation,s.batteries,EquipmentRules.bulk(s),s.weapon,s.armor,s.lampOn,s.switchOn,
                s.flags.contains("constructor-defeated"),s.flags.contains("pod-ready"),s.dead,s.victory,known,visible,pack,floor,actors,List.copyOf(controls),Set.copyOf(s.knowledge),Map.copyOf(identified),List.copyOf(messages));
    }
    private static Gear gear(GameState.Item i,Set<String> flags){return new Gear(i.id,i.type,i.name,i.power,InventoryStacks.equipped(i,flags));}

    static final class Policy {
        final Approach approach;
        final Map<Cell,Integer> visits=new HashMap<>();
        final Map<String,GearAt> seenSupplies=new LinkedHashMap<>();
        int deck=-1,hacks,lastHackTick=-100,redirectWait;
        Cell controller;
        String supplyGoal="";
        String purpose="";
        Policy(Approach approach){this.approach=approach;}
        PlayerAction choose(Observation o){
            if(deck!=o.deck){deck=o.deck;visits.clear();seenSupplies.clear();controller=null;supplyGoal="";hacks=0;redirectWait=0;}
            visits.merge(o.position,1,Integer::sum);
            for(var item:o.floor)seenSupplies.put(item.item.id,item);
            seenSupplies.entrySet().removeIf(e->o.visible[e.getValue().cell.x][e.getValue().cell.y]&&o.floor.stream().noneMatch(i->i.item.id.equals(e.getKey())));
            if(o.deck==8&&controller==null)for(String fact:o.knowledge)if(fact.startsWith("supervisor-chart:")){var parts=fact.split(":");if(parts.length==3)controller=new Cell(Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));}
            for(var a:o.actors)if(a.kind==GameState.ActorKind.CONTROLLER)controller=a.cell;
            var armed=o.actors.stream().filter(a->a.active&&a.kind==GameState.ActorKind.ROVER&&a.fuse>=0&&a.cell.distance(o.position)<=2).toList();
            if(!armed.isEmpty()){
                Cell escape=neighbors(o.position).stream().filter(c->walkable(o.tile(c))&&o.actor(c)==null&&o.tile(c).cost==1)
                        .max(Comparator.comparingInt(c->armed.stream().mapToInt(a->c.distance(a.cell)).min().orElse(0)*100-visits.getOrDefault(c,0))).orElse(null);
                if(escape!=null)return move(o,escape,"evade observed armed unit");
            }
            if(o.hp<=68){var heal=o.pack.stream().filter(i->i.type==GameState.ItemType.MEDICINE&&"heal".equals(o.identified.get(i.name))).findFirst();if(heal.isPresent())return item(USE,heal.get().id,"identified healing");}
            if(o.reserve<=180){var oxygen=o.pack.stream().filter(i->i.type==GameState.ItemType.OXYGEN).findFirst();if(oxygen.isPresent())return item(USE,oxygen.get().id,"refill carried oxygen");}
            if(o.cells<=3){var cell=o.pack.stream().filter(i->i.type==GameState.ItemType.CELL).findFirst();if(cell.isPresent())return item(USE,cell.get().id,"reload finite suit bank");}
            var target=o.actors.stream().filter(a->a.active&&a.kind!=GameState.ActorKind.CAMERA&&a.kind!=GameState.ActorKind.CONTROLLER&&a.kind!=GameState.ActorKind.CONSTRUCTOR&&!a.redirected)
                    .filter(a->a.cell.distance(o.position)<=EquipmentRules.weapon(o.weapon).range()&&clear(o,o.position,a.cell))
                    .filter(a->a.aiming||a.kind==GameState.ActorKind.ROVER||a.cell.distance(o.position)<=4)
                    .min(Comparator.comparingInt(a->(a.kind==GameState.ActorKind.ROVER?-20:0)+(a.aiming?-10:0)+a.cell.distance(o.position))).orElse(null);
            if(target!=null&&o.cells>=EquipmentRules.weapon(o.weapon).batteries())return at(SHOOT,target.cell,"engage observed immediate threat");
            for(var i:o.floor)if(i.cell.equals(o.position)){
                if(i.item.type==GameState.ItemType.OXYGEN){
                    if(o.reserve<=180)return item(USE,i.item.id,"use observed oxygen on floor");
                    if(o.bulk+3<=12&&oxygen(o)<360)return item(PICKUP,i.item.id,"carry observed oxygen");
                }
                if(i.item.type==GameState.ItemType.CELL&&o.bulk<12&&o.pack.stream().filter(g->g.type==GameState.ItemType.CELL).count()<2)return item(PICKUP,i.item.id,"pack observed cells");
                if(i.item.type==GameState.ItemType.MEDICINE&&o.bulk<12&&"heal".equals(o.identified.get(i.item.name)))return item(PICKUP,i.item.id,"pack identified healing");
                if((i.item.type==GameState.ItemType.ARMOR_LIGHT&&o.armor.equals("suit"))||(i.item.type==GameState.ItemType.ARMOR_HEAVY&&!o.armor.equals("heavy")))return item(EQUIP,i.item.id,"wear observed protective shell");
            }
            boolean searchController=o.deck==8&&!o.constructorStopped&&o.release&&controller==null;
            if(searchController&&!o.lamp)return of(LAMP,"illuminate search for an unlocated supervisory fixture");
            if(o.lamp&&!searchController)return of(LAMP,"reduce optical exposure");
            if(o.deck==8&&!o.constructorStopped&&controller!=null&&o.position.distance(controller)<=1){
                if(approach==Approach.CONTROLLER)return at(INTERACT,controller,"physical supervisory shutdown");
                var power=o.controls.stream().filter(c->c.cell.equals(controller)).findFirst().orElse(null);
                if(power!=null&&!power.powered)return at(CIRCUIT,controller,"manually restore observed supervisory circuit before service access");
                if(o.cells>=2&&hacks<7&&(hacks<2||o.tick-lastHackTick>=12)){hacks++;lastHackTick=o.tick;return at(HACK,controller,"paid supervisory unit-redirection request");}
                if(redirectWait++<60)return of(WAIT,"wait for redirected unit using real ticks");
                throw new IllegalStateException("Redirection did not stop fabrication within bounded wait; hacks="+hacks);
            }
            // A nearby service terminal is an observed physical instrument, not injected map knowledge.
            var terminal=o.locations(Tile.TERMINAL).stream().min(Comparator.comparingInt(o.position::distance)).orElse(null);
            if(terminal!=null&&o.position.distance(terminal)<=1&&(!o.knowledge.contains("service-plan-d"+o.deck)||(o.deck==10&&o.release&&!o.podReady))){
                var control=o.controls.stream().filter(c->c.cell.equals(terminal)).findFirst().orElse(null);
                if(control!=null&&!control.powered)return at(CIRCUIT,terminal,"restore observed terminal circuit");
                return at(INTERACT,terminal,"operate observed service terminal");
            }
            // Only observed or remembered supplies can attract the policy off the acquired route.
            boolean oxygenNeed=o.reserve+oxygen(o)<260,cellNeed=o.cells+o.pack.stream().filter(i->i.type==GameState.ItemType.CELL).mapToInt(i->i.power).sum()<10;
            var committed=seenSupplies.get(supplyGoal);if(committed!=null&&((oxygenNeed&&committed.item.type==GameState.ItemType.OXYGEN)||(cellNeed&&committed.item.type==GameState.ItemType.CELL)||o.armor.equals("suit"))){var step=route(o,committed.cell);if(step!=null)return toward(o,step,"finish route to an observed supply");}supplyGoal="";
            var useful=seenSupplies.values().stream().filter(i->(oxygenNeed&&i.item.type==GameState.ItemType.OXYGEN)||(cellNeed&&i.item.type==GameState.ItemType.CELL)||
                            (o.armor.equals("suit")&&(i.item.type==GameState.ItemType.ARMOR_LIGHT||i.item.type==GameState.ItemType.ARMOR_HEAVY)))
                    .filter(i->o.position.distance(i.cell)<=(oxygenNeed?25:8)).sorted(Comparator.comparingInt(i->o.position.distance(i.cell))).toList();
            for(var i:useful){var step=route(o,i.cell);if(step!=null){supplyGoal=i.item.id;return toward(o,step,"recover previously observed "+i.item.type);}}
            Cell goal=null;
            if(!o.knowledge.contains("service-plan-d"+o.deck))goal=terminal;
            else if(o.deck==8&&!o.constructorStopped&&controller!=null)goal=controller;
            else if(searchController)goal=null;
            else if(!o.release)goal=o.locations(Tile.SWITCH).stream().findFirst().orElse(null);
            else if(o.deck==10&&!o.podReady)goal=terminal;
            else goal=o.locations(Tile.HATCH).stream().findFirst().orElse(null);
            if(goal!=null){
                if(o.position.distance(goal)<=1&&o.tile(goal)==Tile.SWITCH&&!o.release)return at(INTERACT,goal,"operate known physical hatch release");
                if(o.position.equals(goal)&&o.tile(goal)==Tile.HATCH&&o.release)return item(ADVANCE,"confirm","confirm irreversible descent");
                var step=route(o,goal);if(step!=null)return toward(o,step,"follow acquired route");
            }
            // Frontier selection consults remembered geometry only, never unobserved floor or objective coordinates.
            var frontiers=new ArrayList<Cell>();for(int x=0;x<o.known.length;x++)for(int y=0;y<o.known[x].length;y++){var c=new Cell(x,y);if(walkable(o.tile(c))&&!c.equals(o.position)&&neighbors(c).stream().anyMatch(n->o.inside(n)&&o.tile(n)==null))frontiers.add(c);}
            frontiers.sort(Comparator.comparingInt(c->o.position.distance(c)+visits.getOrDefault(c,0)*10));
            for(var frontier:frontiers){var step=route(o,frontier);if(step!=null)return toward(o,step,"explore known frontier");}
            var block=o.actors.stream().filter(a->a.cell.distance(o.position)==1&&a.kind!=GameState.ActorKind.CONTROLLER).findFirst().orElse(null);
            if(block!=null){if(o.cells>0&&clear(o,o.position,block.cell))return at(SHOOT,block.cell,"clear observed adjacent obstruction");return at(MELEE,block.cell,"emergency observed obstruction");}
            throw new IllegalStateException("No reachable known objective or frontier");
        }
        PlayerAction toward(Observation o,Cell step,String why){var a=o.actor(step);if(a!=null){
            if(a.kind==GameState.ActorKind.CONTROLLER){if(approach==Approach.CONTROLLER)return at(INTERACT,step,"physical controller on route");controller=step;return of(WAIT,"controller observed; prepare redirect");}
            if(o.cells>0&&clear(o,o.position,step))return at(SHOOT,step,"observed route obstruction");return at(MELEE,step,"emergency route obstruction");
        }if(o.tile(step)==Tile.DOOR_CLOSED)return at(INTERACT,step,"open observed closed door");return move(o,step,why);}
        Cell route(Observation o,Cell goal){
            int w=o.known.length,h=o.known[0].length;int[][] dist=new int[w][h];Cell[][] first=new Cell[w][h];for(var col:dist)Arrays.fill(col,Integer.MAX_VALUE);
            var q=new PriorityQueue<PathNode>(Comparator.comparingInt(PathNode::cost).thenComparingInt(n->n.cell.x).thenComparingInt(n->n.cell.y));dist[o.position.x][o.position.y]=0;q.add(new PathNode(o.position,0));
            while(!q.isEmpty()){var p=q.remove();if(p.cost!=dist[p.cell.x][p.cell.y])continue;if(p.cell.equals(goal))return first[goal.x][goal.y];
                for(var c:neighbors(p.cell)){Tile t=o.tile(c);if(t==null||(!walkable(t)&&t!=Tile.DOOR_CLOSED))continue;var actor=o.actor(c);if(actor!=null&&actor.kind!=GameState.ActorKind.CONTROLLER&&!c.equals(goal))continue;
                    int cost=p.cost+(t==Tile.DOOR_CLOSED?2:t.cost);if(cost<dist[c.x][c.y]){dist[c.x][c.y]=cost;first[c.x][c.y]=p.cell.equals(o.position)?c:first[p.cell.x][p.cell.y];q.add(new PathNode(c,cost));}}
            }return null;
        }
        record PathNode(Cell cell,int cost) {}
        PlayerAction move(Observation o,Cell c,String why){purpose=why;return PlayerAction.move(c.x-o.position.x,c.y-o.position.y);}
        PlayerAction at(PlayerAction.Type t,Cell c,String why){purpose=why;return PlayerAction.at(t,c.x,c.y);}
        PlayerAction item(PlayerAction.Type t,String id,String why){purpose=why;return PlayerAction.item(t,id);}
        PlayerAction of(PlayerAction.Type t,String why){purpose=why;return PlayerAction.of(t);}
    }
    static int oxygen(Observation o){return o.pack.stream().filter(i->i.type==GameState.ItemType.OXYGEN).mapToInt(i->i.power).sum();}
    static List<Cell> neighbors(Cell c){return List.of(new Cell(c.x,c.y-1),new Cell(c.x+1,c.y),new Cell(c.x,c.y+1),new Cell(c.x-1,c.y));}
    static boolean walkable(Tile t){return t!=null&&!t.blocksMovement;}
    static boolean clear(Observation o,Cell from,Cell to){var line=BresenhamLine.lineArray(from.x,from.y,to.x,to.y);for(int i=1;i<line.length;i++){Tile t=o.tile(new Cell(line[i].x,line[i].y));if(t==null)return false;if(i<line.length-1&&(t.blocksVision||t==Tile.COVER||t==Tile.GLASS))return false;}return o.tile(to)!=Tile.GLASS;}

    record Outcome(long seed,Approach approach,boolean victory,String route,int actions,int tick,int deck,int hp,int reserve,String failure,int resumes,String directory) {}
    @Test void observedCampaignsKeepAllAiAndExportDeterministicOrdinaryReplays()throws Exception {
        var root=Path.of("build/evidence/survival-observed",Instant.now().toString().replace(':','-')+"-"+UUID.randomUUID().toString().substring(0,8));Files.createDirectories(root);
        String seeds=System.getenv().getOrDefault("ORIGINWAR_PILOT_SEEDS","0,1,6,14");var outcomes=new ArrayList<Outcome>();
        for(String token:seeds.split(","))for(var approach:Approach.values())outcomes.add(attempt(Long.parseLong(token.trim()),approach,root));
        var table=new StringBuilder("seed\tapproach\tvictory\troute\tactions\tticks\tdeck\thp\treserve\tresumes\tfailure\n");for(var o:outcomes)table.append(o.seed).append('\t').append(o.approach).append('\t').append(o.victory).append('\t').append(o.route).append('\t').append(o.actions).append('\t').append(o.tick).append('\t').append(o.deck).append('\t').append(o.hp).append('\t').append(o.reserve).append('\t').append(o.resumes).append('\t').append(o.failure.replace('\t',' ')).append('\n');Files.writeString(root.resolve("outcomes.tsv"),table);System.out.println(table+"Evidence: "+root.toAbsolutePath());
        for(var approach:Approach.values())assertTrue(outcomes.stream().anyMatch(o->o.victory&&o.approach==approach&&o.route.equals(approach==Approach.REDIRECT?"redirect":"controller")),"No observed-information victory via "+approach+"; see "+root.toAbsolutePath());
    }
    static Outcome attempt(long seed,Approach approach,Path root)throws Exception {
        Path dir=root.resolve("seed"+seed+"-"+approach.name().toLowerCase(Locale.ROOT));Files.createDirectories(dir);var actions=new ArrayList<PlayerAction>();var costs=new ArrayList<Integer>();var events=new ArrayList<String>();
        var commands=new StringBuilder("# Survival rules2; seed="+seed+"; approach="+approach+"; observed-information policy; all AI enabled\n");
        var trace=new StringBuilder("action\ttick\tdeck\tx\ty\thp\treserve\tcells\tpurpose\tcommand\tobservation_sha256\tworld_sha256\tevents\n");
        RunEngine run=new RunEngine(seed);var policy=new Policy(approach);List<String> messages=List.of();String failure="";var checkpoints=new HashSet<String>();int deck10Actions=-1;GameState deck10State=null;
        for(int n=0;n<5000&&!run.saveState().dead&&!run.saveState().victory;n++){
            var before=run.saveState();var observation=observe(before,messages);PlayerAction action;
            try{action=policy.choose(observation);}catch(RuntimeException e){failure="POLICY: "+e.getMessage();break;}
            var result=run.submit(action);if(!result.accepted()){failure="REJECTED "+line(action)+": "+result.reason();break;}
            actions.add(action);costs.add(result.cost());events.add(canonical(result.events()));commands.append(line(action)).append('\n');
            var after=run.saveState();messages=result.events().stream().map(GameEvent::detail).toList();trace.append(actions.size()).append('\t').append(after.tick).append('\t').append(after.deck).append('\t').append(after.playerX).append('\t').append(after.playerY).append('\t').append(after.hp).append('\t').append(after.reserve).append('\t').append(after.batteries).append('\t').append(policy.purpose).append('\t').append(line(action).replace('\t','|')).append('\t').append(hash(observation)).append('\t').append(hash(after)).append('\t').append(String.join(" / ",messages).replace('\t',' ')).append('\n');
            if(before.deck==9&&after.deck==10){deck10Actions=actions.size();deck10State=after.copy();}
            String checkpoint=after.deck>=5&&!checkpoints.contains("midcampaign")?"midcampaign":after.deck==8&&after.actors.stream().anyMatch(a->!a.dead&&after.visible[a.x][a.y]&&a.fuse>=0)&&!checkpoints.contains("live-fuse")?"live-fuse":after.deck==10&&after.flags.contains("enforcement-refused")&&!checkpoints.contains("command-refusal")?"command-refusal":"";
            if(!checkpoint.isEmpty()){String saved=hash(after);run=new RunEngine(after);assertEquals(saved,hash(run.saveState()),"Actual core resume changed state");checkpoints.add(checkpoint);Files.writeString(dir.resolve(checkpoint+".txt"),"actions="+actions.size()+" tick="+after.tick+" world_sha256="+saved+"\n");}
        }
        var end=run.saveState();if(end.dead)failure="DEAD: "+(end.log.isEmpty()?"":end.log.getLast());else if(!end.victory&&failure.isEmpty())failure="ACTION_LIMIT";
        Files.writeString(dir.resolve("actions.tsv"),commands);Files.writeString(dir.resolve("trace.tsv"),trace);
        if(deck10State!=null){
            var prefix=new StringBuilder("# seed="+seed+"; rules=2; new-game prefix through first deck10 state\n");var suffix=new StringBuilder("# seed="+seed+"; rules=2; resume after deck10-prefix.tsv; no state injection\n");
            for(int n=0;n<actions.size();n++)(n<deck10Actions?prefix:suffix).append(line(actions.get(n))).append('\n');
            Files.writeString(dir.resolve("deck10-prefix.tsv"),prefix);Files.writeString(dir.resolve("deck10-suffix.tsv"),suffix);
            Files.writeString(dir.resolve("deck10-split.txt"),"seed="+seed+" approach="+approach+"\nprefixActions="+deck10Actions+" suffixActions="+(actions.size()-deck10Actions)+"\nprefix "+telemetry(deck10State)+"\nprefixWorldSha256="+hash(deck10State)+"\nfinal "+telemetry(end)+"\nfinalWorldSha256="+hash(end)+"\n");
        }
        // The uninterrupted replay receives no policy snapshots, state changes, removals, grants or RNG edits.
        var replay=new RunEngine(seed);for(int n=0;n<actions.size();n++){var r=replay.submit(actions.get(n));assertTrue(r.accepted(),"Replay rejected action "+n);assertEquals(costs.get(n),r.cost());assertEquals(events.get(n),canonical(r.events()));}assertEquals(hash(end),hash(replay.saveState()),"Fresh ordinary-action replay diverged");
        var outcome=new Outcome(seed,approach,end.victory,end.constructorRoute,actions.size(),end.tick,end.deck,end.hp,end.reserve,failure,checkpoints.size(),dir.toAbsolutePath().toString());Files.writeString(dir.resolve("summary.txt"),outcome+"\nAll AI enabled; no privileged geometry/objectives/hidden actors in policy; actual core-copy resumes and uninterrupted replay verified. Not human difficulty, graphical quality, native input or desktop filesystem-save acceptance.\n");System.out.println(outcome);return outcome;
    }
    static String line(PlayerAction a){return a.type()+"\t"+a.x()+"\t"+a.y()+"\t"+a.id();}
    static String telemetry(GameState s){return "deck="+s.deck+" tick="+s.tick+" actionSerial="+s.actionSerial+" hp="+s.hp+" reserve="+s.reserve+" oxygenation="+s.oxygenation+" batteries="+s.batteries+" victory="+s.victory+" constructorRoute="+s.constructorRoute;}
    @Test void hiddenGeometryAndHiddenActorChangesCannotAlterPolicyInputOrDecision(){
        var original=new RunEngine(0).saveState();var altered=original.copy();
        for(int x=0;x<altered.width;x++)for(int y=0;y<altered.height;y++)if(!altered.explored[x][y]){altered.tiles[x][y]=Tile.HAZARD;altered.blueprintTiles[x][y]=Tile.WALL;}
        for(var a:altered.actors)if(!altered.visible[a.x][a.y]){a.hp=1;a.lastX=0;a.lastY=0;a.facing=(a.facing+1)%4;}
        altered.switchX=0;altered.hatchX=0;Arrays.fill(altered.routeCosts,9999);Arrays.fill(altered.publicSupplyCells,0);
        var a=observe(original,List.of());var b=observe(altered,List.of());assertEquals(canonical(a),canonical(b));assertEquals(new Policy(Approach.CONTROLLER).choose(a),new Policy(Approach.CONTROLLER).choose(b));
    }
    static String hash(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    static String canonical(Object value){
        if(value==null)return "null";if(value instanceof String||value instanceof Number||value instanceof Boolean||value instanceof Enum<?>)return value.toString();
        if(value instanceof Map<?,?> m)return m.entrySet().stream().map(e->canonical(e.getKey())+"="+canonical(e.getValue())).sorted().toList().toString();
        if(value instanceof Set<?> s)return s.stream().map(SurvivalObservedPilotTest::canonical).sorted().toList().toString();
        if(value instanceof Iterable<?> list){var r=new ArrayList<String>();for(var v:list)r.add(canonical(v));return r.toString();}
        if(value.getClass().isArray()){var r=new ArrayList<String>();for(int n=0;n<Array.getLength(value);n++)r.add(canonical(Array.get(value,n)));return r.toString();}
        var fields=new TreeMap<String,String>();try{if(value.getClass().isRecord())for(var c:value.getClass().getRecordComponents())fields.put(c.getName(),canonical(c.getAccessor().invoke(value)));else for(var f:value.getClass().getFields())if(!Modifier.isStatic(f.getModifiers()))fields.put(f.getName(),canonical(f.get(value)));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}return value.getClass().getSimpleName()+fields;
    }
}
