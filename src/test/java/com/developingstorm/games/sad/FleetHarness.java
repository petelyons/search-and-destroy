package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import java.util.List;

public class FleetHarness {
    public final Game game;
    public final Player owner;
    public final Unit flagship;
    public final Unit transport;
    public final Fleet fleet;
    public final Unit screen;
    public final Player enemy;

    private FleetHarness(Game g, Player o, Unit flag, Unit tr, Fleet f,
                         Unit screen, Player enemy) {
        this.game = g; this.owner = o; this.flagship = flag;
        this.transport = tr; this.fleet = f;
        this.screen = screen; this.enemy = enemy;
    }

    public static FleetHarness withFlagshipAndTransport() {
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(FleetHarness.class, "MedMap.sdm");
        HexBoardContext ctx = new MockSaDBoardContext();
        Robot a = new Robot("A", 1);
        a.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(a));
        Robot b = new Robot("B", 2);
        b.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(b));
        Game g = new Game(new Player[] {a, b}, map, ctx);
        WaterPair pair = findWaterPair(g, 4);
        Location bbLoc = pair.flag;
        Location trLoc = pair.tr;
        Unit bb = g.createUnit(Type.BATTLESHIP, a, bbLoc);
        a.addUnit(bb);
        Unit tr = g.createUnit(Type.TRANSPORT, a, trLoc);
        a.addUnit(tr);
        Fleet f = g.getFleetRegistry().form(bb, List.of(tr));
        Player enemy = null;
        for (Player p : g.getPlayers()) if (p != a) { enemy = p; break; }
        return new FleetHarness(g, a, bb, tr, f, null, enemy);
    }

    public static FleetHarness withFlagshipAndScreen(Type screenType, int screenDistFromFlagship) {
        FleetHarness base = withFlagshipAndTransport();
        Location screenLoc = null;
        for (Location l : base.flagship.getLocation().getRing(screenDistFromFlagship)) {
            if (base.game.getBoard().isWater(l)) { screenLoc = l; break; }
        }
        if (screenLoc == null) throw new SaDException("no screen water hex");
        Unit screen = base.game.createUnit(screenType, base.owner, screenLoc);
        base.owner.addUnit(screen);
        base.game.getFleetRegistry().add(base.fleet, screen);
        return new FleetHarness(base.game, base.owner, base.flagship, base.transport,
            base.fleet, screen, base.enemy);
    }

    private static class WaterPair {
        final Location flag;
        final Location tr;
        WaterPair(Location f, Location t) { this.flag = f; this.tr = t; }
    }

    // Find two water hexes at the requested distance where a path between them exists.
    private static WaterPair findWaterPair(Game g, int distance) {
        Board board = g.getBoard();
        int w = 50, h = 50;
        for (int x1 = 0; x1 < w; x1++) {
            for (int y1 = 0; y1 < h; y1++) {
                Location a = Location.get(x1, y1);
                if (!board.isWater(a)) continue;
                for (int x2 = 0; x2 < w; x2++) {
                    for (int y2 = 0; y2 < h; y2++) {
                        Location b = Location.get(x2, y2);
                        if (!board.isWater(b)) continue;
                        if (a.distance(b) != distance) continue;
                        return new WaterPair(a, b);
                    }
                }
            }
        }
        throw new SaDException("no water pair at distance " + distance);
    }
}
