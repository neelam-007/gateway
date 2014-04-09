package com.l7tech.policy.assertion.ext.entity;

/**
 * Enumerates supported entity types.<br/>
 * Note that future supported entities will go here.
 */
public enum CustomEntityType {
    /**
     * This represents a password entity, accessed using
     * {@link com.l7tech.policy.assertion.ext.password.SecurePasswordServices SecurePasswordServices}
     */
    SecurePassword,

    /**
     * This represents an entity backed with {@link com.l7tech.policy.assertion.ext.store.KeyValueStore KeyValueStore}
     */
    KeyValueStore
}
