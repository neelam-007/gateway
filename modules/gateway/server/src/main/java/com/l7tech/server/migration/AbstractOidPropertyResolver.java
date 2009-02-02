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
public abstract class AbstractOidPropertyResolver extends AbstractPropertyResolver {
    private static final Logger logger = Logger.getLogger(AbstractOidPropertyResolver.class.getName());

    private EntityFinder entityFinder;

    protected AbstractOidPropertyResolver(PropertyResolverFactory factory, EntityFinder finder) {
        super(factory);
        this.entityFinder = finder;
    }

    public abstract EntityType getTargetType();

    // gets a persistent entity's header out of a long/OID property and its type
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{property.getName(),source});

        final MigrationMappingType type = MigrationUtils.getMappingType(property);
        final boolean exported = MigrationUtils.isExported(property);
        EntityType targetType = getTargetType();

        final Long oid;
        try {
            oid = (Long) property.invoke(entity);
        } catch (Exception e) {
            throw new PropertyResolverException("Error getting property value for entity: " + entity, e);
        }

        Map<ExternalEntityHeader,Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
        try {
            ExternalEntityHeader idpHeader = EntityHeaderUtils.toExternal(entityFinder.findHeader(targetType, oid));
            result.put(idpHeader, Collections.singleton(new MigrationDependency(source, idpHeader, propertyName, type, exported)));
        } catch (FindException e) {
            logger.log(Level.FINE, "No entity found for type: {0} oid: {1}.", new Object[]{targetType, oid});
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

        Method method = MigrationUtils.setterForPropertyName(sourceEntity, propName, long.class);
        if (method == null)
            method = MigrationUtils.setterForPropertyName(sourceEntity, propName, Long.class);

        if (method != null) {
            try {
                method.invoke(sourceEntity, ((PersistentEntity)targetValue).getOid());
            } catch (Exception e) {
                throw new PropertyResolverException("Error applying mapping for property name: " + propName, e);
            }
        } else {
            throw new PropertyResolverException("Error applying mapping: no setter found for the entity:property combination " + sourceEntity.getClass() + " : " + propName);
        }
    }
}
