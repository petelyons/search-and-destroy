package com.developingstorm.games.sad.ui;

import com.developingstorm.games.astar.AStarWatcher;
import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Hex;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.Debug;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameListener;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.NewGameDialog.NewGameValues;
import com.developingstorm.games.sad.ui.controls.PathsCommander;
import com.developingstorm.games.sad.ui.controls.UIController;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.NoKeyScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;
import org.slf4j.helpers.ThreadLocalMapOfStacks;

/**
 * Class information
 */
public class SaDFrame extends JFrame {

    private static final Color MYRED = new Color(250, 100, 100);

    private static final Color MYBLUE = new Color(150, 150, 250);

    private static final Color MYGREEN = new Color(250, 0, 200);

    private static final Color MYPURPLE = new Color(240, 220, 240);

    private static class GameRunner extends Thread {

        private Game g;

        GameRunner(Game game) {
            this.g = game;
            setDaemon(true);
        }

        public void run() {
            this.g.play();
        }

        public void endGame() {
            this.g.end();
        }
    }

    class MapContext implements SaDBoardContext {

        Image[] images;

        MapContext() {
            this.images = GameIcons.get().getImages();
        }

        public Color getPlayerColor(Player p) {
            int id = p.getId();
            switch (id) {
                case 0:
                    return Color.BLACK;
                case 1:
                    return MYRED;
                case 2:
                    return MYBLUE;
                case 3:
                    return MYGREEN;
                case 4:
                    return MYPURPLE;
            }
            return Color.WHITE;
        }

        public Color getBorderColor() {
            return Color.GRAY;
        }

        public int getHeight() {
            return SaDFrame.this.map.getHeight();
        }

        public int getHexSide() {
            return 24;
        }

        public Image[] getImages() {
            return images;
        }

        public int getTerrainImageSelector(int x, int y) {
            return SaDFrame.this.map.getData(x, y);
        }

        public int getUnexploredImageSelector() {
            return GameIcons.iUNEXPLORED;
        }

        public int getPrototypeHex() {
            return 0; // water
        }

        public Color getSelectionColor() {
            return Color.YELLOW;
        }

        public int getWidth() {
            return SaDFrame.this.map.getWidth();
        }

        public Color getXorColor() {
            if (SaDFrame.this.paused) {
                return Color.LIGHT_GRAY;
            }
            return null;
        }

        public int getZs() {
            return 3;
        }

        public boolean showBorder() {
            return true;
        }
    }

    private static final String MAP_EXT = ".sdm";

    private HexBoardMap map;

    private Board board;

    private BoardCanvas canvas;

    private Game game;

    private String fileName = "MedMap" + MAP_EXT;

    private JScrollPane scroll;

    private MapContext ctx;

    // private GameToolbar this.tbar;

    private UnitStatusBar ubar;

    private boolean paused;

    private Unit unitTracked = null;

    private Unit unitChanged = null;

    private Location playLocation;

    private GameRunner runner;

    private UIController controller;

    public static boolean SHOW_AIR_PATHS = true;
    public static boolean SHOW_SEA_PATHS = true;
    public static boolean SHOW_LAND_PATHS = true;

    public static boolean DEBUG_ASTAR = false;
    public static boolean DEBUG_EXPLORE = false;
    public static boolean DEBUG_GOD_LENS = false;

    public static boolean DEBUG_PATH_TOGGLE = true;

    static void exit() {
        System.exit(0); // close the application
    }

    public static final int MAX_PLAYERS = 6;

