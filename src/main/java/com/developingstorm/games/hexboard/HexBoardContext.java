package com.developingstorm.games.hexboard;

import java.awt.Color;
import java.awt.Image;

/**
 * 
 */
public interface HexBoardContext {

  int getPrototypeHex();

  Image[] getImages();

  int getHexSide();

  boolean showBorder();

  Color getBorderColor();

  Color getSelectionColor();

  Color getXorColor();

  int getZs();

  int getWidth();

  int getHeight();

  int getTerrainImageSelector(int x, int y);

  int getUnexploredImageSelector();

}
