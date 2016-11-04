package io.zipcoder;

import squidpony.squidmath.Coord;

/**
 * Created by evanhitchings on 11/4/16.
 */
public class Food extends Item {

    public Food() {
    }

    public Food(Coord position) {
        super(position);
        this.setSymbol('%');
    }
}
