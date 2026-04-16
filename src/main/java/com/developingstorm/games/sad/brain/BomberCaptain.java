package com.developingstorm.games.sad.brain;

import java.util.Set;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Contact;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Travel;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.types.Bomber;
import com.developingstorm.games.sad.util.Log;
import com.developingstorm.util.CollectionUtil;

public class BomberCaptain extends UnitCaptain<Bomber> {


  static final Type[] PrimaryTargetTypes = new Type[] {Type.TRANSPORT, Type.ARMOR, Type.INFANTRY};
  static final Set<Type> PrimaryTargets = CollectionUtil.create(PrimaryTargetTypes);

  static final Type[] SecondaryTargetTypes = new Type[] {Type.DESTROYER, Type.CRUISER, Type.BATTLESHIP, Type.CARRIER};
  static final Set<Type> SecondaryTargets = CollectionUtil.create(SecondaryTargetTypes);

  public BomberCaptain(General gen, Battleplan plan) {
    super(gen, plan);
  }



  @Override
  public Order plan(Bomber u) {
    Order order = planAttack(u, PrimaryTargets);
    if (order == null) {
      order = planAttack(u, SecondaryTargets);
    }
    if (order != null) {
      return order;
    }

    Location ghost = findGhostTarget(u);
    if (ghost != null) {
      Log.info(u, "Investigating ghost at " + ghost);
      return u.newMoveOrder(ghost);
    }

    return explore(u);
  }

  private Location findGhostTarget(Unit unit) {
    Player owner = unit.getOwner();
    Location bestLoc = null;
    double bestScore = -1.0;

    for (Contact c : owner.getGhostContacts()) {
      double score;
      if (c.travelType == Travel.LAND && c.identified && c.type != null) {
        score = c.type.getAttack() + 0.5;
      } else if (c.travelType == Travel.LAND) {
        score = 0.5;
      } else if (c.travelType == Travel.SEA && c.identified && c.type != null) {
        score = c.type.getAttack() + 0.5;
      } else if (c.travelType == Travel.SEA) {
        score = 0.5;
      } else {
        continue; // skip AIR ghosts
      }

      int dist = unit.getLocation().distance(c.location);
      if (dist == 0) continue;
      if (dist > unit.life().remainingFuel()) continue;

      score = score / (double) dist;
      if (score > bestScore) {
        bestScore = score;
        bestLoc = c.location;
      }
    }
    return bestLoc;
  }

}
