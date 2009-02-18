package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.*;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.EntityType;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_CLUSTER_PROPERTY;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_GATEWAY_TIME;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;

/**
 * @author jbufu
 */
public class ServerVariablePropertyResolver extends AbstractPropertyResolver {

    private static final Logger logger = Logger.getLogger(ServerVariablePropertyResolver.class.getName());

    public ServerVariablePropertyResolver(PropertyResolverFactory factory) {
        super(factory);
    }

    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{propertyName, source});

        if (property == null || ! property.getReturnType().isArray() || ! property.getReturnType().getComponentType().equals(String.class) )
            throw new IllegalArgumentException("Cannot handle entity: " + entity);

        final MigrationMappingType type = MigrationUtils.getMappingType(property);
        final boolean exported = MigrationUtils.isExported(property);

        String[] variableNames = (String[]) getPropertyValue(entity, property);

        Map<ExternalEntityHeader,Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();

        for (String varName : variableNames) {
            if (varName.startsWith(PREFIX_CLUSTER_PROPERTY) &&
                varName.length() > PREFIX_CLUSTER_PROPERTY.length() &&
                ! PREFIX_GATEWAY_TIME.equals(varName) /* special case exclude, because PREFIX_GATEWAY_TIME.startsWith(PREFIX_CLUSTER_PROPERTY) */) {
                String cpName = varName.substring(PREFIX_CLUSTER_PROPERTY.length()+1);
                ExternalEntityHeader cpExternalHeader = new ExternalEntityHeader(cpName, EntityType.CLUSTER_PROPERTY, null, cpName, null, null);
                result.put(cpExternalHeader, Collections.singleton(new MigrationDependency(source, cpExternalHeader, propertyName, type, exported)));
            }
        }

        return result;
    }

    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {

    }
}
