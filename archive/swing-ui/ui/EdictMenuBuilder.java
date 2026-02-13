package com.developingstorm.games.sad.ui;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.controls.GameCommander;
import java.awt.Container;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**

 *
 */
public class EdictMenuBuilder {

    private City c;
    private Game game;
    private GameCommander commander;

    private final JMenuItem SEND_SEA_SEL = new JMenuItem("Send Sea Units..");
    private final JMenuItem SEND_AIR_SEL = new JMenuItem("Send Air Units...");
    private final JMenuItem SEND_LAND_SEL = new JMenuItem("Send Land Units...");
    private final JCheckBoxMenuItem AIR_PATROL_SEL = new JCheckBoxMenuItem(
        "Air Patrol"
    );
    private final JCheckBoxMenuItem AUTO_SENTRY_SEL = new JCheckBoxMenuItem(
        "Automatic Sentry"
    );

    public EdictMenuBuilder(
        SaDFrame frame,
        Game g,
        City c,
        GameCommander commander
    ) {
        game = g;
        c = c;
        commander = commander;

        select();
        SEND_SEA_SEL.addActionListener(e -> this.commander.setSeaPath(this.c));

        SEND_AIR_SEL.addActionListener(e -> this.commander.setAirPath(this.c));
        SEND_LAND_SEL.addActionListener(e -> this.commander.setLandPath(this.c));
        AIR_PATROL_SEL.addActionListener(e -> this.commander.setAirPatrol(this.c));
        AUTO_SENTRY_SEL.addActionListener(e -> this.commander.setAutoSentry(this.c));
    }

    public JPopupMenu build() {
        JPopupMenu edictPopup = new JPopupMenu("Edicts");

        fillMenu(this.game, edictPopup, this.c);
        return edictPopup;
    }

    public JMenu getSubmenu() {
        JMenu edictMenu = new JMenu("Edicts");

        fillMenu(this.game, edictMenu, this.c);
        return edictMenu;
    }

    private void fillMenu(Game g, Container menu, City c) {
        JMenuItem menuItem = SEND_AIR_SEL;
        menu.add(menuItem);

        menuItem = SEND_LAND_SEL;
        menu.add(menuItem);

        if (c.isCoastal()) {
            menuItem = SEND_SEA_SEL;
            menu.add(menuItem);
        }

        menuItem = AIR_PATROL_SEL;
        menu.add(menuItem);

        menuItem = AUTO_SENTRY_SEL;
        menu.add(menuItem);
    }

    private void select() {
        if (this.c.getGovernor().hasAirPatrol()) {
            AIR_PATROL_SEL.setSelected(true);
        }
        if (this.c.getGovernor().hasAutoSentry()) {
            AUTO_SENTRY_SEL.setSelected(true);
        }
    }
}
