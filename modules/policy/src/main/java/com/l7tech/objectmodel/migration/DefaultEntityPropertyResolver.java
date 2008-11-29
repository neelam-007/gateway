package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.*;

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
public class DefaultEntityPropertyResolver implements PropertyResolver {

    private static final Logger logger = Logger.getLogger(DefaultEntityPropertyResolver.class.getName());


    public Map<EntityHeader, Set<MigrationMapping>> getDependencies(final EntityHeaderRef source, Object entity, final Method property) throws MigrationException {

        if (!MigrationUtils.isDefaultDependency(property))
            throw new IllegalArgumentException("Cannot handle property: " + property);

        final MigrationMappingType type = MigrationUtils.getMappingType(property);

        String getterName = property.getName();
        String propName = getterName.startsWith("get") && getterName.length() > 3 ? getterName.substring(3, getterName.length()) : getterName;

        final Object propertyValue;
        try {
            propertyValue = property.invoke(entity);
        } catch (Exception e) {
            throw new MigrationException("Error getting property value for entity: " + entity, e);
        }

        Map<EntityHeader, Set<MigrationMapping>> result = new HashMap<EntityHeader, Set<MigrationMapping>>();

        if (propertyValue == null)
            return result;

        else if (propertyValue instanceof EntityHeader) {
            addToResult((EntityHeader) propertyValue,
                new MigrationMapping(source, (EntityHeader) propertyValue, propName, type), result);

        } else if (propertyValue instanceof Entity) {
            addToResult(MigrationUtils.getHeaderFromEntity((Entity) propertyValue),
                new MigrationMapping(source, MigrationUtils.getHeaderFromEntity((Entity) propertyValue), propName, type), result);

        } else { // array or set
            Collection input = null;
            if (propertyValue.getClass().isArray())
                input = Arrays.asList((Object[])propertyValue);
            else if (Collection.class.isAssignableFrom(propertyValue.getClass()))
                input = (Collection) propertyValue;

            if (input != null) {

                for(Object item : input) {
                    if (item instanceof EntityHeader)
                        addToResult((EntityHeader) item, new MigrationMapping(source, (EntityHeader) item, propName, type), result);
                    else if (item instanceof Entity)
                        addToResult(MigrationUtils.getHeaderFromEntity((Entity) item),
                            new MigrationMapping(source, MigrationUtils.getHeaderFromEntity((Entity) item), propName, type), result);
                }
            } else {
                // should not happen
                throw new MigrationException(this.getClass().getName() + " cannot handle properties of type: " + propertyValue.getClass().getName());
            }
        }

        logger.log(Level.FINE, "Found {0} headers for property {1}.", new Object[] { result.size(), property });

        return result;
    }

    void addToResult(EntityHeader header, MigrationMapping mapping, Map<EntityHeader,Set<MigrationMapping>> result) {
        Set<MigrationMapping> mappingsForHeader = result.get(header);
        if (mappingsForHeader == null) {
            mappingsForHeader = new HashSet<MigrationMapping>();
            result.put(header, mappingsForHeader);
        }
        mappingsForHeader.add(mapping);
    }

    public void applyMapping(Entity sourceEntity, String propName, Entity targetEntity) throws MigrationException {

        Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName);

        try {
            // todo: handle multi-value target values
            method.invoke(sourceEntity, targetEntity);
        } catch (Exception e) {
            throw new MigrationException("Error applying mapping: " , e);
        }
    }
}
