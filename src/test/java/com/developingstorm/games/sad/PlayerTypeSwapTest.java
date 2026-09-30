package com.developingstorm.games.sad;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class PlayerTypeSwapTest {

    @Test
    public void equalsIsClassAgnostic() {
        Player human = new Player("Pete", 1);
        Player robot = new Robot("Pete", 1);
        assertEquals(human, robot);
        assertEquals(human.hashCode(), robot.hashCode());
    }

    @Test
    public void humanToRobotPreservesIdentityAndUnits() {
        Player human = new Player("Pete", 1);
        Unit u = TestUnits.stubUnit(Type.INFANTRY, human);
        assertSame(human, u.getOwner());

        Player swapped = human.toRobotForm();

        assertTrue(swapped.isRobot());
        assertEquals(1, swapped.getId());
        assertEquals("Pete", swapped.getName());
        // Unit list reference is moved to the new instance
        assertTrue(swapped.getUnits().contains(u));
    }

    @Test
    public void robotToHumanProducesPlainPlayer() {
        Robot r = new Robot("Mal", 2);
        Unit u = TestUnits.stubUnit(Type.ARMOR, r);

        Player swapped = r.toHumanForm();

        assertFalse(swapped.isRobot());
        assertEquals(2, swapped.getId());
        assertTrue(swapped.getUnits().contains(u));
    }

    @Test
    public void robotToRobotIsNoop() {
        Robot r = new Robot("Mal", 2);
        assertSame(r, r.toRobotForm());
    }

    @Test
    public void humanToHumanIsNoop() {
        Player p = new Player("Pete", 1);
        assertSame(p, p.toHumanForm());
    }

    @Test
    public void unitReplaceOwnerForTypeSwapRewritesField() {
        Player human = new Player("Pete", 1);
        Unit u = TestUnits.stubUnit(Type.INFANTRY, human);
        Player robot = new Robot("Pete", 1);
        u.replaceOwnerForTypeSwap(robot);
        assertSame(robot, u.getOwner());
    }

    @Test
    public void unitReplaceOwnerRejectsMismatchedId() {
        Player a = new Player("A", 1);
        Unit u = TestUnits.stubUnit(Type.INFANTRY, a);
        Player b = new Player("B", 2);
        assertThrows(SaDException.class, () -> u.replaceOwnerForTypeSwap(b));
    }

    @Test
    public void fleetReplaceOwnerRewritesField() {
        Player owner = new Robot("A", 1);
        Unit flagship = TestUnits.stubUnit(Type.BATTLESHIP, owner);
        Fleet f = new Fleet(1L, owner, flagship);

        Player swapped = new Player("A", 1);
        f.replaceOwner(swapped);
        assertSame(swapped, f.getOwner());
    }
}
