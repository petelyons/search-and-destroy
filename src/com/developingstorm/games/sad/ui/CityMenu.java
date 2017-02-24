package com.developingstorm.games.sad.ui;

import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Type;

/**

 * 
 */
public class CityMenu {

  private static final JMenuItem INFANTRY_SEL = new JRadioButtonMenuItem(
      "Infantry");
  private static final JMenuItem ARMOR_SEL = new JRadioButtonMenuItem("Armor");
  private static final JMenuItem FIGHTER_SEL = new JRadioButtonMenuItem(
      "Fighter");
  private static final JMenuItem BOMBER_SEL = new JRadioButtonMenuItem("Bomber");
  private static final JMenuItem CARGO_SEL = new JRadioButtonMenuItem(
      "Air Transport");
  private static final JMenuItem TRANSPORT_SEL = new JRadioButtonMenuItem(
      "Sea Transport");
  private static final JMenuItem DESTROYER_SEL = new JRadioButtonMenuItem(
      "Destroyer");
  private static final JMenuItem SUBMARINE_SEL = new JRadioButtonMenuItem(
      "Submarine");
  private static final JMenuItem CRUISER_SEL = new JRadioButtonMenuItem(
      "Cruiser");
  private static final JMenuItem BATTLESHIP_SEL = new JRadioButtonMenuItem(
      "Battleship");
  private static final JMenuItem AIRCRAFT_CARRIER_SEL = new JRadioButtonMenuItem(
      "Aircraft Carrier");

  private static final JMenuItem UNITS_SEL = new JMenuItem("Units...");
  private static final ProductionListener AL = new ProductionListener();

  static {
    INFANTRY_SEL.addActionListener(AL);
    ARMOR_SEL.addActionListener(AL);
    BOMBER_SEL.addActionListener(AL);
    CARGO_SEL.addActionListener(AL);
    FIGHTER_SEL.addActionListener(AL);
    TRANSPORT_SEL.addActionListener(AL);
    DESTROYER_SEL.addActionListener(AL);
    SUBMARINE_SEL.addActionListener(AL);
    CRUISER_SEL.addActionListener(AL);
    BATTLESHIP_SEL.addActionListener(AL);
    AIRCRAFT_CARRIER_SEL.addActionListener(AL);
    UNITS_SEL.addActionListener(AL);
  }

  public static JPopupMenu get(Game g, City c, UserCommands commander) {
    JPopupMenu cityPopup = new JPopupMenu("City");

    AL.setCity(c);
    AL.setGame(g);
    AL.setCommander(commander);

    fillMenu(cityPopup, c.isCoastal());
    return cityPopup;
  }

  public static JMenu getSubmenu(Game g, City c, UserCommands commander) {
    JMenu city = new JMenu("City");

    AL.setCity(c);
    AL.setGame(g);
    AL.setCommander(commander);

    fillMenu(city, c.isCoastal());
    return city;
  }

  private static void fillMenu(Container menu, boolean isCoastal) {

    ButtonGroup group = new ButtonGroup();

    JMenuItem menuItem = INFANTRY_SEL;
    group.add(menuItem);
    menu.add(menuItem);

    menuItem = ARMOR_SEL;
    group.add(menuItem);
    menu.add(menuItem);

    menuItem = FIGHTER_SEL;
    group.add(menuItem);
    menu.add(menuItem);

    menuItem = BOMBER_SEL;
    group.add(menuItem);
    menu.add(menuItem);

    menuItem = CARGO_SEL;
    group.add(menuItem);
    menu.add(menuItem);

    if (isCoastal) {
      menuItem = TRANSPORT_SEL;
      group.add(menuItem);
      menu.add(menuItem);

      menuItem = DESTROYER_SEL;
      group.add(menuItem);
      menu.add(menuItem);

      menuItem = SUBMARINE_SEL;
      group.add(menuItem);
      menu.add(menuItem);

      menuItem = CRUISER_SEL;
      group.add(menuItem);
      menu.add(menuItem);

      menuItem = BATTLESHIP_SEL;
      group.add(menuItem);
      menu.add(menuItem);

      menuItem = AIRCRAFT_CARRIER_SEL;
      group.add(menuItem);
      menu.add(menuItem);
    }

    if (menu instanceof JPopupMenu) {
      JPopupMenu pm = (JPopupMenu) menu;
      pm.addSeparator();
    }
    if (menu instanceof JMenu) {
      JMenu mm = (JMenu) menu;
      mm.addSeparator();
    }

    menuItem = UNITS_SEL;
    menu.add(menuItem);
  }

