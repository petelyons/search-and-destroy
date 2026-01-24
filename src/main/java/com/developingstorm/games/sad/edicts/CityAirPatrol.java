package com.developingstorm.games.sad.edicts;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Edict;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.SaDException;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.orders.Patrol;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.games.sad.util.json.JsonObj;
import java.util.ArrayList;
import java.util.List;

/**
 * City-level air patrol edict that automatically assigns patrol routes to fighters and bombers.
 * When a fighter or bomber is produced at or lands at the city, they are automatically assigned
 * to patrol the defined waypoints.
 */
public class CityAirPatrol extends Edict {

    private final List<Location> waypoints;
    private final Patrol.PatrolMode mode;

    public CityAirPatrol(
        Player player,
        City city,
        List<Location> waypoints,
        Patrol.PatrolMode mode
    ) {
        super(player, city, EdictType.CITY_AIR_PATROL);
        this.waypoints = new ArrayList<>(waypoints);
        this.mode = mode;
        validateAirPatrol();
    }

    private void validateAirPatrol() {
        if (waypoints.size() < 2) {
            throw new SaDException("Air patrol requires at least 2 waypoints");
        }

        Location first = waypoints.get(0);
        Location last = waypoints.get(waypoints.size() - 1);

        City firstCity = city.getGame().getBoard().getCity(first);
        City lastCity = city.getGame().getBoard().getCity(last);

        if (mode == Patrol.PatrolMode.LOOP) {
            // LOOP: first and last waypoints must be the same city
            if (!first.equals(last)) {
                throw new SaDException(
                    "Air patrol loop must start and end at the same city"
                );
            }
            if (firstCity == null) {
                throw new SaDException(
                    "Air patrol loop waypoint must be a city"
                );
            }
        } else {
            // LINEAR: both endpoints must be cities
            if (firstCity == null || lastCity == null) {
                throw new SaDException(
                    "Air patrol linear mode requires both endpoints to be cities"
                );
            }
        }
    }

    @Override
    public void execute(Game game) {
        // Get all air units at this city
        List<Unit> airUnits = unitsMatchingTravel(Travel.AIR);
        boolean assignedOrders = false;

        for (Unit u : airUnits) {
            // Only assign to combat air units (Fighter, Bomber)
            if (u.getType() == Type.CARGO) {
                continue;
            }

            // Only assign if unit doesn't have orders (or has NONE order)
            if (
                u.getOrder() != null && u.getOrder().getType() != OrderType.NONE
            ) {
                continue;
            }

            // Create patrol order with city's waypoints
            Patrol patrol = new Patrol(game, u, waypoints, mode);
            u.assignOrder(patrol);

            Log.info(
                u,
                "City air patrol assigned from " +
                    city.getName() +
                    ": " +
                    waypoints.size() +
                    " waypoints, mode=" +
                    mode
            );

            // If this unit was waiting for player orders, deselect it and queue for execution
            if (game.selectedUnit() == u) {
                game.deselectUnit();
                assignedOrders = true;
            }
            // Push to pendingPlay so it executes the edict order immediately
            u.getOwner().pushPendingPlay(u);
        }
        // Wake up the game thread if we assigned orders to a unit that was waiting
        if (assignedOrders) {
            game.continueGame();
        }
    }

    public List<Location> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public Patrol.PatrolMode getMode() {
        return mode;
    }

    @Override
    public JsonObj toJson() {
        JsonObj json = super.toJson();
        json.put("mode", mode.name());

        Object[] waypointsArray = new Object[waypoints.size()];
        for (int i = 0; i < waypoints.size(); i++) {
            JsonObj wpJson = new JsonObj();
            wpJson.put("x", waypoints.get(i).getX());
            wpJson.put("y", waypoints.get(i).getY());
            waypointsArray[i] = wpJson;
        }
        json.put("waypoints", waypointsArray);

        return json;
    }

    public static CityAirPatrol fromJson(City city, JsonObj json) {
        Patrol.PatrolMode mode = Patrol.PatrolMode.valueOf(
            json.getString("mode")
        );
        Object[] waypointsArray = json.getArray("waypoints");
        List<Location> waypoints = new ArrayList<>();

        for (Object wpObj : waypointsArray) {
            JsonObj wpJson = (JsonObj) wpObj;
            int x = wpJson.getInteger("x");
            int y = wpJson.getInteger("y");
            waypoints.add(Location.get(x, y));
        }

        return new CityAirPatrol(city.getOwner(), city, waypoints, mode);
    }
}
