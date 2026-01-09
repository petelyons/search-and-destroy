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
    private Battleplan battleplan;
    private General general;

    public RobotBrain(Robot owner) {
        this.owner = owner;
        // battleplan = new Battleplan(this.owner.getGame(), this.owner);
    }

    @Override
    public void startNewTurn() {
        battleplan = new Battleplan(this.owner.getGame(), this.owner);
        Log.info(this.battleplan.toString());
        general = new General(this.battleplan);
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
