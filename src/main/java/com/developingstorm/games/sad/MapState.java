package com.developingstorm.games.sad;

import com.developingstorm.games.astar.AStarPosition;
import com.developingstorm.games.astar.AStarState;
import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import java.util.ArrayList;
import java.util.List;

public class MapState implements AStarState {

    Location loc;

    static Board b = null;
    static Travel travel;
    static Location goal;
    static ArrayList<Unit> units;
    static Player player;
    static Game game;
    static boolean checkedBlocked;
    static boolean canExplore;

    public static void start(
        Game game,
        Board b,
        Travel travel,
        Player player,
        Location goal,
        boolean checkBlocked,
        boolean canExplore
    ) {
        MapState.game = game;
        MapState.b = b;
        MapState.travel = travel;
        MapState.goal = goal;
        MapState.units = null;
        MapState.player = player;
        MapState.checkedBlocked = checkBlocked;
        MapState.canExplore = canExplore;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.developingstorm.astar.State#key()
     */
    public AStarPosition pos() {
        return loc;
    }

    public static void start(
        Board b,
        Travel travel,
        Player player,
        Location goal,
        ArrayList<Unit> units
    ) {
        MapState.b = b;
        MapState.travel = travel;
        MapState.goal = goal;
        MapState.units = units;
        MapState.player = player;
    }

    public static MapState getUntested(Location loc) {
        return new MapState(loc);
    }

    public static MapState getTerrainTested(Location loc) {
        if (travel == Travel.SEA && (b.isWater(loc) || isPlayersCity(loc))) {
            return new MapState(loc);
        } else if (travel == Travel.LAND && (b.isLand(loc))) {
            return new MapState(loc);
        } else if (travel == Travel.AIR) {
            return new MapState(loc);
        }
        return null;
    }

    public static MapState get(Location loc) {
        if (player.isExplored(loc) || canExplore) {
            if (travel == Travel.SEA) {
                if (b.isWater(loc) || isPlayersCity(loc)) {
                    if (!isBlocked(loc)) {
                        return new MapState(loc);
                    }
                    return null;
                }
            } else if (travel == Travel.LAND) {
                if (!isBlocked(loc)) {
                    return new MapState(loc);
                }
                return null;
            } else if (!isBlocked(loc)) {
                return new MapState(loc);
            }
            return null;
        } else {
            if (canExplore) {
                return new MapState(loc);
            } else return null;
        }
    }

    private MapState(Location loc) {
        this.loc = loc;
    }

    public Location getLocation() {
        return loc;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((loc == null) ? 0 : this.loc.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MapState other = (MapState) obj;
        if (loc == null) {
            if (other.loc != null) return false;
        } else if (!this.loc.equals(other.loc)) return false;
        return true;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return this.loc.toString();
    }

    /**
     * @see com.marlinspike.astar.State#estimate(State)
     */
    public int estimate(AStarState goal) {
        MapState ms = (MapState) goal;
        return this.loc.distance(ms.loc);
    }

    public static boolean isBlocked(Location loc) {
        return isBlocked(loc, false);
    }

    public static boolean isBlocked(Location loc, boolean force) {
        if (force || checkedBlocked) {
            if (isPlayersCity(loc)) {
                return false;
            }
            if (travel != Travel.LAND && isNonPlayersCity(loc)) {
                return true;
            }
            List<Unit> list = game.unitsAtLocation(loc);
            if (!(list == null || list.isEmpty())) {
                Unit u = list.get(0);
                if (u != null && player.equals(u.getOwner())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isPlayersCity(Location loc) {
        City c = b.getCity(loc);
        if (c != null && player != null) {
            return player.ownsCity(c);
        }
        return false;
    }

    private static boolean isNonPlayersCity(Location loc) {
        City c = b.getCity(loc);
        if (c != null && player != null) {
            return !player.ownsCity(c);
        }
        return false;
    }

    /**
     * @see com.marlinspike.astar.State#successors()
     */
    public List<AStarState> successors() {
        List<BoardHex> list = b.getRing(this.loc, 1);

        ArrayList<AStarState> v = new ArrayList<AStarState>();

        for (BoardHex bh : list) {
            Location loc = bh.getLocation();

            if (player.isExplored(loc)) {
                if (travel == Travel.SEA) {
                    if (b.isWater(loc) || isPlayersCity(loc)) {
                        if (!isBlocked(loc)) v.add(new MapState(loc));
                    }
                } else if (travel == Travel.LAND) {
                    if (b.isLand(loc)) {
                        if (!isBlocked(loc)) v.add(new MapState(loc));
                    }
                } else {
                    if (!isBlocked(loc)) v.add(new MapState(loc));
                }
            } else {
                if (canExplore) {
                    v.add(new MapState(loc));
                }
            }
        }
        /*
    // Put the location closest to the goal first
    Direction dir = this.loc.direction(this.goal);
    MapState ms = new MapState(this.loc.relative(dir));
    for (x = 0; x < v.size(); x++) {
      MapState temp = (MapState) v.get(x);
      if (temp.equals(ms)) {
        if (x != 0) {
          v.remove(temp);
          v.add(0, ms);
        }
        break;
      }
    }
    */
        return v;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.developingstorm.astar.State#x()
     */
    public int x() {
        return this.loc.x;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.developingstorm.astar.State#y()
     */
    public int y() {
        return this.loc.y;
    }
}
