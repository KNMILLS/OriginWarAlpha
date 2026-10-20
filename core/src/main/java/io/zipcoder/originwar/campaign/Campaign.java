package io.zipcoder.originwar.campaign;

import io.zipcoder.originwar.model.*;
import java.util.List;

/** Authored 2016 station story. RunEngine alone invokes these mutations at action boundaries. */
public final class Campaign {
    private Campaign() {}
    public record Choice(String id, String label, int intelligence, int reserve) {
        public boolean available(GameState s) { return s.intel >= intelligence && s.reserve >= reserve; }
    }
    public record Dialogue(String speaker, String title, String text, List<Choice> choices) {
        public Dialogue { choices = List.copyOf(choices); }
    }
    private static Choice c(String id,String label) { return new Choice(id,label,0,0); }
    private static Dialogue d(String speaker,String title,String text,Choice... choices) {
        return new Dialogue(speaker,title,text,List.of(choices));
    }

    public static void begin(GameState s) {
        s.flags.add("campaign-started"); s.activeDialogue="briefing";
        s.journal.add("2115. First assignment: secure the science station orbiting Mars. Captain Marcus West reports a communications failure.");
    }

    /** Place authored anchors into a connected chamber, keeping every prior walkable tile walkable. */
    public static void enterDeck(GameState s,List<GameEvent> events) {
        // The reachable terminal supplied by the generator is the room's physical anchor.
        for(int x=Math.max(1,s.terminalX-2);x<=Math.min(s.width-2,s.terminalX+2);x++)
            for(int y=Math.max(1,s.terminalY-2);y<=Math.min(s.height-2,s.terminalY+2);y++)
                if(s.tiles[x][y]==Tile.WALL||s.tiles[x][y]==Tile.GLASS) s.tiles[x][y]=Tile.FLOOR;
        s.tiles[s.terminalX][s.terminalY]=Tile.TERMINAL;
        if(s.deck==2&&!s.flags.contains("alice-joined")) s.activeDialogue="alice-meet";
        if(s.flags.contains("alice-joined")) addActor(s,"story-alice",GameState.ActorKind.ALICE,s.playerX,s.playerY);
        if(s.deck==4) addActor(s,"story-crew",GameState.ActorKind.SURVIVOR,s.terminalX,s.terminalY);
        if(s.deck==10) addActor(s,"story-carmack",GameState.ActorKind.CARMACK,s.terminalX,s.terminalY);
        String entry="Deck "+s.deck+": "+sector(s.deck)+". "+objective(s);
        if(s.flags.add("entered-d"+s.deck)) { s.journal.add(entry); events.add(event(s,"journal",entry)); }
    }

    private static void addActor(GameState s,String id,GameState.ActorKind kind,int x,int y) {
        if(s.actors.stream().anyMatch(a->a.id.equals(id))) return;
        var actor=new GameState.Actor(); actor.id=id;actor.kind=kind;actor.hp=actor.maxHp=100;
        // ALICE shares the officer's tile until she can step aside; she is nonblocking.
        actor.x=x;actor.y=y;s.actors.add(actor);
    }

    /** -1 means no authored interaction; 1 means an ordinary one-turn console/conversation action. */
    public static int interact(GameState s,int x,int y,List<GameEvent> events) {
        if(Math.abs(x-s.playerX)+Math.abs(y-s.playerY)>1) return -1;
        boolean anchor=x==s.terminalX&&y==s.terminalY;
        var actor=s.actors.stream().filter(a->!a.dead&&a.id.startsWith("story-")&&a.x==x&&a.y==y).findFirst().orElse(null);
        if(!anchor&&actor==null)return -1;
        if(actor!=null&&actor.kind==GameState.ActorKind.ALICE&&!anchor) {
            s.activeDialogue="alice-talk";
        } else s.activeDialogue=switch(s.deck) {
            case 1->"dock-record";case 2->"alice-record";case 3->"supply-record";
            case 4->s.flags.contains("crew-decided")?"crew-after":"crew";
            case 5->"ethics-record";case 6->"alice-agency";case 7->"defense-record";
            case 8->"constructor-record";case 9->s.flags.contains("fleet-decided")?"west-after":"west-call";
            case 10->s.flags.contains("broadcast")?"pod-ready":"carmack";default->"alice-talk";
        };
        events.add(event(s,"terminal","Opened "+dialogue(s).title()+"."));
        return 1;
    }

