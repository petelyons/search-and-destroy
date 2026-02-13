package com.developingstorm.games.sad.ui;

import java.awt.Color;

import com.developingstorm.games.hexboard.HexBoardContext;
import com.developingstorm.games.sad.Player;

/**

 * 
 */
public interface SaDBoardContext extends HexBoardContext {

  Color getPlayerColor(Player p);

}
