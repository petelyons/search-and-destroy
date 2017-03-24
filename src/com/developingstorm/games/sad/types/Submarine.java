package com.developingstorm.games.sad.types;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;

public class Submarine extends Unit {
  public Submarine(Player owner, Location loc, Game game) {
    super(Type.SUBMARINE, owner, loc, game);
  }
}
