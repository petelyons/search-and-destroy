package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.HexBoardMap;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.events.GameEvent;
import com.developingstorm.games.sad.events.GameEventListener;
import com.developingstorm.games.sad.events.GameEventType;
import com.developingstorm.games.sad.events.GameOverEvent;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

public class GameOverDetectionTest {

    @Test
    public void enemyWithOnlyShipsLeftHasLost() {
        com.developingstorm.util.RandomUtil.setSeed(20261004L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Player human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        Game game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());

        // The robot's last city falls; only a submarine is left at sea
        for (City c : new java.util.ArrayList<>(robot.getCities())) {
            c.setOwner(human);
        }
        Location sea = null;
        for (int x = 0; x < 50 && sea == null; x++) {
            for (int y = 0; y < 50; y++) {
                if (game.getBoard().isWater(Location.get(x, y))) {
                    sea = Location.get(x, y);
                    break;
                }
            }
        }
        Unit sub = game.createUnit(Type.SUBMARINE, robot, sea);
        robot.addUnit(sub);

        AtomicReference<Player> winner = new AtomicReference<>();
        game.getEventBus().addListener(new GameEventListener() {
            @Override
            public void onGameEvent(GameEvent event) {
                winner.set(((GameOverEvent) event).getWinner());
            }

            @Override
            public GameEventType[] getInterestedEventTypes() {
                return new GameEventType[] { GameEventType.GAME_OVER };
            }
        });

        assertTimeoutPreemptively(Duration.ofSeconds(10), game::play);

        assertSame(human, winner.get());
    }

    @Test
    public void defeatIsDeclaredTheMomentItHappens() {
        com.developingstorm.util.RandomUtil.setSeed(20261104L);
        LocationMap.init(50, 50);
        HexBoardMap map = HexBoardMap.loadMapAsResource(getClass(), "MedMap.sdm");
        Player human = new Player("Human", 1);
        Robot robot = new Robot("Robot", 2);
        robot.setBrain(new com.developingstorm.games.sad.brain.RobotBrain(robot));
        Game game = new Game(new Player[] {human, robot}, map, new MockSaDBoardContext());
        AtomicReference<Player> winner = new AtomicReference<>();
        game.getEventBus().addListener(new GameEventListener() {
            @Override
            public void onGameEvent(GameEvent event) {
                winner.set(((GameOverEvent) event).getWinner());
            }

            @Override
            public GameEventType[] getInterestedEventTypes() {
                return new GameEventType[] { GameEventType.GAME_OVER };
            }
        });

        assertFalse(game.checkForDefeat(), "both players still in it");
        assertNull(winner.get());

        // Mid-turn, the robot loses its last city
        for (City c : new java.util.ArrayList<>(robot.getCities())) {
            c.setOwner(human);
        }

        assertTrue(game.checkForDefeat());
        assertSame(human, winner.get());
        assertEquals(GameState.GAME_OVER, game.getGameState());
    }
}
