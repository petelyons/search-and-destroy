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
public class Unload extends Order {

    public Unload(Game g, Unit u) {
        super(g, u, OrderType.UNLOAD);
    }

    public OrderResponse executeInternal() {
        // If this unit is being carried, activate it so it can move off the transport
        if (this.unit.onboard != null) {
            // Activate the unit - this wakes it up AND clears orders
            this.unit.activate();

            // Unit is now active and ready to move off the transport
            return new OrderResponse(ResponseCode.ORDER_COMPLETE, this, null);
        } else {
            // This unit is the transport itself - unload all cargo
            this.unit.setUnloadingMode(true);
            this.unit.unload();
            // Transport's order and turn are completely done after activating a carried unit
            // This prevents the transport from getting another turn until carried units disembark
            return new OrderResponse(
                ResponseCode.ORDER_AND_TURN_COMPLETE,
                this,
                null
            );
        }
    }
}
