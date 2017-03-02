package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.Location;

/**
 * Is their a visible unit at the location
 */
public interface UnitLens  {

  public Unit visibleUnit(Location loc);

}
