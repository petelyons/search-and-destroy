package com.developingstorm.games.sad.ui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

/**

 * 
 */
public class CityDialog {

  private Game _game;
  private City _city;
  private Component _comp;

  CityDialog(Component c, Game game, City city) {
    _game = game;
    _city = city;
    _comp = c;
  }

  public List<Unit> show() {

    List<Unit> units = _game.unitsAtLocation(_city.getLocation());
    String[] values = new String[units.size()];
    int x = 0;

    HashMap<String, Unit> map = new HashMap<String, Unit>();
    for(Unit u : units) {
      values[x] = u.toUIString();
      map.put(values[x], u);
      x++;
    }

    GenericListDialog.initialize(_comp, values, "Units",
        "Issue Orders to Unit(s)");

    Object[] vals = GenericListDialog.showDialog(_comp, "");
    ArrayList<Unit> list = new ArrayList<Unit>(vals.length);
    for (x = 0; x < vals.length; x++) {
      Log.debug("Looking up:" + vals[x]);
      list.add(map.get(vals[x]));
    }
    return list;
  }

}
