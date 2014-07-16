package com.l7tech.policy.assertion.ext.store;

import java.util.List;

/**
 * This class is used to add or remove {@link com.l7tech.policy.assertion.ext.store.KeyValueStoreChangeEventListener.Callback Callbacks} that will be invoked when
 * key values are modified.
 */
public abstract class KeyValueStoreChangeEventListener extends KeyValueStoreListener<KeyValueStoreChangeEventListener.Callback> {

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
    public interface Callback {

        /**
         * Provide the key prefix to identify all key values that should be invoked when modified for this Callback.
         *
         * @return the key prefix
         */
        String getKeyPrefix();

        /**
         * Invoked when key values are modified.
         *
         * @param events the events
         */
        void onEvent(List<Event> events);
    }
}