package io.zipcoder.Entities;


import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidmath.Coord;

/**
 * Created by kenragonese on 11/2/16.
 */
public abstract class Creature{
    private double id;
    private Coord position;
    private int health;
    private int damage;
    private FOV fov;
    private double[][] fovMap, resMap, costArray;

    public Creature(int health, int damage) {
        this.health = health;
        this.damage = damage;

    }
    public Creature(){
        setId(Math.random());

    }

    public double getId() {
        return id;
    }

    public void setId(double id) {
        this.id = id;
    }

    public Coord getPosition() {
        return position;
    }

    public void setPosition(Coord position) {
        this.position = position;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public FOV getFov() {
        return fov;
    }

    public void setFov(FOV fov) {
        this.fov = fov;
    }

    public void initFOV(char[][] dungeon){
        this.fov = new FOV(FOV.RIPPLE_TIGHT);
        this.resMap = DungeonUtility.generateResistances(dungeon);
        shortGrass(dungeon);
        updateFOVMap();

    }


    public double[][] getFovMap() {
        return fovMap;
    }

    public void updateFOVMap(){
        fovMap = fov.calculateFOV(resMap, getPosition().getX(), getPosition().getY(), 8, Radius.CIRCLE);
    }

    public void setFovMap(double[][] fovMap) {
        this.fovMap = fovMap;
    }

    public double[][] getResMap() {
        return resMap;
    }

    public void setResMap(double[][] resMap) {
        this.resMap = resMap;
    }

    public double[][] getCostArray() {
        return costArray;
    }

    public void setCostArray(double[][] costArray) {
        this.costArray = costArray;
    }

    private void shortGrass(char[][] dungeon){
        for (int i = 0; i < resMap.length; i++) {
            for (int j = 0; j < resMap[i].length; j++) {
                if (dungeon[i][j] == '"') {
                    resMap[i][j] = 0.0;
                }
            }
        }
    }
}
