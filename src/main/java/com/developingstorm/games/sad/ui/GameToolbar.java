package com.developingstorm.games.sad.ui;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**

 * 
 */
public class GameToolbar extends JToolBar {

  private ActionListener _alistener;
  private ItemListener _ilistener;

  public GameToolbar(ActionListener alistener, ItemListener ilistener) {

    _alistener = alistener;
    _ilistener = ilistener;

    JToggleButton button = null;

    ButtonGroup bg = new ButtonGroup();

    // first button
    button = makeStateButton("images/play.gif", "PLAY",
        "Play the game: Make moves, carry out attacks...", "Play");

    add(button);
    bg.add(button);

    // second button
    button = makeStateButton("images/pause.gif", "PAUSE",
        "Pause the game. Organize your empire...", "Pause");

    add(button);
    bg.add(button);

    setFloatable(false);
  }

  protected JToggleButton makeStateButton(String imageName,
      String actionCommand, String toolTipText, String altText) {
    // Look for the image.
    ImageIcon icon = GameIcons.get().loadImageIcon(imageName);

    // Create and initialize the button.
    JToggleButton button = new JToggleButton();
    button.setActionCommand(actionCommand);
    button.setToolTipText(toolTipText);
    button.addItemListener(_ilistener);
    if (icon != null) { // image found
      button.setIcon(icon);

    } else { // no image found
      button.setText(altText);
    }
    return button;
  }

}
