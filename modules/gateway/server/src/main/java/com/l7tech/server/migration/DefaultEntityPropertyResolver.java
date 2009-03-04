package com.l7tech.server.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityHeaderUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

/**
 * Implementation for the default extraction of entity headers from property values.
 *
 * The properties handled by this default implementation are those returning on of the following types:
 * <ul>
 * <li>Entity</li>
 * <li>EntityHeader</li>
 * <li>Collections and arrays of the above</li>
 * </ul>
 *  
 * @author jbufu
 */
public class DefaultEntityPropertyResolver extends AbstractPropertyResolver {

    private static final Logger logger = Logger.getLogger(DefaultEntityPropertyResolver.class.getName());

    public DefaultEntityPropertyResolver(PropertyResolverFactory factory, Type type) {
        super(factory, type);
    }

    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{propertyName, source});
        if (!MigrationUtils.isDefaultDependency(property))
            throw new IllegalArgumentException("Cannot handle property: " + property);

        final MigrationMappingSelection mappingType = MigrationUtils.getMappingType(property);
        final MigrationMappingSelection valueMappingType = MigrationUtils.getValueMappingType(property);
        final ExternalEntityHeader.ValueType valueType = MigrationUtils.getValueType(property);
        final boolean exported = MigrationUtils.isExported(property);

        final Object propertyValue = getPropertyValue(entity, property);

        Map<ExternalEntityHeader, Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();

        if (propertyValue == null)
            return result;

        else if (propertyValue instanceof EntityHeader) {
            ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal((EntityHeader) propertyValue);
            externalHeader.setValueMapping(valueMappingType, valueType, propertyValue);
            addToResult(externalHeader, new MigrationDependency(source, externalHeader, propertyName, getType(), mappingType, exported), result);

        } else if (propertyValue instanceof Entity) {
            ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity((Entity) propertyValue));
            externalHeader.setValueMapping(valueMappingType, valueType, propertyValue);
            addToResult(externalHeader, new MigrationDependency(source, externalHeader, propertyName, getType(), mappingType, exported), result);

        } else { // array or set
            Collection input = null;
            if (propertyValue.getClass().isArray())
                input = Arrays.asList((Object[])propertyValue);
            else if (Collection.class.isAssignableFrom(propertyValue.getClass()))
                input = (Collection) propertyValue;

            if (input != null) {
                for(Object item : input) {
                    if (item instanceof EntityHeader) {
                        ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal((EntityHeader) item);
                        externalHeader.setValueMapping(valueMappingType, valueType, item);
                        addToResult(externalHeader, new MigrationDependency(source, externalHeader, propertyName, getType(), mappingType, exported), result);
                    }
                    else if (item instanceof Entity) {
                        ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity( (Entity) item ));
                        externalHeader.setValueMapping(valueMappingType, valueType, item);
                        addToResult(externalHeader, new MigrationDependency(source, externalHeader, propertyName, getType(), mappingType, exported), result);
                    }
                }
            } else {
                // should not happen
                throw new PropertyResolverException(this.getClass().getName() + " cannot handle properties of type: " + propertyValue.getClass().getName());
            }
        }

        logger.log(Level.FINE, "Found {0} headers for property {1}.", new Object[] { result.size(), property });

        return result;
    }

    public void addToResult(ExternalEntityHeader header, MigrationDependency dep, Map<ExternalEntityHeader,Set<MigrationDependency>> result) {
        Set<MigrationDependency> mappingsForHeader = result.get(header);
        if (mappingsForHeader == null) {
            mappingsForHeader = new HashSet<MigrationDependency>();
            result.put(header, mappingsForHeader);
        }
        logger.log(Level.FINEST, "Found migration dependency: {0}", dep);
        mappingsForHeader.add(dep);
    }

    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        if (! (sourceEntity instanceof Entity))
            throw new PropertyResolverException("Cannot handle non-entities; received: " + (sourceEntity == null ? null : sourceEntity.getClass()));

        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{EntityHeaderUtils.fromEntity((Entity) sourceEntity), propName});
        Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName, targetValue.getClass());

        try {
            // todo: handle multi-value target values
            method.invoke(sourceEntity, targetValue);
        } catch (Exception e) {
            throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
        }
    }
}
