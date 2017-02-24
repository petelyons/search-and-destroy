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
public class OrderMenu {

  private static final JMenuItem ACTIVATE_SEL = new JMenuItem("Clear");
  private static final JMenuItem SENTRY_SEL = new JMenuItem("Sentry/Load");
  private static final JMenuItem MOVE_SEL = new JMenuItem("Move");
  private static final JMenuItem EXPLORE_SEL = new JMenuItem("Explore");
  private static final JMenuItem UNLOAD_SEL = new JMenuItem("Unload");
  private static final JMenuItem HEAD_HOME_SEL = new JMenuItem("Head Home");

  private static final OrderListener AL = new OrderListener();

  static {
    ACTIVATE_SEL.addActionListener(AL);
    SENTRY_SEL.addActionListener(AL);
    MOVE_SEL.addActionListener(AL);
    UNLOAD_SEL.addActionListener(AL);
    EXPLORE_SEL.addActionListener(AL);
    HEAD_HOME_SEL.addActionListener(AL);
  }

  public static JPopupMenu get(Game g, List units, UserCommands commander) {
    JPopupMenu ordersPopup = new JPopupMenu("Orders");

    AL.setUnits(units);
    AL.setGame(g);
    AL.setCommander(commander);

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

    if (units.size() == 1) {
      Unit u = (Unit) units.get(0);
      City c = g.cityAtLocation(u.getLocation());
      if (c != null) {
        ordersPopup.addSeparator();
        ordersPopup.add(CityMenu.getSubmenu(g, c, commander));
      }
    }

    return ordersPopup;
  }

  static final private class OrderListener implements ActionListener {

    List _units;
    Game _game;
    UserCommands _commander;

    OrderListener() {

    }

    public void setGame(Game g) {
      _game = g;
    }

    public void setUnits(List u) {
      _units = u;
    }

    public void setCommander(UserCommands c) {
      _commander = c;
    }

    public void actionPerformed(ActionEvent event) {
      Object object = event.getSource();
      if (object == ACTIVATE_SEL) {
        _commander.activate(null);
      }
      if (object == SENTRY_SEL) {
        _commander.sentry();
      } else if (object == UNLOAD_SEL) {
        _commander.unload();

      } else if (object == MOVE_SEL) {
        _commander.moveBegin();
      } else if (object == EXPLORE_SEL) {
        _commander.explore();
      } else if (object == HEAD_HOME_SEL) {
        _commander.headHome();
      }
    }

  }

}
