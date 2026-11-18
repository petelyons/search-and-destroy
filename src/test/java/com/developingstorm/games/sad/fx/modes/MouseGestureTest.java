package com.developingstorm.games.sad.fx.modes;

import static org.junit.jupiter.api.Assertions.*;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.hexboard.LocationMap;
import com.developingstorm.games.sad.Player;
import com.developingstorm.games.sad.Type;
import com.developingstorm.games.sad.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MouseGestureTest {

    private final MouseGesture gesture = new MouseGesture();
    private Location a;
    private Location b;

    @BeforeEach
    public void setUp() {
        LocationMap.init(50, 50);
        a = Location.get(5, 5);
        b = Location.get(6, 5);
    }

    @Test
    public void releaseWithoutOurPressIsIgnored() {
        // e.g. the attack or patrol mode took the press, then switched back to game mode
        assertEquals(MouseGesture.Kind.NONE, gesture.released(b).kind());
    }

    @Test
    public void pressAndReleaseOnOneHexIsAClick() {
        gesture.pressed(a);
        assertEquals(MouseGesture.Kind.CLICK, gesture.released(a).kind());
    }

    @Test
    public void pressAndReleaseOnDifferentHexesIsADragFromThePressedHex() {
        gesture.pressed(a);
        MouseGesture.Result r = gesture.released(b);
        assertEquals(MouseGesture.Kind.DRAG, r.kind());
        assertEquals(a, r.from());
    }

    @Test
    public void eachPressIsUsedOnce() {
        gesture.pressed(a);
        gesture.released(b);
        assertEquals(MouseGesture.Kind.NONE, gesture.released(b).kind());
    }

    @Test
    public void resetForgetsThePress() {
        gesture.pressed(a);
        gesture.reset();
        assertEquals(MouseGesture.Kind.NONE, gesture.released(b).kind());
    }

    @Test
    public void dragMovesTheUnitSelectedByReleaseTime() {
        // The press can land before the game thread selects the next unit
        gesture.pressed(a);
        Unit next = Unit.createForTest(1, Type.INFANTRY, new Player("Human", 1), a);
        assertSame(next, MouseGesture.draggedUnit(gesture.released(b), next));
    }

    @Test
    public void dragFromAHexWithoutTheSelectedUnitMovesNothing() {
        gesture.pressed(b);
        Unit selected = Unit.createForTest(1, Type.INFANTRY, new Player("Human", 1), a);
        assertNull(MouseGesture.draggedUnit(gesture.released(a), selected));
        assertNull(MouseGesture.draggedUnit(new MouseGesture.Result(MouseGesture.Kind.DRAG, a), null));
    }
}
