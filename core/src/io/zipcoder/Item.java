package io.zipcoder;

import squidpony.squidmath.Coord;

/**
 * Created by evanhitchings on 11/4/16.
 */
public class Item {
    private Coord postion;
    private char symbol;

    public Item() {
    }

    public Item(Coord postion) {
        this.postion = postion;
    }

    public Coord getPostion() {
        return postion;
    }

    public void setPostion(Coord postion) {
        this.postion = postion;
    }

    public char getSymbol() {
        return symbol;
    }

    public void setSymbol(char symbol) {
        this.symbol = symbol;
    }
}
