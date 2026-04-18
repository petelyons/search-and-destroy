package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.events.FleetChangedEvent;
import com.developingstorm.games.sad.orders.FleetMember;

public class AddToFleetCommand implements GameCommand {

    private final long fleetId;
    private final Unit member;

    public AddToFleetCommand(long fleetId, Unit member) {
        this.fleetId = fleetId;
        this.member = member;
    }

    @Override
    public void execute(Game game) {
        Fleet f = game.getFleetRegistry().byId(fleetId);
        if (f == null) return;
        game.getFleetRegistry().add(f, member);
        member.clearOrders();
        member.assignOrder(new FleetMember(game, member, f));
        game.getEventBus().publish(new FleetChangedEvent(f));
    }
}
