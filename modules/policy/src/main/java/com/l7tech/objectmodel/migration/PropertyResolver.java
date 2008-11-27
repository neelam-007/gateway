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
    public Map<EntityHeader, Set<MigrationMapping>> getDependencies(EntityHeaderRef source, Object entity, final Method property) throws PropertyResolverException;

    // todo: applyMapping(newValue, propName, entity)
}
