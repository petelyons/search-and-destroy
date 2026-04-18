package com.developingstorm.games.sad.brain;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Contact;
import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.FleetRole;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.MapState;
import com.developingstorm.games.sad.Path;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Unit;
import java.util.ArrayList;
import java.util.List;

public final class ScreenFleetBehavior {

    private ScreenFleetBehavior() {}

    public static Location chooseMove(Unit screen, Fleet fleet, Game game) {
        Unit flagship = fleet.getFlagship();
        if (flagship == null) return null;

        Location intercept = pickInterceptTarget(screen, fleet, game);
        if (intercept != null) return stepToward(screen, intercept);

        Location interpose = pickInterposeTarget(screen, fleet, game);
        if (interpose != null) return stepToward(screen, interpose);

        return stepToward(screen, flagship.getLocation());
    }

    private static Location pickInterceptTarget(Unit screen, Fleet fleet, Game game) {
        int r = screen.getType().getDist();
        List<Unit> protectees = new ArrayList<>();
        protectees.add(fleet.getFlagship());
        for (var e : fleet.membersView().entrySet()) {
            if (e.getValue() == FleetRole.TRANSPORT) protectees.add(e.getKey());
        }
        Player owner = fleet.getOwner();
        Location best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Contact c : owner.getAllContacts()) {
            if (!c.isCurrent(game.getTurn())) continue;
            for (Unit p : protectees) {
                int d = c.location.distance(p.getLocation());
                if (d <= r && d < bestDist) {
                    bestDist = d;
                    best = c.location;
                }
            }
        }
        return best;
    }

    private static Location pickInterposeTarget(Unit screen, Fleet fleet, Game game) {
        Unit flagship = fleet.getFlagship();
        int ring = screen.getType().getVisionDistance();
        Player owner = fleet.getOwner();
        Contact nearest = null;
        int best = Integer.MAX_VALUE;
        for (Contact c : owner.getAllContacts()) {
            if (!c.isCurrent(game.getTurn())) continue;
            int d = c.location.distance(flagship.getLocation());
            if (d < best) { best = d; nearest = c; }
        }
        if (nearest == null) return null;
        List<Location> candidates = flagship.getLocation().getRing(ring);
        Location chosen = null;
        int bestScore = Integer.MAX_VALUE;
        for (Location l : candidates) {
            if (MapState.isBlocked(l, true)) continue;
            int score = l.distance(nearest.location);
            if (score < bestScore) { bestScore = score; chosen = l; }
        }
        return chosen;
    }

    private static Location stepToward(Unit screen, Location target) {
        if (target == null) return null;
        if (screen.getLocation().equals(target)) return target;
        Path p = screen.getPath(target);
        if (p != null && !p.isEmpty()) {
            Location next = p.next(screen.getLocation());
            if (next != null) return next;
        }
        // Target may be blocked (e.g. enemy-occupied); fall back to the neighbor closest to target.
        Location best = null;
        int bestDist = screen.getLocation().distance(target);
        for (Location n : screen.getLocation().getRing(1)) {
            if (MapState.isBlocked(n, true)) continue;
            if (!screen.canTravel(n)) continue;
            int d = n.distance(target);
            if (d < bestDist) { bestDist = d; best = n; }
        }
        return best;
    }
}
