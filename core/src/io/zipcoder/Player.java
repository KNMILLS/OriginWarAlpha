package io.zipcoder;

import squidpony.squidmath.Coord;

public class Player extends Creature{

    private static Player instance;
    private int turns;

    private Player(){
        setId(Math.random());
        this.turns = 0;
        this.setHealth(100);
        Player.instance = this;
    }
    public static Player getPlayer(){
        if(Player.instance == null){
            return new Player();
        }
        return instance;
    }

    @Override
    public void setPosition(Coord position){
        super.setPosition(position);
        this.incrementTurn();

    }

    public int getTurns() {
        return turns;
    }

    public void setTurns(int turns) {
        this.turns = turns;
    }

    public void incrementTurn(){
        this.turns++;
        if(this.turns % 5 == 0){
            this.setHealth(this.getHealth() - 1);
        }
    }

}
