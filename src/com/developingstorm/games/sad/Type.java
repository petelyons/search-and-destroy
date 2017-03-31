package com.developingstorm.games.sad;

import com.developingstorm.games.sad.ui.GameIcons;
import com.developingstorm.util.Enum;
import com.developingstorm.util.EnumClass;

public class Type extends Enum {

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((_abr == null) ? 0 : _abr.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Type other = (Type) obj;
    if (_abr == null) {
      if (other._abr != null)
        return false;
    } else if (!_abr.equals(other._abr))
      return false;
    return true;
  }

  private static final EnumClass _class = new EnumClass("Type");

  /* DST HT CST VDST MX CRY W ATK */
  public static final Type INFANTRY = new Type("Infantry", "I", Travel.LAND, 1, 2, 5,
      Vision.SURFACE, 1, -1, null, 0, 1, 1, GameIcons.iARMY);
  public static final Type ARMOR = new Type("Armor", "A", Travel.LAND, 2, 4, 10,
      Vision.SURFACE, 1, -1, null, 0, 2, 2, GameIcons.iTANK);
  public static final Type FIGHTER = new Type("Fighter", "F", Travel.AIR, 5, 2, 10,
      Vision.SURFACE, 3, 4, null, 0, 1, 1, GameIcons.iFIGHTER);
  public static final Type BOMBER = new Type("Bomber", "B", Travel.AIR, 4, 2, 15,
      Vision.SURFACE, 3, 8, null, 0, 0, 3, GameIcons.iBOMBER);
  public static final Type CARGO = new Type("Cargo Plane", "C", Travel.AIR, 3, 2, 15,
      Vision.SURFACE, 5, 6, new Type[] { INFANTRY }, 1, 0, 0, GameIcons.iCARGO);
  public static final Type DESTROYER = new Type("Destroyer", "DE", Travel.SEA, 3, 3,
      20, Vision.COMPLETE, 2, -1, null, 0, 0, 3, GameIcons.iDESTROYER);
  public static final Type TRANSPORT = new Type("Transport", "TR", Travel.SEA, 2, 2,
      30, Vision.SURFACE, 1, -1, new Type[] { INFANTRY, ARMOR }, 6, 0, 0,
      GameIcons.iTRANSPORT);
  public static final Type SUBMARINE = new Type("Submarine", "SU", Travel.SEA, 2, 4,
      30, Vision.WATER, 2, -1, null, 0, 0, 4, GameIcons.iSUBMARINE);
  public static final Type CRUISER = new Type("Cruiser", "CR", Travel.SEA, 2, 8, 40,
      Vision.COMPLETE, 3, -1, null, 0, 0, 3, GameIcons.iCRUISER);
  public static final Type CARRIER = new Type("Aircraft Carrier", "AC", Travel.SEA,
      2, 6, 50, Vision.SURFACE, 2, -1, new Type[] { FIGHTER }, 6, 0, 1,
      GameIcons.iAIRCRAFTCARRIER);
  public static final Type BATTLESHIP = new Type("Battleship", "BA", Travel.SEA, 2,
      12, 50, Vision.SURFACE, 2, -1, null, 0, 0, 4, GameIcons.iBATTLESHIP);
  
  public static final int INFANTRY_ID = 0;
  public static final int ARMOR_ID = 1;
  public static final int FIGHTER_ID = 2;
  public static final int BOMBER_ID = 3;
  public static final int CARGO_ID = 4;
  public static final int DESTROYER_ID = 5;
  public static final int TRANSPORT_ID = 6;
  public static final int SUBMARINE_ID = 7;
  public static final int CRUISER_ID = 8;
  public static final int CARRIER_ID = 9;
  public static final int BATTLESHIP_ID = 10;
  
  static {
    assert(INFANTRY_ID == INFANTRY.getId());
    assert(ARMOR_ID == ARMOR.getId());
    assert(FIGHTER_ID == FIGHTER.getId());
    assert(BOMBER_ID == BOMBER.getId());
    assert(CARGO_ID == CARGO.getId());
    assert(DESTROYER_ID == DESTROYER.getId());
    assert(TRANSPORT_ID == TRANSPORT.getId());
    assert(SUBMARINE_ID == SUBMARINE.getId());
    assert(CRUISER_ID == CRUISER.getId());
    assert(CARRIER_ID == CARRIER.getId());
    assert(BATTLESHIP_ID == BATTLESHIP.getId());

  }
  
  
  
  private final String _abr;
  private final Travel _travel;
  private final int _hits;
  private final int _dist;
  private final int _cost;
  private final Vision _vis;
  private final int _vdist;
  private final int _max;
  private final Type[] _carryTypes;
  private final int _carryCount;
  private final int _attack;
  private final int _iconID;
  private final int _weight;
  
  public static Type get(String name) {
    return (Type) _class.get(name);
  }

  
  public static Type get(int id) {
    return (Type) _class.get(id);
  }
  
  private Type(String desc, String abr, Travel t, int dist, int hits, int cost,
      Vision vis, int vdist, int maxf, Type[] carryTypes, int carryCount,
      int weight, int attack, int iconID) {
    super(_class, desc);
    _travel = t;
    _dist = dist;
    _hits = hits;
    _cost = cost;
    _vis = vis;
    _vdist = vdist;
    _max = maxf * _dist;
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

  public int getFuel() {
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
  
  
  public static int classItems() {
    return _class.items();
  }

}