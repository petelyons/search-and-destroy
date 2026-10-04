package com.developingstorm.games.sad.fx;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Robot;
import org.junit.jupiter.api.Test;

public class GameOverMessageTest {

    private final Player human = new Player("Pete", 1);
    private final Robot robot = new Robot("Robot", 2);

    @Test
    public void humanWinIsVictory() {
        GameOverMessage m = GameOverMessage.of(human, human);
        assertEquals("Victory", m.header());
        assertTrue(m.content().contains("Pete"));
    }

    @Test
    public void robotWinIsDefeat() {
        GameOverMessage m = GameOverMessage.of(robot, human);
        assertEquals("Defeat", m.header());
        assertTrue(m.content().contains("Robot"));
    }

    @Test
    public void noWinnerIsADraw() {
        assertEquals("Draw", GameOverMessage.of(null, human).header());
    }
}
