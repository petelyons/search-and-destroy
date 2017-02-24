package com.developingstorm.games.sad.ui;

import javax.swing.JFrame;

import com.developingstorm.games.hexboard.Location;

/**

 * 
 */
public interface Presenter {

  JFrame getFrame();

  void showLocation(Location loc);

  void center(Location loc);

  BoardCanvas getCanvas();

}
