package com.l7tech.server.migration;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.EntityHeaderUtils;

import java.util.Set;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Method;

/**
 * Handles persistent OID properties.
 *
 * The EntityType (or implementing entity class) is needed for looking up entities.
 * This abstract resolver needs to be extended by specialized resolvers that know that entity types and pass it through.
 *
 * @author jbufu
 */
public abstract class AbstractOidPropertyResolver implements PropertyResolver {
    private static final Logger logger = Logger.getLogger(AbstractOidPropertyResolver.class.getName());

    private EntityFinder entityFinder;

    protected AbstractOidPropertyResolver(EntityFinder finder) {
        this.entityFinder = finder;
    }

    public abstract EntityType getTargetType();

    // gets a persistent entity's header out of a long/OID property and its type
    public final Map<EntityHeader, Set<MigrationMapping>> getDependencies(final EntityHeaderRef source, Object entity, Method property) throws MigrationException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final MigrationMappingType type = MigrationUtils.getMappingType(property);
        final boolean uploadedByParent = MigrationUtils.getUploadedByParent(property);
        final String propName = MigrationUtils.propertyNameFromGetter(property.getName());
        EntityType targetType = getTargetType();

        final Long oid;
        try {
            oid = (Long) property.invoke(entity);
        } catch (Exception e) {
            throw new MigrationException("Error getting property value for entity: " + entity, e);
        }

        Map<EntityHeader,Set<MigrationMapping>> result = new HashMap<EntityHeader, Set<MigrationMapping>>();
        try {
            EntityHeader idpHeader = entityFinder.findHeader(targetType, oid);
            result.put(idpHeader, Collections.singleton(new MigrationMapping(source, idpHeader, propName, type, uploadedByParent)));
        } catch (FindException e) {
            logger.log(Level.FINE, "No entity found for type: {0} oid: {1}.", new Object[]{targetType, oid});
        }
        return result;
    }

    // assigns the targetEntity's OID to the sourceEntity's property
    public void applyMapping(Entity sourceEntity, String propName, Object targetValue, EntityHeader originalValue) throws MigrationException {
        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{EntityHeaderUtils.fromEntity(sourceEntity), propName});

        if ( ! (targetValue instanceof PersistentEntity) )
            throw new MigrationException("Error applying mapping for property name; invalid target value:" + targetValue);

        Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName, Long.class);
        try {
            method.invoke(sourceEntity, ((PersistentEntity)targetValue).getOid());
        } catch (Exception e) {
            throw new MigrationException("Error applying mapping for property name: " + propName, e);
        }
    }
}
