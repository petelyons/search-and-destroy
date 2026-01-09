package com.developingstorm.games.hexboard.sprites;

import java.awt.Graphics2D;
import java.awt.Image;

/**
 * 
 */
public abstract class Sprite {

  protected int current;
  protected boolean repeat;
  protected int frames;
  protected int rate;
  protected int zPos;
  protected long lastUpdate;

  protected Sprite() {
    current = 0;
    frames = 0;
    repeat = false;
    lastUpdate = 0;
    rate = 100;
    zPos = 0;

  }

  /**
   * @return Returns the current.
   */
  public int getCurrent() {

    return current;
  }

  /**
   * @return Returns the lastUpdate.
   */
  public long getLastUpdate() {

    return lastUpdate;
  }

  /**
   * @param frames
   *          The frames to set.
   */
  public void setFrames(int frames) {

    this.frames = frames;
  }

  /**
   * @param rate
   *          The rate to set.
   */
  public void setRate(int rate) {

    this.rate = rate;
  }

  /**
   * @param repeat
   *          The repeat to set.
   */
  public void setRepeat(boolean repeat) {

    this.repeat = repeat;
  }

  /**
   * @param pos
   *          The zPos to set.
   */
  public void setZPos(int pos) {

    zPos = pos;
  }

  public void draw(long time, Image[] images, Graphics2D g) {
    int old = current;

    if (time - this.lastUpdate > this.rate) {
      this.current++;
      lastUpdate = time;
    }

    if (this.current >= this.frames && this.repeat) {
      current = 0;
    }

    if (this.current >= this.frames || g == null) {
      return;
    }

    if (this.current != old) {
      handleFrameChange(old, this.current);
    }
    handleDraw(time, images, g);
  }

  public boolean check(long time) {
    return (time - this.lastUpdate > this.rate);
  }

  protected abstract void handleFrameChange(int old, int current);

  protected abstract void handleDraw(long time, Image[] images, Graphics2D g);

  public int getZPos() {
    return zPos;
  }

  public boolean done() {
    return this.current > frames;
  }
}
