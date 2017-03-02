package com.developingstorm.games.sad.ui.controls;

import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import com.developingstorm.games.sad.ui.UserActionOwner;

/**
 * 
 */
public abstract class BaseController implements UserActionOwner {

  public abstract MouseListener mouseListener();

  public abstract MouseMotionListener mouseMotionListener();

  public abstract KeyListener keyListener();
}
