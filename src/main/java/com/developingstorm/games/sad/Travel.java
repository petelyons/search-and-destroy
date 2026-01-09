package com.developingstorm.games.sad;

public enum Travel {
    LAND("land"),
    SEA("sea"),
    AIR("air");

    private final String description;

    Travel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
