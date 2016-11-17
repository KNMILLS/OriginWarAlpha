package io.zipcoder.Entities;

import io.zipcoder.Items.Oxygen;
import squidpony.squidmath.Coord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player extends Creature{

    private static Player instance;
    private int turns;
    private Boolean alive;
    private int hpColor;
    private ArrayList<Oxygen> oxygenStash;
    private boolean usingOxygen;
    private int encumberance;


    private Player(){
        super();
        this.turns = 0;
        this.setHealth(100);
        this.setDamage(4);
        this.alive = true;
        Player.instance = this;
        this.oxygenStash = new ArrayList<>(4);
        this.usingOxygen = false;
        this.encumberance = 0;
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
        //this.incrementTurn();

    }

    public int getEncumberance() {
        return encumberance;
    }

    public void setEncumberance(){
        this.encumberance = Math.max(5, this.getOxygenStash().size());
    }

    public boolean isUsingOxygen() {
        return usingOxygen;
    }

    public void setUsingOxygen(boolean usingOxygen) {
        this.usingOxygen = usingOxygen;
    }



    public int getTurns() {
        return turns;
    }

    public void setTurns(int turns) {
        this.turns = turns;
    }

    public void incrementTurn(){
        this.turns++;
        breathe();


    }

    public void breathe(){
        if(getOxygenStash().size() < 1){
            usingOxygen = false;
        }
        setEncumberance();
        if(this.turns % encumberance == 0){
            if(usingOxygen){
                Oxygen currentOxygen = this.getOxygenStash().get(0);
                currentOxygen.setRemaining(currentOxygen.getRemaining() - 1);
                if(currentOxygen.getRemaining() < 1){
                    getOxygenStash().remove(0);
                }
                if(this.turns % 10 == 0){
                    getPlayer().setHealth(getHealth() + 1);
                }

            } else {
                getPlayer().setHealth(getHealth() - 1);
            }
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

    public ArrayList<Oxygen> getOxygenStash() {
        Collections.sort(oxygenStash);
        return oxygenStash;
    }

    public void useOxygen(){
        if(!usingOxygen && getPlayer().getHealth() > 10){
            usingOxygen = true;
        } else {
            usingOxygen = false;
        }
    }

    public void pickUpOxygen(Oxygen oxygen){
        if(oxygenStash.size() <= 5){
            oxygenStash.add(oxygen);
        }

    }

    public Oxygen dropOxygen(){
        Oxygen oxygen = getOxygenStash().remove(0);
        oxygen.setPosition(getPlayer().getPosition());
        getPlayer().setEncumberance();
        return oxygen;
    }

    @Override
    public void setHealth(int health){
        if(health >= 125){
            super.setHealth(125);
        } else {
            super.setHealth(health);
        }
    }

    public void resetPlayer(){
        super.setHealth(100);
        getPlayer().setAlive(true);
        getPlayer().setTurns(-1);
        getPlayer().oxygenStash.clear();
        getPlayer().usingOxygen = false;
        getPlayer().setEncumberance();
    }
}
