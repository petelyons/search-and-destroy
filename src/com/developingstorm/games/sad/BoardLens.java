package com.developingstorm.games.sad;

import com.developingstorm.games.hexboard.HexBoardLens;
import com.developingstorm.games.hexboard.Location;

/**
 * Class information
 */
public interface BoardLens extends HexBoardLens {

  public Unit visibleUnit(Location loc);

}
