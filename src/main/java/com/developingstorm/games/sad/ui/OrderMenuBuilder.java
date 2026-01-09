package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.controls.GameCommander;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**

 *
 */
public class OrderMenuBuilder {

    private final JMenuItem ACTIVATE_SEL = new JMenuItem("Clear");
    private final JMenuItem SENTRY_SEL = new JMenuItem("Sentry/Load");
    private final JMenuItem MOVE_SEL = new JMenuItem("Move");
    private final JMenuItem EXPLORE_SEL = new JMenuItem("Explore");
    private final JMenuItem UNLOAD_SEL = new JMenuItem("Unload");
    private final JMenuItem HEAD_HOME_SEL = new JMenuItem("Head Home");

    private List<Unit> units;
    private Game game;
    private GameCommander commander;
    private SaDFrame frame;

    public OrderMenuBuilder(
        SaDFrame frame,
        Game g,
        List<Unit> units,
        GameCommander commander
    ) {
        this.frame = frame;
        this.units = units;
        this.game = g;
        this.commander = commander;

        ACTIVATE_SEL.addActionListener(e -> this.commander.activate(null));
        SENTRY_SEL.addActionListener(e -> this.commander.sentry());
        MOVE_SEL.addActionListener(e -> this.commander.moveBegin());
        UNLOAD_SEL.addActionListener(e -> this.commander.unload());
        EXPLORE_SEL.addActionListener(e -> this.commander.explore());
        HEAD_HOME_SEL.addActionListener(e -> this.commander.headHome());
    }

    public JPopupMenu build() {
        JPopupMenu ordersPopup = new JPopupMenu("Orders");

        JMenuItem menuItem = ACTIVATE_SEL;
        ordersPopup.add(menuItem);

        menuItem = SENTRY_SEL;
        ordersPopup.add(menuItem);

        menuItem = UNLOAD_SEL;
        ordersPopup.add(menuItem);

        menuItem = MOVE_SEL;
        ordersPopup.add(menuItem);

        menuItem = EXPLORE_SEL;
        ordersPopup.add(menuItem);

        menuItem = HEAD_HOME_SEL;
        ordersPopup.add(menuItem);

        if (this.units.size() == 1) {
            Unit u = (Unit) this.units.get(0);
            City c = this.game.cityAtLocation(u.getLocation());
            if (c != null) {
                CityMenuBuilder cm = new CityMenuBuilder(
                    this.frame,
                    this.game,
                    c,
                    this.commander
                );
                ordersPopup.addSeparator();
                ordersPopup.add(cm.getSubmenu());
            }
        }

        return ordersPopup;
    }
}
