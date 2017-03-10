package com.developingstorm.games.sad.ui;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.controls.GameCommander;

/**

 * 
 */
public class EdictMenuBuilder {
  
  private City _c;
  private Game _game;
  private GameCommander _commander;

  private final JMenuItem SEND_SEA_SEL = new JMenuItem("Send Sea Units..");
  private final JMenuItem SEND_AIR_SEL = new JMenuItem("Send Air Units...");
  private final JMenuItem SEND_LAND_SEL = new JMenuItem("Send Land Units...");
  private final JCheckBoxMenuItem AIR_PATROL_SEL = new JCheckBoxMenuItem("Air Patrol");
  private final JCheckBoxMenuItem AUTO_SENTRY_SEL = new JCheckBoxMenuItem("Automatic Sentry");
  
 

  public EdictMenuBuilder(SaDFrame frame, Game g, City c, GameCommander commander) {
    
    _game = g;
    _c = c;
    _commander = commander;
    
    select();
    SEND_SEA_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _commander.setSeaPath(_c);
      }});
    
    SEND_AIR_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _commander.setAirPath(_c);
      }});
    SEND_LAND_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _commander.setLandPath(_c);
      }});
    AIR_PATROL_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _commander.setAirPatrol(_c);
      }});
    AUTO_SENTRY_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _commander.setAutoSentry(_c);
      }});
  
  }

  
  public JPopupMenu build() {
    JPopupMenu edictPopup = new JPopupMenu("Edicts");

    fillMenu(_game, edictPopup, _c);
    return edictPopup;
  }
  
  public JMenu getSubmenu() {
    JMenu edictMenu = new JMenu("Edicts");

    fillMenu(_game, edictMenu, _c);
    return edictMenu;
  }

  
  private void fillMenu(Game g, Container menu, City c) {

    JMenuItem menuItem = SEND_AIR_SEL;
    menu.add(menuItem);

    menuItem = SEND_LAND_SEL;
    menu.add(menuItem);
    
    if (c.isCoastal()) {
      menuItem = SEND_SEA_SEL;
      menu.add(menuItem);
    }
    
    menuItem = AIR_PATROL_SEL;
    menu.add(menuItem);

    menuItem = AUTO_SENTRY_SEL;
    menu.add(menuItem);

  }
  
  private void select() {
    if (_c.getGovernor().hasAirPatrolEdict()) {
      AIR_PATROL_SEL.setSelected(true);
    }
    if (_c.getGovernor().hasAutoSentryEdict()) {
      AUTO_SENTRY_SEL.setSelected(true);
    }

  }
}
