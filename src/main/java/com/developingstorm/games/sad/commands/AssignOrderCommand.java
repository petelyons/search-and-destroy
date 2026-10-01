package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Unit;

/**
 * Command to assign an order to a unit.
 */
public class AssignOrderCommand implements GameCommand {

    private final Unit unit;
    private final Order order;

    public AssignOrderCommand(Unit unit, Order order) {
        this.unit = unit;
        this.order = order;
    }

    @Override
    public void execute(Game game) {
        if (unit != null && order != null) {
            // Sentry is how a player ends an unload. Moving keeps unloading on, so cargo wakes
            // at each coastal stop (unloading still ends when the transport leaves the coast).
            if (order.getType() == OrderType.SENTRY) {
                unit.cancelUnloading();
            }
            unit.assignOrder(order);
        }
    }
}
