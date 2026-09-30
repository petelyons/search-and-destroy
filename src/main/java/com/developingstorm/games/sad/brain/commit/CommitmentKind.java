package com.developingstorm.games.sad.brain.commit;

/**
 * What a unit has committed to, in priority order. A commitment can only be pre-empted by a
 * strictly higher-priority kind.
 */
public enum CommitmentKind {
    OPERATION(5),
    DEFEND(4),
    CAPTURE_CITY(3),
    STAGE(2),
    EXPLORE(1);

    private final int priority;

    CommitmentKind(int priority) {
        this.priority = priority;
    }

    public boolean outranks(CommitmentKind other) {
        return this.priority > other.priority;
    }
}
