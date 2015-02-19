package com.l7tech.server.migration;

import com.l7tech.objectmodel.migration.*;
import com.l7tech.objectmodel.*;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_CLUSTER_PROPERTY;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_GATEWAY_RANDOM;
import static com.l7tech.policy.variable.BuiltinVariables.PREFIX_GATEWAY_TIME;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.cluster.ClusterProperty;

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

    ClusterPropertyManager manager;
    private ServerConfig serverConfig;

    public ServerVariablePropertyResolver(PropertyResolverFactory factory, Type type, ClusterPropertyManager manager, ServerConfig serverConfig) {
        super(factory, type);
        this.manager = manager;
        this.serverConfig = serverConfig;
    }

    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        logger.log(Level.FINEST, "Getting dependencies for property {0} of entity with header {1}.", new Object[]{propertyName, source});

        if (property == null || ! property.getReturnType().isArray() || ! property.getReturnType().getComponentType().equals(String.class) )
            throw new IllegalArgumentException("Cannot handle entity: " + entity);

        final MigrationMappingSelection mappingType = MigrationUtils.getMappingType(property);
        final MigrationMappingSelection valueMappingType = MigrationUtils.getValueMappingType(property);
        final boolean exported = MigrationUtils.isExported(property);

        String[] variableNames = (String[]) getPropertyValue(entity, property);

        Map<ExternalEntityHeader,Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();

        for (String varName : variableNames) {
            if (varName.startsWith(PREFIX_CLUSTER_PROPERTY) &&
                varName.length() > PREFIX_CLUSTER_PROPERTY.length() &&
                !varName.startsWith(PREFIX_GATEWAY_RANDOM) &&
                !varName.startsWith(PREFIX_GATEWAY_TIME) /* special case exclude, because PREFIX_GATEWAY_RANDOM and PREFIX_GATEWAY_TIME.startsWith(PREFIX_CLUSTER_PROPERTY) */) {
                String cpName = varName.substring(PREFIX_CLUSTER_PROPERTY.length()+1);
                ExternalEntityHeader cpExternalHeader = new ExternalEntityHeader(cpName, EntityType.CLUSTER_PROPERTY, null, cpName, null, null);
                try {
                    String cpValue = manager.getProperty(cpName);
                    if (cpValue == null)
                        cpValue = "";
                    cpExternalHeader.setValueMapping(
                        serverConfig.getClusterPropertyNames().containsKey(cpName) ? MigrationMappingSelection.NONE : valueMappingType,
                        ExternalEntityHeader.ValueType.TEXT, cpValue );
                } catch (Exception e) {
                    throw new PropertyResolverException("Error loading cluster property: " + cpName, e);
                }
                result.put(cpExternalHeader, Collections.singleton(new MigrationDependency(source, cpExternalHeader, propertyName, getType(), mappingType, exported)));
            }
        }

        return result;
    }

    public void applyMapping(Object sourceEntity, String propName, ExternalEntityHeader targetHeader, Object targetValue, ExternalEntityHeader originalHeader) throws PropertyResolverException {
        // do nothing
    }

    public Entity valueMapping(ExternalEntityHeader header) throws PropertyResolverException {
        if (EntityType.CLUSTER_PROPERTY == header.getType() && header.getMappedValue() != null) {
            return new ClusterProperty(header.getName(), (String) header.getValueType().deserialize(header.getMappedValue()));
        } else {
            return null;
        }
    }
}
