package com.comp460.experimental.battle;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Created by matthewhammond on 1/29/17.
 */
public interface IRenderable {

    TextureRegion getSprite();
    float getScreenX();
    float getScreenY();

    default void render(SpriteBatch batch) {
        if (batch.isDrawing()) {
            batch.draw(getSprite(), getScreenX(), getScreenY());
        }
    }
}