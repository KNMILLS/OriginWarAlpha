package io.zipcoder;

import squidpony.squidmath.Coord;

public class Player extends Creature{

    private static Player instance;
    private int turns;
    private int health;


    private Player(){
        setId(Math.random());
        this.turns = 0;
        this.health = 100;
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

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void incrementTurn(){
        this.turns++;
        if(this.turns % 5 == 0){
            this.health --;
        }
    }

}
