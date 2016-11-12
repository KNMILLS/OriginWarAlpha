package io.zipcoder.Util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

/**
 * Created by evanhitchings on 11/11/16.
 */
public class SoundSingleton {
    private Sound backgroundMusic;
    private Sound stairSound;
    private Sound oxygenSound;
    private Sound keySound;
    private Sound waterStep;
    private Sound doorSound;
    private Sound footStep;
    private Sound breathSound;
    private Sound heartbeatSound;
    private Sound whatAreYouDoingHereSound;
    private Sound monsterSound;
    private Sound evilLaugh;
    private Sound cardLockSound;
    private Sound laboredBreathing;
    private static SoundSingleton instance;

    private SoundSingleton(){
        backgroundMusic = Gdx.audio.newSound(Gdx.files.internal("21_Derelict_Freighter.mp3"));
        stairSound = Gdx.audio.newSound(Gdx.files.internal("nextLevel.wav"));
        oxygenSound = Gdx.audio.newSound(Gdx.files.internal("airtank.wav"));
        whatAreYouDoingHereSound = Gdx.audio.newSound(Gdx.files.internal("whatAreYouDoingHere.wav"));
        waterStep = Gdx.audio.newSound(Gdx.files.internal("splash.wav"));
        doorSound = Gdx.audio.newSound(Gdx.files.internal("door.wav"));
        footStep = Gdx.audio.newSound(Gdx.files.internal("footstep.wav"));
        breathSound = Gdx.audio.newSound(Gdx.files.internal("breathing.wav"));
        heartbeatSound = Gdx.audio.newSound(Gdx.files.internal("heartbeat.mp3"));
        evilLaugh = Gdx.audio.newSound(Gdx.files.internal("evilLaugh.wav"));
        monsterSound = Gdx.audio.newSound(Gdx.files.internal("monster.wav"));
        cardLockSound = Gdx.audio.newSound(Gdx.files.internal("cardlock.wav"));
        laboredBreathing = Gdx.audio.newSound(Gdx.files.internal("labored.wav"));
        SoundSingleton.instance = this;
    }

    public static SoundSingleton getSoundSingleton(){
        if(SoundSingleton.instance == null){
            return new SoundSingleton();
        }
        return instance;
    }

    public Sound getBackgroundMusic() {
        return backgroundMusic;
    }

    public void setBackgroundMusic(Sound backgroundMusic) {
        this.backgroundMusic = backgroundMusic;
    }

    public Sound getStairSound() {
        return stairSound;
    }

    public void setStairSound(Sound stairSound) {
        this.stairSound = stairSound;
    }

    public Sound getOxygenSound() {
        return oxygenSound;
    }

    public void setOxygenSound(Sound oxygenSound) {
        this.oxygenSound = oxygenSound;
    }

    public Sound getKeySound() {
        return keySound;
    }

    public void setKeySound(Sound keySound) {
        this.keySound = keySound;
    }

    public Sound getWaterStep() {
        return waterStep;
    }

    public void setWaterStep(Sound waterStep) {
        this.waterStep = waterStep;
    }

    public Sound getDoorSound() {
        return doorSound;
    }

    public void setDoorSound(Sound doorSound) {
        this.doorSound = doorSound;
    }

//    public Sound getGreenStep() {
//        return greenStep;
//    }
//
//    public void setGreenStep(Sound greenStep) {
//        this.greenStep = greenStep;
//    }

    public Sound getFootStep() {
        return footStep;
    }

    public void setFootStep(Sound footStep) {
        this.footStep = footStep;
    }

    public Sound getBreathSound() {
        return breathSound;
    }

    public void setBreathSound(Sound breathSound) {
        this.breathSound = breathSound;
    }

    public Sound getHeartbeatSound() {
        return heartbeatSound;
    }

    public void setHeartbeatSound(Sound heartbeatSound) {
        this.heartbeatSound = heartbeatSound;
    }

    public Sound getWhatAreYouDoingHereSound() {
        return whatAreYouDoingHereSound;
    }

    public void setWhatAreYouDoingHereSound(Sound whatAreYouDoingHereSound) {
        this.whatAreYouDoingHereSound = whatAreYouDoingHereSound;
    }

    public Sound getMonsterSound() {
        return monsterSound;
    }

    public void setMonsterSound(Sound monsterSound) {
        this.monsterSound = monsterSound;
    }

    public Sound getEvilLaugh() {
        return evilLaugh;
    }

    public void setEvilLaugh(Sound evilLaugh) {
        this.evilLaugh = evilLaugh;
    }

    public Sound getCardLockSound() {
        return cardLockSound;
    }

    public void setCardLockSound(Sound cardLockSound) {
        this.cardLockSound = cardLockSound;
    }

    public Sound getLaboredBreathing() {
        return laboredBreathing;
    }

    public void setLaboredBreathing(Sound laboredBreathing) {
        this.laboredBreathing = laboredBreathing;
    }
}
