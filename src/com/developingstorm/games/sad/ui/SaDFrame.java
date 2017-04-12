package com.developingstorm.games.sad.ui;

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

/**
 * Class information
 */
public class SaDFrame extends JFrame {
 
  private static final Color MYRED = new Color(250, 100, 100);

  private static final Color MYBLUE = new Color(150, 150, 250);

  private static final Color MYGREEN = new Color(250, 0, 200);

  private static final Color MYPURPLE = new Color(240, 220, 240);
  
  private static class GameRunner extends Thread {
    
    private Game _g;
    
    GameRunner(Game game) {
      _g = game;
      setDaemon(true);
    }

    public void run() {
      _g.play();
    }

    public void endGame() {
      _g.end();
      
    }
  }

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

  private final static String MAP_EXT = ".sdm";

  private HexBoardMap _map;

  private Board _board;

  private BoardCanvas _canvas;

  private Game _game;

  private String _fileName = "MedMap" + MAP_EXT;

  private JScrollPane _scroll;

  private MapContext _ctx;

 // private GameToolbar _tbar;

  private UnitStatusBar _ubar;

  private boolean _paused;

  private Unit _unitTracked = null;

  private Unit _unitChanged = null;

  private Location _playLocation;

  private GameRunner _runner;

  private UIController _controller;
  
  
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

