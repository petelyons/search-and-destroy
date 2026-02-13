package com.developingstorm.games.sad.ui.controls;

import java.awt.event.MouseEvent;

import com.developingstorm.games.hexboard.BoardHex;

public interface IHexMouseListener {

  void hexMousePressed(MouseEvent event, BoardHex hex);

  void hexMouseReleased(MouseEvent event, BoardHex hex);

  void hexMouseClicked(MouseEvent e, BoardHex hex);

  void hexMouseEntered(MouseEvent e, BoardHex hex);

  void hexMouseExited(MouseEvent e, BoardHex hex);

}