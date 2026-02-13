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

  private ImageIcon[] icons;
  private Image[] images;

  private static GameIcons s_icons = new GameIcons();

  public static GameIcons get() {
    return s_icons;
  }

  ImageIcon loadImageIcon(String name) {
    return ResourceUtil.loadImageIcon(getClass().getClassLoader(), name);
  }

  private GameIcons() {

    icons = new ImageIcon[MAX_ICONS];
    images = new Image[MAX_ICONS];

    this.icons[iWATER] = loadImageIcon(PATH + "water.gif");
    this.icons[iLAND] = loadImageIcon(PATH + "land.gif");
    this.icons[iFOREST] = loadImageIcon(PATH + "forest.gif");
    this.icons[iSAND] = loadImageIcon(PATH + "sand.gif");
    this.icons[iARID] = loadImageIcon(PATH + "arid.gif");
    this.icons[iSWAMP] = loadImageIcon(PATH + "swamp.gif");
    this.icons[iMOUNTAIN] = loadImageIcon(PATH + "mountain.gif");

    this.icons[iARMY] = loadImageIcon(PATH + "army.gif");
    this.icons[iFIGHTER] = loadImageIcon(PATH + "fighter.gif");
    this.icons[iTRANSPORT] = loadImageIcon(PATH + "transport.gif");
    this.icons[iDESTROYER] = loadImageIcon(PATH + "destroyer.gif");
    this.icons[iSUBMARINE] = loadImageIcon(PATH + "sub.gif");
    this.icons[iCRUISER] = loadImageIcon(PATH + "cruiser.gif");
    this.icons[iBATTLESHIP] = loadImageIcon(PATH + "battleship.gif");
    this.icons[iAIRCRAFTCARRIER] = loadImageIcon(PATH + "carrier.gif");
    this.icons[iFULLTRANSPORT] = loadImageIcon(PATH + "fulltransport.gif");
    this.icons[iFULLCARRIER] = loadImageIcon(PATH + "fullcarrier.gif");
    this.icons[iSENTRYARMY] = loadImageIcon(PATH + "tent.gif");
    this.icons[iTANK] = loadImageIcon(PATH + "tank.gif");
    this.icons[iCARGO] = loadImageIcon(PATH + "cargo.gif");
    this.icons[iBOMBER] = loadImageIcon(PATH + "bomber.gif");
    this.icons[iSENTRYTANK] = loadImageIcon(PATH + "sentrytank.gif");
    this.icons[iFULLCARGO] = loadImageIcon(PATH + "fullcargo.gif");

    this.icons[iEXPLOSION0] = loadImageIcon(PATH + "explosion0.gif");
    this.icons[iEXPLOSION1] = loadImageIcon(PATH + "explosion1.gif");
    this.icons[iEXPLOSION2] = loadImageIcon(PATH + "explosion2.gif");
    this.icons[iEXPLOSION3] = loadImageIcon(PATH + "explosion3.gif");
    this.icons[iEXPLOSION4] = loadImageIcon(PATH + "explosion4.gif");

    this.icons[iUNEXPLORED] = loadImageIcon(PATH + "unexplored.gif");
    this.icons[iANCHOR] = loadImageIcon(PATH + "anchor.gif");

    for (int x = 0; x < MAX_ICONS; x++) {
      this.images[x] = this.icons[x].getImage();
    }

  }

  public Image[] getImages() {
    return images;
  }

}
