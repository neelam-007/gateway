package com.l7tech.server.migration;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.objectmodel.migration.DefaultEntityPropertyResolver;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.service.ServiceDocumentManager;

import java.util.Map;
import java.util.HashMap;

/**
 * @author jbufu
 */
public class PropertyResolverFactory {

    private EntityFinder entityFinder;
    private ServiceDocumentManager serviceDocumentManager;

    private Map<EntityType, PropertyResolver> registry = new HashMap<EntityType, PropertyResolver>();

    public PropertyResolverFactory(EntityFinder entityFinder, ServiceDocumentManager serviceDocumentManager) {
        this.entityFinder = entityFinder;
        this.serviceDocumentManager = serviceDocumentManager;
        initRegistry();
    }

    private void initRegistry() {
        // todo: better registry initialization
        registry.put(EntityType.ANY, new DefaultEntityPropertyResolver());
        // todo: these two should be merged, policy should not really be a dependency
        registry.put(EntityType.POLICY, new PolicyXmlPropertyResolver());
        registry.put(EntityType.POLICY_ALIAS, new ServicePolicyPropertyResolver());
        registry.put(EntityType.SERVICE_DOCUMENT, new ServiceDocumentResolver(serviceDocumentManager));
        registry.put(EntityType.SERVICE, new AbstractOidPropertyResolver(entityFinder) {
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });
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
