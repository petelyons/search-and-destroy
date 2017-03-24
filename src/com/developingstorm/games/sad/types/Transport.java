package com.developingstorm.games.sad.types;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;

public class Transport extends Unit {
  public Transport(Player owner, Location loc, Game game) {
    super(Type.TRANSPORT, owner, loc, game);
  }
}