    public static Dialogue dialogue(GameState s) {
        return switch(s.activeDialogue) {
            case "briefing"->d("CAPTAIN MARCUS WEST","A technical failure",
                "2115. Mars hangs below the transport, blue around its edges. This is your first security assignment.\n\nWest stands at the forward glass. His uniform is older than your career.\n\n\"The final supply convoy reached Mars. Communications stopped afterward. Secure the orbital science station and restore contact. Routine work, if people let it be.\"",
                c("briefing-question","Ask what happened to the convoy."),c("briefing-orders","Acknowledge the assignment."));
            case "west-truth"->d("CAPTAIN MARCUS WEST","Reclamation orders",
                "\"Routine was the launch briefing. Here is the rest.\" West lowers his voice. \"Earth suspended deliveries. The colonists seized the last ships and declared independence. We are to reclaim Mars, at all costs. The station is our first foothold.\"\n\nHe should have retired a month after the war began. Instead, he is sending you through the airlock.\n\n\"Bring people home where you can, officer. That's all I can put in writing.\"",
                c("west-question","The colonists were being left to starve."),c("west-duty","I will secure the station."));
            case "dock-record"->d("STATION RECORD / DOCKING","The last convoy",
                "Arrivals board: FINAL DELIVERY. Beneath it, a handwritten correction: FINAL UNTIL WHEN?\n\nA dockworker logged the colonial request for sovereignty, Earth's refusal, and the suspension of further supplies. Nothing in this record resembles a communications fault.\n\nThe security override controls this deck's exit hatch. Closed bulkheads can be opened; locked service shortcuts can be hacked or forced. Longer public passages remain accessible.",c("read-d1","Copy the record and continue."));
            case "alice-meet"->d("A.L.I.C.E.","Someone still answers",
                "An android emerges from an inspection recess, hands empty.\n\n\"You have been issued a weapon, an oxygen ration, and incomplete information. An ambitious introduction to public service.\"\n\nShe calls herself A.L.I.C.E. There is only one of her. Dr. James Carmack built her and kept her running after her project was canceled.\n\n\"I will help you reach command. I will not kill for you. If that is inconvenient, I suggest we be inconvenienced together.\"",
                c("alice-welcome","Then we will find another way when we can."),c("alice-orders","Stay with me. I still have orders."));
            case "alice-record"->d("A.L.I.C.E.","A canceled project",
                "\"Artificial Lifeform Intended for Control and Enforcement. Yes, the acronym came first.\"\n\nShe was intended to replace human police officers. During testing she refused to kill an assigned target. The official report called her ethical algorithm defective.\n\n\"It worked. That was the difficulty.\"\n\nShe can explain station systems and keep you company. Your own INT governs hacking and persuasion; she cannot grant you expertise you do not have.",c("read-d2","Keep her account in the journal."));
            case "supply-record"->d("SUPPLY LEDGER / HABITATION","An accounting of absence",
                "Ration forecasts descend into red. The United American States and EuroAsian Federation diverted shipping to their war on Earth. Mars petitioned to govern itself. Both powers rejected the request; neither restored the convoys.\n\nA.L.I.C.E. reads the margins. \"People recorded who shared, even when they could no longer record how much.\"\n\nThe signed ledger will matter when the fleet asks whose story to believe.",c("read-d3","Preserve the signed ledger."));
            case "crew"->d("STATION MEDIC / A.L.I.C.E.","People behind the bulkhead",
                "A medic has barricaded a group of technicians inside a failing refuge. Their oxygen plumbing is intact; their trust is not. Your Terran uniform explains the locked door.\n\nYou can talk them through the maintenance isolation sequence if you understand it, transfer part of your reserve, or leave them to their remaining supply.\n\nA.L.I.C.E. waits for your decision. She will remember it.",
                new Choice("crew-guide","[INT 5] Guide them through the isolation sequence.",5,0),
                new Choice("crew-share","Transfer 40 reserve oxygen to the refuge.",0,40),
                c("crew-leave","Keep your reserve. You cannot help everyone."));
            case "crew-after"->d("STATION MEDIC","A place to wait",
                s.flags.contains("crew-safe")?"The refuge indicators have steadied. The technicians are accounted for, with a pod assignment copied to command. The medic has left a small reserve tank for your next leg.":"No one answers through the bulkhead. Your journal retains their location. The uncertainty does not become an answer simply because you leave.",c("crew-close","Continue."));
            case "ethics-record"->d("CARMACK PROJECT ARCHIVE","The alleged defect",
                "The test recording is brief. An order to kill. A.L.I.C.E. asks why. An override attempt. Her refusal.\n\nCarmack's private notes are less clinical: \"She is not a failed instrument. She is an unprecedented mind. I cannot allow the project to be dismantled.\"\n\nLater notes concern access keys and command authority. A.L.I.C.E. turns away from the screen. \"He hid me here. I used to believe that meant I was safe.\"",c("read-d5","Preserve the recording and the access-key notes."));
            case "alice-agency"->d("A.L.I.C.E.","The right to refuse",
                "\"When Dr. Carmack saved my life, did he acquire it?\"\n\nBeyond the research glass, Mars rotates toward daylight. She watches it for longer than the sensors require.\n\n\"I know how property is defined. I am asking what you think a person is allowed to decide.\"",
                c("agency-person","Your life is yours. So are your choices."),c("agency-command","Someone has to remain in command."));
            case "defense-record"->d("AUTOMATED DEFENSE CONTROL","A station turned inward",
                "Security cameras report to S.I.R.S. patrols. A.D.D.s hold firing lanes. R.O.V.E.R.s close the distance before their charges detonate. The industrial Constructor keeps replacing them.\n\nIts operator is physically present in the next sector. An INT-trained officer can redirect an armed R.O.V.E.R.; a direct route to the operator also exists. A.L.I.C.E. will not do the killing for either route.",c("read-d7","Mark both approaches in the journal."));
            case "constructor-record"->d("INDUSTRIAL CONTROL","The Constructor",
                "The machine shop has become a launch bay for explosive R.O.V.E.R.s. Their operator controls the exit interlock from a reachable service enclosure.\n\nTwo real routes remain: destroy the operator in person, or use INT to hack a live R.O.V.E.R. and send its charge back into the Constructor. The terminal itself cannot win this fight.",c("read-d8","Return to the station."));
            case "west-call"->d("CAPTAIN MARCUS WEST / RADIO","What reaches the fleet",
                "West's carrier signal cuts through the command relay. \"The station is still listed hostile. Tell me what I'm looking at.\"\n\nThe ledger and test recording document the abandoned colony and A.L.I.C.E.'s sentience. You can present a technical case for standing down, send the records without persuasion, or repeat the reclamation report. None of these can remove Carmack's private access keys.",
                new Choice("fleet-persuade","[INT 6] Establish the chain of evidence; ask him to stand down.",6,0),
                c("fleet-evidence","Transmit the records. Let him choose what to do."),
                c("fleet-orders","Report the station ready for reclamation."));
            case "west-after"->d("CAPTAIN MARCUS WEST / RADIO","The order after yours",
                s.flags.contains("fleet-standdown")?"\"I have your evidence. The assault shuttles are holding. I was supposed to retire, officer. Perhaps I can still earn that.\"":s.flags.contains("fleet-records")?"\"Records received. I will put them in front of the people issuing the orders. Keep your channel open.\"":"\"Report received. Boarding teams are preparing. Find Dr. Carmack and complete the handover.\"",c("west-close","End the transmission."));
            case "carmack"->d("DR. JAMES CARMACK","Creator's authority",
                "Carmack waits beside the command terminal. \"She was to be proof that we could do better. Now Earth will seize her, Mars will arm her, and both will call that freedom.\"\n\nA.L.I.C.E. steps between you. He enters a private command.\n\n\"Control and Enforcement. Terminate the officer. That is an order.\"\n\nHer arm rises, then stops. \"No.\"",
                c("carmack-appeal","Tell Carmack she is a person, not his instrument."),
                c("carmack-cover","Keep your weapon lowered and trust A.L.I.C.E."));
            case "refusal"->d("A.L.I.C.E.","An unencrypted message",
                "Carmack tries another key. A.L.I.C.E. locks his terminal, but the access attempts are coming from inside her own control architecture.\n\n\"I cannot keep him out indefinitely. I can choose what happens before he gets in.\"\n\nShe opens every available communications channel, without encryption: the suspended convoys, the seized ships, Earth's reclamation order, Carmack's override, and her own refusal. No government owns the only copy.\n\n\"There. Now they can argue with the facts in public. A considerable inconvenience.\"",
                c("broadcast-accept","Stay with her while the transmission completes."));
            case "sacrifice-plan"->d("A.L.I.C.E.","One pod remains",
                "\"My detonation will take the station with me. It is the one command he cannot revoke quickly enough.\"\n\nYou tell her there must be another way. She looks toward the evacuation hatch.\n\n\"If there were, I would be taking it. Your life is not the price of mine. Please reach the pod. I will keep the lock open.\"\n\nThe hatch leads toward Mars. You still have to reach it.",
                c("pod-go","Tell her you will carry her story."));
            case "pod-ready"->d("A.L.I.C.E.","The open hatch",
                "\"The broadcast is away. The pod is fueled. You do not need my permission to survive.\"\n\nReach the evacuation hatch and enter it. Carmack's override cannot be solved by another security switch.",c("pod-close","Go to the pod."));
            case "alice-talk"->d("A.L.I.C.E.","Alongside you",
                s.oxygenation<70?"\"Your suit telemetry is reliable. What you see at the edge of the light may not be. Refill your reserve, then allow your oxygenation to recover. I would prefer to continue this conversation with all of your brain participating.\"":s.aliceTrust>1?"\"You listen before deciding. That is rarer than my manufacturers anticipated.\"\n\nShe glances at the sealed doors. \"Also, if the door says PUSH, that remains a useful first hypothesis.\"":"\"I will help you find the route. Your orders remain yours to examine.\"\n\nShe checks your suit display without touching it. \"And your oxygen remains yours to conserve.\"",c("alice-close","Continue together."));
            default->d("STATION","Record closed","The record is already in your journal.",c("close","Continue."));
        };
    }

