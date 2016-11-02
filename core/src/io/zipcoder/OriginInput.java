package io.zipcoder;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import squidpony.squidgrid.gui.gdx.SquidInput;
import squidpony.squidgrid.gui.gdx.SquidMouse;

/**
 * Created by evanhitchings on 11/2/16.
 */
public class OriginInput extends SquidInput{

    public OriginInput() {
        super();
    }

    public OriginInput(KeyHandler keyHandler, SquidMouse mouse, boolean ignoreInput) {
        super(keyHandler, mouse, ignoreInput);
    }

    public OriginInput(KeyHandler keyHandler, SquidMouse mouse) {
        super(keyHandler, mouse);
    }

    @Override
    public boolean keyTyped (char character) {

        if(ignoreInput) return false;
        boolean alt =  Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT),
                ctrl =  Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT),
                shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        char mods = 0;
        if(character != '\0') {
            queue.add(character);
            mods |= (alt) ? 1 : 0;
            mods |= (ctrl) ? 2 : 0;
            mods |= (shift) ? 4 : 0;
            queue.add(mods);
        }
        return false;
    }
}
