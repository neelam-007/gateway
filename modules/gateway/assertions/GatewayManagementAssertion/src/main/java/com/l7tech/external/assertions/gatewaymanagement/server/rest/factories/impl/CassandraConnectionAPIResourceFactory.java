package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.CassandraConnectionTransformer;
import com.l7tech.gateway.api.CassandraConnectionMO;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.cassandra.CassandraConnectionEntityManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class CassandraConnectionAPIResourceFactory extends
        EntityManagerAPIResourceFactory<CassandraConnectionMO, CassandraConnection, EntityHeader> {

    @Inject
    private CassandraConnectionTransformer transformer;
    @Inject
    private CassandraConnectionEntityManager cassandraConnectionEntityManager;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.CASSANDRA_CONFIGURATION;
    }

    @Override
    protected CassandraConnection convertFromMO(CassandraConnectionMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource).getEntity();
    }

    @Override
    protected CassandraConnectionMO convertToMO(CassandraConnection entity) {
        return transformer.convertToMO(entity);
    }

    @Override
    protected CassandraConnectionEntityManager getEntityManager() {
        return cassandraConnectionEntityManager;
    }
}
