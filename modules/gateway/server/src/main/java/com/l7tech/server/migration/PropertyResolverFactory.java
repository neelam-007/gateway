package com.l7tech.server.migration;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.ServiceDocumentManager;

import java.util.Map;
import java.util.HashMap;

/**
 * @author jbufu
 */
public class PropertyResolverFactory {

    private EntityFinder entityFinder;
    private SsgKeyStoreManager keyManager;
    private ServiceDocumentManager serviceDocumentManager;

    private Map<PropertyResolver.Type, PropertyResolver> registry = new HashMap<PropertyResolver.Type, PropertyResolver>();

    public PropertyResolverFactory(EntityFinder entityFinder, ServiceDocumentManager serviceDocumentManager, SsgKeyStoreManager keyManager) {
        this.entityFinder = entityFinder;
        this.keyManager = keyManager;
        this.serviceDocumentManager = serviceDocumentManager;
        initRegistry();
    }

    private void initRegistry() {
        // todo: better registry initialization
        registry.put(PropertyResolver.Type.DEFAULT, new DefaultEntityPropertyResolver(this));
        registry.put(PropertyResolver.Type.SERVICE_DOCUMENT, new ServiceDocumentResolver(this, serviceDocumentManager));
        registry.put(PropertyResolver.Type.SERVICE, new AbstractOidPropertyResolver(this, entityFinder) {
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });
        registry.put(PropertyResolver.Type.SERVICE_ALIAS, new AbstractOidPropertyResolver(this, entityFinder) {
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });        registry.put(PropertyResolver.Type.POLICY, new PolicyPropertyResolver(this));
        registry.put(PropertyResolver.Type.POLICY_ALIAS, new AbstractOidPropertyResolver(this, entityFinder) {
            public EntityType getTargetType() { return EntityType.POLICY; }
        });        registry.put(PropertyResolver.Type.ASSERTION, new AssertionPropertyResolver(this));
        registry.put(PropertyResolver.Type.ID_PROVIDER_CONFIG, new AbstractOidPropertyResolver(this, entityFinder) {
            public EntityType getTargetType() { return EntityType.ID_PROVIDER_CONFIG; }
        });
        registry.put(PropertyResolver.Type.USERGROUP, new UserGroupResolver(this));
        registry.put(PropertyResolver.Type.VALUE_REFERENCE, new ValueReferencePropertyResolver(this));
        registry.put(PropertyResolver.Type.SSGKEY, new SsgKeyResolver(this, keyManager));
    }

    /**
     * Retrieves a property resolver that is able to lookup entities of
     */
    public PropertyResolver getPropertyResolver(PropertyResolver.Type targetType) {
        return registry.get(targetType);
    }
}
