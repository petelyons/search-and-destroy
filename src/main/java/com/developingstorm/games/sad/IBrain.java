package com.developingstorm.games.sad;

public interface IBrain {
  void startNewTurn();
  Order getOrders(Unit u);
  Type getProduction(City c);
}
