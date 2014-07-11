package com.l7tech.server.migration;

import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.migration.*;
import com.l7tech.policy.assertion.JdbcConnectionable;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.server.jdbc.JdbcConnectionManager;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author jbufu
 */
public class JdbcConnectionPropertyResolver extends AbstractPropertyResolver {

    private static final Logger logger = Logger.getLogger(JdbcConnectionPropertyResolver.class.getName());
    private JdbcConnectionManager connectionManager;

    public JdbcConnectionPropertyResolver(PropertyResolverFactory factory, Type type, JdbcConnectionManager manager) {
        super(factory, type);
        this.connectionManager = manager;
    }

    @Override
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{propertyName, source});

        if (! (entity instanceof JdbcConnectionable) )
            throw new IllegalArgumentException("Cannot handle entity: " + entity);

        Map<ExternalEntityHeader, Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();

        final MigrationMappingSelection mappingType = MigrationUtils.getMappingType(property);
        final MigrationMappingSelection valueMappingType = MigrationUtils.getValueMappingType(property);
        final ExternalEntityHeader.ValueType valueType = MigrationUtils.getValueType(property);
        final boolean exported = MigrationUtils.isExported(property);

        final String connectionName = ((JdbcConnectionable) entity).getConnectionName();
        try {
            // if the connection name is referenced from a context variable we cannot resolve it (there is no context available),
            // therefore don't try to find the jdbc connection as a dependency.
            if(Syntax.getSingleVariableReferenced(connectionName) == null) {
                JdbcConnection connection = connectionManager.getJdbcConnection(connectionName);
                ExternalEntityHeader dependency = EntityHeaderUtils.toExternal(EntityHeaderUtils.fromEntity(connection));
                dependency.setValueMapping(valueMappingType, valueType, getPropertyValue(entity, property));
                result.put(dependency, Collections.singleton(new MigrationDependency(source, dependency, propertyName, getType(), mappingType, exported)));
            }
        } catch (Exception e) {
            throw new PropertyResolverException("Error retrieving JDBC connection: " + connectionName );
        }

        return result;
    }

    @Override
    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        logger.log(Level.FINEST, "Applying mapping for {0} : {1}.", new Object[]{sourceEntity, propName});

        if ( ! (sourceEntity instanceof JdbcConnectionable) || ! (targetValue instanceof JdbcConnection) )
            throw new PropertyResolverException("Cannot apply dependency value for: " + sourceEntity + " : " + targetValue);

        JdbcConnectionable connectionable = (JdbcConnectionable) sourceEntity;
        JdbcConnection connection = (JdbcConnection) targetValue;
        connectionable.setConnectionName(connection.getName());
    }
}
