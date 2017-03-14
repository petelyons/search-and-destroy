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

  private Unit _unit = null;
  private Game _game = null;

  private JLabel _unitlabel = new JLabel();
  private JLabel _unitDesc = new JLabel();
  private JLabel _loclabel = new JLabel();
  private JLabel _location = new JLabel();
  private JLabel _statlabel = new JLabel();
  private JLabel _status = new JLabel();
  private JLabel _healthlabel = new JLabel();
  private JLabel _health = new JLabel();
  private JLabel _movelabel = new JLabel();
  private JLabel _move = new JLabel();
  private JLabel _carrieslabel = new JLabel();
  private JLabel _carries = new JLabel();
  private JLabel _turnlabel = new JLabel();
  private JLabel _turnDesc = new JLabel();
  private JLabel _spacer = new JLabel();

  public UnitStatusBar() {
    setLayout(new FlowLayout(FlowLayout.LEFT));

    _turnlabel.setText(TURN);
    add(_turnlabel);

    _turnDesc.setText("1");
    add(_turnDesc);

    _spacer.setText("  :  ");
    add(_spacer);

    _unitlabel.setText(UNIT);
    add(_unitlabel);

    _unitDesc.setText("(Unknown)");
    add(_unitDesc);
    _unitDesc.setForeground(LABEL_COLOR);

    _loclabel.setText(LOCATION);
    add(_loclabel);

    _location.setText("(unknown)");
    add(_location);
    _location.setForeground(LABEL_COLOR);

    _healthlabel.setText(HEALTH);
    add(_healthlabel);

    _health.setText("(unknown)");
    add(_health);
    _health.setForeground(LABEL_COLOR);

    _movelabel.setText(MOVE);
    add(_movelabel);

    _move.setText("(unknown)");
    add(_move);
    _move.setForeground(LABEL_COLOR);
    
    _carrieslabel.setText(CARRIES);
    add(_carrieslabel);

    _carries.setText("(none)");
    add(_carries);

    _statlabel.setText(STATUS);
    add(_statlabel);

    _status.setText("(unknown)");
    add(_status);

  }

  public void setGame(Game g) {
    _game = g;
  }

  public void setUnit(Unit u) {
    _turnDesc.setText("" + _game.getTurn());

    _unit = u;
    _unitDesc.setText(_unit.typeDesc());

    _location.setText(_unit.locationDesc());

    _health.setText(_unit.healthDesc());

    _move.setText(_unit.moveDesc());
    
    _carries.setText(_unit.carriesDesc());
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