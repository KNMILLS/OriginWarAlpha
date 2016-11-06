package io.zipcoder;

import squidpony.squidmath.Coord;

/**
 * Created by evanhitchings on 11/4/16.
 */
public class Item {
    private Coord position;
    private char symbol;

    public Item() {
    }

    public Item(Coord position) {
        this.position = position;
    }

    public Coord getPosition() {
        return position;
    }

    public void setPosition(Coord position) {
        this.position = position;
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }
}
