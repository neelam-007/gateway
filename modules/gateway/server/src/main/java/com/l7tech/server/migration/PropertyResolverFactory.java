package com.l7tech.server.migration;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver;
import com.l7tech.server.EntityFinder;

import java.util.Map;
import java.util.HashMap;

/**
 * @author jbufu
 */
public class PropertyResolverFactory {

    private EntityFinder entityFinder;

    private Map<EntityType, PropertyResolver> registry = new HashMap<EntityType, PropertyResolver>();

    public PropertyResolverFactory(EntityFinder entityFinder) {
        this.entityFinder = entityFinder;
        initRegistry();
    }

    private void initRegistry() {
        // todo: better registry initialization
        registry.put(EntityType.ANY, new DefaultEntityPropertyResolver());
        registry.put(EntityType.ID_PROVIDER_CONFIG, new AbstractOidPropertyResolver(entityFinder) {
            public EntityType getTargetType() { return EntityType.ID_PROVIDER_CONFIG; }
        });
    }

    /**
     * Retrieves a property resolver that is able to lookup entities of
     */
    public PropertyResolver getPropertyResolver(EntityType targetType) {
        return registry.get(targetType);
    }
}
