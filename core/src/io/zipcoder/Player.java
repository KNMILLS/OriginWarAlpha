package io.zipcoder;

import squidpony.squidmath.Coord;

public class Player extends Creature{

    private static Player instance;

    private Player(){
        setId(Math.random());
        Player.instance = this;
    }
    public static Player getPlayer(){
        if(Player.instance == null){
            return new Player();
        }
        return instance;
    }


}
