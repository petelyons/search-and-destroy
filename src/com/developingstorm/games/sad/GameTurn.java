package com.developingstorm.games.sad;

import java.util.List;
import com.developingstorm.games.sad.util.Log;

/**
 * 
 * 
 *
 */
class GameTurn {

  private final long _turn;
  private final Player _player;
  private final Game _game;
  private TurnState _turnState;
  private boolean _unitKilled;
  
  GameTurn(Game game, Player player, long turn) {
    _game = game;
    _player = player;
    _turn = turn; 
    _turnState = TurnState.START;
    _unitKilled = false;
  }
  
  private OrderResponse play(Unit u, TurnState turnState) {
  
    OrderResponse lastOrderResponse = null;
    if (u.isDead()) {
      throw new SaDException("Dead units should not be playing");
    }

    if (u.turn().isDone() || u.turn().awaitingOrders()){
      throw new SaDException("Attempting to play unit that is not ready");
    }
    
    Log.debug(this, "Getting units orders");
    Order order = u.getOrder();
    if (order == null) {
      throw new SaDException("Attempting to play unit with no order!");
    }
    if (u != order.getAssignee()) {
      throw new SaDException("Order does not belong to unit running it!");
    }

    lastOrderResponse = order.execute(turnState);
    return lastOrderResponse;
  }
  
  @Override
  public String toString() {
    return "GameTurn [turn=" + _turn + " state= " + _turnState + " player=" + _player + "]";
  }
  

  static class OrderStateCounts {
    int ready = 0;
    int yielding = 0;
    int awaiting = 0;
  }
  
  private static OrderStateCounts analyseUnplayed(List<Unit> units) {
    OrderStateCounts counts = new OrderStateCounts();
    for(Unit u: units) {
      if (u.turn().isReady()) {
        counts.ready++;
      }
      else if (u.turn().awaitingOrders()) {
        counts.awaiting++;
      }
      else if (u.turn().isYielding()) {
        counts.yielding++;
      }
    }
    return counts;
  }

  public void play() {
   
   // int pass = 0;
    int previousPassYields = 0;
    do {
      _player.startTurnPass(_turn, _turnState);
      
      List<Unit> unplayed = _player.unplayedUnits();
      if (unplayed.isEmpty()) {
        Log.info(this, "Turn over");
        return;
      }
     
      OrderStateCounts orderStats = analyseUnplayed(unplayed);
      if (orderStats.ready == 0) {
        Log.debug(this, "Needs more order");
        _player.unitsNeedOrders();
        orderStats = analyseUnplayed(unplayed);
      }
      
      if (orderStats.yielding > 0) {
        throw new SaDException("Nothing should be yielding at this point!");
      }
      
      Unit pending = _player.popPendingPlay();
      // Move the unit the user interacted with
      if (pending != null && pending.hasOrders() && unplayed.contains(pending)) {
        unplayed.remove(pending);
        process(pending);
      }
     
      for (Unit u : unplayed) {
        if (!u.hasOrders()) {
          continue;
        }
        process(u);
      }
      orderStats = analyseUnplayed(unplayed);
      
      if (orderStats.yielding > 0 && orderStats.yielding == previousPassYields) {
        _turnState = TurnState.END;
      }
      else {
        _turnState = TurnState.LOOP;
      }
      previousPassYields = orderStats.yielding;
    // pass++;
      
    } while (true);
  }

  private void process(Unit u) {
    if (u.isDead()) {
      Log.debug(u, "is dead. Skipping play");
      return;
    }
    Log.debug(this, "playing unit: " + u + " with order " + u.getOrder());

    
    _game.unitChange(u);
    OrderResponse response = play(u, _turnState);
    if (response == null) {
      Log.warn(u, "No response from play");
      return;
    }
    ResponseCode code = response.getCode();
    Log.debug(u, "Response from order: " + code);
    if (code == ResponseCode.CANCEL_ORDER) {
      Log.info(u, "Cancelling order");
      u.clearOrders();
    }else if (code == ResponseCode.STEP_COMPLETE) {
      u.clearOrders();
    } else if (code == ResponseCode.ORDER_AND_TURN_COMPLETE) {
      u.clearOrderAndCompleteTurn();
    } else if (code == ResponseCode.ORDER_COMPLETE) {
      u.clearOrders();
    } else if (code == ResponseCode.YIELD_PASS) {
      Log.debug(u, "Yielding this turn pass");
      if (_turnState == TurnState.END) {
        u.completeTurn();
      }
      else {
        u.turn().yieldTurn();
      }
    } else if (code == ResponseCode.TURN_COMPLETE) {
      u.completeTurn();
    } else if (code == ResponseCode.DIED) {
      u.completeTurn();
    } else if (code == ResponseCode.BLOCKED) {
      Log.warn("Unit blocked");
    }
    else {
      Log.debug(this, "****Unhandled response code:" + code);
    }
    _player.adjustVisibility(u);
    
    if (u.turn().isReady()) {
      throw new SaDException("A unit MUST NOT be READY after a turn pass");
    }
    
  }

  public void unitKilled() {
    _unitKilled = true;
    
  }


}
