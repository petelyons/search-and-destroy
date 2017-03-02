package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;

public interface IHexMouseMotionListener {

  void hexMouseDragged(MouseEvent e, BoardHex hex);

  void hexMouseMoved(MouseEvent e, BoardHex hex);

 }