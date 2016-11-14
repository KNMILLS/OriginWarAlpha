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


    private Player(){
        super();
        this.turns = 0;
        this.setHealth(100);
        this.setDamage(4);
        this.alive = true;
        Player.instance = this;
        this.oxygenStash = new ArrayList<>(2);
        this.usingOxygen = false;
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
        if(getOxygenStash().size() < 1){
            usingOxygen = false;
        }
        this.turns++;
        int encumbranceMod = 5;
        if(this.oxygenStash.size() > 2){
            encumbranceMod = Math.max(4, 4 - (oxygenStash.size() - 2));
        }
        if(this.turns % encumbranceMod == 0){
            if(usingOxygen){
                Oxygen currentOxygen = this.getOxygenStash().get(0);
                currentOxygen.setRemaining(currentOxygen.getRemaining() - 1);
                getPlayer().setHealth(getHealth() + 1);
                if(currentOxygen.getRemaining() < 1){
                    getOxygenStash().remove(0);
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
        if(!usingOxygen){
            usingOxygen = true;
        } else {
            usingOxygen = false;
        }
    }

    public void pickUpOxygen(Oxygen oxygen){
        oxygenStash.add(oxygen);
    }

    public Oxygen dropOxygen(){
        Oxygen oxygen = getOxygenStash().remove(0);
        return oxygen;
    }
}
