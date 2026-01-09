package com.developingstorm.games.sad;

import com.developingstorm.games.sad.ui.GameIcons;

public enum Type {
    /* DST HT CST VDST MX CRY W ATK */
    INFANTRY(
        "Infantry",
        "I",
        Travel.LAND,
        1,
        2,
        5,
        Vision.SURFACE,
        1,
        -1,
        null,
        0,
        1,
        1,
        GameIcons.iARMY
    ),
    ARMOR(
        "Armor",
        "A",
        Travel.LAND,
        2,
        4,
        10,
        Vision.SURFACE,
        1,
        -1,
        null,
        0,
        2,
        2,
        GameIcons.iTANK
    ),
    FIGHTER(
        "Fighter",
        "F",
        Travel.AIR,
        5,
        2,
        10,
        Vision.SURFACE,
        3,
        4,
        null,
        0,
        1,
        1,
        GameIcons.iFIGHTER
    ),
    BOMBER(
        "Bomber",
        "B",
        Travel.AIR,
        4,
        2,
        15,
        Vision.SURFACE,
        3,
        8,
        null,
        0,
        0,
        3,
        GameIcons.iBOMBER
    ),
    CARGO(
        "Cargo Plane",
        "C",
        Travel.AIR,
        3,
        2,
        15,
        Vision.SURFACE,
        5,
        6,
        null,
        1,
        0,
        0,
        GameIcons.iCARGO
    ),
    DESTROYER(
        "Destroyer",
        "DE",
        Travel.SEA,
        3,
        3,
        20,
        Vision.COMPLETE,
        2,
        -1,
        null,
        0,
        0,
        3,
        GameIcons.iDESTROYER
    ),
    TRANSPORT(
        "Transport",
        "TR",
        Travel.SEA,
        2,
        2,
        30,
        Vision.SURFACE,
        1,
        -1,
        null,
        6,
        0,
        0,
        GameIcons.iTRANSPORT
    ),
    SUBMARINE(
        "Submarine",
        "SU",
        Travel.SEA,
        2,
        4,
        30,
        Vision.WATER,
        2,
        -1,
        null,
        0,
        0,
        4,
        GameIcons.iSUBMARINE
    ),
    CRUISER(
        "Cruiser",
        "CR",
        Travel.SEA,
        2,
        8,
        40,
        Vision.COMPLETE,
        3,
        -1,
        null,
        0,
        0,
        3,
        GameIcons.iCRUISER
    ),
    CARRIER(
        "Aircraft Carrier",
        "AC",
        Travel.SEA,
        2,
        6,
        50,
        Vision.SURFACE,
        2,
        -1,
        null,
        6,
        0,
        1,
        GameIcons.iAIRCRAFTCARRIER
    ),
    BATTLESHIP(
        "Battleship",
        "BA",
        Travel.SEA,
        2,
        12,
        50,
        Vision.SURFACE,
        2,
        -1,
        null,
        0,
        0,
        4,
        GameIcons.iBATTLESHIP
    );

    static {
        // Initialize carry types after all enum constants are created
        CARGO.carryTypes = new Type[] { INFANTRY };
        TRANSPORT.carryTypes = new Type[] { INFANTRY, ARMOR };
        CARRIER.carryTypes = new Type[] { FIGHTER };
    }

    private final String description;
    private final String abr;
    private final Travel travel;
    private final int hits;
    private final int dist;
    private final int cost;
    private final Vision vis;
    private final int vdist;
    private final int max;
    private Type[] carryTypes; // mutable to allow static initialization
    private final int carryCount;
    private final int attack;
    private final int iconID;
    private final int weight;

    Type(
        String desc,
        String abr,
        Travel t,
        int dist,
        int hits,
        int cost,
        Vision vis,
        int vdist,
        int maxf,
        Type[] carryTypes,
        int carryCount,
        int weight,
        int attack,
        int iconID
    ) {
        this.description = desc;
        this.travel = t;
        this.dist = dist;
        this.hits = hits;
        this.cost = cost;
        this.vis = vis;
        this.vdist = vdist;
        this.max = maxf * dist;
        this.carryTypes = carryTypes;
        this.carryCount = carryCount;
        this.weight = weight;
        this.attack = attack;
        this.iconID = iconID;
        this.abr = abr;
    }

    public String getAbr() {
        return abr;
    }

    public int getCost() {
        return cost;
    }

    public int getDist() {
        return dist;
    }

    public int getHits() {
        return hits;
    }

    public Vision getVision() {
        return vis;
    }

    public int getVisionDistance() {
        return vdist;
    }

    public Travel getTravel() {
        return travel;
    }

    public int getFuel() {
        return max;
    }

    public Type[] getCarryTypes() {
        return carryTypes;
    }

    public int getCarryCount() {
        return carryCount;
    }

    public int getAttack() {
        return attack;
    }

    public int getIcon() {
        return iconID;
    }

    public int getWeight() {
        return weight;
    }

    public boolean canCarry(Type t) {
        if (carryCount == 0) {
            return false;
        }

        if (carryTypes == null) {
            return false;
        }

        for (Type t2 : carryTypes) {
            if (t == t2) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return description;
    }

    // Compatibility methods for migration from custom enum
    public static Type get(String name) {
        return valueOf(name.toUpperCase().replace(" ", "_"));
    }

    public static Type get(int id) {
        return values()[id];
    }

    public int getId() {
        return ordinal();
    }

    public String getName() {
        return name();
    }

    public Object toJsonLink() {
        return name();
    }

    public static int classItems() {
        return values().length;
    }

    // Legacy ID constants for compatibility
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
}
