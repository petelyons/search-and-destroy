package com.developingstorm.games.sad.edicts;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Edict;
import com.developingstorm.games.sad.EdictType;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.RandomUtil;
import java.util.List;

public class AirPatrol extends Edict {

    public AirPatrol(Player p, City c) {
        super(p, c, EdictType.AIR_PATROL);
    }

    @Override
    public void execute(Game game) {
        List<Unit> units = unitsMatchingTravel(Travel.AIR);
        boolean assignedOrders = false;
        if (!units.isEmpty()) {
            for (Unit u : units) {
                // Only assign patrol if unit doesn't have orders (or has NONE order)
                if (
                    u.getOrder() != null &&
                    u.getOrder().getType() !=
                    com.developingstorm.games.sad.OrderType.NONE
                ) {
                    continue;
                }

                List<Location> locs = this.city.getLocation().getCircle(
                    u.life().turnAroundDist()
                );
                if (locs != null && !locs.isEmpty()) {
                    Location loc = RandomUtil.randomValue(locs);
                    if (loc != null) {
                        u.orderMove(loc);
                        Log.debug(u, "Applying air patrol to " + loc);

                        // If this unit was waiting for player orders, deselect it and queue for execution
                        if (game.selectedUnit() == u) {
                            game.deselectUnit();
                            assignedOrders = true;
                        }
                        // Push to pendingPlay so it executes the edict order immediately
                        u.getOwner().pushPendingPlay(u);
                    } else {
                        Log.warn(
                            u,
                            "Air patrol: randomValue returned null, skipping"
                        );
                    }
                } else {
                    Log.warn(
                        u,
                        "Air patrol: no valid patrol locations for turnaround distance " +
                            u.life().turnAroundDist()
                    );
                }
            }
        }
        // Wake up the game thread if we assigned orders to a unit that was waiting
        if (assignedOrders) {
            game.continueGame();
        }
    }
}
