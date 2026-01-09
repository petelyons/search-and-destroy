package com.developingstorm.games.hexboard.sprites;

import com.developingstorm.games.hexboard.HexCanvas;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**

 *
 */
public class SpriteEngine {

    private Image[] images;
    private List<Sprite>[] sprites;
    private HexCanvas canvas;
    private long time;
    private Graphics2D g2;
    private int numZ;
    private AnimationThread thread;

    private class AnimationThread extends Thread {

        private boolean stop;
        //private HexCanvas canvas;
        private long frameRate;

        AnimationThread() {
            setDaemon(true);
            stop = false;
            //      /canvas = c;
            frameRate = 100;
        }

        public void requestStop() {
            stop = true;
            try {
                join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (stop == false) {
                boolean repaint;

                repaint = false;
                try {
                    Thread.sleep(this.frameRate);
                    long time = System.currentTimeMillis();

                    for (int z = 0; z < numZ; z++) {
                        if (!SpriteEngine.this.sprites[z].isEmpty()) {
                            Iterator<Sprite> itr =
                                SpriteEngine.this.sprites[z].iterator();
                            while (itr.hasNext()) {
                                Sprite s = (Sprite) itr.next();
                                if (s.check(time)) {
                                    repaint = true;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (repaint) {
                    SpriteEngine.this.canvas.repaint();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public SpriteEngine(HexCanvas canvas, Image[] images, int numZ) {
        images = images;
        this.canvas = canvas;
        time = 0;
        g2 = null;
        this.numZ = numZ;
        sprites = (List<Sprite>[]) new List<?>[this.numZ];
        for (int i = 0; i < numZ; i++) {
            this.sprites[i] = new ArrayList<Sprite>();
        }

        thread = null;
    }

    public void stop() {
        if (this.thread != null) {
            this.thread.requestStop();
            thread = null;
        }
    }

    public void start() {
        if (this.thread != null) {
            stop();
        }
        thread = new AnimationThread();
        this.thread.start();
    }

    public synchronized void add(Sprite s) {
        this.sprites[s.getZPos()].add(s);
    }

    public synchronized void remove(Sprite s) {
        this.sprites[s.getZPos()].remove(s);
    }

    public boolean contains(Sprite s) {
        return this.sprites[s.getZPos()].contains(s);
    }

    public synchronized void beginDraw(Graphics2D g) {
        time = System.currentTimeMillis();
        g2 = g;
    }

    public synchronized void endDraw() {
        time = 0;
        g2 = null;
    }

    public synchronized void draw(int z) {
        if (time == 0) {
            throw new IllegalStateException(
                "beginDraw() must be called before draw()"
            );
        }

        if (this.sprites[z].isEmpty()) {
            return;
        }
        List<Sprite> newList = new ArrayList<Sprite>();
        Iterator<Sprite> itr = this.sprites[z].iterator();
        while (itr.hasNext()) {
            Sprite s = itr.next();
            s.draw(this.time, this.images, this.g2);
            if (!s.done()) {
                newList.add(s);
            }
        }
        this.sprites[z] = newList;
    }
}
