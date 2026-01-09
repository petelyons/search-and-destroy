package com.developingstorm.games.sad;

/**
 * Response codes returned from order execution
 */
public enum ResponseCode {
    STEP_COMPLETE("Step-Complete"),
    TURN_COMPLETE("Turn-Complete"),
    ORDER_AND_TURN_COMPLETE("Order-And-Turn-Complete"),
    YIELD_PASS("Yield-Pass"),
    DIED("Died"),
    BLOCKED("Blocked"),
    CANCEL_ORDER("Cancel-Order"),
    ORDER_COMPLETE("Order-Complete(Needs-New-Order)");

    private final String displayName;

    ResponseCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
