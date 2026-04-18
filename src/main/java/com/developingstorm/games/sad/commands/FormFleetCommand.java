package com.developingstorm.games.sad.commands;

import com.developingstorm.games.sad.Fleet;
import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.Unit;
import com.developingstorm.games.sad.events.FleetChangedEvent;
import com.developingstorm.games.sad.orders.FleetMember;
import java.util.ArrayList;
import java.util.List;

public class FormFleetCommand implements GameCommand {

    private final Unit flagship;
    private final List<Unit> initialMembers;

    public FormFleetCommand(Unit flagship, List<Unit> initialMembers) {
        this.flagship = flagship;
        this.initialMembers = new ArrayList<>(initialMembers);
    }

    @Override
    public void execute(Game game) {
        Fleet f = game.getFleetRegistry().form(flagship, initialMembers);
        for (Unit m : initialMembers) {
            m.clearOrders();
            m.assignOrder(new FleetMember(game, m, f));
        }
        game.getEventBus().publish(new FleetChangedEvent(f));
    }
}