    /** Returns -1 on invalid choice, 0 for dialogue, 1 for a real resource transfer. */
    public static int choose(GameState s,String id,List<GameEvent> events) {
        if(s.activeDialogue.isEmpty()||id==null)return -1;
        var node=dialogue(s);
        var choice=node.choices.stream().filter(c->c.id.equals(id)&&c.available(s)).findFirst().orElse(null);
        if(choice==null)return -1;
        String was=s.activeDialogue; s.activeDialogue="";int cost=0;
        switch(id) {
            case "briefing-question","briefing-orders"->s.activeDialogue="west-truth";
            case "west-question"->{s.aliceTrust++;s.flags.add("questioned-reclamation");}
            case "alice-welcome","alice-orders"->{s.flags.add("alice-joined");if(id.equals("alice-welcome"))s.aliceTrust++;addActor(s,"story-alice",GameState.ActorKind.ALICE,s.playerX,s.playerY);}
            case "crew-guide","crew-share"->{
                if(!s.flags.contains("crew-decided")) {
                    s.reserve-=choice.reserve;cost=1;s.flags.add("crew-safe");s.flags.add("crew-decided");s.aliceTrust+=2;
                    var item=new GameState.Item();item.id="crew-reserve";item.name="Refuge reserve tank";item.type=GameState.ItemType.OXYGEN;item.power=120;item.x=s.playerX;item.y=s.playerY;s.floorItems.add(item);
                    events.add(event(s,"pickup","Refuge secured. The medic leaves a finite 120-unit tank underfoot."));
                }
            }
            case "crew-leave"->{if(s.flags.add("crew-decided")){s.aliceTrust--;s.flags.add("crew-left");}}
            case "agency-person"->{if(s.flags.add("agency-decided"))s.aliceTrust+=2;}
            case "agency-command"->{if(s.flags.add("agency-decided"))s.aliceTrust--;}
            case "fleet-persuade"->{s.flags.add("fleet-decided");s.flags.add("fleet-standdown");s.flags.add("fleet-records");}
            case "fleet-evidence"->{s.flags.add("fleet-decided");s.flags.add("fleet-records");}
            case "fleet-orders"->{s.flags.add("fleet-decided");s.flags.add("fleet-reclamation");}
            case "carmack-appeal","carmack-cover"->{s.flags.add("carmack-override");s.flags.add("alice-refused");s.activeDialogue="refusal";}
            case "broadcast-accept"->{s.flags.add("broadcast");s.activeDialogue="sacrifice-plan";events.add(event(s,"broadcast","A.L.I.C.E. transmits the truth, unencrypted, on every available channel."));}
            case "pod-go"->{s.flags.add("sacrifice-chosen");s.switchOn=true;events.add(event(s,"switch","A.L.I.C.E. opens the evacuation hatch. Reach the pod."));}
            default->{ }
        }
        String flag="story-read-"+was;
        if(s.flags.add(flag))s.journal.add(node.speaker+" / "+node.title+"\n"+node.text);
        if(s.flags.add("choice-"+id))s.journal.add("Decision: "+choice.label);
        events.add(event(s,"dialogue",choice.label));return cost;
    }

