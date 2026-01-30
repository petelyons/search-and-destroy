package com.developingstorm.games.sad.fx.sprites;

import javafx.scene.canvas.GraphicsContext;

/**
 * Base class for JavaFX sprites - drawable overlays on the map.
 * Simplified from Swing version - no animation frames, just draw.
 */
public abstract class FxSprite {

    protected int zPos;

    protected FxSprite() {
        this.zPos = 0;
    }

    /**
     * Set the z-order position for layering sprites.
     */
    public void setZPos(int pos) {
        this.zPos = pos;
    }

    public int getZPos() {
        return zPos;
    }

    /**
     * Draw the sprite on the canvas.
     */
    public abstract void draw(GraphicsContext gc);

    /**
     * Check if the sprite is done and should be removed.
     * Default is false (sprite stays until explicitly removed).
     */
    public boolean done() {
        return false;
    }
}
