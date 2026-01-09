package com.developingstorm.games.sad;

/**
 * Exception thrown when a game operation fails.
 * This is a RuntimeException to avoid forcing callers to handle checked exceptions,
 * while still providing meaningful error information.
 */
public class GameException extends RuntimeException {

    /**
     * Constructs a new GameException with the specified detail message.
     *
     * @param message the detail message
     */
    public GameException(String message) {
        super(message);
    }

    /**
     * Constructs a new GameException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
