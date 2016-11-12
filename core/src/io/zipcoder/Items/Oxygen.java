package io.zipcoder.Items;

import squidpony.squidmath.Coord;

/**
 * Created by evanhitchings on 11/4/16.
 */
public class Oxygen extends Item {

    public Oxygen() {
    }

    public Oxygen(Coord position) {
        super(position);
        this.setSymbol('%');
    }
}
