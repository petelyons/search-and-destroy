package com.developingstorm.games.sad.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.UIManager;

import com.developingstorm.games.astar.AStarWatcher;
import com.developingstorm.games.gridmap.GridMap;
import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Hex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Debug;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.GameListener;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.NoKeyScrollPane;
import com.developingstorm.util.ResourceUtil;

/**
 * Class information
 */
public class SaDFrame extends JFrame implements ItemListener, ActionListener,
    GameListener, Presenter {

  private static final Color MYRED = new Color(250, 100, 100);

  private static final Color MYBLUE = new Color(150, 150, 250);

  private static final Color MYGREEN = new Color(250, 0, 200);

  private static final Color MYPURPLE = new Color(240, 220, 240);

  class MapContext implements SaDBoardContext {

    Image[] _images;

    MapContext() {

      _images = GameIcons.get().getImages();

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

      return _map.getHeight();
    }

    public int getHexSide() {

      return 24;
    }

    public Image[] getImages() {

      return _images;
    }

    public int getTerrainImageSelector(int x, int y) {
      return _map.getData(x, y);
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

      return _map.getWidth();
    }

    public Color getXorColor() {
      if (_paused) {
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

  static SaDFrame INSTANCE;

  private final static int iARID = 3;

  private final static int iFOREST = 4;

  private final static int iLAND = 1;

  private final static int iMOUNTAIN = 5;

  private final static int iSAND = 2;

  private final static int iWATER = 0;

  private final static int iSWAMP = 6;

  private final static String MAP_EXT = ".sdm";

  private GridMap _map;

  private Board _board;

  private BoardCanvas _canvas;

  private Game _game;

  private String _fileName = "MedMap" + MAP_EXT;

  private BoardHex _focus;

  private BoardHex _oldFocus;

  private int _range = 0;

  private List _rangeList;

  private UserAction _currentAction;

  private JScrollPane _scroll;

  private int _terrainType = iLAND;

  private City _currentCity = null;

  private int[][] _terrainTypes;

  private MapContext _ctx;

  private GameToolbar _tbar;

  private UnitStatusBar _ubar;

  private UIListeners _playControls;

  private UIListeners _pauseControls;

  private boolean _paused;

  private Unit _unitTracked = null;

  private Unit _unitChanged = null;

  private Location _playLocation;

  private UserCommands _commander;

  static void exit() {

    System.exit(0); // close the application
  }

  static public final int MAX_PLAYERS = 6;

  public static void main(String[] args) {

    String os = System.getProperty("os.name");
    if (os.startsWith("Windows")) {
      try {
        UIManager
            .setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    SaDFrame frame = new SaDFrame();

  }

  public SaDFrame() {

    INSTANCE = this;

    setTitle("Search And Destroy");

    try {
      InputStream is = ResourceUtil.openResourceStream(getClass()
          .getClassLoader(), "MedMap.sdm");
      InputStreamReader sr = new InputStreamReader(is);
      _map = GridMap.loadMap(sr);
    } catch (Exception e) {
      e.printStackTrace();
      throw new SaDException("Bad Map:" + e);
    }

    _terrainTypes = _map.getData();

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {

        System.exit(0);
      }
    });

    _focus = null;
    _oldFocus = null;
    _currentAction = null;

    GameIcons icons = GameIcons.get();

    _playLocation = null;

    _ctx = new MapContext();

    Player[] players = new Player[2];
    players[0] = new Player("Pete", 1);
    players[1] = new Robot("Robby", 2);

    _game = new Game(players, _map, _ctx);
    _game.setGameListener(this);

    _tbar = new GameToolbar(null, this);
    _ubar = new UnitStatusBar();
    _board = _game.getBoard();
    _ubar.setGame(_game);

    _canvas = new BoardCanvas(_game, icons, _ctx);

    _commander = new UserCommands(this, _canvas, _game);

    _playControls = new UIListeners(_canvas, new PlayMouseControls(_commander),
        new PlayKeyboardControls(_commander));
    _pauseControls = new UIListeners(_canvas,
        new PauseMouseControls(_commander), new PauseKeyboardControls(
            _commander));
    _paused = false;
    _playControls.enable();

    _scroll = new NoKeyScrollPane();

    JViewport vp = _scroll.getViewport();
    vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    vp.add(_canvas);

    Container pane = getContentPane();
    pane.setLayout(new BorderLayout(0, 0));
    pane.add(BorderLayout.CENTER, _scroll);
    pane.add(BorderLayout.SOUTH, _ubar);
    pane.add(BorderLayout.NORTH, _tbar);

    setSize(600, 500);

    initMenus();

    setVisible(true);

    initGame();
  }

  public JFrame getFrame() {
    return this;
  }

  public void initGame() {
    _canvas.startAmination();
    play();
  }

  public void initMenus() {
    setJMenuBar(Menus.getMainMenuBar(this));

  }

  public void actionPerformed(ActionEvent event) {

    Object object = event.getSource();
    if (object == Menus.EXIT) {
      exit();
    } else if (object == Menus.SAVE) {
      onSave();
    } else if (object == Menus.SAVEAS) {
      onSaveAs();
    } else if (object == Menus.OPEN) {
      onOpen();
    } else if (object == Menus.ABOUT) {
      onAbout();
    } else if (object == Menus.VIEW_CENTER) {
      center(_game.selectedUnit().getLocation());
    }
  }

  /**
	 *  
	 */

  public AStarWatcher getWatcher() {
    if (Menus.DEBUG_ASTAR_SEL.getState())
      return _canvas;
    else
      return null;
  }

  /**
	 *  
	 */
  public void newTurn(int t) {

  }

  public void selectPlayer(Player p) {
    if (Menus.DEBUG_LENS_SEL.getState())
      _canvas.setLens(_game);
    else
      _canvas.setLens(p);
    _canvas.resetPaths(p, true, true, true);
    _canvas.repaint();
  }

  public void trackUnit(Unit u) {
    _unitTracked = u;
    _ubar.setUnit(u);
  }

  public void selectUnit(Unit u) {
    _unitChanged = u;
    _ubar.setUnit(u);
    _board.clearSelected();
    Debug.setDebugExplore(Menus.DEBUG_EXPLORE.getState());
    if (Debug.getDebugExplore()) {
      List list = Debug.getDebugLocations();
      if (list != null) {
        _board.setLocationsSelected(list, true);
      }
    }

  }

  public void killUnit(Unit u, boolean showDeath) {
    if (showDeath) {
      _canvas.addExplosion(u.getLocation());
    }
  }

  public void hitLocation(Location loc) {
    _canvas.addExplosion(loc);
  }

  public void select(Location loc) {
    // showLocation(loc);
    _playLocation = loc;
    _canvas.setCursor(loc);
  }

  private class GameRunner extends Thread {
    GameRunner() {
      setDaemon(true);
    }

    public void run() {
      
      _game.play();
    }
  }

  public void play() {
    _canvas.requestFocus();
    GameRunner runner = new GameRunner();
    runner.start();
  }

  public boolean isOnScreen(int x, int y) {
    JViewport viewport = _scroll.getViewport();
    Dimension d = viewport.getExtentSize();
    Point vp = viewport.getViewPosition();
    return (x >= vp.x && y >= vp.y && x <= vp.x + d.width && y <= vp.y
        + d.height);
  }

  public boolean isOnScreen(Point p) {
    return (isOnScreen(p.x, p.y));
  }

  public void notifyWait() {

    if (_unitChanged != null) {
      centerIfOff(_unitChanged.getLocation());
      _canvas.setCursor(_unitChanged.getLocation());
      _unitChanged = null;
      _unitTracked = null;
    } else if (_unitTracked != null) {
      showLocation(_unitTracked.getLocation());
      _canvas.setCursor(_unitTracked.getLocation());
      _unitTracked = null;
    }
    else {
      throw new SaDException("No UNIT");
    }

  }

  public boolean isOnScreen(Location loc) {
    BoardHex hex = _board.get(loc);
    Hex h = hex.getHex();

    for (int np = 0; np < h.npoints; np++) {
      if (!isOnScreen(h.xpoints[np], h.ypoints[np])) {
        return false;
      }
    }
    return true;
  }

  public void center(Location loc) {
    JViewport viewport = _scroll.getViewport();
    Dimension d = viewport.getExtentSize();
    BoardHex hex = _board.get(loc);
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

    Dimension cd = _canvas.getSize();

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

    JViewport viewport = _scroll.getViewport();
    Dimension d = viewport.getExtentSize();

    Point vp = viewport.getViewPosition();
    BoardHex hex = _board.get(loc);
    Hex h = hex.getHex();
    Point p = h.getCenter();

    int newx = vp.x;
    int newy = vp.y;

    int iconSize = _ctx.getHexSide() * 2;

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

    Dimension cd = _canvas.getSize();

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
    return _canvas;

  }

  public void itemStateChanged(ItemEvent event) {
    Object source = event.getSource();
    if (source instanceof JToggleButton) {
      JToggleButton jt = (JToggleButton) source;

      String cmd = jt.getActionCommand();
      if (cmd.equals("PLAY") && event.getStateChange() == ItemEvent.SELECTED) {
        _paused = false;
        _pauseControls.disable();
        _playControls.enable();
        if (_playLocation != null)
          select(_playLocation);
        _canvas.requestFocus();
        _game.continuePlay();
        _commander.setPlayMode();
      } else if (cmd.equals("PAUSE")
          && event.getStateChange() == ItemEvent.SELECTED) {
        _paused = true;
        _playControls.disable();
        _pauseControls.enable();
        _canvas.requestFocus();
        _game.pausePlay();
        _commander.setPauseMode();
      }

    }
  }

  void onOpen() {
    try {
      FileDialog openFileDialog = new FileDialog(this, "Open", FileDialog.LOAD);
      openFileDialog.setDirectory(".");
      openFileDialog.setVisible(true);
      _fileName = openFileDialog.getFile();
      if (!_fileName.endsWith(MAP_EXT)) {
        return;
      }
      repaint();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void onSave() {
    if (_fileName == null) {
      onSaveAs();
      return;
    }

    try {
      _map.saveMap(_fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void onSaveAs() {
    try {
      FileDialog openFileDialog = new FileDialog(this, "Save", FileDialog.SAVE);
      openFileDialog.setDirectory(".");
      openFileDialog.setVisible(true);
      _fileName = openFileDialog.getFile();
      if (!_fileName.endsWith(MAP_EXT))
        _fileName = _fileName + MAP_EXT;
      _map.saveMap(_fileName);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }

  void onAbout() {
    try {
      (new AboutDialog(this, true)).setVisible(true);
    } catch (java.lang.Exception e) {
    }
  }

  public void abort() {
    System.exit(1);
  }
}