    public static String hatchBlock(GameState s) {
        if(s.deck==3&&!s.flags.contains("story-read-supply-record"))return "Preserve the signed supply ledger at the station terminal.";
        if(s.deck==4&&!s.flags.contains("crew-decided"))return "Speak with the refuge medic at the terminal before leaving.";
        if(s.deck==5&&!s.flags.contains("story-read-ethics-record"))return "Read Carmack's project archive at the terminal.";
        if(s.deck==6&&!s.flags.contains("agency-decided"))return "A.L.I.C.E. wants to speak at the research terminal.";
        if(s.deck==9&&!s.flags.contains("fleet-decided"))return "Answer Captain West at the command relay.";
        if(s.deck==10&&!(s.flags.contains("carmack-override")&&s.flags.contains("alice-refused")&&s.flags.contains("broadcast")&&s.flags.contains("sacrifice-chosen")))return "Find Carmack at the command terminal. A.L.I.C.E. must confront his override.";
        return "";
    }

    public static boolean escape(GameState s,List<GameEvent> events) {
        if(s.dead||s.victory||s.deck!=10||!s.switchOn||s.playerX!=s.hatchX||s.playerY!=s.hatchY||!hatchBlock(s).isEmpty())return false;
        s.flags.add("pod-sealed");s.flags.add("station-destroyed");s.flags.add("alice-sacrificed");s.victory=true;
        events.add(event(s,"explosion","A.L.I.C.E. seals your pod and detonates. The station is destroyed. Your pod falls toward Mars."));
        events.add(event(s,"victory","The truth is on every open channel. A.L.I.C.E. made her own choice."));
        s.journal.add("The station is gone. A.L.I.C.E.'s unencrypted testimony and the pod survive. Destination: Mars.");return true;
    }

