package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.PlayerAction;
import java.util.*;

/** Logical pack slots over stable saved item identities. Grouping never writes the world. */
public final class InventoryStacks {
    private InventoryStacks() {}
    public record Key(GameState.ItemType type,String name,int power) {}
    public static Key key(GameState.Item item) {
        int power=switch(item.type){case OXYGEN,CELL->item.power;default->0;};
        return new Key(item.type,item.name,power);
    }
    public static int count(List<GameState.Item> inventory) {
        var keys=new HashSet<Key>();for(var item:inventory)keys.add(key(item));return keys.size();
    }
    public static List<Stack> group(List<GameState.Item> inventory) {
        var groups=new LinkedHashMap<Key,List<GameState.Item>>();
        for(var item:inventory)groups.computeIfAbsent(key(item),ignored->new ArrayList<>()).add(item);
        var rows=new ArrayList<Stack>();
        for(var entry:groups.entrySet()){
            int quantity=0;for(var item:entry.getValue())quantity=Math.addExact(quantity,item.quantity);
            rows.add(new Stack(entry.getKey(),entry.getValue(),quantity));
        }
        return List.copyOf(rows);
    }
    public static int selectionAfter(List<Stack> rows,Key previous,int fallback) {
        for(int i=0;i<rows.size();i++)if(rows.get(i).key().equals(previous))return i;
        return Math.max(0,Math.min(fallback,rows.size()-1));
    }
    public static boolean equipped(GameState.Item item,Set<String> flags) {
        return flags.contains("equipped-weapon:"+item.id)||flags.contains("equipped-armor:"+item.id);
    }
    public record Stack(Key key,List<GameState.Item> items,int quantity) {
        public Stack { items=items.stream().map(GameState.Item::copy).toList(); }
        public GameState.Item representative(){return items.getFirst().copy();}
        public boolean equipped(Set<String> flags){return items.stream().anyMatch(i->InventoryStacks.equipped(i,flags));}
        public boolean dropsEquipped(Set<String> flags){return quantity==1&&equipped(flags);}
        public String actionId(PlayerAction.Type action,Set<String> flags) {
            if(action==PlayerAction.Type.DROP)
                for(var item:items)if(item.quantity>1||!InventoryStacks.equipped(item,flags))return item.id;
            if(action==PlayerAction.Type.EQUIP)
                for(var item:items)if(InventoryStacks.equipped(item,flags))return item.id;
            return items.getFirst().id;
        }
    }
}
