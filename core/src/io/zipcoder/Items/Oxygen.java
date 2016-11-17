package io.zipcoder.Items;

import squidpony.squidmath.Coord;

import java.util.Objects;

public class Oxygen extends Item implements Comparable{

    private int remaining;

    public Oxygen() {
    }

    public Oxygen(Coord position) {
        super(position);
        this.setSymbol('%');
        this.remaining = 10;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = remaining;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof Oxygen){
            if(this.remaining > ((Oxygen) o).remaining){
                return 1;
            }
            if(this.remaining < ((Oxygen) o).remaining){
                return -1;
            }
            return 0;
        }
        return 0;
    }
}
