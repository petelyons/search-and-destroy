package com.developingstorm.games.sad;

import com.developingstorm.games.sad.ui.GameIcons;
import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

public class Type extends Enum {

  private static final EnumClass _class = new EnumClass("Type");

  /* DST HT CST VDST MX CRY W ATK */
  public static Type INFANTRY = new Type("Infantry", "I", Travel.LAND, 1, 2, 5,
      Vision.SURFACE, 1, -1, null, 0, 1, 1, GameIcons.iARMY);
  public static Type ARMOR = new Type("Armor", "A", Travel.LAND, 2, 4, 10,
      Vision.SURFACE, 1, -1, null, 0, 2, 2, GameIcons.iTANK);
  public static Type FIGHTER = new Type("Fighter", "F", Travel.AIR, 5, 2, 10,
      Vision.SURFACE, 3, 15, null, 0, 1, 1, GameIcons.iFIGHTER);
  public static Type BOMBER = new Type("Bomber", "B", Travel.AIR, 4, 2, 15,
      Vision.SURFACE, 3, 20, null, 0, 0, 3, GameIcons.iBOMBER);
  public static Type CARGO = new Type("Cargo Plane", "C", Travel.AIR, 3, 2, 15,
      Vision.SURFACE, 3, 20, new Type[] { INFANTRY }, 1, 0, 0, GameIcons.iCARGO);
  public static Type DESTROYER = new Type("Destroyer", "DE", Travel.SEA, 3, 3,
      20, Vision.COMPLETE, 2, -1, null, 0, 0, 3, GameIcons.iDESTROYER);
  public static Type TRANSPORT = new Type("Transport", "TR", Travel.SEA, 2, 2,
      30, Vision.SURFACE, 1, -1, new Type[] { INFANTRY, ARMOR }, 6, 0, 0,
      GameIcons.iTRANSPORT);
  public static Type SUBMARINE = new Type("Submarine", "SU", Travel.SEA, 2, 4,
      30, Vision.WATER, 2, -1, null, 0, 0, 4, GameIcons.iSUBMARINE);
  public static Type CRUISER = new Type("Cruiser", "CR", Travel.SEA, 2, 8, 40,
      Vision.COMPLETE, 3, -1, null, 0, 0, 3, GameIcons.iCRUISER);
  public static Type CARRIER = new Type("Aircraft Carrier", "AC", Travel.SEA,
      2, 6, 50, Vision.SURFACE, 2, -1, new Type[] { FIGHTER }, 6, 0, 1,
      GameIcons.iAIRCRAFTCARRIER);
  public static Type BATTLESHIP = new Type("Battleship", "BA", Travel.SEA, 2,
      12, 50, Vision.SURFACE, 2, -1, null, 0, 0, 4, GameIcons.iBATTLESHIP);

  static final int TYPEMAX = 8;

  String _abr;
  Travel _travel;
  int _hits;
  int _dist;
  int _cost;
  Vision _vis;
  int _vdist;
  int _max;
  Type[] _carryTypes;
  int _carryCount;
  int _attack;
  int _iconID;
  int _weight;

  private Type(String desc) {
    super(_class, desc);
  }

  private Type(String desc, String abr, Travel t, int dist, int hits, int cost,
      Vision vis, int vdist, int max, Type[] carryTypes, int carryCount,
      int weight, int attack, int iconID) {
    super(_class, desc);
    _travel = t;
    _dist = dist;
    _hits = hits;
    _cost = cost;
    _vis = vis;
    _vdist = vdist;
    _max = max;
    _carryTypes = carryTypes;
    _carryCount = carryCount;
    _weight = weight;
    _attack = attack;
    _iconID = iconID;
    _abr = abr;
  }

  public String getAbr() {
    return _abr;
  }

  public int getCost() {
    return _cost;
  }

  public int getDist() {
    return _dist;
  }

  public int getHits() {
    return _hits;
  }

  public Vision getVision() {
    return _vis;
  }

  public int getVisionDistance() {
    return _vdist;
  }

  public Travel getTravel() {
    return _travel;
  }

  public int getMaxTravel() {
    return _max;
  }

  public Type[] getCarryTypes() {
    return _carryTypes;
  }

  public int getCarryCount() {
    return _carryCount;
  }

  public int getAttack() {
    return _attack;
  }

  public int getIcon() {
    return _iconID;
  }

  public int getWeight() {
    return _weight;
  }

  public boolean canCarry(Type t) {
    if (_carryCount == 0) {
      return false;
    }

    for (Type t2 : _carryTypes) {
      if (t == t2) {
        return true;
      }
    }
    return false;

  }

}