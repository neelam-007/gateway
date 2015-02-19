package com.l7tech.server.migration;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.migration.PropertyResolver;
import com.l7tech.server.EntityFinder;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.globalresources.ResourceEntryManager;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.ServiceDocumentManager;

import java.util.HashMap;
import java.util.Map;

import static com.l7tech.objectmodel.migration.PropertyResolver.Type.*;

/**
 * @author jbufu
 */
public class PropertyResolverFactory {

    private EntityFinder entityFinder;
    private SsgKeyStoreManager keyManager;
    private ServiceDocumentManager serviceDocumentManager;
    private ClusterPropertyManager cpManager;
    private ResourceEntryManager resourceEntryManager;
    private ServerConfig serverConfig;
    private JdbcConnectionManager jdbcConnectionManager;
    private CassandraConnectionEntityManager cassandraConnectionEntityManager;

    private Map<PropertyResolver.Type, PropertyResolver> registry = new HashMap<PropertyResolver.Type, PropertyResolver>();

    public PropertyResolverFactory(EntityFinder entityFinder, ServiceDocumentManager serviceDocumentManager, SsgKeyStoreManager keyManager,
                                   ClusterPropertyManager cpManager, ResourceEntryManager resourceEntryManager, ServerConfig serverConfig,
                                   JdbcConnectionManager jdbcConnectionManager, CassandraConnectionEntityManager cassandraConnectionEntityManager) {
        this.entityFinder = entityFinder;
        this.keyManager = keyManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.cpManager = cpManager;
        this.resourceEntryManager = resourceEntryManager;
        this.serverConfig = serverConfig;
        this.jdbcConnectionManager = jdbcConnectionManager;
        this.cassandraConnectionEntityManager = cassandraConnectionEntityManager;
        initRegistry();
    }

    private void initRegistry() {
        // todo: better registry initialization
        addToRegistry(new DefaultEntityPropertyResolver(this, DEFAULT));
        addToRegistry(new ServiceDocumentResolver(this, SERVICE_DOCUMENT, serviceDocumentManager));
        addToRegistry(new AbstractGoidPropertyResolver(this, SERVICE, entityFinder) {
            @Override
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });
        addToRegistry(new AbstractGoidPropertyResolver(this, SERVICE_ALIAS, entityFinder) {
            @Override
            public EntityType getTargetType() { return EntityType.SERVICE; }
        });
        addToRegistry(new PolicyPropertyResolver(this, POLICY));
        addToRegistry(new AbstractGoidPropertyResolver(this, POLICY_ALIAS, entityFinder) {
            @Override
            public EntityType getTargetType() { return EntityType.POLICY; }
        });
        addToRegistry(new AssertionPropertyResolver(this, ASSERTION));
        addToRegistry(new AbstractGoidPropertyResolver(this, ID_PROVIDER_CONFIG, entityFinder) {
            @Override
            public EntityType getTargetType() { return EntityType.ID_PROVIDER_CONFIG; }
        });
        addToRegistry(new UserGroupResolver(this, USERGROUP));
        addToRegistry(new ValueReferencePropertyResolver(this, VALUE_REFERENCE));
        addToRegistry(new SsgKeyResolver(this, SSGKEY, keyManager));
        addToRegistry(new ServerVariablePropertyResolver(this, SERVER_VARIABLE, cpManager, serverConfig));
        addToRegistry(new ResourceEntryPropertyResolver(this, RESOURCE_ENTRY, resourceEntryManager ));
        addToRegistry(new JdbcConnectionPropertyResolver(this, JDBC_CONNECTION, jdbcConnectionManager));
        addToRegistry(new CassandraConnectionPropertyResolver(this, CASSANDRA_CONNECTION, cassandraConnectionEntityManager));
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
