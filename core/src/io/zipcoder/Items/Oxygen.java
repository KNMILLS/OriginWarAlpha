package io.zipcoder.Items;

import squidpony.squidmath.Coord;

public class Oxygen extends Item {

    public Oxygen() {
    }

    public Oxygen(Coord position) {
        super(position);
        this.setSymbol('%');
    }
}
