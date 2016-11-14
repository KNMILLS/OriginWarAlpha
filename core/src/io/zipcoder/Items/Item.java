package io.zipcoder.Items;

import squidpony.squidmath.Coord;

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
