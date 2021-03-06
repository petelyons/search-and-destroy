package com.developingstorm.games.sad.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.developingstorm.games.astar.AStarNode;
import com.developingstorm.games.astar.AStarPosition;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.astar.AStarWatcher;
import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Hex;
import com.developingstorm.games.hexboard.HexCanvas;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.sprites.ArrowSprite;
import com.developingstorm.games.hexboard.sprites.Sprite;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.sprites.ExplosionSprite;



/**
 * A BoardCanvas draws a games state atop a HexCanvas.  It derives from the HexCanvas class.
 * 
 */
public class BoardCanvas extends HexCanvas {
  

  static final class PathError {
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((end == null) ? 0 : end.hashCode());
      result = prime * result + ((start == null) ? 0 : start.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      PathError other = (PathError) obj;
      if (end == null) {
        if (other.end != null)
          return false;
      } else if (!end.equals(other.end))
        return false;
      if (start == null) {
        if (other.start != null)
          return false;
      } else if (!start.equals(other.start))
        return false;
      return true;
    }
    Location start;
    Location end;
  }
  
  
  private static final long serialVersionUID = 7394548177221258018L;

  static final boolean USE_BACKING_BUFFER = false;
  
  public static boolean SHOW_CONTINENT_NUMBERS = false;

  public static boolean SHOW_LOCATIONS = false;

  public static boolean SHOW_PATH_ERRORS = false;

  private Board _board;
//  private int _width;
//  private int _height;
  private Game _game;
  private Image[] _mapImages;  
  private GameIcons _icons;
  private UIMode _uiMode = UIMode.GAME;
  private volatile Location _gameCursor;


  private SaDBoardContext _ctx;

  private List<Sprite> _seaPaths;
  private List<Sprite> _airPaths;
  private List<Sprite> _groundPaths;
  
  Set<PathError> _pathErrors;
  
  PathError _specificPathAnimationLine;

  private List<AStarState> _astarStates;

  public BoardCanvas(Game game, GameIcons icons, SaDBoardContext ctx) {

    super(ctx, game.getBoard());
    _ctx = ctx;

    _icons = icons;
    _game = game;
    _board = game.getBoard();
 //   _width = _board.getWidth();
 //   _height = _board.getHeight();

    _seaPaths = null;
    _airPaths = null;
    _groundPaths = null;
    
    _pathErrors = new HashSet<PathError>();

  }

  public boolean isOpaque() {
    return true;
  }

  public void setCursor(Location loc) {
    switch (_uiMode) {
    case GAME: {
      _gameCursor = loc;
      showGameCursor();
      break;
    }
    case PATHS:
      break;
    }
    
  }
  
  void showGameCursor() {
    if (_uiMode != UIMode.GAME) {
      throw new SaDException("Not in GAME mode");
    }
    if (_gameCursor == null) {
      return;
    }
    BoardHex hex = _board.get(_gameCursor);
   _board.setFocus(hex);
  }
  
  void clearCursor() {
   _board.setFocus(null);
  }


  public void setSelection(Location start, Location end) {
    /*
     * Location oldStart = _selStart; Location oldEnd = _selEnd;
     * 
     * if (start != null && end != null) { int minx = start.x; if (minx > end.x)
     * minx = end.x;
     * 
     * int maxx = end.x; if (maxx < start.x) maxx = start.x;
     * 
     * int miny = start.y; if (miny > end.y) miny = end.y;
     * 
     * int maxy = end.y; if (maxy < start.y) maxy = start.y;
     * 
     * _selStart = Location.get(minx, miny); _selEnd = Location.get(maxx, maxy);
     * } else { _selStart = null; _selEnd = null; }
     */
  }

  public void setLine(Location start, Location end) {
    if (start == null || end == null) {
      setArrow(null, null);
      return;
    }

    BoardHex bh1 = _board.get(start);
    BoardHex bh2 = _board.get(end);
    Point p1 = bh1.getHex().getCenter();
    Point p2 = bh2.getHex().getCenter();
    setArrow(p1, p2);
  }

