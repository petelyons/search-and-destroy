package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.types.Armor;

public class ArmorCaptain extends UnitCaptain<Armor> {

    public ArmorCaptain(General gen, Battleplan plan) {
        super(gen, plan);
    }

    public ArmorCaptain(
        General gen,
        Battleplan plan,
        OperationsCoordinator coordinator
    ) {
        super(gen, plan, coordinator);
    }

    @Override
    public Order plan(Armor u) {
        // Check if assigned to an amphibious operation
        if (coordinator != null && coordinator.isAssigned(u)) {
            Order operationOrder = coordinator.getOperationOrder(u);
            if (operationOrder != null) {
                return operationOrder;
            }
        }

        // Armor can defend, but not if:
        // - Being carried on a transport (in transit for amphibious ops)
        // - In a city producing them (just spawned, need to get to front)
        boolean canDefend = !u.isCarried() && !isInProducingCity(u);

        if (canDefend) {
            Order defensiveOrder = executeDefensiveStrategy(u);
            if (defensiveOrder != null) {
                return defensiveOrder;
            }
        }

        // Otherwise, focus on expansion/occupation
        return occupyLandStrategy(u);
    }

    /**
     * Check if unit is in a city that just produced it
     */
    private boolean isInProducingCity(Armor u) {
        // If unit is in a city location, it was likely just produced
        // (units move out of cities after being produced in subsequent turns)
        return plan.getBoard().getCity(u.getLocation()) != null;
    }
}
