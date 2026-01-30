package com.developingstorm.games.sad.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/**
 * Thread-safe event bus for game events.
 *
 * Key features:
 * - Can be called from any thread (game thread or EDT)
 * - Automatically marshalls all listener callbacks to EDT
 * - Listeners can filter events by type
 * - No blocking - events are queued and processed asynchronously
 */
public class GameEventBus {
    // CopyOnWriteArrayList for thread-safe iteration without locking
    private final List<ListenerRegistration> listeners = new CopyOnWriteArrayList<>();

    /**
     * Register a listener to receive game events.
     *
     * @param listener The listener to register
     */
    public void addListener(GameEventListener listener) {
        GameEventType[] interestedTypes = listener.getInterestedEventTypes();
        listeners.add(new ListenerRegistration(listener, interestedTypes));
    }

    /**
     * Unregister a listener.
     *
     * @param listener The listener to remove
     */
    public void removeListener(GameEventListener listener) {
        listeners.removeIf(reg -> reg.listener == listener);
    }

    /**
     * Publish an event to all interested listeners.
     * This method can be called from any thread.
     * Listeners will be notified on the EDT asynchronously.
     *
     * @param event The event to publish
     */
    public void publish(GameEvent event) {
        if (event == null) {
            return;
        }

        // Get snapshot of current listeners
        List<GameEventListener> interestedListeners = new ArrayList<>();
        for (ListenerRegistration reg : listeners) {
            if (reg.isInterested(event.getEventType())) {
                interestedListeners.add(reg.listener);
            }
        }

        // If already on EDT, notify synchronously
        if (SwingUtilities.isEventDispatchThread()) {
            notifyListeners(interestedListeners, event);
        } else {
            // Otherwise, marshall to EDT
            SwingUtilities.invokeLater(() -> notifyListeners(interestedListeners, event));
        }
    }

    /**
     * Notify all listeners of an event.
     * Must be called on EDT.
     */
    private void notifyListeners(List<GameEventListener> listeners, GameEvent event) {
        for (GameEventListener listener : listeners) {
            try {
                listener.onGameEvent(event);
            } catch (Exception e) {
                // Log error but don't let one listener break others
                System.err.println("Error notifying listener " + listener.getClass().getName() +
                                 " of event " + event + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Remove all listeners.
     * Useful for cleanup during shutdown.
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * Internal class to track listener registrations with their event type filters.
     */
    private static class ListenerRegistration {
        final GameEventListener listener;
        final GameEventType[] interestedTypes;

        ListenerRegistration(GameEventListener listener, GameEventType[] interestedTypes) {
            this.listener = listener;
            this.interestedTypes = interestedTypes;
        }

        boolean isInterested(GameEventType eventType) {
            // null means interested in all events
            if (interestedTypes == null) {
                return true;
            }

            // Check if event type is in the interested list
            return Arrays.asList(interestedTypes).contains(eventType);
        }
    }
}
