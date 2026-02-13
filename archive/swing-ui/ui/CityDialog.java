package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**

 *
 */
public class CityDialog {

    private Game game;
    private City city;
    private Component comp;

    CityDialog(Component c, Game game, City city) {
        this.game = game;
        this.city = city;
        this.comp = c;
    }

    public List<Unit> show() {
        long start = System.currentTimeMillis();
        List<Unit> units = this.game.unitsAtLocation(this.city.getLocation());
        String[] values = new String[units.size()];
        int x = 0;

        HashMap<String, Unit> map = new HashMap<String, Unit>();
        for (Unit u : units) {
            values[x] = u.toUIString();
            map.put(values[x], u);
            x++;
        }
        long afterPrep = System.currentTimeMillis();
        System.out.println(
            "  CityDialog: Data preparation took: " + (afterPrep - start) + "ms"
        );

        long beforeInit = System.currentTimeMillis();
        GenericListDialog.initialize(
            this.comp,
            values,
            "Units",
            "Issue Orders to Unit(s)"
        );
        long afterInit = System.currentTimeMillis();
        System.out.println(
            "  CityDialog: GenericListDialog.initialize took: " +
                (afterInit - beforeInit) +
                "ms"
        );

        long beforeShow = System.currentTimeMillis();
        Object[] vals = GenericListDialog.showDialog(this.comp, "");
        long afterShow = System.currentTimeMillis();
        System.out.println(
            "  CityDialog: GenericListDialog.showDialog (includes user interaction) took: " +
                (afterShow - beforeShow) +
                "ms"
        );

        long beforeMapping = System.currentTimeMillis();
        ArrayList<Unit> list = new ArrayList<Unit>(vals.length);
        for (x = 0; x < vals.length; x++) {
            Log.debug("Looking up:" + vals[x]);
            list.add(map.get(vals[x]));
        }
        long afterMapping = System.currentTimeMillis();
        System.out.println(
            "  CityDialog: Result mapping took: " +
                (afterMapping - beforeMapping) +
                "ms"
        );

        return list;
    }
}
