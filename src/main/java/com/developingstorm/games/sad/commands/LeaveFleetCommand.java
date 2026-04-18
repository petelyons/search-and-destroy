package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.events.FleetChangedEvent;

public class LeaveFleetCommand implements GameCommand {

    private final Unit unit;

    public LeaveFleetCommand(Unit unit) {
        this.unit = unit;
    }

    @Override
    public void execute(Game game) {
        Fleet f = unit.getFleet();
        if (f == null) return;
        long priorId = f.getId();
        game.getFleetRegistry().leave(unit);
        unit.clearOrders();
        unit.life().wake();
        Fleet current = game.getFleetRegistry().byId(priorId);
        game.getEventBus().publish(new FleetChangedEvent(current));
    }
}
