package com.l7tech.server.migration;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.PropertyResolver;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.DEFAULT;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.SERVICE_DOCUMENT;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.SERVICE;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.SERVICE_ALIAS;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.POLICY;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.POLICY_ALIAS;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.ASSERTION;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.ID_PROVIDER_CONFIG;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.USERGROUP;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.VALUE_REFERENCE;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.SSGKEY;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.SERVER_VARIABLE;
import static com.l7tech.objectmodel.migration.PropertyResolver.Type.SCHEMA_ENTRY;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.communityschemas.SchemaEntryManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
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
    private ClusterPropertyManager cpManager;
    private SchemaEntryManager schemaManager;
    private ServerConfig serverConfig;

    private Map<PropertyResolver.Type, PropertyResolver> registry = new HashMap<PropertyResolver.Type, PropertyResolver>();

    public PropertyResolverFactory(EntityFinder entityFinder, ServiceDocumentManager serviceDocumentManager, SsgKeyStoreManager keyManager,
                                   ClusterPropertyManager cpManager, SchemaEntryManager schemaManager, ServerConfig serverConfig) {
        this.entityFinder = entityFinder;
        this.keyManager = keyManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.cpManager = cpManager;
        this.schemaManager = schemaManager;
        this.serverConfig = serverConfig;
        initRegistry();
    }

    private void initRegistry() {
        // todo: better registry initialization
        addToRegistry(new DefaultEntityPropertyResolver(this, DEFAULT));
        addToRegistry(new ServiceDocumentResolver(this, SERVICE_DOCUMENT, serviceDocumentManager));
        addToRegistry(new AbstractOidPropertyResolver(this, SERVICE, entityFinder) {
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });
        addToRegistry(new AbstractOidPropertyResolver(this, SERVICE_ALIAS, entityFinder) {
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });
        addToRegistry(new PolicyPropertyResolver(this, POLICY));
        addToRegistry(new AbstractOidPropertyResolver(this, POLICY_ALIAS, entityFinder) {
            public EntityType getTargetType() { return EntityType.POLICY; }
        });
        addToRegistry(new AssertionPropertyResolver(this, ASSERTION));
        addToRegistry(new AbstractOidPropertyResolver(this, ID_PROVIDER_CONFIG, entityFinder) {
            public EntityType getTargetType() { return EntityType.ID_PROVIDER_CONFIG; }
        });
        addToRegistry(new UserGroupResolver(this, USERGROUP));
        addToRegistry(new ValueReferencePropertyResolver(this, VALUE_REFERENCE));
        addToRegistry(new SsgKeyResolver(this, SSGKEY, keyManager));
        addToRegistry(new ServerVariablePropertyResolver(this, SERVER_VARIABLE, cpManager, serverConfig));
        addToRegistry(new SchemaEntryPropertyResolver(this, SCHEMA_ENTRY, schemaManager));
    }

    private void addToRegistry(PropertyResolver resolver) {
        registry.put(resolver.getType(), resolver);
    }

    /**
     * Retrieves a property resolver that is able to lookup entities of
     */
    public PropertyResolver getPropertyResolver(PropertyResolver.Type targetType) {
        return registry.get(targetType);
    }
}
