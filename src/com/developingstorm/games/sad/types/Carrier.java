package com.developingstorm.games.sad.types;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;

public class Carrier extends Unit {
  public Carrier(Player owner, Location loc, Game game) {
    super(Type.CARRIER, owner, loc, game);
  }
}
