package com.developingstorm.games.hexboard.sprites;

import java.awt.Graphics2D;
import java.awt.Image;

/**
 * 
 */
public abstract class Sprite {

  protected int _current;
  protected boolean _repeat;
  protected int _frames;
  protected int _rate;
  protected int _zPos;
  protected long _lastUpdate;

  protected Sprite() {
    _current = 0;
    _frames = 0;
    _repeat = false;
    _lastUpdate = 0;
    _rate = 100;
    _zPos = 0;

  }

  /**
   * @return Returns the current.
   */
  public int getCurrent() {

    return _current;
  }

  /**
   * @return Returns the lastUpdate.
   */
  public long getLastUpdate() {

    return _lastUpdate;
  }

  /**
   * @param frames
   *          The frames to set.
   */
  public void setFrames(int frames) {

    _frames = frames;
  }

  /**
   * @param rate
   *          The rate to set.
   */
  public void setRate(int rate) {

    _rate = rate;
  }

  /**
   * @param repeat
   *          The repeat to set.
   */
  public void setRepeat(boolean repeat) {

    _repeat = repeat;
  }

  /**
   * @param pos
   *          The zPos to set.
   */
  public void setZPos(int pos) {

    _zPos = pos;
  }

  public void draw(long time, Image[] images, Graphics2D g) {
    int old = _current;

    if (time - _lastUpdate > _rate) {
      _current++;
      _lastUpdate = time;
    }

    if (_current >= _frames && _repeat) {
      _current = 0;
    }

    if (_current >= _frames || g == null) {
      return;
    }

    if (_current != old) {
      handleFrameChange(old, _current);
    }
    handleDraw(time, images, g);
  }

  public boolean check(long time) {
    return (time - _lastUpdate > _rate);
  }

  protected abstract void handleFrameChange(int old, int current);

  protected abstract void handleDraw(long time, Image[] images, Graphics2D g);

  public int getZPos() {
    return _zPos;
  }

  public boolean done() {
    return _current > _frames;
  }
}
