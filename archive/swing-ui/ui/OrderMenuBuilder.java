package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.ui.controls.GameCommander;
import com.developingstorm.games.sad.util.Log;
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
    private final JMenuItem PATROL_SEL = new JMenuItem("Define Patrol...");
    private final JMenuItem ATTACK_SEL = new JMenuItem("Bombard...");
    private final JMenuItem ESCORT_SEL = new JMenuItem("Escort...");

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
        PATROL_SEL.addActionListener(e -> {
            if (this.units.size() == 1) {
                this.frame.startPatrolMode(this.units.get(0));
            }
        });
        ATTACK_SEL.addActionListener(e -> {
            if (this.units.size() == 1) {
                this.frame.startAttackMode(this.units.get(0));
            }
        });
        ESCORT_SEL.addActionListener(e -> {
            if (this.units.size() == 1) {
                this.frame.startEscortMode(this.units.get(0));
            }
        });
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

        menuItem = PATROL_SEL;
        ordersPopup.add(menuItem);

        // Add bombardment option for battleships and cruisers
        if (this.units.size() == 1) {
            Unit u = this.units.get(0);
            if (u.isBattleship() || u.isCruiser()) {
                menuItem = ATTACK_SEL;
                ordersPopup.add(menuItem);
            }

            // Add escort option for sea units
            if (u.getTravel() == com.developingstorm.games.sad.Travel.SEA) {
                menuItem = ESCORT_SEL;
                ordersPopup.add(menuItem);
            }
        }

        if (this.units.size() == 1) {
            Unit u = (Unit) this.units.get(0);
            City c = this.game.cityAtLocation(u.getLocation());
            Log.debug(
                "Checking for city at unit location: " +
                    u.getLocation() +
                    ", city=" +
                    c
            );
            if (c != null) {
                Log.debug("Adding city submenu for city: " + c.getName());
                CityMenuBuilder cm = new CityMenuBuilder(
                    this.frame,
                    this.game,
                    c,
                    this.commander
                );
                ordersPopup.addSeparator();
                ordersPopup.add(cm.getSubmenu());
            } else {
                Log.debug(
                    "No city found at location or city not owned by current player"
                );
            }
        }

        return ordersPopup;
    }
}
