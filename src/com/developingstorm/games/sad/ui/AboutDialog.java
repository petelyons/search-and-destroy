package com.developingstorm.games.sad.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class AboutDialog extends JDialog {

  public AboutDialog(Frame parent, boolean modal) {
    super(parent, modal);

    Container pane = getContentPane();
    pane.setLayout(null);
    pane.setFont(new Font("SansSerif", Font.PLAIN, 11));
    setSize(158 + 249, 170);
    setVisible(false);
    label1.setText("Search and Destroy");
    pane.add(label1);
    label1.setFont(new Font("SansSerif", Font.PLAIN, 20));
    label1.setBounds(138 + 56, 20, 178, 37);
    okButton.setLabel("OK");
    pane.add(okButton);
    okButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
    okButton.setBounds(158 + 88, 116, 66, 20);
    label2.setText("Version 0.3 (c) DevelopingStorm.com 2017");
    // label2.setAlignment(java.awt.Label.CENTER);
    pane.add(label2);
    label2.setBounds(158 + 20, 80, 212, 32);
    imagePanel1.setImageURL("images/empire.gif");
    imagePanel1.setLayout(null);
    pane.add(imagePanel1);
    imagePanel1.setBounds(8, 8, 159, 144);
    setTitle("About");
    // }}

    // {{REGISTER_LISTENERS
    SymWindow aSymWindow = new SymWindow();
    this.addWindowListener(aSymWindow);
    SymAction lSymAction = new SymAction();
    okButton.addActionListener(lSymAction);
    // }}

  }

  public AboutDialog(Frame parent, String title, boolean modal) {
    this(parent, modal);
    setTitle(title);
  }

  public void addNotify() {
    // Record the size of the window prior to calling parents addNotify.
    Dimension d = getSize();

    super.addNotify();

    // Only do this once.
    if (fComponentsAdjusted)
      return;

    // Adjust components according to the insets
    Insets insets = getInsets();
    setSize(insets.left + insets.right + d.width, insets.top + insets.bottom
        + d.height);
    Component components[] = getComponents();
    for (int i = 0; i < components.length; i++) {
      Point p = components[i].getLocation();
      p.translate(insets.left, insets.top);
      components[i].setLocation(p);
    }

    // Used for addNotify check.
    fComponentsAdjusted = true;
  }

  public void setVisible(boolean b) {
    if (b) {
      Rectangle bounds = getParent().getBounds();
      Rectangle abounds = getBounds();

      setLocation(bounds.x + (bounds.width - abounds.width) / 2, bounds.y
          + (bounds.height - abounds.height) / 2);
    }

    super.setVisible(b);
  }

  // {{DECLARE_CONTROLS
  JLabel label1 = new JLabel();
  JButton okButton = new JButton();
  JLabel label2 = new JLabel();
  ImagePanel imagePanel1 = new ImagePanel();
  // }}

  // Used for addNotify check.
  boolean fComponentsAdjusted = false;

  class SymAction implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent event) {
      Object object = event.getSource();
      if (object == okButton)
        okButton_ActionPerformed(event);
    }
  }

  void okButton_ActionPerformed(java.awt.event.ActionEvent event) {
    // to do: code goes here.

    okButton_ActionPerformed_Interaction1(event);
  }

  void okButton_ActionPerformed_Interaction1(java.awt.event.ActionEvent event) {
    try {
      this.dispose();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  class SymWindow extends java.awt.event.WindowAdapter {
    public void windowClosing(java.awt.event.WindowEvent event) {
      Object object = event.getSource();
      if (object == AboutDialog.this)
        AboutDialog_WindowClosing(event);
    }
  }

  void AboutDialog_WindowClosing(java.awt.event.WindowEvent event) {
    // to do: code goes here.

    AboutDialog_WindowClosing_Interaction1(event);
  }

  void AboutDialog_WindowClosing_Interaction1(java.awt.event.WindowEvent event) {
    try {
      this.dispose();
    } catch (java.lang.Exception e) {
    }
  }

}
