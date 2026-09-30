package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.IBrain;
import com.developingstorm.games.sad.Order;
import com.developingstorm.games.sad.OrderType;
import com.developingstorm.games.sad.Robot;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.brain.commit.BattleplanCommitmentContext;
import com.developingstorm.games.sad.brain.commit.Commitment;
import com.developingstorm.games.sad.brain.commit.CommitmentBook;
import com.developingstorm.games.sad.brain.commit.CommitmentContext;
import com.developingstorm.games.sad.brain.commit.CommitmentRules;
import com.developingstorm.games.sad.brain.commit.CommitmentStatus;
import com.developingstorm.games.sad.orders.Move;
import com.developingstorm.games.sad.util.Log;

public class RobotBrain implements IBrain {

    private final Robot owner;
    private final AIConfiguration config;
    private Battleplan battleplan;
    private General general;
    private StrategyMemory strategyMemory;
    private OperationsCoordinator operationsCoordinator;
    private final CommitmentRules rules = new CommitmentRules();
    private CommitmentContext commitmentContext;

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

    Battleplan getBattleplan() {
        return battleplan;
    }

    StrategyMemory getStrategyMemory() {
        return strategyMemory;
    }

    OperationsCoordinator getOperationsCoordinator() {
        return operationsCoordinator;
    }

    @Override
    public void startNewTurn() {
        battleplan = new Battleplan(
            this.owner.getGame(),
            this.owner,
            this.config,
            this.strategyMemory
        );
        Log.info(this.battleplan.toString());

        // Plan amphibious operations before assigning unit orders
        operationsCoordinator.planOperations(battleplan);

        general = new General(this.battleplan, operationsCoordinator);
        commitmentContext = new BattleplanCommitmentContext(this.battleplan, operationsCoordinator);
        this.owner.forEachUnit(u -> {
            Order o = orderFor(u);
            if (o != u.getOrder()) {
                u.assignOrder(o);
            }
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
        return orderFor(u);
    }

    /**
     * The order a unit should carry out. A unit with an active, un-pre-empted commitment keeps
     * its current order if that order still serves the commitment; otherwise its captain
     * continues the commitment. Everything else is planned from scratch.
     */
    Order orderFor(Unit u) {
        CommitmentBook book = this.strategyMemory.getCommitments();
        Commitment c = book.get(u);
        UnitCaptain<?> captain = this.general.captainFor(u);
        if (c != null && captain != null) {
            CommitmentStatus status = this.rules.status(c, this.commitmentContext);
            String preempted = status == CommitmentStatus.ACTIVE ? captain.preemption(u, c) : null;
            if (status == CommitmentStatus.ACTIVE && preempted == null) {
                Order current = u.getOrder();
                if (current != null && serves(current, c)) {
                    return current;
                }
                Order next = captain.continueCommitment(u, c);
                if (next != null) {
                    return next;
                }
                book.release(u, "no order to continue");
            } else {
                book.release(u, preempted != null ? "pre-empted by " + preempted : status.toString());
            }
        } else if (c != null) {
            book.release(u, "no commitment captain");
        }
        return this.general.getOrders(u);
    }

    static boolean serves(Order o, Commitment c) {
        switch (c.kind) {
            case CAPTURE_CITY:
            case STAGE:
                return o instanceof Move m && c.target.equals(m.getDestination());
            case EXPLORE:
                return o.getType() == OrderType.EXPLORE;
            default:
                return false;
        }
    }

    @Override
    public Type getProduction(City c) {
        if (battleplan == null) {
            return Type.INFANTRY;
        }
        return this.battleplan.productionChoice(c);
    }
}
