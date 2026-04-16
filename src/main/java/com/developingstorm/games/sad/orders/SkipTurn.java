package com.developingstorm.games.sad.orders;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderResponse;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.ResponseCode;
import com.developingstorm.games.sad.Unit;

/**

 *
 */
public class SkipTurn extends Order {

    public SkipTurn(Game g, Unit u) {
        super(g, u, OrderType.SKIPTURN);
    }

    public OrderResponse executeInternal() {
        // If this unit is on a transport, skip all other carried units too
        if (this.unit.isCarried() && this.unit.onboard != null) {
            for (Unit sibling : this.unit.onboard.carries) {
                if (sibling != this.unit && !sibling.turn().isDone()) {
                    sibling.turn().completeTurn();
                }
            }
        }
        return new OrderResponse(ResponseCode.TURN_COMPLETE, this, null);
    }
}
