package com.l7tech.policy.assertion.ext.store;

import java.util.List;

/**
 * This class is used to add or remove {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener.EventCallback EventCallbacks} that will be invoked when
 * key values are modified.
 */
public abstract class KeyValueStoreChangeEventListener {

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
    public static abstract class Event {

        /**
         * * Gets the ID of key that has been modified.
         *
         * @return the ID
         */
        public abstract String getKey();

        /**
         * Gets the operation.
         *
         * @return the operation.
         */
        public abstract Operation getOperation();
    }

    /**
     * The event callback interface. When key values are modified, {@link #onEvent(java.util.List) onEvent} method is invoked.
     */
    public interface EventCallback {
        /**
         * Invoked when key values are modified.
         *
         * @param events the events
         */
        void onEvent(List<Event> events);
    }

    /**
     * Adds an {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener.EventCallback EventCallback} that will be invoked when key values are modified.
     * <p/>
     * The {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener.EventCallback EventCallback} will receive a list of {@link KeyValueStoreChangeEventListener.Event Events}
     * when key values with the given key prefix have been modified.
     *
     * @param keyPrefix the key prefix
     * @param callback the callback to be invoked
     */
    public abstract void add(String keyPrefix, EventCallback callback);

    /**
     * Removes an {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener.EventCallback EventCallback} that has been added with the given key prefix.
     *
     * @param keyPrefix the key prefix
     * @param callback the callback to remove
     */
    public abstract void remove(String keyPrefix, EventCallback callback);
}