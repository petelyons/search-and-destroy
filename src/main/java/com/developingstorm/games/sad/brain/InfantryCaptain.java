package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Infantry;

public class InfantryCaptain extends UnitCaptain<Infantry> {

    public InfantryCaptain(General gen, Battleplan plan) {
        super(gen, plan);
    }

    @Override
    public Order plan(Infantry u) {
        // Check if we should be defending first
        Order defensiveOrder = executeDefensiveStrategy(u);
        if (defensiveOrder != null) {
            return defensiveOrder;
        }

        // Otherwise, use normal occupation strategy
        return occupyLandStrategy(u);
    }
}
