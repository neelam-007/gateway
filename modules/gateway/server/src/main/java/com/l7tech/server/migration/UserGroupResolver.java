package com.l7tech.server.migration;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.objectmodel.migration.MigrationDependency;
import com.l7tech.objectmodel.migration.PropertyResolverException;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;

/**
 * Customizes the mapping types returned for the specified properties as follows:
 * <ul>
 * <li>dependencies of type USER and GROUP will be marked as requiring mapping</li>
 * <li>dependencies of other types (typically identity provider configuration) will not be added as dependencies</li>
 * </ul>
 *
 * @author jbufu
 */
public class UserGroupResolver extends AssertionPropertyResolver {

    public UserGroupResolver(PropertyResolverFactory factory, Type type) {
        super(factory, type);
    }

    @Override
    public Map<ExternalEntityHeader, Set<MigrationDependency>> getDependencies(ExternalEntityHeader source, Object entity, Method property, String propertyName) throws PropertyResolverException {
        Map<ExternalEntityHeader, Set<MigrationDependency>> result = new HashMap<ExternalEntityHeader, Set<MigrationDependency>>();
        Map<ExternalEntityHeader, Set<MigrationDependency>> dependencies = super.getDependencies(source, entity, property, propertyName);
        for(ExternalEntityHeader header : dependencies.keySet()) {
            if (header.getType() == EntityType.USER || header.getType() == EntityType.GROUP) {
                for (MigrationDependency dep : dependencies.get(header)) {
                    dep.setMappingType(MigrationMappingSelection.REQUIRED);
                }
                result.put(header, dependencies.get(header));
            }
        }
        return result;
    }
}
