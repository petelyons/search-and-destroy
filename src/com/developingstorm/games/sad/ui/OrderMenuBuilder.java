package com.developingstorm.games.sad.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;

/**

 * 
 */
public class OrderMenuBuilder {

  private final JMenuItem ACTIVATE_SEL = new JMenuItem("Clear");
  private final JMenuItem SENTRY_SEL = new JMenuItem("Sentry/Load");
  private final JMenuItem MOVE_SEL = new JMenuItem("Move");
  private final JMenuItem EXPLORE_SEL = new JMenuItem("Explore");
  private final JMenuItem UNLOAD_SEL = new JMenuItem("Unload");
  private final JMenuItem HEAD_HOME_SEL = new JMenuItem("Head Home");

  List _units;
  Game _game;
  UserCommands _commander;
  SaDFrame _frame;

  OrderMenuBuilder(SaDFrame frame, Game g, List units, UserCommands commander) {
    
    _frame = frame;
    _units = units;
    _game = g;
    _commander = commander;
    
    ACTIVATE_SEL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _commander.activate(null);
      }
    });
    SENTRY_SEL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _commander.sentry();
      }
    });
    MOVE_SEL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _commander.moveBegin();
      }
    });
    UNLOAD_SEL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _commander.unload();
      }
    });
    EXPLORE_SEL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _commander.explore();
      }
    });
    HEAD_HOME_SEL.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        _commander.headHome();
      }
    });
  }

  public JPopupMenu build() {
    JPopupMenu ordersPopup = new JPopupMenu("Orders");


    JMenuItem menuItem = ACTIVATE_SEL;
    ordersPopup.add(menuItem);

    menuItem = SENTRY_SEL;
    ordersPopup.add(menuItem);

    menuItem = UNLOAD_SEL;
    ordersPopup.add(menuItem);

    menuItem = MOVE_SEL;
    ordersPopup.add(menuItem);

    menuItem = EXPLORE_SEL;
    ordersPopup.add(menuItem);

    menuItem = HEAD_HOME_SEL;
    ordersPopup.add(menuItem);

    if (_units.size() == 1) {
      Unit u = (Unit) _units.get(0);
      City c = _game.cityAtLocation(u.getLocation());
      if (c != null) {
        CityMenuBuilder cm = new CityMenuBuilder(_frame, _game, c, _commander);
        ordersPopup.addSeparator();
        ordersPopup.add(cm.getSubmenu());
      }
    }

    return ordersPopup;
  }
}
