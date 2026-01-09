package com.developingstorm.games.sad.ui.controls;

import com.developingstorm.games.sad.Game;
import com.developingstorm.games.sad.ui.BoardCanvas;
import com.developingstorm.games.sad.ui.SaDFrame;
import com.developingstorm.games.sad.ui.UIMode;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Acts as a splitter to send UI Listeners to the mode appropriate handler
 */
public class UIController {

    static class Mode {

        public BaseCommander commander;
        public BaseController controller;
    }

    static class GameMode extends Mode {

        GameMode(SaDFrame frame, Game game) {
            this.commander = new GameCommander(frame, game);
            this.controller = new GameModeController(
                frame,
                (GameCommander) this.commander
            );
        }
    }

    static class PathsMode extends Mode {

        PathsMode(SaDFrame frame, Game game) {
            this.commander = new PathsCommander(frame, game);
            this.controller = new PathsModeController(
                frame,
                (PathsCommander) this.commander
            );
        }
    }

    static class ExploreMode extends Mode {

        ExploreMode(SaDFrame frame, Game game) {
            this.commander = new ExploreCommander(frame, game);
            this.controller = new ExploreModeController(
                frame,
                (ExploreCommander) this.commander
            );
        }
    }

    private UIMode modeMode;
    private Mode mode;
    private SaDFrame frame;
    private Game game;
    private BoardCanvas canvas;
    private MouseListener mouseListener;
    private MouseMotionListener mouseMotionListener;
    private KeyListener keyboardListener;
    private Map<UIMode, Mode> modes;

    public UIController(SaDFrame frame, Game game) {
        this.modes = new HashMap<UIMode, Mode>();

        this.frame = frame;
        this.game = game;
        this.canvas = this.frame.getCanvas();
        this.modes.put(UIMode.GAME, new GameMode(this.frame, this.game));
        this.modes.put(UIMode.PATHS, new PathsMode(this.frame, this.game));
        this.modes.put(UIMode.EXPLORE, new ExploreMode(this.frame, this.game));

        switchMode(UIMode.GAME);

        this.mouseListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UIController.this.mode.controller.mouseListener().mouseClicked(
                    e
                );
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                UIController.this.mode.controller.mouseListener().mouseEntered(
                    e
                );
            }

            @Override
            public void mouseExited(MouseEvent e) {
                UIController.this.mode.controller.mouseListener().mouseExited(
                    e
                );
            }

            @Override
            public void mousePressed(MouseEvent e) {
                UIController.this.mode.controller.mouseListener().mousePressed(
                    e
                );
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                UIController.this.mode.controller.mouseListener().mouseReleased(
                    e
                );
            }
        };

        this.mouseMotionListener = new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                UIController.this.mode.controller.mouseMotionListener().mouseDragged(
                    e
                );
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                UIController.this.mode.controller.mouseMotionListener().mouseMoved(
                    e
                );
            }
        };

        this.keyboardListener = new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                UIController.this.mode.controller.keyListener().keyPressed(e);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                UIController.this.mode.controller.keyListener().keyReleased(e);
            }

            @Override
            public void keyTyped(KeyEvent e) {
                UIController.this.mode.controller.keyListener().keyTyped(e);
            }
        };
    }

    public void switchMode(UIMode modeMode) {
        if (modeMode == this.modeMode) {
            return;
        }
        this.modeMode = modeMode;
        this.mode = this.modes.get(this.modeMode);
        this.canvas.setUIMode(this.modeMode);
    }

    public MouseListener mouseListener() {
        return this.mouseListener;
    }

    public MouseMotionListener mouseMotionListener() {
        return this.mouseMotionListener;
    }

    public KeyListener keyListener() {
        return this.keyboardListener;
    }

    public UIMode getUIMode() {
        return this.modeMode;
    }

    public PathsCommander getPathsCommander() {
        Mode mode = this.modes.get(UIMode.PATHS);
        return (PathsCommander) mode.commander;
    }
}