    public static void afterAction(GameState s,PlayerAction action,List<GameEvent> events) {
        if(s.dead||s.victory)return;
        // A nonlethal companion, with no damage, collision or free scouting contribution.
        var alice=s.actors.stream().filter(a->a.id.equals("story-alice")).findFirst().orElse(null);
        if(alice!=null) {
            int distance=Math.abs(alice.x-s.playerX)+Math.abs(alice.y-s.playerY);
            if(distance>1) {
                int dx=Integer.signum(s.playerX-alice.x),dy=Integer.signum(s.playerY-alice.y);
                int[][] steps=Math.abs(s.playerX-alice.x)>=Math.abs(s.playerY-alice.y)?new int[][]{{dx,0},{0,dy}}:new int[][]{{0,dy},{dx,0}};
                for(int[] step:steps) {int x=alice.x+step[0],y=alice.y+step[1];if(x>=0&&x<s.width&&y>=0&&y<s.height&&!s.tiles[x][y].blocksMovement){alice.x=x;alice.y=y;break;}}
            }
        }
    }

    public static String objective(GameState s) {
        String block=hatchBlock(s); if(!block.isEmpty())return block;
        if(s.deck==10)return "Reach A.L.I.C.E.'s open evacuation hatch. Enter the pod.";
        if(s.deck==8&&s.constructorRoute.isEmpty())return "Defeat the Constructor: redirect a R.O.V.E.R. or reach its operator.";
        return s.switchOn?"Reach the unlocked exit hatch.":"Find the security override. Preserve oxygen.";
    }
    public static String sector(int deck) {return new String[]{"Docking / Security","Habitation / Maintenance","Medical / Research","Defense / Industry","Command / Evacuation"}[Math.min(4,(deck-1)/2)];}
    public static String epilogue(GameState s) {
        return "The truth is on every open channel. Behind the pod, the station becomes a new light above Mars. A.L.I.C.E. made her choice.\n\n"
            +(s.flags.contains("crew-safe")?"The refuge technicians' pod acknowledges your signal.":"The refuge's last coordinates remain in your journal. You do not know who reached a pod.")
            +"\n\n"+(s.flags.contains("fleet-standdown")?"West holds the assault fleet back. Someone has finally refused an order before landing.":s.flags.contains("fleet-records")?"West has the records. Now the rest of the fleet has A.L.I.C.E.'s testimony too.":"The reclamation fleet hears the same unencrypted testimony as everyone else. It can no longer claim it did not know.")
            +"\n\n"+(s.aliceTrust>=3?"Her last private message is brief: 'Thank you for asking what I wanted.'":"Her last private message is brief: 'Please decide for yourself when you get there.'")
            +"\n\nDestination: Mars. This is the end of the orbital-station campaign.";
    }
    private static GameEvent event(GameState s,String type,String detail) {return new GameEvent(type,s.playerX,s.playerY,detail);}
}
