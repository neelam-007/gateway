package com.l7tech.server.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.objectmodel.migration.MigrationMappingType;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Include;

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

    public Map<EntityHeader, Set<MigrationDependency>> getDependencies(final EntityHeader source, Object entity, final Method property, final String propertyName) throws PropertyResolverException {

        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final MigrationMappingType type = MigrationUtils.getMappingType(property);
        final boolean exported = MigrationUtils.isExported(property);

        try {
            property.invoke(entity);
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting property value for entity: " + entity, e);
        }

        return new HashMap<EntityHeader, Set<MigrationDependency>>() {{
            ValueReferenceEntityHeader dependencyHeader = new ValueReferenceEntityHeader(source, propertyName);
            put(dependencyHeader, Collections.singleton(new MigrationDependency(source, dependencyHeader, propertyName, type, exported)));
        }};
    }

    public void applyMapping(Object sourceEntity, String propName, EntityHeader targetHeader, Object targetValue, EntityHeader originalHeader) throws PropertyResolverException {
        if ( !(targetHeader instanceof ValueReferenceEntityHeader) )
            throw new PropertyResolverException("Cannot apply mapping for non ValueReferenceEntityHeader target type: " + targetHeader);

        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{sourceEntity, propName});

        // where is the value obtained from
        Object target = dereference(targetValue, ((ValueReferenceEntityHeader)targetHeader).getPropertyName());
        String targetPropName = stripPropName(((ValueReferenceEntityHeader)targetHeader).getPropertyName());

        try {
            // get the actual value
            Method getter = MigrationUtils.getterForPropertyName(target, targetPropName);
            Object value = getter.invoke(target);

            // apply dependency value
            if (value != null) {
                Method setter = MigrationUtils.setterForPropertyName(sourceEntity, propName, value.getClass());
                setter.invoke(sourceEntity, value);
            }
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
        }
    }

    private String stripPropName(String propertyName) {
        if (propertyName.contains(":")) {
            int index = propertyName.lastIndexOf(":");
            return index + 1 < propertyName.length()? propertyName.substring(index+1) : "";
        } else {
            return propertyName;
        }
    }

    private Object dereference(Object sourceEntity, String propName) throws PropertyResolverException {
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
