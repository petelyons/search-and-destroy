package com.developingstorm.games.sad.ui.sprites;

import com.developingstorm.games.hexboard.sprites.ImageSprite;
import com.developingstorm.games.sad.ui.GameIcons;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;

/**

 *
 */
public class ExplosionSprite extends ImageSprite {

    protected int[] imageSelectors;
    private Point point;

    public ExplosionSprite() {
        int[] imgs = new int[5];
        imgs[0] = GameIcons.iEXPLOSION0;
        imgs[1] = GameIcons.iEXPLOSION1;
        imgs[2] = GameIcons.iEXPLOSION2;
        imgs[3] = GameIcons.iEXPLOSION3;
        imgs[4] = GameIcons.iEXPLOSION4;
        setImageSelectors(imgs);
    }

    protected void handleDraw(long time, Image[] images, Graphics2D g) {
        // Guard against null images or imageSelectors
        if (
            images == null || this.imageSelectors == null || this.point == null
        ) {
            return;
        }

        // Guard against invalid index
        if (this.current < 0 || this.current >= this.imageSelectors.length) {
            return;
        }

        int imageIndex = this.imageSelectors[this.current];
        if (imageIndex < 0 || imageIndex >= images.length) {
            return;
        }

        g.drawImage(images[imageIndex], this.point.x, this.point.y, null);
    }

    /**
     * @param imageSelectors
     *          The imageSelectors to set.
     */
    public void setImageSelectors(int[] imageSelectors) {
        this.imageSelectors = imageSelectors;
        setFrames(this.imageSelectors.length);
    }

    public void setPoint(Point p) {
        point = p;
    }
}
