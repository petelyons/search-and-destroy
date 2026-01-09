package com.developingstorm.games.sad.ui;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.UnitStats;
import com.developingstorm.games.sad.ui.controls.GameCommander;
import java.awt.Container;
import java.awt.Point;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**

 *
 */
public class CityMenuBuilder {

    private City _c;
    private Game _game;
    private GameCommander _commander;

    private final JMenuItem INFANTRY_SEL = new JRadioButtonMenuItem("Infantry");
    private final JMenuItem ARMOR_SEL = new JRadioButtonMenuItem("Armor");
    private final JMenuItem FIGHTER_SEL = new JRadioButtonMenuItem("Fighter");
    private final JMenuItem BOMBER_SEL = new JRadioButtonMenuItem("Bomber");
    private final JMenuItem CARGO_SEL = new JRadioButtonMenuItem(
        "Air Transport"
    );
    private final JMenuItem TRANSPORT_SEL = new JRadioButtonMenuItem(
        "Sea Transport"
    );
    private final JMenuItem DESTROYER_SEL = new JRadioButtonMenuItem(
        "Destroyer"
    );
    private final JMenuItem SUBMARINE_SEL = new JRadioButtonMenuItem(
        "Submarine"
    );
    private final JMenuItem CRUISER_SEL = new JRadioButtonMenuItem("Cruiser");
    private final JMenuItem BATTLESHIP_SEL = new JRadioButtonMenuItem(
        "Battleship"
    );
    private final JMenuItem AIRCRAFT_CARRIER_SEL = new JRadioButtonMenuItem(
        "Aircraft Carrier"
    );

    private final JMenuItem SEND_SEA_SEL = new JMenuItem("Set Sea Path...");
    private final JMenuItem SEND_AIR_SEL = new JMenuItem("Set Air Path...");
    private final JMenuItem SEND_LAND_SEL = new JMenuItem("Set Land Path...");
    private final JMenuItem CLEAR_SEA_SEL = new JMenuItem("Cancel Sea Path");
    private final JMenuItem CLEAR_AIR_SEL = new JMenuItem("Cancel Air Path");
    private final JMenuItem CLEAR_LAND_SEL = new JMenuItem("Cancel Land Path");

    private final JCheckBoxMenuItem AIR_PATROL_SEL = new JCheckBoxMenuItem(
        "Air Patrol"
    );
    private final JCheckBoxMenuItem AUTO_SENTRY_SEL = new JCheckBoxMenuItem(
        "Automatic Sentry"
    );

    private final JMenuItem UNITS_SEL = new JMenuItem("Units...");

    private final SaDFrame _frame;

    private String formatName(Type type, UnitStats stats) {
        StringBuffer sb = new StringBuffer();
        String name = type.getName();
        sb.append(name);
        sb.append(" (");
        sb.append(stats.getCount(type));
        sb.append('/');
        sb.append(stats.getProduction(type));
        sb.append(')');
        return sb.toString();
    }

