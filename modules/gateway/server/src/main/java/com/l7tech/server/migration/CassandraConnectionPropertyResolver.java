package com.l7tech.server.migration;

import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.MigrationUtils;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.policy.assertion.CassandraConnectionable;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2014
 * User: ymoiseyenko
 * Date: 11/25/14
 */
public class CassandraConnectionPropertyResolver extends AbstractPropertyResolver {

    private static final Logger logger = Logger.getLogger(CassandraConnectionPropertyResolver.class.getName());
    private final CassandraConnectionEntityManager connectionManager;

    public CassandraConnectionPropertyResolver(PropertyResolverFactory factory, Type type, CassandraConnectionEntityManager manager) {
        super(factory, type);
        this.connectionManager = manager;
    }
    /**
     * Extracts dependency headers and mappings from the supplied entity object's property.
     *
     * @param source       the dependant entity header; needed to create a MigrationMapping
     * @param entity       the object from which the dependencies are extracted; usually an Entity, but not required to be.
     * @param property     the method for which the property value is retrieved
     * @param propertyName the property's name; normally the method's name, but can be different,
     *                     e.g. in the case where structured names are required
     * @return a map with migration dependencies as values, keyed on the (target) dependency header.
     */
    @Override
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{propertyName, source});

        if (! (entity instanceof CassandraConnectionable) )
            throw new IllegalArgumentException("Cannot handle entity: " + entity);

        Map<ExternalEntityHeader, Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();

        final MigrationMappingSelection mappingType = MigrationUtils.getMappingType(property);
        final MigrationMappingSelection valueMappingType = MigrationUtils.getValueMappingType(property);
        final ExternalEntityHeader.ValueType valueType = MigrationUtils.getValueType(property);
        final boolean exported = MigrationUtils.isExported(property);
        final String connectionName = ((CassandraConnectionable) entity).getConnectionName();
        try {
            // if the connection name is referenced from a context variable we cannot resolve it (there is no context available),
            // therefore don't try to find the cassandra connection as a dependency.
            if(Syntax.getSingleVariableReferenced(connectionName) == null) {
                CassandraConnection connection = connectionManager.getCassandraConnectionEntity(connectionName);
                ExternalEntityHeader dependency = EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(connection));
                dependency.setValueMapping(valueMappingType, valueType, getPropertyValue(entity, property));
                result.put(dependency, Collections.singleton(new MigrationDependency(source, dependency, propertyName, getType(), mappingType, exported)));
            }
        } catch (Exception e) {
            throw new PropertyResolverException("Error retrieving Cassandra connection: " + connectionName );
        }

        return result;
    }

    /**
     * Applies a mapped value to a property of an entity.
     *
     * @param sourceEntity   the entity on which a new property value is set.
     * @param propName       the property of the sourceEntity for which a new value is set.
     * @param targetHeader   the target value's (entity) header;
     *                       needed if the target value is not enough to reconstruct the target header (e.g. value references)
     * @param targetValue    the new value that is set for the sourceEntity's propName
     * @param originalHeader the dependant header from the source cluster; needed in some cases (e.g. UsesEntities) to apply the mappings
     */
    @Override
    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{sourceEntity, propName});

        if ( ! (sourceEntity instanceof CassandraConnectionable) || ! (targetValue instanceof CassandraConnection) )
            throw new PropertyResolverException("Cannot apply dependency value for: " + sourceEntity + " : " + targetValue);

        CassandraConnectionable connectionable = (CassandraConnectionable) sourceEntity;
        CassandraConnection connection = (CassandraConnection) targetValue;
        connectionable.setConnectionName(connection.getName());
    }
}
