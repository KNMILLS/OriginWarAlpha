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
    private static SoundSingleton instance;

    private SoundSingleton(){
        backgroundMusic = Gdx.audio.newSound(Gdx.files.internal("21_Derelict_Freighter.mp3"));
        stairSound = Gdx.audio.newSound(Gdx.files.internal("door.wav"));
        oxygenSound = Gdx.audio.newSound(Gdx.files.internal("cha-ching.wav"));
        keySound = Gdx.audio.newSound(Gdx.files.internal("ohyeah.wav"));
        waterStep = Gdx.audio.newSound(Gdx.files.internal("splash.wav"));
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
}
