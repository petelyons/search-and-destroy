package com.developingstorm.games.sad.ui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;

import javax.swing.JPanel;

/**
 */
public class ImagePanel extends JPanel implements ImageObserver {

  Image _image;

  public void setImageURL(String file) {
    Toolkit tk = Toolkit.getDefaultToolkit();
    _image = tk.getImage(file);
    setVisible(true);
  }

  public void paint(Graphics g) {

    g.drawImage(_image, 0, 0, this);
    Rectangle r = getBounds();
    g.drawRect(0, 0, r.width, r.height);
  }

  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width,
      int height) {
    if ((infoflags & ImageObserver.ALLBITS) != 0) {
      if (img == _image)
        repaint();
    }
    return true;
  }

}
