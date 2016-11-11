package io.zipcoder.Entities;

import squidpony.squidmath.Coord;

public class Player extends Creature{

    private static Player instance;
    private int turns;
    private Boolean alive;
    private int hpColor;

    private Player(){
        super();
        this.turns = 0;
        this.setHealth(100);
        this.setDamage(4);
        this.alive = true;
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
        if(this.getHealth() >= 125){
            this.turns++;
        }
        this.turns++;
        if(this.turns % 4 == 0){
            //this.setHealth(this.getHealth() - 20);
            this.setHealth(this.getHealth() - 1);
        }
    }

    public Boolean isAlive() {
        return alive;
    }

    public void setAlive(Boolean alive) {
        this.alive = alive;
    }

    public Boolean getAlive() {
        return alive;
    }

    public int getHpColor() {
        return hpColor;
    }

    public void setHpColor(int hpColor) {
        this.hpColor = hpColor;
    }
}
