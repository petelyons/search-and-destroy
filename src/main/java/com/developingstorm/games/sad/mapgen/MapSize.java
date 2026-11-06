package com.developingstorm.games.sad.mapgen;

/**
 * The map sizes a new game can be generated at.
 */
public enum MapSize {
    SMALL(25, "Small 25×25", 3, 5),
    MEDIUM(50, "Medium 50×50", 4, 9),
    LARGE(75, "Large 75×75", 5, 12),
    XLARGE(100, "X-Large 100×100", 6, 15);

    private final int dimension;
    private final String label;
    private final int minContinents;
    private final int maxContinents;

    MapSize(int dimension, String label, int minContinents, int maxContinents) {
        this.dimension = dimension;
        this.label = label;
        this.minContinents = minContinents;
        this.maxContinents = maxContinents;
    }

    /** Width and height of the (square) map. */
    public int dimension() {
        return dimension;
    }

    /** Range of continents the generator aims for at this size. */
    int minContinents() {
        return minContinents;
    }

    int maxContinents() {
        return maxContinents;
    }

    @Override
    public String toString() {
        return label;
    }
}
