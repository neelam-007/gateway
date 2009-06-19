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
 * <li>identity providers will not be added as dependencies, unless there are no USER or GROUP dependencies.</li>
 * <li>other dependencies will not be changed.</li>
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

        boolean hasUserOrGroupDependency = false;
        for(ExternalEntityHeader header : dependencies.keySet()) {
            if (header.getType() == EntityType.USER || header.getType() == EntityType.GROUP) {
                hasUserOrGroupDependency = true;
                break;
            }
        }

        for(ExternalEntityHeader header : dependencies.keySet()) {
            if (header.getType() == EntityType.USER || header.getType() == EntityType.GROUP) {
                // copy all user/group dependencies and mark as required
                for (MigrationDependency dep : dependencies.get(header)) {
                    dep.setMappingType(MigrationMappingSelection.REQUIRED);
                }
                result.put(header, dependencies.get(header));
            } else if (header.getType() == EntityType.ID_PROVIDER_CONFIG) {
                // copy identity provider dependencies only if there are no user or group dependencies
                if ( !hasUserOrGroupDependency ) {
                    result.put(header, dependencies.get(header));
                }
            } else {
                // copy everything else
                result.put(header, dependencies.get(header));
            }
        }

        return result;
    }
}
