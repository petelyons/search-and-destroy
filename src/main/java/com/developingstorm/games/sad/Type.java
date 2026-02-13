package com.developingstorm.games.sad;

public enum Type {
    INFANTRY(
        builder()
            .description("Infantry")
            .abr("I")
            .travel(Travel.LAND)
            .dist(1)
            .hits(2)
            .cost(5)
            .vision(Vision.SURFACE)
            .visionDistance(1)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(1)
            .attack(1)
            .iconID(7)
    ),
    ARMOR(
        builder()
            .description("Armor")
            .abr("A")
            .travel(Travel.LAND)
            .dist(2)
            .hits(4)
            .cost(10)
            .vision(Vision.SURFACE)
            .visionDistance(1)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(2)
            .attack(2)
            .iconID(25)
    ),
    FIGHTER(
        builder()
            .description("Fighter")
            .abr("F")
            .travel(Travel.AIR)
            .dist(5)
            .hits(2)
            .cost(10)
            .vision(Vision.SURFACE)
            .visionDistance(3)
            .maxFuelMultiplier(4)
            .carryCount(0)
            .weight(1)
            .attack(1)
            .iconID(8)
    ),
    BOMBER(
        builder()
            .description("Bomber")
            .abr("B")
            .travel(Travel.AIR)
            .dist(4)
            .hits(2)
            .cost(15)
            .vision(Vision.SURFACE)
            .visionDistance(3)
            .maxFuelMultiplier(8)
            .carryCount(0)
            .weight(0)
            .attack(3)
            .iconID(26)
    ),
    CARGO(
        builder()
            .description("Cargo Plane")
            .abr("C")
            .travel(Travel.AIR)
            .dist(3)
            .hits(2)
            .cost(15)
            .vision(Vision.SURFACE)
            .visionDistance(5)
            .maxFuelMultiplier(6)
            .carryCount(1)
            .weight(0)
            .attack(0)
            .iconID(27)
    ),
    DESTROYER(
        builder()
            .description("Destroyer")
            .abr("DE")
            .travel(Travel.SEA)
            .dist(3)
            .hits(3)
            .cost(20)
            .vision(Vision.COMPLETE)
            .visionDistance(2)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(0)
            .attack(3)
            .iconID(10)
    ),
    TRANSPORT(
        builder()
            .description("Transport")
            .abr("TR")
            .travel(Travel.SEA)
            .dist(2)
            .hits(2)
            .cost(30)
            .vision(Vision.SURFACE)
            .visionDistance(1)
            .maxFuelMultiplier(-1)
            .carryCount(6)
            .weight(0)
            .attack(0)
            .iconID(9)
    ),
    SUBMARINE(
        builder()
            .description("Submarine")
            .abr("SU")
            .travel(Travel.SEA)
            .dist(2)
            .hits(4)
            .cost(30)
            .vision(Vision.WATER)
            .visionDistance(2)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(0)
            .attack(4)
            .iconID(11)
    ),
    CRUISER(
        builder()
            .description("Cruiser")
            .abr("CR")
            .travel(Travel.SEA)
            .dist(2)
            .hits(8)
            .cost(40)
            .vision(Vision.COMPLETE)
            .visionDistance(3)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(0)
            .attack(3)
            .iconID(12)
    ),
    CARRIER(
        builder()
            .description("Aircraft Carrier")
            .abr("AC")
            .travel(Travel.SEA)
            .dist(2)
            .hits(6)
            .cost(50)
            .vision(Vision.SURFACE)
            .visionDistance(2)
            .maxFuelMultiplier(-1)
            .carryCount(6)
            .weight(0)
            .attack(1)
            .iconID(14)
    ),
    BATTLESHIP(
        builder()
            .description("Battleship")
            .abr("BA")
            .travel(Travel.SEA)
            .dist(2)
            .hits(12)
            .cost(50)
            .vision(Vision.SURFACE)
            .visionDistance(2)
            .maxFuelMultiplier(-1)
            .carryCount(0)
            .weight(0)
            .attack(4)
            .iconID(13)
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

    Type(Builder builder) {
        this.description = builder.description;
        this.abr = builder.abr;
        this.travel = builder.travel;
        this.dist = builder.dist;
        this.hits = builder.hits;
        this.cost = builder.cost;
        this.vis = builder.vision;
        this.vdist = builder.visionDistance;
        this.max =
            builder.maxFuelMultiplier < 0
                ? -1
                : builder.maxFuelMultiplier * builder.dist;
        this.carryTypes = builder.carryTypes;
        this.carryCount = builder.carryCount;
        this.weight = builder.weight;
        this.attack = builder.attack;
        this.iconID = builder.iconID;
    }

    private static Builder builder() {
        return new Builder();
    }

    private static class Builder {

        private String description;
        private String abr;
        private Travel travel;
        private int hits;
        private int dist;
        private int cost;
        private Vision vision;
        private int visionDistance;
        private int maxFuelMultiplier;
        private Type[] carryTypes;
        private int carryCount;
        private int attack;
        private int iconID;
        private int weight;

        Builder description(String description) {
            this.description = description;
            return this;
        }

        Builder abr(String abr) {
            this.abr = abr;
            return this;
        }

        Builder travel(Travel travel) {
            this.travel = travel;
            return this;
        }

        Builder dist(int dist) {
            this.dist = dist;
            return this;
        }

        Builder hits(int hits) {
            this.hits = hits;
            return this;
        }

        Builder cost(int cost) {
            this.cost = cost;
            return this;
        }

        Builder vision(Vision vision) {
            this.vision = vision;
            return this;
        }

        Builder visionDistance(int visionDistance) {
            this.visionDistance = visionDistance;
            return this;
        }

        Builder maxFuelMultiplier(int maxFuelMultiplier) {
            this.maxFuelMultiplier = maxFuelMultiplier;
            return this;
        }

        Builder carryTypes(Type[] carryTypes) {
            this.carryTypes = carryTypes;
            return this;
        }

        Builder carryCount(int carryCount) {
            this.carryCount = carryCount;
            return this;
        }

        Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        Builder attack(int attack) {
            this.attack = attack;
            return this;
        }

        Builder iconID(int iconID) {
            this.iconID = iconID;
            return this;
        }
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
