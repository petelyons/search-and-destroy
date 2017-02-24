package com.developingstorm.games.sad.ui;

import java.awt.Image;

import javax.swing.ImageIcon;

import com.developingstorm.util.ResourceUtil;

/**

 * 
 */
public class GameIcons {

  static final String PATH = "images/";

  public static final int iWATER = 0;
  public static final int iLAND = 1;
  public static final int iFOREST = 2;
  public static final int iSAND = 3;
  public static final int iARID = 4;
  public static final int iSWAMP = 5;
  public static final int iMOUNTAIN = 6;
  public static final int iARMY = 7;
  public static final int iFIGHTER = 8;
  public static final int iTRANSPORT = 9;
  public static final int iDESTROYER = 10;
  public static final int iSUBMARINE = 11;
  public static final int iCRUISER = 12;
  public static final int iBATTLESHIP = 13;
  public static final int iAIRCRAFTCARRIER = 14;
  public static final int iFULLTRANSPORT = 15;
  public static final int iFULLCARRIER = 16;
  public static final int iSENTRYARMY = 17;
  public static final int iEXPLOSION0 = 18;
  public static final int iEXPLOSION1 = 19;
  public static final int iEXPLOSION2 = 20;
  public static final int iEXPLOSION3 = 21;
  public static final int iEXPLOSION4 = 22;
  public static final int iUNEXPLORED = 23;
  public static final int iANCHOR = 24;
  public static final int iTANK = 25;
  public static final int iBOMBER = 26;
  public static final int iCARGO = 27;
  public static final int iSENTRYTANK = 28;
  public static final int iFULLCARGO = 29;

  private static final int MAX_ICONS = 30;

  private ImageIcon[] _icons;
  private Image[] _images;

  private static GameIcons s_icons = new GameIcons();

  public static GameIcons get() {
    return s_icons;
  }

  ImageIcon loadImageIcon(String name) {
    return ResourceUtil.loadImageIcon(getClass().getClassLoader(), name);
  }

  private GameIcons() {

    _icons = new ImageIcon[MAX_ICONS];
    _images = new Image[MAX_ICONS];

    _icons[iWATER] = loadImageIcon(PATH + "water.gif");
    _icons[iLAND] = loadImageIcon(PATH + "land.gif");
    _icons[iFOREST] = loadImageIcon(PATH + "forest.gif");
    _icons[iSAND] = loadImageIcon(PATH + "sand.gif");
    _icons[iARID] = loadImageIcon(PATH + "arid.gif");
    _icons[iSWAMP] = loadImageIcon(PATH + "swamp.gif");
    _icons[iMOUNTAIN] = loadImageIcon(PATH + "mountain.gif");

    _icons[iARMY] = loadImageIcon(PATH + "army.gif");
    _icons[iFIGHTER] = loadImageIcon(PATH + "fighter.gif");
    _icons[iTRANSPORT] = loadImageIcon(PATH + "transport.gif");
    _icons[iDESTROYER] = loadImageIcon(PATH + "destroyer.gif");
    _icons[iSUBMARINE] = loadImageIcon(PATH + "sub.gif");
    _icons[iCRUISER] = loadImageIcon(PATH + "cruiser.gif");
    _icons[iBATTLESHIP] = loadImageIcon(PATH + "battleship.gif");
    _icons[iAIRCRAFTCARRIER] = loadImageIcon(PATH + "carrier.gif");
    _icons[iFULLTRANSPORT] = loadImageIcon(PATH + "fulltransport.gif");
    _icons[iFULLCARRIER] = loadImageIcon(PATH + "fullcarrier.gif");
    _icons[iSENTRYARMY] = loadImageIcon(PATH + "tent.gif");
    _icons[iTANK] = loadImageIcon(PATH + "tank.gif");
    _icons[iCARGO] = loadImageIcon(PATH + "cargo.gif");
    _icons[iBOMBER] = loadImageIcon(PATH + "bomber.gif");
    _icons[iSENTRYTANK] = loadImageIcon(PATH + "sentrytank.gif");
    _icons[iFULLCARGO] = loadImageIcon(PATH + "fullcargo.gif");

    _icons[iEXPLOSION0] = loadImageIcon(PATH + "explosion0.gif");
    _icons[iEXPLOSION1] = loadImageIcon(PATH + "explosion1.gif");
    _icons[iEXPLOSION2] = loadImageIcon(PATH + "explosion2.gif");
    _icons[iEXPLOSION3] = loadImageIcon(PATH + "explosion3.gif");
    _icons[iEXPLOSION4] = loadImageIcon(PATH + "explosion4.gif");

    _icons[iUNEXPLORED] = loadImageIcon(PATH + "unexplored.gif");
    _icons[iANCHOR] = loadImageIcon(PATH + "anchor.gif");

    for (int x = 0; x < MAX_ICONS; x++) {
      _images[x] = _icons[x].getImage();
    }

  }

  public Image[] getImages() {
    return _images;
  }

}
