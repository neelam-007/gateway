package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityType;

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
public class UserGroupResolver extends DefaultEntityPropertyResolver {

    @Override
    public Map<EntityHeader, Set<MigrationMapping>> getDependencies(EntityHeaderRef source, Object entity, Method property) throws PropertyResolverException {
        Map<EntityHeader, Set<MigrationMapping>> result = new HashMap<EntityHeader, Set<MigrationMapping>>();
        Map<EntityHeader, Set<MigrationMapping>> dependencies = super.getDependencies(source, entity, property);
        for(EntityHeader header : dependencies.keySet()) {
            if (header.getType() == EntityType.USER || header.getType() == EntityType.GROUP) {
                for (MigrationMapping mapping : dependencies.get(header)) {
                    mapping.setType(new MigrationMappingType(MigrationMappingSelection.REQUIRED, MigrationMappingSelection.REQUIRED));
                }
                result.put(header, dependencies.get(header));
            }
        }
        return result;
    }
}
