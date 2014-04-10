package com.l7tech.policy.assertion.ext.entity.panels;

import com.l7tech.policy.assertion.ext.cei.UsesConsoleContext;
import com.l7tech.policy.assertion.ext.entity.CustomEntityDescriptor;

import javax.swing.*;

/**
 * Represents the user interface object for creating the missing external entity.<br/>
 * In case when its appropriate to modify the missing entity, before creating, implement this abstract class, providing
 * code for modifying the missing entity.
 *
 * @param <E>    {@link CustomEntityDescriptor} or its subclass, specifying the external entity type.
 */
public abstract class CustomEntityCreateUiPanel<E extends CustomEntityDescriptor> extends JPanel implements UsesConsoleContext<String, Object> {
    private static final long serialVersionUID = -5069372108298175819L;

    /**
     * Initialize the UI using the missing {@code entity} object.
     * @param entity    the missing entity object
     */
    public abstract void initialize(E entity);

    /**
     * Validate all entity properties, entered by the user.
     * Return the modified missing entity object when validation succeed, or {@code null} otherwise.
     */
    public abstract E validateEntity();
}
