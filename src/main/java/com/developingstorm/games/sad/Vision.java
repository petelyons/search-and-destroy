package com.developingstorm.games.sad;

public enum Vision {
    NONE("none"),
    SURFACE("surface"),
    COMPLETE("complete"),
    WATER("water");

    private final String description;

    Vision(String description) {
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