  int getImageOffset(Image img) {
    for (int i = 0; i < _mapImages.length; i++) {
      if (_mapImages[i] == img) {
        return i;
      }
    }
    return -1;
  }


  private void drawCity(City city, boolean fortified, Graphics2D g,
      BoardHex bh, Location loc, Point center) {
    Player p = city.getOwner();
    Color c = Color.BLACK;
    int size = 3;
    if (p != null) {
      c = _ctx.getPlayerColor(p);
      size = 5;
    }
    Color oldColor = g.getColor();

    if (fortified) {
      size += 2;
      drawRect(g, false, new Point(center.x + 2, center.y + 2), c, size);
      drawRect(g, false, center, c, size);
    } else {
      drawRect(g, false, center, c, size);
    }
    g.setColor(c);
    if (p != null)
      g.drawPolygon(bh.getHex());

    Font f = g.getFont();
    FontRenderContext frc = g.getFontRenderContext();
    String name = city.getName();
    Type t = city.getProduction();
    if (t != null) {
      name = name + " (" + t.getAbr() + ")";
    }
    Rectangle2D rd = f.getStringBounds(name, frc);
    int w = (int) rd.getWidth();
    int h = (int) rd.getHeight();
    g.setColor(Color.BLACK);

    g.drawString(name, center.x - (w / 2), center.y + h + (h / 2));
    g.setColor(oldColor);

  }
  
  
  private static void drawContinentNumber(int num, Graphics2D g, Point center) {
    Color oldColor = g.getColor();
    g.setColor(Color.LIGHT_GRAY);
    Font f = g.getFont();
    FontRenderContext frc = g.getFontRenderContext();
    String val = "" + num;
    Rectangle2D rd = f.getStringBounds(val, frc);
    int w = (int) rd.getWidth();
    int h = (int) rd.getHeight();
    g.drawString(val, center.x - (w / 2), (center.y - h ) + (h / 2));
    g.setColor(oldColor);
  
  }
  
  
  private static void drawLocation(Graphics2D g, Location loc, Point center) {
    Color oldColor = g.getColor();
    g.setColor(Color.LIGHT_GRAY);
    Font f = g.getFont();
    FontRenderContext frc = g.getFontRenderContext();
    String val = loc.toString();
    Rectangle2D rd = f.getStringBounds(val, frc);
    int w = (int) rd.getWidth();
    int h = (int) rd.getHeight();
    g.drawString(val, center.x - (w / 2), (center.y + h ) + (h / 2));
    g.setColor(oldColor);
  
  }

  private static void drawRect(Graphics g, boolean highlight, Point p, Color c,
      int size) {
    g.setColor(c);

    int x = p.x - size;
    int y = p.y - size;
    int w = size * 2;
    int h = size * 2;
    g.fillRect(x, y, w, h);
    g.setColor(Color.BLACK);
    g.drawRect(x, y, w, h);

    if (highlight) {
      g.setColor(Color.LIGHT_GRAY);
      g.drawLine(x, y, x + w, y);
      g.drawLine(x, y, x, y + h);
    }
  }
  
  private void drawPathError(Graphics g, PathError pe, Color color) {
    g.setColor(color);

    BoardHex hex1 = _board.get(pe.start);
    BoardHex hex2 = _board.get(pe.end);

    Point p1 = hex1.center();
    Point p2 = hex2.center();
 
    g.drawLine(p1.x, p1.y, p2.x, p2.y);
  }


  private Image getUnitImage(Unit u) {

    Image[] imgs = _icons.getImages();

    Type t = u.getType();
    if (t == Type.INFANTRY && u.inSentryMode()) {
      return imgs[GameIcons.iSENTRYARMY];
    } else if (t == Type.ARMOR && u.inSentryMode()) {
      return imgs[GameIcons.iSENTRYTANK];
    } else if (t == Type.TRANSPORT && u.carriedWeight() > 0) {
      return imgs[GameIcons.iFULLTRANSPORT];
    } else if (t == Type.CARGO && u.carriedWeight() > 0) {
      return imgs[GameIcons.iFULLCARGO];
    } else {
      return imgs[t.getIcon()];
    }
  }