    setTitle("Search And Destroy");

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {

        System.exit(0);
      }
    });

   

    /*    _tbar = new GameToolbar(null, new ItemListener() {

    @Override
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

      }}});
*/




    _paused = false;
    _scroll = new NoKeyScrollPane();
    _ubar = new UnitStatusBar();
    
    
    NewGameValues vals = new NewGameValues();
    
    vals.gameSize = 1;
    vals.player1Type = 0;
    vals.player2Type = 1;
    vals.player1Name = "Pete";
    vals.player2Name = "Jayne";
    
    startNewGame(vals);
    
 

    Container pane = getContentPane();
    pane.setLayout(new BorderLayout(0, 0));
    pane.add(BorderLayout.CENTER, _scroll);
    pane.add(BorderLayout.SOUTH, _ubar);
   // pane.add(BorderLayout.NORTH, _tbar);

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
    
    _map = HexBoardMap.loadMapAsResource(this, "MedMap.sdm");
    _playLocation = null;
    _ctx = new MapContext();

    Player[] players = new Player[2];    
    players[0] = createPlayer(vals.player1Type, vals.player1Name, 1);
    players[1] = createPlayer(vals.player2Type, vals.player2Name, 2);
    
    if (vals.player1Type == 1 && vals.player2Type == 1) {
      DEBUG_GOD_LENS = true;
    }
    
       
    _game = new Game(players, _map, _ctx);
    _game.setGameListener(new GameListener() {

      @Override
      public void abort() {
        System.exit(1);
      }

      @Override
      public void newTurn(int t) { 
      }

      @Override
      public void trackUnit(Unit u) {
        _unitTracked = u;
        EventQueue.invokeLater(()-> {
          _ubar.setUnit(u);        });
      }
      @Override
      public void selectUnit(Unit u) {
        _unitChanged = u;

        EventQueue.invokeLater(()-> {
          _ubar.setUnit(u);
          _board.clearSelected();
         
          if (Debug.getDebugExplore()) {
            List<Location> list = Debug.getDebugLocations();
            if (list != null) {
              _board.setLocationsSelected(list, true);
            }
          }
        });
      }
      
      @Override
      public void killUnit(Unit u, boolean showDeath) {
        if (showDeath) {
          EventQueue.invokeLater(()-> {_canvas.addExplosion(u.getLocation());});
        }
      }
      @Override
      public void hitLocation(Location loc) {
        EventQueue.invokeLater(()-> {_canvas.addExplosion(loc);});
      }

    
      @Override
      public AStarWatcher getWatcher() {
        return _canvas.getAStarWatcher();
      }

      @Override
      public void selectPlayer(Player p) {
        EventQueue.invokeLater(()-> {SaDFrame.this.selectPlayer(p);});
      }

      @Override
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

      @Override
      public void gameOver(Player winner) {
        EventQueue.invokeLater(()-> {JOptionPane.showMessageDialog(SaDFrame.this,
                "GAME OVER.",
                "We have a winner!",
                JOptionPane.WARNING_MESSAGE);
          });
      }
    });


    _board = _game.getBoard();
    _ubar.setGame(_game);
    GameIcons icons = GameIcons.get();
    
    
    BoardCanvas oldCanvas = _canvas;
    _canvas = new BoardCanvas(_game, icons, _ctx);

    JViewport vp = _scroll.getViewport();
    if (oldCanvas != null) {
      vp.remove(oldCanvas);
    }
    vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    vp.add(_canvas);
    
    initGame();
  }


  private void selectPlayer(Player p) {
    if (DEBUG_GOD_LENS)
      _canvas.setLens(_game);
    else
      _canvas.setLens(p);
    _canvas.resetPaths(p, SHOW_AIR_PATHS, SHOW_LAND_PATHS, SHOW_SEA_PATHS);
    _canvas.repaint();
  }

  public void initGame() {
    if (_runner != null) {
      _runner.endGame();
    }
    
    
    _canvas.startAmination();
    _canvas.requestFocus();
    _runner = new GameRunner(_game);
    _runner.start();
    
    
    _controller = new UIController(this, _game);
    
    _canvas.addMouseListener(_controller.mouseListener());
    _canvas.addMouseMotionListener(_controller.mouseMotionListener());
    _canvas.addKeyListener(_controller.keyListener());
    
  
    _controller.switchMode(UIMode.GAME);
    
    _game.postAndRunGameAction(new Runnable() {

      @Override
      public void run() {
        Unit u = _game.selectedUnit();
        if (u != null) {
    //      center(u.getLocation());
        }
      }
      
    });
 }
  
  
  public void termGame() {
    if (_runner != null) {
      _runner.endGame();
    }
    if (_canvas != null) {
      _canvas.removeMouseListener(_controller.mouseListener());
      _canvas.removeMouseMotionListener(_controller.mouseMotionListener());
      _canvas.removeKeyListener(_controller.keyListener());
    }
  }


  public void initMenuBar() {
    MenuBarBuilder menus = new MenuBarBuilder(this, new MenuBarHandler() {
      @Override
      public void onExit() {
        System.exit(0);
      }

      @Override
      public void onCenter() {
        Unit u = _game.selectedUnit();
        if (u != null) {
          Location loc = u.getLocation();
          center(loc);
        }
      }

      @Override
      public void onNew() {
        NewGameDialog dialog = new NewGameDialog(SaDFrame.this, "New Game", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setVisible(true);
        NewGameValues vals = dialog.getValues();
        if (vals.exitButton == 0) {
          startNewGame(vals);
        }
      }

      @Override
      public void onDebugAstar(boolean v) {
        DEBUG_ASTAR = v;
        _canvas.validate();
      }

      @Override
      public void onDebugExplore(boolean v) {
        DEBUG_EXPLORE = v;
        Debug.setDebugExplore(v);
        _canvas.validate();
        
      }

      @Override
      public void onDebugGodLens(boolean v) {
        DEBUG_GOD_LENS = v;  
        selectPlayer(_game.currentPlayer());
      }

      @Override
      public void onDebugDump() {
        _game.dump();
      }

      public void onOpen() {
        try {
          FileDialog openFileDialog = new FileDialog(SaDFrame.this, "Open", FileDialog.LOAD);
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

      public void onSave() {
        Log.info("Saving game...");
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

      public void onSaveAs() {
        try {
          FileDialog openFileDialog = new FileDialog(SaDFrame.this, "Save", FileDialog.SAVE);
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

      public void onAbout() {
        try {
          (new AboutDialog(SaDFrame.this, true)).setVisible(true);
        } catch (java.lang.Exception e) {
        }
      }

      @Override
      public void onGameMode() {
        _controller.switchMode(UIMode.GAME);
      }

      @Override
      public void onExploreMode() {
        _controller.switchMode(UIMode.EXPLORE);
      }

      @Override
      public void onDebugContinentNumbers(boolean selected) {
        BoardCanvas.SHOW_CONTINENT_NUMBERS = selected;
        _canvas.validate();
      }

      @Override
      public void onDebugLocations(boolean selected) {
        BoardCanvas.SHOW_LOCATIONS = selected;
        _canvas.validate();
      }

      @Override
      public void onDebugPathErrors(boolean selected) {
        BoardCanvas.SHOW_PATH_ERRORS = selected;
        _canvas.validate();
      }
      
    });
    
    setJMenuBar(menus.build());
  }

  public void select(Location loc) {
    // showLocation(loc);
    _playLocation = loc;
    _canvas.setCursor(loc);
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

  public boolean isGameMode() {
    if (_controller == null) {
      return true;
    }
    return _controller.getUIMode() == UIMode.GAME;
  }

  public boolean isExploreMode() {
    if (_controller == null) {
      return false;
    }
    return _controller.getUIMode() == UIMode.EXPLORE;
  }
  
  
  
  
  public PathsCommander startPathsMode() {
    _controller.switchMode(UIMode.PATHS);
    return _controller.getPathsCommander();
    
  }
  
  public void returnGameMode() {
    _controller.switchMode(UIMode.GAME);  
    selectPlayer(_game.currentPlayer());
  }

}