    public static void main(String[] args) {
        String os = System.getProperty("os.name");
        if (os.startsWith("Windows")) {
            try {
                UIManager.setLookAndFeel(
                    "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        SaDFrame frame = new SaDFrame();
    }

    public SaDFrame() {
        setTitle("Search And Destroy");

        addWindowListener(
            new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            }
        );

        /*    this.tbar = new GameToolbar(null, new ItemListener() {

    @Override
    public void itemStateChanged(ItemEvent event) {
      Object source = event.getSource();
      if (source instanceof JToggleButton) {
        JToggleButton jt = (JToggleButton) source;

        String cmd = jt.getActionCommand();
        if (cmd.equals("PLAY") && event.getStateChange() == ItemEvent.SELECTED) {
          this.paused = false;
          this.pauseControls.disable();
          this.playControls.enable();
          if (this.playLocation != null)
            select(this.playLocation);
          this.canvas.requestFocus();
          this.game.continuePlay();
          this.commander.setPlayMode();
        } else if (cmd.equals("PAUSE")
            && event.getStateChange() == ItemEvent.SELECTED) {
          this.paused = true;
          this.playControls.disable();
          this.pauseControls.enable();
          this.canvas.requestFocus();
          this.game.pausePlay();
          this.commander.setPauseMode();
        }

      }}});
*/

        this.paused = false;
        this.scroll = new NoKeyScrollPane();
        this.ubar = new UnitStatusBar();

        NewGameValues vals = new NewGameValues();

        vals.gameSize = 1;
        vals.player1Type = 0;
        vals.player2Type = 1;
        vals.player1Name = "Pete";
        vals.player2Name = "Jayne";

        startNewGame(vals);

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout(0, 0));
        pane.add(BorderLayout.CENTER, this.scroll);
        pane.add(BorderLayout.SOUTH, this.ubar);
        // pane.add(BorderLayout.NORTH, this.tbar);

        setSize(600, 500);

        initMenuBar();

        setVisible(true);
    }

    private Player createPlayer(int type, String name, int id) {
        if (type == 0) {
            return new Player(name, id);
        } else {
            return new Robot(name, id);
        }
    }

    private void startNewGame(NewGameValues vals) {
        termGame();

        this.map = HexBoardMap.loadMapAsResource(this, "MedMap.sdm");
        this.playLocation = null;
        this.ctx = new MapContext();

        Player[] players = new Player[2];
        players[0] = createPlayer(vals.player1Type, vals.player1Name, 1);
        players[1] = createPlayer(vals.player2Type, vals.player2Name, 2);

        if (vals.player1Type == 1 && vals.player2Type == 1) {
            DEBUG_GOD_LENS = true;
        }

        game = new Game(players, this.map, this.ctx);
        this.game.setGameListener(
            new GameListener() {
                @Override
                public void abort() {
                    System.exit(1);
                }

                @Override
                public void newTurn(int t) {}

                @Override
                public void trackUnit(Unit u) {
                    SaDFrame.this.unitTracked = u;
                    EventQueue.invokeLater(() -> {
                        SaDFrame.this.ubar.setUnit(u);
                    });
                }

                @Override
                public void selectUnit(Unit u) {
                    SaDFrame.this.unitChanged = u;

                    EventQueue.invokeLater(() -> {
                        SaDFrame.this.ubar.setUnit(u);
                        SaDFrame.this.board.clearSelected();

                        if (Debug.getDebugExplore()) {
                            List<Location> list = Debug.getDebugLocations();
                            if (list != null) {
                                SaDFrame.this.board.setLocationsSelected(
                                    list,
                                    true
                                );
                            }
                        }
                    });
                }

                @Override
                public void killUnit(Unit u, boolean showDeath) {
                    if (showDeath) {
                        EventQueue.invokeLater(() -> {
                            SaDFrame.this.canvas.addExplosion(u.getLocation());
                        });
                    }
                }

                @Override
                public void hitLocation(Location loc) {
                    EventQueue.invokeLater(() -> {
                        SaDFrame.this.canvas.addExplosion(loc);
                    });
                }

                @Override
                public AStarWatcher getWatcher() {
                    return SaDFrame.this.canvas.getAStarWatcher();
                }

                @Override
                public void selectPlayer(Player p) {
                    EventQueue.invokeLater(() -> {
                        SaDFrame.this.selectPlayer(p);
                    });
                }

                @Override
                public void notifyWait() {
                    if (SaDFrame.this.unitChanged != null) {
                        centerIfOff(SaDFrame.this.unitChanged.getLocation());
                        SaDFrame.this.canvas.setCursor(
                            SaDFrame.this.unitChanged.getLocation()
                        );
                        SaDFrame.this.unitChanged = null;
                        SaDFrame.this.unitTracked = null;
                    } else if (SaDFrame.this.unitTracked != null) {
                        showLocation(SaDFrame.this.unitTracked.getLocation());
                        SaDFrame.this.canvas.setCursor(
                            SaDFrame.this.unitTracked.getLocation()
                        );
                        SaDFrame.this.unitTracked = null;
                    } else {
                        throw new SaDException("No UNIT");
                    }
                }

                @Override
                public void gameOver(Player winner) {
                    EventQueue.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                            SaDFrame.this,
                            "GAME OVER.",
                            "We have a winner!",
                            JOptionPane.WARNING_MESSAGE
                        );
                    });
                }
            }
        );

        this.board = this.game.getBoard();
        this.ubar.setGame(this.game);
        GameIcons icons = GameIcons.get();

        BoardCanvas oldCanvas = this.canvas;
        this.canvas = new BoardCanvas(this.game, icons, this.ctx);

        JViewport vp = this.scroll.getViewport();
        if (oldCanvas != null) {
            vp.remove(oldCanvas);
        }
        vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
        vp.add(this.canvas);

        initGame();
    }

    private void selectPlayer(Player p) {
        if (DEBUG_GOD_LENS) this.canvas.setLens(this.game);
        else this.canvas.setLens(p);
        this.canvas.resetPaths(
            p,
            SHOW_AIR_PATHS,
            SHOW_LAND_PATHS,
            SHOW_SEA_PATHS
        );
        this.canvas.repaint();
    }

    public void initGame() {
        if (this.runner != null) {
            this.runner.endGame();
        }

        this.canvas.startAmination();
        this.canvas.requestFocus();
        this.runner = new GameRunner(this.game);
        this.runner.start();

        this.controller = new UIController(this, this.game);

        this.canvas.addMouseListener(this.controller.mouseListener());
        this.canvas.addMouseMotionListener(
            this.controller.mouseMotionListener()
        );
        this.canvas.addKeyListener(this.controller.keyListener());

        this.controller.switchMode(UIMode.GAME);

        this.game.postAndRunGameAction(
            new Runnable() {
                @Override
                public void run() {
                    Unit u = SaDFrame.this.game.selectedUnit();
                    if (u != null) {
                        //      center(u.getLocation());
                    }
                }
            }
        );
    }

    public void termGame() {
        if (this.runner != null) {
            this.runner.endGame();
        }
        if (this.canvas != null) {
            this.canvas.removeMouseListener(this.controller.mouseListener());
            this.canvas.removeMouseMotionListener(
                this.controller.mouseMotionListener()
            );
            this.canvas.removeKeyListener(this.controller.keyListener());
        }
    }

    public void initMenuBar() {
        MenuBarBuilder menus = new MenuBarBuilder(
            this,
            new MenuBarHandler() {
                @Override
                public void onExit() {
                    System.exit(0);
                }

                @Override
                public void onCenter() {
                    Unit u = SaDFrame.this.game.selectedUnit();
                    if (u != null) {
                        Location loc = u.getLocation();
                        center(loc);
                    }
                }

                @Override
                public void onNew() {
                    NewGameDialog dialog = new NewGameDialog(
                        SaDFrame.this,
                        "New Game",
                        Dialog.ModalityType.APPLICATION_MODAL
                    );
                    dialog.setVisible(true);
                    NewGameValues vals = dialog.getValues();
                    if (vals.exitButton == 0) {
                        startNewGame(vals);
                    }
                }

                @Override
                public void onDebugAstar(boolean v) {
                    DEBUG_ASTAR = v;
                    SaDFrame.this.canvas.validate();
                }

                @Override
                public void onDebugExplore(boolean v) {
                    DEBUG_EXPLORE = v;
                    Debug.setDebugExplore(v);
                    SaDFrame.this.canvas.validate();
                }

                @Override
                public void onDebugGodLens(boolean v) {
                    DEBUG_GOD_LENS = v;
                    selectPlayer(SaDFrame.this.game.currentPlayer());
                }

                @Override
                public void onDebugDump() {
                    SaDFrame.this.game.dump();
                }

                public void onOpen() {
                    try {
                        FileDialog openFileDialog = new FileDialog(
                            SaDFrame.this,
                            "Open",
                            FileDialog.LOAD
                        );
                        openFileDialog.setDirectory(".");
                        openFileDialog.setVisible(true);
                        fileName = openFileDialog.getFile();
                        if (!SaDFrame.this.fileName.endsWith(MAP_EXT)) {
                            return;
                        }
                        repaint();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onSave() {
                    Log.info("Saving game...");
                    if (fileName == null) {
                        onSaveAs();
                        return;
                    }

                    try {
                        SaDFrame.this.map.saveMap(SaDFrame.this.fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                public void onSaveAs() {
                    try {
                        FileDialog openFileDialog = new FileDialog(
                            SaDFrame.this,
                            "Save",
                            FileDialog.SAVE
                        );
                        openFileDialog.setDirectory(".");
                        openFileDialog.setVisible(true);
                        fileName = openFileDialog.getFile();
                        if (
                            !SaDFrame.this.fileName.endsWith(MAP_EXT)
                        ) fileName = SaDFrame.this.fileName + MAP_EXT;
                        SaDFrame.this.map.saveMap(SaDFrame.this.fileName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                }

                public void onAbout() {
                    try {
                        (new AboutDialog(SaDFrame.this, true)).setVisible(true);
                    } catch (java.lang.Exception e) {}
                }

                @Override
                public void onGameMode() {
                    SaDFrame.this.controller.switchMode(UIMode.GAME);
                }

                @Override
                public void onExploreMode() {
                    SaDFrame.this.controller.switchMode(UIMode.EXPLORE);
                }

                @Override
                public void onDebugContinentNumbers(boolean selected) {
                    BoardCanvas.SHOW_CONTINENT_NUMBERS = selected;
                    SaDFrame.this.canvas.validate();
                }

                @Override
                public void onDebugLocations(boolean selected) {
                    BoardCanvas.SHOW_LOCATIONS = selected;
                    SaDFrame.this.canvas.validate();
                }

                @Override
                public void onDebugPathErrors(boolean selected) {
                    BoardCanvas.SHOW_PATH_ERRORS = selected;
                    SaDFrame.this.canvas.validate();
                }
            }
        );

        setJMenuBar(menus.build());
    }

    public void select(Location loc) {
        // showLocation(loc);
        this.playLocation = loc;
        this.canvas.setCursor(loc);
    }

    public boolean isOnScreen(int x, int y) {
        JViewport viewport = this.scroll.getViewport();
        Dimension d = viewport.getExtentSize();
        Point vp = viewport.getViewPosition();
        return (
            x >= vp.x &&
            y >= vp.y &&
            x <= vp.x + d.width &&
            y <= vp.y + d.height
        );
    }

    public boolean isOnScreen(Point p) {
        return (isOnScreen(p.x, p.y));
    }

    public boolean isOnScreen(Location loc) {
        BoardHex hex = this.board.get(loc);
        Hex h = hex.getHex();

        for (int np = 0; np < h.npoints; np++) {
            if (!isOnScreen(h.xpoints[np], h.ypoints[np])) {
                return false;
            }
        }
        return true;
    }

    public void center(Location loc) {
        JViewport viewport = this.scroll.getViewport();
        Dimension d = viewport.getExtentSize();
        BoardHex hex = this.board.get(loc);
        Hex h = hex.getHex();
        Point p = h.getCenter();

        int viewportCenterX = d.width / 2;
        int viewportCenterY = d.height / 2;

        int newx = p.x - viewportCenterX;
        int newy = p.y - viewportCenterY;

        if (newx < 0) {
            newx = 0;
        }
        if (newy < 0) {
            newy = 0;
        }

        Dimension cd = this.canvas.getSize();

        int bw = cd.width;
        int bh = cd.height;
        if (newx > bw - d.width) {
            newx = bw - d.width;
        }
        if (newy > bh - d.height) {
            newy = bh - d.height;
        }

        Point newPos = new Point(newx, newy);
        viewport.setViewPosition(newPos);
    }

    public void centerIfOff(Location loc) {
        if (!isOnScreen(loc)) {
            center(loc);
        }
    }

    public void showLocation(Location loc) {
        JViewport viewport = this.scroll.getViewport();
        Dimension d = viewport.getExtentSize();

        Point vp = viewport.getViewPosition();
        BoardHex hex = this.board.get(loc);
        Hex h = hex.getHex();
        Point p = h.getCenter();

        int newx = vp.x;
        int newy = vp.y;

        int iconSize = this.ctx.getHexSide() * 2;

        if (p.x < vp.x + iconSize) {
            newx = p.x - iconSize;
        }
        if (p.y < vp.y + iconSize) {
            newy = p.y - iconSize;
        }
        int bottomx = vp.x + d.width;
        if (p.x + iconSize > bottomx - iconSize) {
            newx = vp.x + ((p.x + iconSize) - (bottomx - iconSize));
        }
        int bottomy = vp.y + d.height;
        if (p.y + iconSize > bottomy - iconSize) {
            newy = vp.y + ((p.y + iconSize) - (bottomy - iconSize));
        }

        if (newx < 0) {
            newx = 0;
        }
        if (newy < 0) {
            newy = 0;
        }

        Dimension cd = this.canvas.getSize();

        int bw = cd.width;
        int bh = cd.height;
        if (newx > bw - d.width) {
            newx = bw - d.width;
        }
        if (newy > bh - d.height) {
            newy = bh - d.height;
        }
        Point newPos = new Point(newx, newy);
        viewport.setViewPosition(newPos);
    }

    public BoardCanvas getCanvas() {
        return canvas;
    }

    public boolean isGameMode() {
        if (controller == null) {
            return true;
        }
        return this.controller.getUIMode() == UIMode.GAME;
    }

    public boolean isExploreMode() {
        if (controller == null) {
            return false;
        }
        return this.controller.getUIMode() == UIMode.EXPLORE;
    }

    public PathsCommander startPathsMode() {
        this.controller.switchMode(UIMode.PATHS);
        return this.controller.getPathsCommander();
    }

    public void returnGameMode() {
        this.controller.switchMode(UIMode.GAME);
        selectPlayer(this.game.currentPlayer());
    }
}
