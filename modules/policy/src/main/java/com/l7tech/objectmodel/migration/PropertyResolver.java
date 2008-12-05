package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderRef;

import java.util.Set;
import java.util.Map;
import java.lang.reflect.Method;

/**
 * Extracts the ItemHeader from a property value.
 */
public interface PropertyResolver {

    /**
     * Extracts dependency headers and mappings from the supplied entity object's property.
     *
     * @param source the dependant entity header; needed to create a MigrationMapping
     * @param entity the object from which the dependencies are extracted; usually an Entity, but not required to be.
     * @param property the method for which the property value is retrieved
     * @return a map with migration mappings as values, keyed on the (target) dependency header.
     */
    public Map<EntityHeader, Set<MigrationMapping>> getDependencies(EntityHeaderRef source, Object entity, final Method property) throws MigrationException;

    /**
     * Applies a mapped value to a property of an entity.
     *
     * @param sourceEntity the entity on which a new property value is set.
     * @param propName the property of the sourceEntity for which a new value is set.
     * @param targetEntity the new value that is set for the sourceEntity's propName
     */
    // todo: target value should be EntityHeader, and each resolver load the entity if it needs to
    public void applyMapping(Entity sourceEntity, String propName, Object targetValue, EntityHeader originalHeader) throws MigrationException;
}
