package com.developingstorm.games.hexboard.test;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.HexBoard;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexCanvas;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;

/**
 * Class information
 */
public class TestHexBoardFrame extends JFrame implements MouseListener,
    MouseMotionListener, ActionListener {

  private HexBoard _board;
  private HexCanvas _canvas;

  private JScrollPane _scroll;
  private BoardHex _focus;
  private BoardHex _oldFocus;
  private int _range = 0;
  private List<BoardHex> _rangeList;
  private BoardHex _mouseDown;

  // Used for addNotify check.
  boolean fComponentsAdjusted = false;

  public TestHexBoardFrame() {

    addWindowListener(new WindowAdapter() {

      public void windowClosing(WindowEvent e) {

        System.exit(0);
      }

    });

    _mouseDown = null;
    _focus = null;
    _oldFocus = null;

    TestContext ctx = new TestContext();
    
    LocationMap.init(ctx.getWidth(), ctx.getHeight());
    _board = new HexBoard(ctx);

    _scroll = new JScrollPane();
    _scroll.setOpaque(true);

    _canvas = new HexCanvas(ctx, _board) {

      public void paint(Graphics g) {

        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;

        if (_focus != null) {
          highlight(_focus, g2);
          if (_range > 0) {
            List<?> list = _rangeList;
            if (list != null) {
              Iterator<?> itr = list.iterator();
              while (itr.hasNext()) {
                BoardHex h = (BoardHex) itr.next();
                highlight(h, g2);
              }
            }
          }

        }

      }
    };
    _canvas.addMouseListener(this);
    _canvas.addMouseMotionListener(this);
    _canvas.startAmination();

    Container pane = getContentPane();
    JViewport vp = _scroll.getViewport();
    vp.setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
    vp.add(_canvas);
    pane.add(_scroll);
    pack();
    setSize(600, 500);

    // setJMenuBar(getMainMenuBar());

    setVisible(true);
  }

  /*
   * public JMenuBar getMainMenuBar() {
   * 
   * JMenuBar mainMenuBar = new JMenuBar();
   * 
   * JMenu test = new JMenu();
   * 
   * OPEN_SEL.addActionListener(this); EXIT_SEL.addActionListener(this);
   * 
   * NEW_SEL.setLabel("New"); OPEN_SEL.setLabel("Open...");
   * 
   * 
   * SAVE_SEL.setEnabled(true); NEW_SEL.setEnabled(false);
   * 
   * test.setLabel("File"); test.add(NEW_SEL); test.add(OPEN_SEL);
   * 
   * 
   * mainMenuBar.add(test); return mainMenuBar; }
   */
  public void mouseClicked(MouseEvent arg0) {

  }

  public void mouseEntered(MouseEvent arg0) {

  }

  public void mouseExited(MouseEvent arg0) {

  }

  public void mouseDragged(MouseEvent e) {
    BoardHex bh = _board.get(e.getPoint());

    if (_mouseDown != null && bh != null && !bh.equals(_mouseDown)) {
      Point p1 = _mouseDown.getHex().getCenter();
      Point p2 = bh.getHex().getCenter();
      _canvas.setArrow(p1, p2);
      repaint();
    }
  }

  public void mouseMoved(MouseEvent e) {

  }

  public void mousePressed(MouseEvent e) {

    int button = e.getButton();
    if (button == MouseEvent.BUTTON1) {

      BoardHex old = _focus;
      _oldFocus = _focus;
      _focus = _board.get(e.getPoint());
      _board.setFocus(_focus);
      _mouseDown = _focus;
      if (_focus == null) {
        return;
      }
      if (_focus.equals(old)) {
        _range++;
        _rangeList = _focus.getRing(_range);
        _board.setSelected(_rangeList, true);
      } else {
        _board.clearSelected();
        _rangeList = null;
        _range = 0;
      }

      repaint();
    }
  }

  public void mouseReleased(MouseEvent arg0) {
    _mouseDown = null;
  }

  public void actionPerformed(ActionEvent event) {

    Object object = event.getSource();
    // if (object == OPEN_SEL) {
    // onOpen();
    // }

  }

  private void highlight(BoardHex hex, Graphics2D g) {

    g.setColor(Color.RED);
    Font f = g.getFont();
    FontRenderContext frc = g.getFontRenderContext();
    Location loc = hex.getLocation();
    String s = loc.toString();
    Rectangle2D rd = f.getStringBounds(s, frc);
    int w = (int) rd.getWidth();
    int h = (int) rd.getHeight();
    Point p = hex.center();
    // g.fillOval(p.x - 3, p.y - 3, 6, 6);
    g.drawString(s, p.x - (w / 2), p.y + h);
  }

  static void exit() {

    System.exit(0); // close the application
  }

  public static void main(String[] args) {

    try {
      UIManager
          .setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    TestHexBoardFrame frame = new TestHexBoardFrame();
  }

  static class TestContext implements HexBoardContext {

    Image[] _images;

    TestContext() {

      _images = new Image[2];
      _images[0] = new ImageIcon("images/water.gif").getImage();
      _images[1] = new ImageIcon("images/land.gif").getImage();

    }

    public int getZs() {

      return 3;
    }

    public int getPrototypeHex() {

      return 0; // water
    }

    public Color getBorderColor() {

      return Color.GRAY;
    }

    public Color getSelectionColor() {

      return Color.YELLOW;
    }

    public Color getXorColor() {

      return Color.GRAY;
    }

    public int getHeight() {

      return 20;
    }

    public int getHexSide() {

      return 24;
    }

    public Image[] getImages() {

      return _images;
    }

    public int getTerrainImageSelector(int x, int y) {
      if (x > 5 && y > 5) {
        return 1;
      }
      return 0;
    }

    public int getUnexploredImageSelector() {
      return 0;
    }

    public int getWidth() {

      return 20;
    }

    public boolean showBorder() {

      return true;
    }
  }

}