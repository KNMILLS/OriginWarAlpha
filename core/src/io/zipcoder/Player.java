package io.zipcoder;

import squidpony.squidmath.Coord;

public class Player extends Creature{

    private Coord position;
    private static Player instance;
    private double id;


    private Player(){
        id = Math.random();
        Player.instance = this;
    }

    public static Player getPlayer(){
        if(Player.instance == null){
            return new Player();
        }
        return instance;
    }

    public double getId() {
        return id;
    }

    public void setId(double id) {
        this.id = id;
    }

    public Coord getPosition() {
        return position;
    }

    public void setPosition(Coord position) {
        this.position = position;
    }
}
