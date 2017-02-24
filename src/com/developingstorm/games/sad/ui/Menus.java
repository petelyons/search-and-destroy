package com.developingstorm.games.sad.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**

 * 
 */
public class Menus {

  static final JMenu FILE = new JMenu("File");
  static final JMenu DEBUG = new JMenu("Debug");
  static final JMenu VIEW = new JMenu("View");
  static final JMenu HELP = new JMenu("Help");

  static final JMenuItem NEW = new JMenuItem("New");
  static final JMenuItem OPEN = new JMenuItem("Open...");
  static final JMenuItem SAVE = new JMenuItem("Save");
  static final JMenuItem SAVEAS = new JMenuItem("Save As...");
  static final JMenuItem EXIT = new JMenuItem("Exit");

  static final JMenuItem ABOUT = new JMenuItem("About...");

  static final JCheckBoxMenuItem DEBUG_ASTAR_SEL = new JCheckBoxMenuItem(
      "Track A*");
  static final JCheckBoxMenuItem DEBUG_EXPLORE = new JCheckBoxMenuItem(
      "Explore");
  static final JCheckBoxMenuItem DEBUG_LENS_SEL = new JCheckBoxMenuItem(
      "God Lens");

  static final JMenuItem VIEW_CENTER = new JMenuItem("Center");
  static final JCheckBoxMenuItem VIEW_SEA_PATHS = new JCheckBoxMenuItem(
      "Sea Paths");
  static final JCheckBoxMenuItem VIEW_AIR_PATHS = new JCheckBoxMenuItem(
      "Air Paths");
  static final JCheckBoxMenuItem VIEW_GROUND_PATHS = new JCheckBoxMenuItem(
      "Ground Paths");

  static final ResetableActionListener s_al = new ResetableActionListener();

  static {
    FILE.add(NEW);
    FILE.add(OPEN);
    FILE.addSeparator();
    FILE.add(SAVE);
    FILE.add(SAVEAS);
    FILE.addSeparator();
    FILE.add(EXIT);

    VIEW.add(VIEW_CENTER);
    VIEW.addSeparator();
    VIEW.add(VIEW_SEA_PATHS);
    VIEW.add(VIEW_AIR_PATHS);
    VIEW.add(VIEW_GROUND_PATHS);

    DEBUG.add(DEBUG_ASTAR_SEL);
    DEBUG.add(DEBUG_LENS_SEL);
    DEBUG.add(DEBUG_EXPLORE);

    HELP.add(ABOUT);

    NEW.addActionListener(s_al);
    OPEN.addActionListener(s_al);
    SAVE.addActionListener(s_al);
    SAVEAS.addActionListener(s_al);
    EXIT.addActionListener(s_al);

    VIEW_CENTER.addActionListener(s_al);

    DEBUG_ASTAR_SEL.addActionListener(s_al);
    DEBUG_LENS_SEL.addActionListener(s_al);
    DEBUG_EXPLORE.addActionListener(s_al);

    DEBUG_ASTAR_SEL.addActionListener(s_al);
    DEBUG_LENS_SEL.addActionListener(s_al);
    DEBUG_EXPLORE.addActionListener(s_al);

    ABOUT.addActionListener(s_al);

  }

  static final class ResetableActionListener implements ActionListener {
    ActionListener _al;

    ResetableActionListener() {
      _al = null;
    }

    public void setActionListener(ActionListener al) {
      _al = al;
    }

    public void actionPerformed(ActionEvent event) {
      _al.actionPerformed(event);
    }

  }

  public static JMenuBar getMainMenuBar(ActionListener al) {

    JMenuBar mainMenuBar = new JMenuBar();

    s_al.setActionListener(al);

    mainMenuBar.add(FILE);
    mainMenuBar.add(VIEW);
    mainMenuBar.add(DEBUG);
    mainMenuBar.add(HELP);

    return mainMenuBar;
  }

}
