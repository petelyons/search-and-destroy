package com.developingstorm.games.sad.testing;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Board;
import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.CombatResult;
import com.developingstorm.games.sad.Continent;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.events.CombatResolvedEvent;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.TurnCompleteEvent;
import com.developingstorm.games.sad.events.UnitKilledEvent;
import com.developingstorm.games.sad.orders.Move;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feeds a {@link BehaviorTracker} from a live game: snapshots a player's units at the end of
 * each of its turns, and forwards kills and combats.
 */
public class AIBehaviorMonitor implements GameEventListener {

    private final Game game;
    private final BehaviorTracker tracker = new BehaviorTracker();

    public AIBehaviorMonitor(Game game) {
        this.game = game;
    }

    public BehaviorReport report() {
        return this.tracker.report();
    }

    @Override
    public GameEventType[] getInterestedEventTypes() {
        return new GameEventType[] {
            GameEventType.TURN_ENDED,
            GameEventType.UNIT_KILLED,
            GameEventType.COMBAT_RESOLVED,
        };
    }

    @Override
    public void onGameEvent(GameEvent event) {
        if (event instanceof TurnCompleteEvent tce) {
            snapshot(tce.getPlayer());
        } else if (event instanceof UnitKilledEvent uke) {
            Unit u = uke.getUnit();
            boolean outOfFuel = u.getTravel() == Travel.AIR && !u.life().hasFuel();
            this.tracker.onUnitKilled(u.getOwner().getId(), u.id, u.getTravel(), outOfFuel);
        } else if (event instanceof CombatResolvedEvent cre) {
            CombatResult r = cre.getResult();
            if (r != null && r.getAttackerOwner() != null && r.getAttackerTypeEnum() != null) {
                this.tracker.onCombat(r.getAttackerOwner().getId(), r.getAttackerTypeEnum().getTravel());
            }
        }
    }

    private void snapshot(Player player) {
        Board board = this.game.getBoard();
        List<UnitSnapshot> snaps = new ArrayList<>();
        for (Unit u : new ArrayList<>(player.getUnits())) {
            Location loc = u.getLocation();
            boolean transport = u.isTransport();
            boolean idleExempt = u.isCarried() || u.inSentryMode() || (transport && board.isCity(loc));
            Location dest = (u.getOrder() instanceof Move m) ? m.getDestination() : null;
            int cargo = u.carries == null ? 0 : u.carries.size();
            snaps.add(new UnitSnapshot(
                u.id, u.getTravel(), loc, u.isCarried(), idleExempt, transport, cargo,
                isOpenSea(board, loc), dest, landContinentId(board, loc)
            ));
        }

        Map<Location, Integer> owned = new HashMap<>();
        for (City c : player.getCities()) {
            Continent cont = c.getContinent();
            owned.put(c.getLocation(), cont == null ? -1 : cont.getId());
        }
        this.tracker.onTurnEnd(player.getId(), snaps, owned);
    }

    private static boolean isOpenSea(Board board, Location loc) {
        if (!board.isWater(loc)) {
            return false;
        }
        for (Location n : loc.getRing(1)) {
            if (board.isLand(n)) {
                return false;
            }
        }
        return true;
    }

    private static int landContinentId(Board board, Location loc) {
        if (!board.isLand(loc)) {
            return -1;
        }
        Continent cont = board.getContinent(loc);
        return cont == null ? -1 : cont.getId();
    }
}
