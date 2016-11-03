package io.zipcoder;


import squidpony.squidmath.Coord;

/**
 * Created by kenragonese on 11/2/16.
 */
public abstract class Creature{
    private double id;
    private Coord position;

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
