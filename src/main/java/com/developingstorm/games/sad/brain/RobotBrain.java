package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.IBrain;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.util.Log;

public class RobotBrain implements IBrain {

    private final Robot owner;
    private final AIConfiguration config;
    private Battleplan battleplan;
    private General general;
    private StrategyMemory strategyMemory;
    private OperationsCoordinator operationsCoordinator;

    public RobotBrain(Robot owner) {
        this(owner, new AIConfiguration());
    }

    public RobotBrain(Robot owner, AIConfiguration config) {
        this.owner = owner;
        this.config = config;
        this.strategyMemory = new StrategyMemory();
        this.operationsCoordinator = new OperationsCoordinator(
            owner,
            strategyMemory
        );
    }

    public AIConfiguration getConfig() {
        return config;
    }

    @Override
    public void startNewTurn() {
        battleplan = new Battleplan(
            this.owner.getGame(),
            this.owner,
            this.config
        );
        Log.info(this.battleplan.toString());

        // Plan amphibious operations before assigning unit orders
        operationsCoordinator.planOperations(battleplan);

        general = new General(this.battleplan, operationsCoordinator);
        this.owner.forEachUnit(u -> {
            u.assignOrder(this.general.getOrders(u));
        });

        for (City c : this.owner.getCities()) {
            if (c.productionCompleted()) {
                Type t = this.battleplan.productionChoice(c);
                Log.debug(
                    this.owner,
                    "Resetting production of " + c + " to: " + t
                );
                c.produce(t);
            }
        }
    }

    @Override
    public Order getOrders(Unit u) {
        return this.general.getOrders(u);
    }

    @Override
    public Type getProduction(City c) {
        if (battleplan == null) {
            return Type.INFANTRY;
        }
        return this.battleplan.productionChoice(c);
    }
}
