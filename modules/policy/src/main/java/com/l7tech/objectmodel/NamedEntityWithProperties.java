package com.l7tech.objectmodel;

import javax.persistence.Column;
import javax.persistence.Lob;

/**
 * This is a named entity with properties.
 *
 * @author Victor Kazakov
 */
public interface NamedEntityWithProperties extends NamedEntity {

    /**
     * Get the value for the property key given. If no such key exists null is returned.
     *
     * @param propertyName The key of the property to get the value for.
     * @return The property value. Null is no such property exists.
     */
    public String getProperty(String propertyName);

    /**
     * Sets the property to the given value.
     *
     * @param propertyName  The property to set.
     * @param propertyValue The value to set the property to.
     */
    public void setProperty(String propertyName, String propertyValue);
}
