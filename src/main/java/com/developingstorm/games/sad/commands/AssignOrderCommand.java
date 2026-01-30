package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Order;
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
            unit.assignOrder(order);
        }
    }
}
