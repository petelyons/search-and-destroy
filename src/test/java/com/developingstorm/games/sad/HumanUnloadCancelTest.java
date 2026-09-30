package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.commands.AssignOrderCommand;
import com.developingstorm.games.sad.orders.Unload;
import org.junit.jupiter.api.Test;

public class HumanUnloadCancelTest {

    private Game game;
    private Unit transport;
    private Unit infantry;

    private void setUp() {
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        HexBoardContext ctx = new MockSaDBoardContext();
        Player human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        game = new Game(new Player[] {human, robot}, map, ctx);

        Location coast = findCoastalWater();
        transport = game.createUnit(Type.TRANSPORT, human, coast);
        human.addUnit(transport);
        infantry = game.createUnit(Type.INFANTRY, human, coast);
        human.addUnit(infantry);
        transport.addCarried(infantry);
    }

    private Location findCoastalWater() {
        Board board = game.getBoard();
        for (int x = 1; x < 49; x++) {
            for (int y = 1; y < 49; y++) {
                Location l = Location.get(x, y);
                if (!board.isWater(l) || !game.unitsAtLocation(l).isEmpty()) continue;
                for (Location adj : l.getRing(1)) {
                    if (board.onBoard(adj) && !board.isWater(adj)
                        && game.unitsAtLocation(adj).isEmpty()) {
                        return l;
                    }
                }
            }
        }
        throw new SaDException("no coastal water hex");
    }

    @Test
    public void newOrderOnTransportEndsUnloading() {
        setUp();
        new Unload(game, transport).executeInternal();
        assertTrue(transport.isUnloadingMode());
        assertFalse(infantry.inSentryMode(), "cargo should be woken to disembark");

        new AssignOrderCommand(transport, transport.newSentryOrder()).execute(game);

        assertFalse(transport.isUnloadingMode(), "new order should cancel unloading");
        assertTrue(infantry.inSentryMode(), "woken cargo should go back to sentry");
    }
}
