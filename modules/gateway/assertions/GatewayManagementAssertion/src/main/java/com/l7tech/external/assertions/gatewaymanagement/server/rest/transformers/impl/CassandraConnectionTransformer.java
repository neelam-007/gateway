package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.SecretsEncryptor;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.EntityAPITransformer;
import com.l7tech.gateway.api.CassandraConnectionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.bundling.EntityContainer;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class CassandraConnectionTransformer extends EntityManagerAPITransformer<CassandraConnectionMO, CassandraConnection> implements EntityAPITransformer<CassandraConnectionMO, CassandraConnection> {

    @Inject
    ServiceManager serviceManager;

    @Inject
    SecurityZoneManager securityZoneManager;

    @NotNull
    @Override
    public String getResourceType() {
        return EntityType.CASSANDRA_CONFIGURATION.toString();
    }

    @NotNull
    @Override
    public CassandraConnectionMO convertToMO(@NotNull EntityContainer<CassandraConnection> userEntityContainer,  SecretsEncryptor secretsEncryptor) {
        return convertToMO(userEntityContainer.getEntity(), secretsEncryptor);
    }


    @NotNull
    public CassandraConnectionMO convertToMO(@NotNull CassandraConnection cassandraConnection) {
        return convertToMO(cassandraConnection, null);
    }

    @NotNull
    @Override
    public CassandraConnectionMO convertToMO(@NotNull CassandraConnection cassandraConnection,  SecretsEncryptor secretsEncryptor) {
        CassandraConnectionMO cassandraConnectionMO = ManagedObjectFactory.createCassandraConnectionMO();
        cassandraConnectionMO.setId(cassandraConnection.getId());
        cassandraConnectionMO.setVersion(cassandraConnection.getVersion());
        cassandraConnectionMO.setName(cassandraConnection.getName());
        cassandraConnectionMO.setKeyspace(cassandraConnection.getKeyspaceName());
        cassandraConnectionMO.setContactPoint(cassandraConnection.getContactPoints());
        cassandraConnectionMO.setPort(cassandraConnection.getPort());
        cassandraConnectionMO.setUsername(cassandraConnection.getUsername());
        cassandraConnectionMO.setPasswordId(cassandraConnection.getPasswordGoid() == null ? null : cassandraConnection.getPasswordGoid().toString());
        cassandraConnectionMO.setCompression(cassandraConnection.getCompression());
        cassandraConnectionMO.setSsl(cassandraConnection.isSsl());
        cassandraConnectionMO.setTlsciphers(cassandraConnection.getTlsEnabledCipherSuites());
        cassandraConnectionMO.setEnabled(cassandraConnection.isEnabled());
        cassandraConnectionMO.setProperties(cassandraConnection.getProperties());
        doSecurityZoneToMO(cassandraConnectionMO, cassandraConnection);

        return cassandraConnectionMO;
    }

    @NotNull
    @Override
    public EntityContainer<CassandraConnection> convertFromMO(@NotNull CassandraConnectionMO cassandraConnectionMO, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        return convertFromMO(cassandraConnectionMO, true, secretsEncryptor);
    }

    @NotNull
    @Override
    public EntityContainer<CassandraConnection> convertFromMO(@NotNull CassandraConnectionMO cassandraConnectionMO, boolean strict, SecretsEncryptor secretsEncryptor)
            throws ResourceFactory.InvalidResourceException {
        CassandraConnection cassandraConnection = new CassandraConnection();
        cassandraConnection.setId(cassandraConnectionMO.getId());
        if (cassandraConnectionMO.getVersion() != null) {
            cassandraConnection.setVersion(cassandraConnectionMO.getVersion());
        }
        cassandraConnection.setName(cassandraConnectionMO.getName());
        cassandraConnection.setKeyspaceName(cassandraConnectionMO.getKeyspace());
        cassandraConnection.setContactPoints(cassandraConnectionMO.getContactPoint());
        cassandraConnection.setPort(cassandraConnectionMO.getPort());
        cassandraConnection.setUsername(cassandraConnectionMO.getUsername());
        cassandraConnection.setPasswordGoid(cassandraConnectionMO.getPasswordId() == null ? null : new Goid(cassandraConnectionMO.getPasswordId()));
        cassandraConnection.setCompression(cassandraConnectionMO.getCompression());
        cassandraConnection.setSsl(cassandraConnectionMO.isSsl());
        cassandraConnection.setTlsEnabledCipherSuites(cassandraConnectionMO.getTlsciphers());
        cassandraConnection.setEnabled(cassandraConnectionMO.isEnabled());
        cassandraConnection.setProperties(cassandraConnectionMO.getProperties());
        doSecurityZoneFromMO(cassandraConnectionMO, cassandraConnection, strict);

        return new EntityContainer<>(cassandraConnection);
    }

    @NotNull
    @Override
    public Item<CassandraConnectionMO> convertToItem(@NotNull CassandraConnectionMO m) {
        return new ItemBuilder<CassandraConnectionMO>(m.getName(), m.getId(), EntityType.CASSANDRA_CONFIGURATION.name())
                .setContent(m)
                .build();
    }
}