  private Point getCityCenter(City c) {
    BoardHex bh1 = _board.get(c.getLocation());
    return bh1.center();
  }

  public void resetPaths(Player player, boolean air, boolean ground, boolean sea) {
    removeSprites(_airPaths);
    removeSprites(_groundPaths);
    removeSprites(_seaPaths);

    if (player.isRobot()) {
      return;
    }

    _airPaths = new ArrayList<Sprite>();
    _groundPaths = new ArrayList<Sprite>();
    _seaPaths = new ArrayList<Sprite>();

    List<City> cities = player.getCities();
    for(City c : cities) {
      
      City airPath = c.getGovernor().getAirPathDest();
      City groundPath = c.getGovernor().getLandPathDest();
      City seaPath = c.getGovernor().getSeaPathDest();

      ArrowSprite s;
      if (air && airPath != null) {
        s = new ArrowSprite(9, Color.GRAY);
        s.setArrow(getCityCenter(c), getCityCenter(airPath));
        _airPaths.add(s);

      }
      if (sea && seaPath != null) {
        s = new ArrowSprite(9, Color.BLUE);
        s.setArrow(getCityCenter(c), getCityCenter(seaPath));
        _seaPaths.add(s);

      }
      if (ground && groundPath != null) {
        s = new ArrowSprite(9, Color.GREEN.darker().darker());
        s.setArrow(getCityCenter(c), getCityCenter(groundPath));
        _groundPaths.add(s);

      }
    }

    addSprites(_airPaths);
    addSprites(_groundPaths);
    addSprites(_seaPaths);

  }

  private void drawUnit(Unit u, Graphics g, BoardHex bh, Location loc,
      Point center) {
    

    if (u.life().hasMoves()) {
      Color oldColor = g.getColor();
      g.setColor(Color.YELLOW);
      g.fillOval(center.x - 18, center.y - 18, 36, 36);
      g.setColor(oldColor);
    }
    
    
    Player p = u.getOwner();
    int size = 14;
    Color c = _ctx.getPlayerColor(p);
    Color oldColor = g.getColor();
    drawRect(g, true, center, c, size);
    g.setColor(oldColor);
    Image[] imgs = _icons.getImages();

    g.drawImage(getUnitImage(u), center.x - 12, center.y - 12, 24, 24, null);
    if (u.inSentryMode() && u.getTravel() == Travel.SEA) {
      g.drawImage(imgs[GameIcons.iANCHOR], center.x - 12, center.y - 12, 24,
          24, null);
    }

  }

  protected void drawBoard(int z, Graphics2D g) {

    super.drawBoard(z, g);
    switch(_uiMode) {
    case GAME:
      drawGameBoard(z, g);
      break;
    case PATHS:
      drawPathsBoard(z, g);
      break;
    case EXPLORE:
      drawGameBoard(z, g);
      break;
    }  
  }
    
  private void drawContinentNumbers(Graphics2D g) {
    for (Continent c : _board.getContinents()) {
      Set<Location> locations = c.getLocations();
      for (Location loc : locations) {
        BoardHex hex = _board.get(loc.x, loc.y);
        Hex h = hex.getHex();
        Point center = h.getCenter();
        if (_lens != null && _lens.isExplored(loc)) {
          drawContinentNumber(c.getID(), g, center);
        }
      }
    }
  }
  
  
  private void drawCities(Graphics2D g) {
    for (City c : _board.getCities()) {
      Location loc = c.getLocation();
      BoardHex hex = _board.get(loc.x, loc.y);
      Hex h = hex.getHex();
      Point center = h.getCenter();
      if (_lens != null && _lens.isExplored(loc)) {
        List<Unit> list =_game.unitsAtLocation(loc);
        if (list == null || list.isEmpty()) {
          drawCity(c, false, g, hex, loc, center);
        }
        else {
          drawCity(c, true, g, hex, loc, center);
        }
      }
    }
  }
  
