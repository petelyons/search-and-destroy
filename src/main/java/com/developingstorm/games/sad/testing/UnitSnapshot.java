package com.developingstorm.games.sad.testing;

import com.developingstorm.games.hexboard.Location;
import com.developingstorm.games.sad.Travel;

/**
 * One unit's state at the end of its owner's turn, as seen by {@link BehaviorTracker}.
 *
 * @param idleExempt carried, sentried, or a transport waiting in a city — not counted as idle
 * @param openSea on water with no adjacent land
 * @param moveDest destination of the unit's Move order, or null
 * @param landContinentId continent id when on land, else -1
 */
public record UnitSnapshot(
    long id,
    Travel travel,
    Location loc,
    boolean carried,
    boolean idleExempt,
    boolean transport,
    int cargo,
    boolean openSea,
    Location moveDest,
    int landContinentId
) {}
