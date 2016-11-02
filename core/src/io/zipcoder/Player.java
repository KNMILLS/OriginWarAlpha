package io.zipcoder;

import squidpony.squidmath.Coord;

public class Player extends Creature{

    private Coord position;

    public Player(Coord position) {
        this.position = position;
    }
    public Player(){
    }

    public Coord getPosition() {
        return position;
    }

    public void setPosition(Coord position) {
        this.position = position;
    }
}