  private void drawUnits(Graphics2D g) {
    for (Unit u : _game.units()) {
      Location loc = u.getLocation();
      BoardHex hex = _board.get(loc.x, loc.y);
      Hex h = hex.getHex();
      Point center = h.getCenter();
      if (_lens != null && _lens.isExplored(loc)) {
        
        if (!u.isCarried() && !_board.isCity(loc)) {
          drawUnit(u, g, hex, loc, center);
        }
      }
    }
  }
  
  protected void drawPathsBoard(int z, Graphics2D g) {
    super.drawBoard(z, g);
    
    
    drawCities(g);

  }

  
  protected void drawGameBoard(int z, Graphics2D g) {

    super.drawBoard(z, g);
    
   
   
    if (z == 1) {
      
      if (SHOW_CONTINENT_NUMBERS) {
        drawContinentNumbers(g);
      }
      drawCities(g);
      drawUnits(g);
              
      // draw selected unit on-top
      if (_game != null) {
        Player p = _game.currentPlayer();
        if (p != null) {
          Unit u = _game.selectedUnit();
          if (u != null) {
            Location loc = u.getLocation();
            BoardHex hex = _board.get(loc.x, loc.y);
            drawUnit(u, g, hex, loc, hex.center());
          }
        }
      }
    } else if (z == 2) {
      if (_astarStates != null) {
        for (AStarState s : _astarStates) {
          
          AStarPosition pos = s.pos();
          BoardHex hex = _board.get(pos.getX(), pos.getY());
          Point p = hex.center();

          g.setColor(Color.BLACK);
          g.fillOval(p.x - 6, p.y - 6, 12, 12);
        }
      }

      if (SHOW_PATH_ERRORS) {
        for (PathError pe : _pathErrors) {
          drawPathError(g, pe, Color.RED);
        }
      }
      
      if (_specificPathAnimationLine != null) {
        drawPathError(g, _specificPathAnimationLine, Color.ORANGE);
      }
      
      if (SHOW_LOCATIONS) {
        drawLocationLabels(g);
      }
    }
  }

  private void drawLocationLabels(Graphics2D g) {
    int w = _board.getWidth();
    int h = _board.getHeight();
    for (int x = 0; x < w; x++) {
      for (int y = 0; y < h; y++) {
        Location loc = Location.get(x, y);
        BoardHex hex = _board.get(loc);
        drawLocation(g, loc, hex.center());
      }
    }
    
  }

  public void addExplosion(Location loc) {
    ExplosionSprite es = new ExplosionSprite();
    BoardHex hex = _board.get(loc);
    Point p = hex.center();
    Point p2 = new Point(p.x - 12, p.y - 12);
    es.setPoint(p2);
    addSprite(es);
  }

 
  public AStarWatcher getAStarWatcher() {
    
    return new AStarWatcher() {
      public void watch(boolean knownError, AStarWatcher.AStarRequestState states) {
        if ((SaDFrame.DEBUG_ASTAR || knownError) && SaDFrame.DEBUG_PATH_TOGGLE) { //Use F5 to toggle the path display
          if (states != null) {
            _astarStates = states.states;
            if (_astarStates != null) {
              _specificPathAnimationLine = new PathError();
              _specificPathAnimationLine.start = getLocation(states.start);
              _specificPathAnimationLine.end = getLocation(states.end);
            }
          }
          else {
            _astarStates = null;
            _specificPathAnimationLine = null;
          }
          
          if (_astarStates != null) {
            try {
              Thread.sleep(200);
            } catch (Exception e) {
            }
          }
        }
      }
      
      private Location getLocation(AStarNode node) {
        int x = node.state.pos().getX();
        int y = node.state.pos().getY();
        return Location.get(x, y);
      }
      
      
      public void displayError(AStarNode start, AStarNode end) {
        PathError pe = new PathError();
        pe.start = getLocation(start);
        pe.end = getLocation(end);
        _pathErrors.add(pe);
      }
    };
  }
  
  public void setUIMode(UIMode mode) {
    _uiMode = mode;
    switch (mode) {
    case GAME:
      showGameCursor();
      break;
    case PATHS:
      clearCursor();
      break;
    }
  }
  
  public UIMode getUIMode() {
    return _uiMode;
  }

  public void clearArrow() {
    setArrow(null, null);
  }


  
}
