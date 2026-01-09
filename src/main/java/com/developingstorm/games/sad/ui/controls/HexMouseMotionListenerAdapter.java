package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.hexboard.BoardHex;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Adapts typical java mouse events to Hex board related mouse events
 */
public class HexMouseMotionListenerAdapter implements MouseMotionListener {

    protected BaseCommander commander;
    private IHexMouseMotionListener extListener;

    HexMouseMotionListenerAdapter(
        BaseCommander c,
        IHexMouseMotionListener extListener
    ) {
        this.commander = c;
        this.extListener = extListener;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        this.extListener.hexMouseDragged(e, hex);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        BoardHex hex = this.commander.trans(e.getPoint());
        this.extListener.hexMouseMoved(e, hex);
    }
}
