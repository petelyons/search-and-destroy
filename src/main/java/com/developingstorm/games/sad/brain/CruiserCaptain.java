package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Contact;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Cruiser;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.CollectionUtil;

public class CruiserCaptain  extends UnitCaptain<Cruiser> {


  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.DESTROYER, Type.SUBMARINE};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);


  static final Type[] SecondaryTargetTypes = new Type[] {Type.CRUISER, Type.BOMBER, Type.FIGHTER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);



  public CruiserCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }


  @Override
  public Order plan(Cruiser u) {
    // No live naval target — investigate sea ghost contacts if we have any
    Location ghost = findGhostTarget(u);
    if (ghost != null) {
      Log.info(u, "Investigating sea ghost at " + ghost);
      return u.newMoveOrder(ghost);
    }

    return attackShipStrategy(u, PrimaryTargets, SecondaryTargets);
  }

  /**
   * Returns the location of the best sea-ghost contact this captain should
   * investigate, or null if no useful ghost exists. Prefers identified
   * ghosts of higher-value targets; falls back to unidentified SEA ghosts.
   */
  private Location findGhostTarget(Unit unit) {
    Player owner = unit.getOwner();

    Location bestLoc = null;
    double bestScore = -1.0;

    for (Contact c : owner.getGhostContacts()) {
      if (c.travelType != Travel.SEA) continue;

      double score;
      if (c.identified && c.type != null) {
        score = c.type.getAttack() + 0.5;
      } else {
        score = 0.5;
      }

      int dist = unit.getLocation().distance(c.location);
      if (dist == 0) continue;
      score = score / (double) dist;

      if (score > bestScore) {
        bestScore = score;
        bestLoc = c.location;
      }
    }
    return bestLoc;
  }

}
