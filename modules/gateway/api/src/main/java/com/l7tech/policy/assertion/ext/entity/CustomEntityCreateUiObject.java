package com.l7tech.policy.assertion.ext.entity;

import com.l7tech.policy.assertion.ext.cei.UsesConsoleContext;

/**
 * Represents the user interface object for creating the missing external entity.<br/>
 * In case when its appropriate to modify the missing entity before creating implement this interface, providing
 * code for modifying the missing entity.
 *
 * @param <E>    {@link CustomEntityDescriptor} or its subclass, specifying the external entity type.
 */
public interface CustomEntityCreateUiObject<E extends CustomEntityDescriptor> extends UsesConsoleContext<String, Object> {
    /**
     * Initialize the UI using the missing {@code entity} object.
     * @param entity    the missing entity object
     */
    void initialize(E entity);

    /**
     * Validate all entity properties, entered by the user.
     * Return the modified missing entity object when validation succeed, or {@code null} otherwise.
     */
    E validateEntity();
}
