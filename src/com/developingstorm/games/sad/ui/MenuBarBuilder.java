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
public class MenuBarBuilder {

  private final JMenu FILE = new JMenu("File");
  private final JMenu DEBUG = new JMenu("Debug");
  private final JMenu VIEW = new JMenu("View");
  private final JMenu HELP = new JMenu("Help");

  private final JMenuItem NEW = new JMenuItem("New");
  private final JMenuItem OPEN = new JMenuItem("Open...");
  private final JMenuItem SAVE = new JMenuItem("Save");
  private final JMenuItem SAVEAS = new JMenuItem("Save As...");
  private final JMenuItem EXIT = new JMenuItem("Exit");

  private final JMenuItem ABOUT = new JMenuItem("About...");

  private final JCheckBoxMenuItem DEBUG_ASTAR_SEL = new JCheckBoxMenuItem("Track A*");
  private final JCheckBoxMenuItem DEBUG_EXPLORE = new JCheckBoxMenuItem("Explore");
  private final JCheckBoxMenuItem DEBUG_LENS_SEL = new JCheckBoxMenuItem("God Lens");

  private final JMenuItem VIEW_CENTER = new JMenuItem("Center");
  private final JCheckBoxMenuItem VIEW_SEA_PATHS = new JCheckBoxMenuItem("Sea Paths");
  private final JCheckBoxMenuItem VIEW_AIR_PATHS = new JCheckBoxMenuItem("Air Paths");
  private final JCheckBoxMenuItem VIEW_GROUND_PATHS = new JCheckBoxMenuItem("Ground Paths");

  private final MenuBarHandler _handler;

  MenuBarBuilder(MenuBarHandler iMenusHandler) {
    _handler = iMenusHandler;
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

    NEW.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onNew();
      }});
    
    OPEN.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onOpen();
      }});
    SAVE.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onSave();
      }});
    SAVEAS.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onSaveAs();
      }});
     
    EXIT.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onExit();
      }});

    VIEW_CENTER.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onCenter();
      }});

    DEBUG_ASTAR_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onDebugAstar(DEBUG_ASTAR_SEL.isSelected());
      }});
    DEBUG_LENS_SEL.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onDebugGodLens(DEBUG_LENS_SEL.isSelected());
      }});
    DEBUG_EXPLORE.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onDebugExplore(DEBUG_EXPLORE.isSelected());
      }});



    ABOUT.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        _handler.onAbout();
      }});

  }

  public JMenuBar build() {

    JMenuBar mainMenuBar = new JMenuBar();

    mainMenuBar.add(FILE);
    mainMenuBar.add(VIEW);
    mainMenuBar.add(DEBUG);
    mainMenuBar.add(HELP);

    return mainMenuBar;
  }

}
