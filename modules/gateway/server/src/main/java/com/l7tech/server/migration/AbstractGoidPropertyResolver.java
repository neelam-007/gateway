package com.l7tech.server.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityHeaderUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistent GOID properties.
 *
 * The EntityType (or implementing entity class) is needed for looking up entities.
 * This abstract resolver needs to be extended by specialized resolvers that know that entity types and pass it through.
 *
 * @author jbufu
 */
public abstract class AbstractGoidPropertyResolver extends AbstractPropertyResolver {
    private static final Logger logger = Logger.getLogger(AbstractGoidPropertyResolver.class.getName());

    private EntityFinder entityFinder;

    protected AbstractGoidPropertyResolver(PropertyResolverFactory factory, Type type, EntityFinder finder) {
        super(factory, type);
        this.entityFinder = finder;
    }

    public abstract EntityType getTargetType();

    // gets a persistent entity's header out of a long/OID property and its type
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final MigrationMappingSelection mappingType = MigrationUtils.getMappingType(property);
        final MigrationMappingSelection valueMappingType = MigrationUtils.getValueMappingType(property);
        final ExternalEntityHeader.ValueType valueType = MigrationUtils.getValueType(property);
        final boolean exported = MigrationUtils.isExported(property);

        EntityType targetType = getTargetType();

        final Goid goid;
        try {
            goid = (Goid) getPropertyValue(entity, property);
        } catch (RuntimeException e) {
            throw new PropertyResolverException("Error getting property value for entity: " + entity, e);
        }

        Map<ExternalEntityHeader,Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
        try {
            ExternalEntityHeader externalHeader = EntityHeaderUtils.toExternal(
                EntityHeaderUtils.fromEntity(entityFinder.find(EntityTypeRegistry.getEntityClass(targetType), goid)) );
            externalHeader.setValueMapping(valueMappingType, valueType, goid);
            result.put(externalHeader, Collections.singleton(new MigrationDependency(source, externalHeader, propertyName, getType(), mappingType, exported)));
        } catch (FindException e) {
            logger.log(Level.FINE, "No entity found for type: {0} oid: {1}.", new Object[]{targetType, goid});
        }
        return result;
    }

    // assigns the targetEntity's OID to the sourceEntity's property
    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        if (! (sourceEntity instanceof Entity))
            throw new PropertyResolverException("Cannot handle non-entities; received: " + (sourceEntity == null ? null : sourceEntity.getClass()));

        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{EntityHeaderUtils.fromEntity((Entity) sourceEntity), propName});

        if ( ! (targetValue instanceof PersistentEntity) )
            throw new PropertyResolverException("Error applying mapping for property name; invalid target value:" + targetValue);

        Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName, Goid.class);
        if (method != null) {
            try {
                method.invoke(sourceEntity, ((PersistentEntity)targetValue).getGoid());
            } catch (Exception e) {
                throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
            }
        } else {
            throw new PropertyResolverException("Error applying mapping: no setter found for the entity:property combination " + sourceEntity.getClass() + " : " + propName);
        }
    }
}
