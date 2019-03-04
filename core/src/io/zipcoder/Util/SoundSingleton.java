package io.zipcoder.Util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import java.util.ArrayList;
import java.util.List;

public class SoundSingleton {
    private Sound backgroundMusic;
    private Sound stairSound;
    private Sound dispenseOxygenSound;
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
    private Sound rapidHR;
    private Sound metalStepSound;
    private Sound playerDeathSound;
    private Sound romeroSound;
    private Sound schizophrenicVoices;
    private Sound creditsMusic;
    private Sound pickupOxygenSound;
    private static SoundSingleton instance;
    private List<Sound> allSounds;

    private SoundSingleton(){
        backgroundMusic = Gdx.audio.newSound(Gdx.files.internal("core/assets/21_Derelict_Freighter.mp3"));
        stairSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/nextLevel.wav"));
        dispenseOxygenSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/airtank.wav"));
        whatAreYouDoingHereSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/whatAreYouDoingHere.wav"));
        waterStep = Gdx.audio.newSound(Gdx.files.internal("core/assets/splash.wav"));
        doorSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/door.wav"));
        footStep = Gdx.audio.newSound(Gdx.files.internal("core/assets/footstep.wav"));
        breathSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/breathing.wav"));
        heartbeatSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/heartbeat.mp3"));
        evilLaugh = Gdx.audio.newSound(Gdx.files.internal("core/assets/evilLaugh.wav"));
        monsterSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/monster.wav"));
        cardLockSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/cardlock.wav"));
        laboredBreathing = Gdx.audio.newSound(Gdx.files.internal("core/assets/labored.wav"));
        rapidHR = Gdx.audio.newSound(Gdx.files.internal("core/assets/rapidHR.mp3"));
        metalStepSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/metalStep.wav"));
        playerDeathSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/playerdeath.mp3"));
        romeroSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/romero.mp3"));
        schizophrenicVoices = Gdx.audio.newSound(Gdx.files.internal("core/assets/schizophrenia.mp3"));
        creditsMusic = Gdx.audio.newSound(Gdx.files.internal("core/assets/credits.wav"));
        pickupOxygenSound = Gdx.audio.newSound(Gdx.files.internal("core/assets/pickupOxygen.wav"));
        allSounds = new ArrayList<>();
        setAllSounds();
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

    public Sound getDispenseOxygenSound() {
        return dispenseOxygenSound;
    }

    public void setDispenseOxygenSound(Sound dispenseOxygenSound) {
        this.dispenseOxygenSound = dispenseOxygenSound;
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

    public Sound getMetalStepSound() {
        return metalStepSound;
    }

    public void setMetalStepSound(Sound metalStepSound) {
        this.metalStepSound = metalStepSound;
    }

    public Sound getPlayerDeathSound() {
        return playerDeathSound;
    }

    public void setPlayerDeathSound(Sound playerDeathSound) {
        this.playerDeathSound = playerDeathSound;
    }

    public Sound getRomeroSound() {
        return romeroSound;
    }

    public void setRomeroSound(Sound romeroSound) {
        this.romeroSound = romeroSound;
    }

    public Sound getSchizophrenicVoices() {
        return schizophrenicVoices;
    }

    public void setSchizophrenicVoices(Sound schizophrenicVoices) {
        this.schizophrenicVoices = schizophrenicVoices;
    }

    public List<Sound> setAllSounds() {
        allSounds.add(backgroundMusic);
        allSounds.add(breathSound);
        allSounds.add(heartbeatSound);
        allSounds.add(rapidHR);
        allSounds.add(laboredBreathing);
        allSounds.add(schizophrenicVoices);
        return allSounds;
    }

    public List<Sound> getAllSounds() {
        return allSounds;
    }

    public void setAllSounds(List<Sound> allSounds) {
        this.allSounds = allSounds;
    }

    public Sound getCreditsMusic() {
        return creditsMusic;
    }

    public void setCreditsMusic(Sound creditsMusic) {
        this.creditsMusic = creditsMusic;
    }

    public Sound getRapidHR() {
        return rapidHR;
    }

    public void setRapidHR(Sound rapidHR) {
        this.rapidHR = rapidHR;
    }

    public Sound getPickupOxygenSound() {
        return pickupOxygenSound;
    }
    public void setPickupOxygenSound(Sound pickupOxygenSound) {
        this.pickupOxygenSound = pickupOxygenSound;
    }
}
