package com.developingstorm.games.hexboard.sprites;

import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.developingstorm.games.hexboard.HexCanvas;

/**

 * 
 */
public class SpriteEngine {

  private Image[] _images;
  private List<Sprite>[] _sprites;
  private HexCanvas _canvas;
  private long _time;
  private Graphics2D _g2;
  private int _numZ;
  private AnimationThread _thread;

  private class AnimationThread extends Thread {

    private boolean _stop;
    //private HexCanvas _canvas;
    private long _frameRate;

    AnimationThread() {
      setDaemon(true);
      _stop = false;
//      /_canvas = c;
      _frameRate = 100;
    }


    public void requestStop() {
      _stop = true;
      try {
        join();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void run() {

      while (_stop == false) {
        boolean repaint;

        repaint = false;
        try {
          Thread.sleep(_frameRate);
          long time = System.currentTimeMillis();

          for (int z = 0; z < _numZ; z++) {
            if (!_sprites[z].isEmpty()) {
              Iterator<Sprite> itr = _sprites[z].iterator();
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
          _canvas.repaint();

        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  public SpriteEngine(HexCanvas canvas, Image[] images, int numZ) {

    _images = images;
    _canvas = canvas;
    _time = 0;
    _g2 = null;
    _numZ = numZ;
    _sprites = (List<Sprite>[]) new List<?>[_numZ];
    for (int i = 0; i < _numZ; i++) {
      _sprites[i] = new ArrayList<Sprite>();
    }

    _thread = null;
  }

  public void stop() {
    if (_thread != null) {
      _thread.requestStop();
      _thread = null;
    }
  }

  public void start() {

    if (_thread != null) {
      stop();

    }
    _thread = new AnimationThread();
    _thread.start();

  }

  public synchronized void add(Sprite s) {
    _sprites[s.getZPos()].add(s);
  }

  public synchronized void remove(Sprite s) {
    _sprites[s.getZPos()].remove(s);
  }

  public boolean contains(Sprite s) {
    return _sprites[s.getZPos()].contains(s);
  }

  public synchronized void beginDraw(Graphics2D g) {
    _time = System.currentTimeMillis();
    _g2 = g;
  }

  public synchronized void endDraw() {
    _time = 0;
    _g2 = null;
  }

  public synchronized void draw(int z) {

    if (_time == 0) {
      throw new IllegalStateException(
          "beginDraw() must be called before draw()");
    }

    if (_sprites[z].isEmpty()) {
      return;
    }
    List<Sprite> newList = new ArrayList<Sprite>();
    Iterator<Sprite> itr = _sprites[z].iterator();
    while (itr.hasNext()) {
      Sprite s = itr.next();
      s.draw(_time, _images, _g2);
      if (!s.done()) {
        newList.add(s);
      }
    }
    _sprites[z] = newList;
  }
}
