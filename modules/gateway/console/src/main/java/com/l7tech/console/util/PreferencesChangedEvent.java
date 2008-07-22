/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.util;

import org.springframework.context.ApplicationEvent;

/**
 * @author emil
 * @version Dec 3, 2004
 */
public class PreferencesChangedEvent extends ApplicationEvent {
    /**
     * Create a new PreferencesChangedEvent.
     *
     * @param source the component that published the event
     */
    public PreferencesChangedEvent(Object source) {
        super(source);
    }


    /**
     * Constructs a new <code>PreferencesChangedEvent</code>.
     *
     * @param source       The bean that fired the event.
     * @param propertyName The programmatic name of the property
     *                     that was changed.
     * @param oldValue     The old value of the property.
     * @param newValue     The new value of the property.
     */
    public PreferencesChangedEvent(Object source, String propertyName,
                               Object oldValue, Object newValue) {
        super(source);
        this.propertyName = propertyName;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    /**
     * Gets the programmatic name of the property that was changed.
     *
     * @return The programmatic name of the property that was changed.
     *         May be null if multiple properties have changed.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the new value for the property, expressed as an Object.
     *
     * @return The new value for the property, expressed as an Object.
     *         May be null if multiple properties have changed.
     */
    public Object getNewValue() {
        return newValue;
    }

    /**
     * Gets the old value for the property, expressed as an Object.
     *
     * @return The old value for the property, expressed as an Object.
     *         May be null if multiple properties have changed.
     */
    public Object getOldValue() {
        return oldValue;
    }

    /**
     * name of the property that changed.  May be null, if not known.
     *
     * @serial
     */
    private String propertyName;

    /**
     * New value for property.  May be null if not known.
     *
     * @serial
     */
    private Object newValue;

    /**
     * Previous value for property.  May be null if not known.
     *
     * @serial
     */
    private Object oldValue;
}