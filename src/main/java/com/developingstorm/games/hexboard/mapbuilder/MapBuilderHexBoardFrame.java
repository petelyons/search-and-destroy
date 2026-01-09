package com.developingstorm.games.hexboard.mapbuilder;

import java.awt.Color;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.HexBoard;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.HexCanvas;
import com.developingstorm.games.hexboard.Location;

/**
 * Class information
 */
public class MapBuilderHexBoardFrame extends JFrame implements MouseListener,
    MouseMotionListener, ActionListener {

   private static final long serialVersionUID = 5049729837199753440L;

  class MapContext implements HexBoardContext {

    Image[] images;

    MapContext() {

      images = new Image[7];
      this.images[iWATER] = new ImageIcon("images/water.gif").getImage();
      this.images[iLAND] = new ImageIcon("images/land.gif").getImage();
      this.images[iFOREST] = new ImageIcon("images/forest.gif").getImage();
      this.images[iSAND] = new ImageIcon("images/sand.gif").getImage();
      this.images[iARID] = new ImageIcon("images/arid.gif").getImage();
      this.images[iSWAMP] = new ImageIcon("images/swamp.gif").getImage();
      this.images[iMOUNTAIN] = new ImageIcon("images/mountain.gif").getImage();

    }

    public Color getBorderColor() {

      return Color.GRAY;
    }

    public int getHeight() {

      return MapBuilderHexBoardFrame.this.map.getHeight();
    }

    public int getHexSide() {

      return 24;
    }

    public Image[] getImages() {

      return images;
    }

    public int getTerrainImageSelector(int x, int y) {
      return MapBuilderHexBoardFrame.this.terrainTypes[x][y];
    }

    public int getUnexploredImageSelector() {
      return 0;
    }

    public int getPrototypeHex() {

      return 0; // water
    }

    public Color getSelectionColor() {

      return Color.YELLOW;
    }

    public int getWidth() {

      return MapBuilderHexBoardFrame.this.map.getHeight();
    }

    public Color getXorColor() {

      return Color.GRAY;
    }

    public int getZs() {

      return 3;
    }

    public boolean showBorder() {

      return true;
    }
  }

  private final static int iARID = 3;
  private final static int iFOREST = 4;
  private final static int iLAND = 1;
  private final static int iMOUNTAIN = 5;
  private final static int iSAND = 2;
  private final static int iWATER = 0;
  private final static int iSWAMP = 6;

  private final static String MAP_EXT = ".sdm";

  private HexBoard board;
  private HexCanvas canvas;
  private int brushSize = 0;

  private String fileName = "map1" + MAP_EXT;

  private JScrollPane scroll;
  private int terrainType = iLAND;

  private int[][] terrainTypes;
  private HexBoardMap map;

  private JMenuItem ARID = new JRadioButtonMenuItem();
  private JMenuItem FOREST = new JRadioButtonMenuItem();
  private JMenuItem LAND = new JRadioButtonMenuItem();
  private JMenuItem SWAMP = new JRadioButtonMenuItem();
  private JMenuItem MOUNTAIN = new JRadioButtonMenuItem();
  private JMenuItem SAND = new JRadioButtonMenuItem();

  private JMenuItem SMALL = new JRadioButtonMenuItem();
  private JMenuItem MEDIUM = new JRadioButtonMenuItem();
  private JMenuItem LARGE = new JRadioButtonMenuItem();

  private JMenuItem NEW = new JMenuItem();
  private JMenuItem OPEN = new JMenuItem();

  private JMenuItem SAVE = new JMenuItem();
  private JMenuItem SAVEAS = new JMenuItem();
  private JMenuItem EXIT = new JMenuItem();

  private JMenuItem WATER = new JRadioButtonMenuItem();

  static void exit() {

    System.exit(0); // close the application
  }

  public static void main(String[] args) {

    try {
      UIManager
          .setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

    } catch (Throwable ex) {
      ex.printStackTrace();
    }

    try {
     @SuppressWarnings("unused")
    MapBuilderHexBoardFrame mapBuilderHexBoardFrame = new MapBuilderHexBoardFrame();
    } catch (Throwable ex) {
      ex.printStackTrace();
    }

  }

  public MapBuilderHexBoardFrame() {

    setTitle("Search And Destroy - Map Builder");

    map = new HexBoardMap(50, 50);
    terrainTypes = this.map.getData();

    addWindowListener(new WindowAdapter() {

      public void windowClosing(WindowEvent e) {

        System.exit(0);
      }

    });

     MapContext ctx = new MapContext();
    board = new HexBoard(ctx);
    scroll = new JScrollPane();
    this.scroll.setOpaque(true);
    canvas = new HexCanvas(ctx, this.board);
    this.canvas.setLevelZeroCache(false);
    this.canvas.addMouseListener(this);
    this.canvas.addMouseMotionListener(this);
    Container pane = getContentPane();
    JViewport vp = this.scroll.getViewport();
    vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    vp.add(this.canvas);
    pane.add(this.scroll);
    pack();
    setSize(600, 500);

    setJMenuBar(getMainMenuBar());

    setVisible(true);
  }

  void resetBoardAndCanvas() {

    JViewport vp = this.scroll.getViewport();
    vp.remove(this.canvas);

    MapContext ctx = new MapContext();
    board = new HexBoard(ctx);
    canvas = new HexCanvas(ctx, this.board);
    this.canvas.addMouseListener(this);
    this.canvas.addMouseMotionListener(this);
    vp.add(this.canvas);
    this.canvas.stopAmination();
    this.canvas.setVisible(true);
  }

  public void actionPerformed(ActionEvent event) {

    Object object = event.getSource();
    if (object == EXIT) {
      exit();
    } else if (object == WATER) {
      terrainType = iWATER;
    } else if (object == LAND) {
      terrainType = iLAND;
    } else if (object == FOREST) {
      terrainType = iFOREST;
    } else if (object == SAND) {
      terrainType = iSAND;
    } else if (object == SWAMP) {
      terrainType = iSWAMP;
    } else if (object == ARID) {
      terrainType = iARID;
    } else if (object == MOUNTAIN) {
      terrainType = iMOUNTAIN;
    } else if (object == SAVE) {
      onSave();
    } else if (object == SAVEAS) {
      onSaveAs();
    } else if (object == OPEN) {
      onOpen();
    } else if (object == SMALL) {
      brushSize = 0;
    } else if (object == MEDIUM) {
      brushSize = 1;
    } else if (object == LARGE) {
      brushSize = 2;
    }

  }

  private JMenuBar getMainMenuBar() {

    JMenuBar mainMenuBar = new JMenuBar();

    initMenuItem(NEW, "New");
    initMenuItem(OPEN, "Open...");
    initMenuItem(SAVE, "Save");
    initMenuItem(SAVEAS, "Save As...");
    initMenuItem(EXIT, "Exit");

    JMenu file = new JMenu();
    file.setText("File");
    file.add(NEW);
    file.add(OPEN);
    file.addSeparator();
    file.add(SAVE);
    file.add(SAVEAS);
    file.addSeparator();
    file.add(EXIT);

    JMenu terrain = new JMenu();
    ButtonGroup group = new ButtonGroup();
    initRadioMenuItem(group, WATER, "Water");
    initRadioMenuItem(group, LAND, "Land");
    initRadioMenuItem(group, SAND, "Sand");
    initRadioMenuItem(group, FOREST, "Forest");
    initRadioMenuItem(group, ARID, "Arid");
    initRadioMenuItem(group, SWAMP, "Swamp");
    initRadioMenuItem(group, MOUNTAIN, "Mountain");

    terrain.setText("Terrain");
    terrain.add(WATER);
    terrain.add(LAND);
    terrain.add(SAND);
    terrain.add(FOREST);
    terrain.add(ARID);
    terrain.add(SWAMP);
    terrain.add(MOUNTAIN);

    LAND.setSelected(true);

    JMenu brush = new JMenu();
    ButtonGroup group2 = new ButtonGroup();
    initRadioMenuItem(group2, SMALL, "Small");
    initRadioMenuItem(group2, MEDIUM, "Medium");
    initRadioMenuItem(group2, LARGE, "Large");

    brush.setText("Brush");
    brush.add(SMALL);
    brush.add(MEDIUM);
    brush.add(LARGE);

    SMALL.setSelected(true);

    mainMenuBar.add(file);
    mainMenuBar.add(terrain);
    mainMenuBar.add(brush);

    return mainMenuBar;
  }

  private void initMenuItem(JMenuItem mi, String s) {
    mi.addActionListener(this);
    mi.setText(s);
    mi.setEnabled(true);
  }

  private void initRadioMenuItem(ButtonGroup g, JMenuItem mi, String s) {
    g.add(mi);
    mi.addActionListener(this);
    mi.setText(s);
    mi.setEnabled(true);
  }

  private void setHexTerrain(BoardHex bh) {
    bh.setImageSelector(this.terrainType);
    Location loc = bh.getLocation();
    this.terrainTypes[loc.x][loc.y] = terrainType;
  }

  private void brushTerrain(BoardHex bh) {
    if (bh != null) {
      setHexTerrain(bh);
      if (this.brushSize > 0) {
        List<BoardHex> list = bh.getRing(this.brushSize);
        for (BoardHex bh2 : list) {
          if (bh2 != null) {
            setHexTerrain(bh2);
          }
        }
      }
    }
  }

  public void mouseClicked(MouseEvent e) {

  }

  public void mouseDragged(MouseEvent e) {
    BoardHex bh = this.board.get(e.getPoint());
    brushTerrain(bh);
    repaint();
  }

  public void mouseEntered(MouseEvent e) {

  }

  public void mouseExited(MouseEvent e) {

  }

  public void mouseMoved(MouseEvent e) {

  }

  public void mousePressed(MouseEvent e) {
    int button = e.getButton();
    if (button == MouseEvent.BUTTON1) {
      BoardHex bh = this.board.get(e.getPoint());
      brushTerrain(bh);
      repaint();
    }
  }

  public void mouseReleased(MouseEvent arg0) {

  }

  void onOpen() {
    try {
      FileDialog openFileDialog = new FileDialog(this, "Open", FileDialog.LOAD);
      openFileDialog.setDirectory(".");
      openFileDialog.setVisible(true);
      fileName = openFileDialog.getFile();
      if (fileName == null) {
        return;
      }
      if (!this.fileName.endsWith(MAP_EXT)) {
        return;
      }

      map = HexBoardMap.loadMap(this.fileName);
      terrainTypes = this.map.getData();

      resetBoardAndCanvas();
      repaint();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void onSave() {
    if (fileName == null) {
      onSaveAs();
      return;
    }

    try {
      this.map.saveMap(this.fileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void onSaveAs() {
    try {
      FileDialog openFileDialog = new FileDialog(this, "Save", FileDialog.SAVE);
      openFileDialog.setDirectory(".");
      openFileDialog.setVisible(true);
      fileName = openFileDialog.getFile();
      if (fileName == null) {
        return;
      }
      if (!this.fileName.endsWith(MAP_EXT))
        fileName = this.fileName + MAP_EXT;
      this.map.saveMap(this.fileName);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

  }

}