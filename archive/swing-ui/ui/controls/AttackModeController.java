package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.ui.SaDFrame;
import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Controller for attack/bombardment mode
 */
public class AttackModeController extends BaseController {

    private final AttackCommander commander;
    private final SaDFrame frame;
    private final KeyListener keyListener;
    private final HexMouseListenerAdapter hexMouseListenerAdapter;
    private final HexMouseMotionListenerAdapter hexMouseMotionListenerAdapter;

    public AttackModeController(SaDFrame frame, AttackCommander commander) {
        this.frame = frame;
        this.commander = commander;

        keyListener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    AttackModeController.this.commander.cancelAttack();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}

            @Override
            public void keyTyped(KeyEvent e) {}
        };

        hexMouseListenerAdapter = new HexMouseListenerAdapter(
            commander,
            new IHexMouseListener() {
                @Override
                public void hexMousePressed(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseReleased(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseClicked(MouseEvent e, BoardHex hex) {
                    if (hex == null) return;

                    Location loc = hex.getLocation();

                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Left click - select target
                        AttackModeController.this.commander.selectTarget(loc);
                    } else if (
                        e.getButton() == MouseEvent.BUTTON3 ||
                        e.isPopupTrigger()
                    ) {
                        // Right click - cancel
                        AttackModeController.this.commander.cancelAttack();
                    }
                }

                @Override
                public void hexMouseEntered(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseExited(MouseEvent e, BoardHex hex) {}
            }
        );

        hexMouseMotionListenerAdapter = new HexMouseMotionListenerAdapter(
            commander,
            new IHexMouseMotionListener() {
                @Override
                public void hexMouseDragged(MouseEvent e, BoardHex hex) {}

                @Override
                public void hexMouseMoved(MouseEvent e, BoardHex hex) {}
            }
        );
    }

    @Override
    public void clearAction() {
        // Restore default cursor when leaving attack mode
        this.frame.getCanvas().setCursor(
            Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        );
    }

    public void activate() {
        updateStatusMessage();
        // Set crosshair cursor to indicate targeting mode
        this.frame.getCanvas().setCursor(
            Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
        );
    }

    private void updateStatusMessage() {
        String msg =
            "Bombardment Mode: Click on a highlighted hex to attack. ESC or right-click to cancel.";
        // TODO: Re-enable when setStatusMessage is available
        // this.frame.setStatusMessage(msg);
    }

    @Override
    public MouseListener mouseListener() {
        return hexMouseListenerAdapter;
    }

    @Override
    public MouseMotionListener mouseMotionListener() {
        return hexMouseMotionListenerAdapter;
    }

    @Override
    public KeyListener keyListener() {
        return keyListener;
    }
}
