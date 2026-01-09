package com.developingstorm.games.sad.turn;

public enum OrderState {
    AWAITING_ORDERS("awaiting-orders"),
    READY("ready"),
    YIELDING("yielding"),
    SLEEPING("sleeping"),
    DONE("done");

    private final String description;

    OrderState(String description) {
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