    public CityMenuBuilder(
        SaDFrame frame,
        Game g,
        City c,
        GameCommander commander
    ) {
        _frame = frame;
        _game = g;
        _c = c;
        _commander = commander;

        UnitStats stats = c.getOwner().unitStats();

        selectProduction(c.getProduction());
        INFANTRY_SEL.setText(formatName(Type.INFANTRY, stats));
        INFANTRY_SEL.addActionListener(e -> _c.produce(Type.INFANTRY));

        ARMOR_SEL.setText(formatName(Type.ARMOR, stats));
        ARMOR_SEL.addActionListener(e -> _c.produce(Type.ARMOR));
        BOMBER_SEL.setText(formatName(Type.BOMBER, stats));
        BOMBER_SEL.addActionListener(e -> _c.produce(Type.BOMBER));
        CARGO_SEL.setText(formatName(Type.CARGO, stats));
        CARGO_SEL.addActionListener(e -> _c.produce(Type.CARGO));
        FIGHTER_SEL.setText(formatName(Type.FIGHTER, stats));
        FIGHTER_SEL.addActionListener(e -> _c.produce(Type.FIGHTER));
        TRANSPORT_SEL.setText(formatName(Type.TRANSPORT, stats));
        TRANSPORT_SEL.addActionListener(e -> _c.produce(Type.TRANSPORT));
        DESTROYER_SEL.setText(formatName(Type.DESTROYER, stats));
        DESTROYER_SEL.addActionListener(e -> _c.produce(Type.DESTROYER));
        SUBMARINE_SEL.setText(formatName(Type.SUBMARINE, stats));
        SUBMARINE_SEL.addActionListener(e -> _c.produce(Type.SUBMARINE));
        CRUISER_SEL.setText(formatName(Type.CRUISER, stats));
        CRUISER_SEL.addActionListener(e -> _c.produce(Type.CRUISER));
        BATTLESHIP_SEL.setText(formatName(Type.BATTLESHIP, stats));
        BATTLESHIP_SEL.addActionListener(e -> _c.produce(Type.BATTLESHIP));
        AIRCRAFT_CARRIER_SEL.setText(formatName(Type.CARRIER, stats));
        AIRCRAFT_CARRIER_SEL.addActionListener(e -> _c.produce(Type.CARRIER));
        UNITS_SEL.addActionListener(e -> {
            CityDialog cd = new CityDialog(frame, _game, _c);
            List<Unit> list = cd.show();

            if (!list.isEmpty()) {
                GameCommander specialCtx =
                    _commander.commanderForSpecifiedUnits(list);

                OrderMenuBuilder orderMenu = new OrderMenuBuilder(
                    frame,
                    _game,
                    list,
                    specialCtx
                );

                JPopupMenu om = orderMenu.build();

                Location loc = _c.getLocation();
                BoardHex hex = _game.getBoard().get(loc);
                Point p = hex.center();

                om.show(frame.getCanvas(), p.x, p.y);
            }
        });

        SEND_SEA_SEL.addActionListener(e -> _commander.setSeaPath(_c));

        SEND_AIR_SEL.addActionListener(e -> _commander.setAirPath(_c));
        SEND_LAND_SEL.addActionListener(e -> _commander.setLandPath(_c));

        CLEAR_SEA_SEL.addActionListener(e -> {
            // TODO Move to Commander - all the postGameAction calls
            _game.postAndRunGameAction(() -> _c.getGovernor().clearSeaPath());
        });

        CLEAR_AIR_SEL.addActionListener(e -> {
            _game.postAndRunGameAction(() -> _c.getGovernor().clearAirPath());
        });
        CLEAR_LAND_SEL.addActionListener(e -> {
            _game.postAndRunGameAction(() -> _c.getGovernor().clearLandPath());
        });

        AIR_PATROL_SEL.setSelected(_c.getGovernor().hasAirPatrol());
        AIR_PATROL_SEL.addActionListener(e -> {
            _game.postAndRunGameAction(() -> {
                if (_c.getGovernor().hasAirPatrol()) {
                    _c.getGovernor().clearAirPatrol();
                } else {
                    _c.getGovernor().setAirPatrol();
                }
            });
        });
        AUTO_SENTRY_SEL.setSelected(_c.getGovernor().hasAutoSentry());
        AUTO_SENTRY_SEL.addActionListener(e -> {
            _game.postAndRunGameAction(() -> {
                if (_c.getGovernor().hasAutoSentry()) {
                    _c.getGovernor().clearAutoSenty();
                } else {
                    _c.getGovernor().setAutoSentry();
                }
            });
        });
    }

    public JPopupMenu build() {
        JPopupMenu cityPopup = new JPopupMenu("City");

        fillMenu(_game, cityPopup, _c);
        return cityPopup;
    }

    public JMenu getSubmenu() {
        JMenu cityMenu = new JMenu("City");

        fillMenu(_game, cityMenu, _c);
        return cityMenu;
    }

    private static void addSep(Container menu) {
        if (menu instanceof JPopupMenu) {
            JPopupMenu pm = (JPopupMenu) menu;
            pm.addSeparator();
        }
        if (menu instanceof JMenu) {
            JMenu mm = (JMenu) menu;
            mm.addSeparator();
        }
    }

    private void fillMenu(Game g, Container menu, City c) {
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

        if (c.isCoastal()) {
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

        addSep(menu);

        menuItem = _c.getGovernor().hastAirPath()
            ? CLEAR_AIR_SEL
            : SEND_AIR_SEL;
        menu.add(menuItem);

        menuItem = _c.getGovernor().hasLandPath()
            ? CLEAR_LAND_SEL
            : SEND_LAND_SEL;
        menu.add(menuItem);

        if (c.isCoastal()) {
            menuItem = _c.getGovernor().hasSeaPath()
                ? CLEAR_SEA_SEL
                : SEND_SEA_SEL;
            menu.add(menuItem);
        }

        menuItem = AIR_PATROL_SEL;
        menu.add(menuItem);

        menuItem = AUTO_SENTRY_SEL;
        menu.add(menuItem);

        addSep(menu);

        menuItem = UNITS_SEL;
        menu.add(menuItem);
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
