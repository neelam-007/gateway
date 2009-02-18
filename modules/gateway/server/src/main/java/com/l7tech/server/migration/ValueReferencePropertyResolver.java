package com.l7tech.server.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.objectmodel.migration.MigrationMappingType;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Include;
import com.l7tech.server.EntityHeaderUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

/**
 * @author jbufu
 */
public class ValueReferencePropertyResolver extends AbstractPropertyResolver {
    private static final Logger logger = Logger.getLogger(ValueReferencePropertyResolver.class.getName());

    public ValueReferencePropertyResolver(PropertyResolverFactory factory) {
        super(factory);
    }

    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(final ExternalEntityHeader source, Object entity, Method property, final String propertyName) throws PropertyResolverException {

        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final MigrationMappingType mappingType = MigrationUtils.getMappingType(property);
        final ValueReferenceEntityHeader.Type valueType = MigrationUtils.getValueType(property);
        final boolean exported = MigrationUtils.isExported(property);
        final String displayValue = valueType.serialize(getPropertyValue(entity, property));

        return new HashMap<ExternalEntityHeader, Set<MigrationDependency>>() {{
            ValueReferenceEntityHeader dependencyHeader = new ValueReferenceEntityHeader(EntityHeaderUtils.toExternal(source), propertyName, valueType, displayValue);
            put(dependencyHeader, Collections.singleton(new MigrationDependency(source, dependencyHeader, propertyName, mappingType, exported)));
        }};
    }

    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {

        if ( !(targetHeader instanceof ValueReferenceEntityHeader) ) {
            throw new PropertyResolverException("Cannot apply mapping for non ValueReferenceEntityHeader target type: " + targetHeader);
        }

        ValueReferenceEntityHeader targetValueHeader = (ValueReferenceEntityHeader) targetHeader;

        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{sourceEntity, propName});

        // where is the value obtained from
        Object value;
        if (targetValue instanceof Entity) {
            // mapped to an entity's property from the target cluster
            value = getValue(targetValueHeader, targetValue);
        } else {
            // mapped to a value provided from the enterprise manager UI
            value = targetValueHeader.getValueType().deserialize((String) targetValue);
        }

        try {
            // apply dependency value
            if (value != null) {
                Method setter = MigrationUtils.setterForPropertyName(dereference(sourceEntity, propName), stripPropName(propName), value.getClass());
                setter.invoke(sourceEntity, value);
            }
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
        }
    }

    private static Object getValue(ValueReferenceEntityHeader vRefHeader, Object entity) throws PropertyResolverException {
        Object value;
        Object target = dereference(entity, vRefHeader.getPropertyName());
        String targetPropName = stripPropName(vRefHeader.getPropertyName());
        try {
            // get the actual value
            Method getter = MigrationUtils.getterForPropertyName(target, targetPropName);
            value = getPropertyValue(target, getter);
        } catch (Exception e) {
            throw new PropertyResolverException("Error retrieving value from value reference header: " + vRefHeader, e);
        }
        return value;
    }

    /**
     * If the property name is structured, return the actual method name (the last part of the structured name).
     */
    private static String stripPropName(String propertyName) {
        if (propertyName.contains(":")) {
            int index = propertyName.lastIndexOf(":");
            return index + 1 < propertyName.length()? propertyName.substring(index+1) : "";
        } else {
            return propertyName;
        }
    }

    /**
     * If the property name is structured, return the entity / object that actually owns the property.
     */
    private static Object dereference(Object sourceEntity, String propName) throws PropertyResolverException {
        if (propName.contains(":")) {
            Policy policy;
            if (sourceEntity instanceof PublishedService) {
                policy = ((PublishedService)sourceEntity).getPolicy();
            } else if (sourceEntity instanceof Include) {
                policy = ((Include)sourceEntity).retrieveFragmentPolicy();
            } else if (sourceEntity instanceof Policy) {
                policy = (Policy) sourceEntity;
            } else {
                throw new PropertyResolverException("Cannot dereference property " + propName + " for object of type " + sourceEntity.getClass());
            }

            return MigrationUtils.getAssertion(policy, propName);
        } else {
            return sourceEntity;
        }
    }
}
