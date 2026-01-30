package com.developingstorm.games.sad.brain;

import com.developingstorm.games.sad.City;
import com.developingstorm.games.sad.Type;

/**
 * Represents a production pair: one coastal city producing transports
 * paired with one nearby inland city producing infantry.
 *
 * This pair operates as a dedicated amphibious assault factory,
 * ignoring most other strategic concerns to focus on invasion operations.
 */
public class ProductionPair {

    private final City coastalCity;  // Produces transports
    private final City inlandCity;   // Produces infantry
    private final int distance;      // Distance between the cities

    public ProductionPair(City coastalCity, City inlandCity, int distance) {
        if (!coastalCity.isCoastal()) {
            throw new IllegalArgumentException("Coastal city must be coastal: " + coastalCity);
        }
        if (inlandCity.isCoastal()) {
            throw new IllegalArgumentException("Inland city must not be coastal: " + inlandCity);
        }

        this.coastalCity = coastalCity;
        this.inlandCity = inlandCity;
        this.distance = distance;
    }

    public City getCoastalCity() {
        return coastalCity;
    }

    public City getInlandCity() {
        return inlandCity;
    }

    public int getDistance() {
        return distance;
    }

    /**
     * Get production type for the coastal city (always TRANSPORT).
     */
    public Type getCoastalProduction() {
        return Type.TRANSPORT;
    }

    /**
     * Get production type for the inland city (always INFANTRY).
     */
    public Type getInlandProduction() {
        return Type.INFANTRY;
    }

    /**
     * Check if a city is part of this pair.
     */
    public boolean contains(City city) {
        return city.equals(coastalCity) || city.equals(inlandCity);
    }

    /**
     * Get the rally point for this pair (coastal city location).
     */
    public com.developingstorm.games.hexboard.Location getRallyPoint() {
        return coastalCity.getLocation();
    }

    @Override
    public String toString() {
        return String.format("ProductionPair[coastal=%s, inland=%s, dist=%d]",
            coastalCity.getLocation(), inlandCity.getLocation(), distance);
    }
}
