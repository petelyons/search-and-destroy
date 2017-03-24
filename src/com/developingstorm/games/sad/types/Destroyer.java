package com.developingstorm.games.sad.types;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;

public class Destroyer extends Unit {
  public Destroyer(Player owner, Location loc, Game game) {
    super(Type.DESTROYER, owner, loc, game);
  }
}
