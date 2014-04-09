package com.l7tech.policy.assertion.ext.entity;

import java.io.Serializable;

/**
 * Serializer class for external entities, like entities stored into
 * {@link com.l7tech.policy.assertion.ext.store.KeyValueStore KeyValueStore}.
 * <p/>
 * When an entity depends on another entity or entities, then the Custom Assertion Developer must implement this interface.
 * Otherwise the Gateway will not be able to identify the dependent entities and will not be able to migrate them.
 *
 * @param <T>    Specifies the referenced Entity type.
 */
public interface CustomEntitySerializer<T> extends Serializable {
    /**
     * Serialize specified <tt>entity</tt> into byte array.
     *
     * @param entity    the entity object, can be {@code null}
     * @return entity row-bytes or {@code null} if <tt>entity</tt> cannot be serialized.
     */
    byte[] serialize(T entity);

    /**
     * Deserialize the bytes into a {@link T} object.<br/>
     * Note that the bytes can be from a previous version of the entity
     * it's the responsibility of custom assertion developer to be able to deserialize previous versions of the object bytes.
     *
     * @param bytes    row-bytes, can be {@code null}.
     * @return the object {@link T} associated with the <tt>bytes</tt>, or {@code null} if <tt>bytes</tt> are
     * {@code null} or incompatible with the object.
     */
    T deserialize(byte[] bytes);
}
