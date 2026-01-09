package com.developingstorm.games.sad.ui;

import java.awt.event.KeyEvent;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;

/**
 * Builds the menu bar for the game
 */
public class MenuBarBuilder {

    private final JMenu FILE = new JMenu("File");
    private final JMenu DEBUG = new JMenu("Debug");
    private final JMenu VIEW = new JMenu("View");
    private final JMenu CONTROLS = new JMenu("Controls");
    private final JMenu HELP = new JMenu("Help");

    private final JMenuItem NEW = new JMenuItem("New");
    private final JMenuItem OPEN = new JMenuItem("Open...");
    private final JMenuItem SAVE = new JMenuItem("Save");
    private final JMenuItem SAVEAS = new JMenuItem("Save As...");
    private final JMenuItem EXIT = new JMenuItem("Exit");

    private final JMenuItem ABOUT = new JMenuItem("About...");

    private final JCheckBoxMenuItem DEBUG_ASTAR_SEL = new JCheckBoxMenuItem(
        "Track A*"
    );
    private final JCheckBoxMenuItem DEBUG_EXPLORE = new JCheckBoxMenuItem(
        "Explore"
    );
    private final JCheckBoxMenuItem DEBUG_LENS_SEL = new JCheckBoxMenuItem(
        "God Lens"
    );
    private final JCheckBoxMenuItem DEBUG_CONTINENT_NUMBERS_SEL =
        new JCheckBoxMenuItem("Continent Numbers");
    private final JCheckBoxMenuItem DEBUG_LOCATIONS_SEL = new JCheckBoxMenuItem(
        "Locations"
    );
    private final JCheckBoxMenuItem DEBUG_PATH_ERRORS_SEL =
        new JCheckBoxMenuItem("Path Errors");

    private final JMenuItem DEBUG_DUMP = new JMenuItem("Dump State");

    private final JMenuItem VIEW_CENTER = new JMenuItem("Center");
    private final JCheckBoxMenuItem VIEW_SEA_PATHS = new JCheckBoxMenuItem(
        "Sea Paths"
    );
    private final JCheckBoxMenuItem VIEW_AIR_PATHS = new JCheckBoxMenuItem(
        "Air Paths"
    );
    private final JCheckBoxMenuItem VIEW_GROUND_PATHS = new JCheckBoxMenuItem(
        "Ground Paths"
    );

    private final JMenuItem GAME_MODE = new JRadioButtonMenuItem("Game Mode");
    private final JMenuItem EXPLORE_MODE = new JRadioButtonMenuItem(
        "Explore Mode"
    );

    private final MenuBarHandler _handler;

    MenuBarBuilder(SaDFrame frame, MenuBarHandler iMenusHandler) {
        _handler = iMenusHandler;

        HELP.add(ABOUT);

        NEW.setMnemonic(KeyEvent.VK_N);
        NEW.addActionListener(e -> _handler.onNew());

        OPEN.setMnemonic(KeyEvent.VK_O);
        OPEN.addActionListener(e -> _handler.onOpen());

        SAVE.setMnemonic(KeyEvent.VK_S);
        SAVE.setAccelerator(
            KeyStroke.getKeyStroke('S', KeyEvent.ALT_DOWN_MASK)
        );
        SAVE.addActionListener(e -> _handler.onSave());

        SAVEAS.setMnemonic(KeyEvent.VK_A);
        SAVEAS.addActionListener(e -> _handler.onSaveAs());

        EXIT.setAccelerator(
            KeyStroke.getKeyStroke('X', KeyEvent.ALT_DOWN_MASK)
        );
        EXIT.setMnemonic(KeyEvent.VK_X);
        EXIT.addActionListener(e -> _handler.onExit());

        VIEW_CENTER.setMnemonic(KeyEvent.VK_C);
        VIEW_CENTER.addActionListener(e -> _handler.onCenter());

        DEBUG_ASTAR_SEL.addActionListener(e ->
            _handler.onDebugAstar(DEBUG_ASTAR_SEL.isSelected())
        );
        DEBUG_LENS_SEL.addActionListener(e ->
            _handler.onDebugGodLens(DEBUG_LENS_SEL.isSelected())
        );
        DEBUG_EXPLORE.addActionListener(e ->
            _handler.onDebugExplore(DEBUG_EXPLORE.isSelected())
        );
        DEBUG_CONTINENT_NUMBERS_SEL.addActionListener(e ->
            _handler.onDebugContinentNumbers(
                DEBUG_CONTINENT_NUMBERS_SEL.isSelected()
            )
        );
        DEBUG_LOCATIONS_SEL.addActionListener(e ->
            _handler.onDebugLocations(DEBUG_LOCATIONS_SEL.isSelected())
        );
        DEBUG_PATH_ERRORS_SEL.addActionListener(e ->
            _handler.onDebugPathErrors(DEBUG_PATH_ERRORS_SEL.isSelected())
        );
        DEBUG_DUMP.addActionListener(e -> _handler.onDebugDump());

        ABOUT.addActionListener(e -> _handler.onAbout());

        GAME_MODE.setSelected(frame.isGameMode());
        GAME_MODE.addActionListener(e -> _handler.onGameMode());

        EXPLORE_MODE.setSelected(frame.isExploreMode());
        EXPLORE_MODE.addActionListener(e -> _handler.onExploreMode());

        VIEW_SEA_PATHS.setSelected(SaDFrame.SHOW_SEA_PATHS);
        VIEW_SEA_PATHS.addActionListener(e ->
            SaDFrame.SHOW_SEA_PATHS = !SaDFrame.SHOW_SEA_PATHS
        );

        VIEW_AIR_PATHS.setSelected(SaDFrame.SHOW_AIR_PATHS);
        VIEW_AIR_PATHS.addActionListener(e ->
            SaDFrame.SHOW_AIR_PATHS = !SaDFrame.SHOW_AIR_PATHS
        );

        VIEW_GROUND_PATHS.setSelected(SaDFrame.SHOW_LAND_PATHS);
        VIEW_GROUND_PATHS.addActionListener(e ->
            SaDFrame.SHOW_LAND_PATHS = !SaDFrame.SHOW_LAND_PATHS
        );

        ButtonGroup group = new ButtonGroup();
        group.add(GAME_MODE);
        group.add(EXPLORE_MODE);

        FILE.setMnemonic(KeyEvent.VK_F);
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

        CONTROLS.add(GAME_MODE);
        CONTROLS.add(EXPLORE_MODE);

        DEBUG.add(DEBUG_ASTAR_SEL);
        DEBUG.add(DEBUG_LENS_SEL);
        DEBUG.add(DEBUG_CONTINENT_NUMBERS_SEL);
        DEBUG.add(DEBUG_LOCATIONS_SEL);
        DEBUG.add(DEBUG_PATH_ERRORS_SEL);
        DEBUG.addSeparator();
        DEBUG.add(DEBUG_DUMP);
    }

    public JMenuBar build() {
        JMenuBar mainMenuBar = new JMenuBar();

        mainMenuBar.add(FILE);
        mainMenuBar.add(VIEW);
        mainMenuBar.add(CONTROLS);
        mainMenuBar.add(DEBUG);
        mainMenuBar.add(HELP);

        return mainMenuBar;
    }
}
