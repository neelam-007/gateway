package com.l7tech.policy.assertion.ext.store;

import java.util.List;

/**
 * The listener interface for receiving events when key values are modified. When key values are modified, {@link #onEvent(java.util.List)} onEvent}
 * is invoked.
 */
public interface KeyValueStoreChangeEventListener {

    /**
     * The enumeration of operations.
     */
    public enum Operation {
        CREATE,
        DELETE,
        UPDATE
    }

    /**
     * The event that indicates that a key value has been modified.
     */
    interface Event {

        /**
         * * Gets the ID of key that has been modified.
         *
         * @return the ID
         */
        String getKey();

        /**
         * Gets the operation.
         *
         * @return the operation.
         */
        Operation getOperation();
    }

    /**
     * Invoked when key values are modified.
     *
     * @param events the events
     */
    void onEvent(List<KeyValueStoreChangeEventListener.Event> events);
}