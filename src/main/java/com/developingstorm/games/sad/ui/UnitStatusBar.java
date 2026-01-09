package com.developingstorm.games.sad.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;

public class UnitStatusBar extends JComponent {

  private static final String TURN = "Turn:";
  private static final String UNIT = "Unit:";
  private static final String LOCATION = "Location:";
  private static final String STATUS = "Status:";
  private static final String HEALTH = "Health:";
  private static final String MOVE = "Moved:";
  private static final String CARRIES = "Carries:";
  private static final Color LABEL_COLOR = new Color(20, 0, 100);

  private Unit unit = null;
  private Game game = null;

  private JLabel unitlabel = new JLabel();
  private JLabel unitDesc = new JLabel();
  private JLabel loclabel = new JLabel();
  private JLabel location = new JLabel();
  private JLabel statlabel = new JLabel();
  private JLabel status = new JLabel();
  private JLabel healthlabel = new JLabel();
  private JLabel health = new JLabel();
  private JLabel movelabel = new JLabel();
  private JLabel move = new JLabel();
  private JLabel carrieslabel = new JLabel();
  private JLabel carries = new JLabel();
  private JLabel turnlabel = new JLabel();
  private JLabel turnDesc = new JLabel();
  private JLabel spacer = new JLabel();

  public UnitStatusBar() {
    setLayout(new FlowLayout(FlowLayout.LEFT));

    this.turnlabel.setText(TURN);
    add(this.turnlabel);

    this.turnDesc.setText("1");
    add(this.turnDesc);

    this.spacer.setText("  :  ");
    add(this.spacer);

    this.unitlabel.setText(UNIT);
    add(this.unitlabel);

    this.unitDesc.setText("(Unknown)");
    add(this.unitDesc);
    this.unitDesc.setForeground(LABEL_COLOR);

    this.loclabel.setText(LOCATION);
    add(this.loclabel);

    this.location.setText("(unknown)");
    add(this.location);
    this.location.setForeground(LABEL_COLOR);

    this.healthlabel.setText(HEALTH);
    add(this.healthlabel);

    this.health.setText("(unknown)");
    add(this.health);
    this.health.setForeground(LABEL_COLOR);

    this.movelabel.setText(MOVE);
    add(this.movelabel);

    this.move.setText("(unknown)");
    add(this.move);
    this.move.setForeground(LABEL_COLOR);
    
    this.carrieslabel.setText(CARRIES);
    add(this.carrieslabel);

    this.carries.setText("(none)");
    add(this.carries);

    this.statlabel.setText(STATUS);
    add(this.statlabel);

    this.status.setText("(unknown)");
    add(this.status);

  }

  public void setGame(Game g) {
    game = g;
  }

  public void setUnit(Unit u) {
    this.turnDesc.setText("" + this.game.getTurn());

    unit = u;
    this.unitDesc.setText(this.unit.typeDesc());

    this.location.setText(this.unit.locationDesc());

    this.health.setText(this.unit.life().healthDesc());

    this.move.setText(this.unit.life().moveDesc());
    
    this.carries.setText(this.unit.carriesDesc());
    validate();
    //repaint();
  }

  public Dimension preferredSize() {
    return new Dimension(300, 22);
  }

  public Dimension getMinimumSize() {
    return new Dimension(100, 22);
  }

}