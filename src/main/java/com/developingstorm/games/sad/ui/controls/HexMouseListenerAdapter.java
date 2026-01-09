package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * Adapts typical java mouse events to Hex board related mouse events
 */
public class HexMouseListenerAdapter implements MouseListener {

    protected BaseCommander commander;
    private IHexMouseListener extListener;

    HexMouseListenerAdapter(BaseCommander c, IHexMouseListener extListener) {
        this.commander = c;
        this.extListener = extListener;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        if (isChooseEvent(e)) {
            this.commander.choose(hex);
            return;
        }
        this.extListener.hexMousePressed(e, hex);
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        if (isChooseEvent(e)) {
            this.commander.choose(hex);
            return;
        }
        this.extListener.hexMouseReleased(e, hex);
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        this.extListener.hexMouseClicked(e, hex);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        this.commander.refocus();
        this.extListener.hexMouseEntered(e, hex);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        this.extListener.hexMouseExited(e, hex);
    }

    public static boolean isChooseEvent(MouseEvent e) {
        return (e.isPopupTrigger());
    }
}