  static final private class ProductionListener implements ActionListener {

    City _c;
    Game _game;
    UserCommands _commander;

    ProductionListener() {

    }

    public void setGame(Game g) {
      _game = g;
    }

    public void setCity(City c) {
      _c = c;
      selectProduction(c.getProduction());
    }

    public void setCommander(UserCommands c) {
      _commander = c;
    }

    public void actionPerformed(ActionEvent event) {
      Object source = event.getSource();
      if (source == INFANTRY_SEL) {
        _c.produce(Type.INFANTRY);
      }
      if (source == ARMOR_SEL) {
        _c.produce(Type.ARMOR);
      } else if (source == FIGHTER_SEL) {
        _c.produce(Type.FIGHTER);
      } else if (source == BOMBER_SEL) {
        _c.produce(Type.BOMBER);
      } else if (source == CARGO_SEL) {
        _c.produce(Type.CARGO);
      } else if (source == TRANSPORT_SEL) {
        _c.produce(Type.TRANSPORT);
      } else if (source == DESTROYER_SEL) {
        _c.produce(Type.DESTROYER);
      } else if (source == SUBMARINE_SEL) {
        _c.produce(Type.SUBMARINE);
      } else if (source == CRUISER_SEL) {
        _c.produce(Type.CRUISER);
      } else if (source == BATTLESHIP_SEL) {
        _c.produce(Type.BATTLESHIP);
      } else if (source == AIRCRAFT_CARRIER_SEL) {
        _c.produce(Type.CARRIER);
      } else if (source == UNITS_SEL) {

        CityDialog cd = new CityDialog(SaDFrame.INSTANCE, _game, _c);
        List list = cd.show();

        if (!list.isEmpty()) {
          UserCommands specialCtx = _commander.specialContext(list);
          JPopupMenu om = OrderMenu.get(_game, list, specialCtx);

          Location loc = _c.getLocation();
          BoardHex hex = _game.getBoard().get(loc);
          Point p = hex.center();

          om.show(SaDFrame.INSTANCE.getCanvas(), p.x, p.y);
        }
      }
    }

    private void selectProduction(Type t) {
      if (t == Type.INFANTRY) {
        INFANTRY_SEL.setSelected(true);
      }
      if (t == Type.ARMOR) {
        ARMOR_SEL.setSelected(true);
      }
      if (t == Type.FIGHTER) {
        FIGHTER_SEL.setSelected(true);
      }
      if (t == Type.BOMBER) {
        BOMBER_SEL.setSelected(true);
      }
      if (t == Type.CARGO) {
        CARGO_SEL.setSelected(true);
      }
      if (t == Type.TRANSPORT) {
        TRANSPORT_SEL.setSelected(true);
      }
      if (t == Type.DESTROYER) {
        DESTROYER_SEL.setSelected(true);
      }
      if (t == Type.SUBMARINE) {
        SUBMARINE_SEL.setSelected(true);
      }
      if (t == Type.CRUISER) {
        CRUISER_SEL.setSelected(true);
      }
      if (t == Type.BATTLESHIP) {
        BATTLESHIP_SEL.setSelected(true);
      }
      if (t == Type.CARRIER) {
        AIRCRAFT_CARRIER_SEL.setSelected(true);
      }
    }

  }

}
