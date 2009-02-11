package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.ExternalEntityHeader;

import java.util.Set;
import java.util.Map;
import java.lang.reflect.Method;

/**
 * Extracts the ItemHeader from a property value.
 */
public interface PropertyResolver {

    public static enum Type {
        DEFAULT,
        SERVICE,
        SERVICE_ALIAS,
        SERVICE_DOCUMENT,
        POLICY,
        POLICY_ALIAS,
        ASSERTION,
        ID_PROVIDER_CONFIG,
        USERGROUP,
        VALUE_REFERENCE,
        SSGKEY,
        SERVER_VARIABLE
    }

    /**
     * Extracts dependency headers and mappings from the supplied entity object's property.
     *
     * @param source the dependant entity header; needed to create a MigrationMapping
     * @param entity the object from which the dependencies are extracted; usually an Entity, but not required to be.
     * @param property the method for which the property value is retrieved
     * @param propertyName the property's name; normally the method's name, but can be different,
     *                     e.g. in the case where structured names are required
     * @return a map with migration mappings as values, keyed on the (target) dependency header.
     */
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, final Method property, String propertyName)
        throws PropertyResolverException;

    /**
     * Applies a mapped value to a property of an entity.
     *
     * @param sourceEntity the entity on which a new property value is set.
     * @param propName the property of the sourceEntity for which a new value is set.
     * @param targetEntity the new value that is set for the sourceEntity's propName
     */
    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader)
        throws PropertyResolverException;

    /**
     * Retrieves the resolver for a given property. Needed if a resolver need to process dependencies itself,
     * rather than returning new dependencies (to the migration manager) directly.
     *
     * @param property  The property method for which a resolver is retrieved
     * @return          The property resolver for the specified property
     */
    public PropertyResolver getResolver(Method property) throws PropertyResolverException;
}
