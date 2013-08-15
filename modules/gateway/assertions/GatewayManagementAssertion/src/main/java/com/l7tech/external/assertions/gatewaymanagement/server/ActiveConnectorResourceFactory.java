package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import org.springframework.transaction.PlatformTransactionManager;


import java.util.HashMap;
import java.util.Map;

/**
 * Resource factory for SsgActiveConnector.
 *
 */
@ResourceFactory.ResourceType(type=ActiveConnectorMO.class)
public class ActiveConnectorResourceFactory extends SecurityZoneableEntityManagerResourceFactory<ActiveConnectorMO, SsgActiveConnector, SsgActiveConnectorHeader> {

    public ActiveConnectorResourceFactory(final RbacServices services,
                                          final SecurityFilter securityFilter,
                                          final PlatformTransactionManager transactionManager,
                                          final SsgActiveConnectorManager ssgActiveConnectorManager,
                                          final SecurityZoneManager securityZoneManager) {
        super(false, true, services, securityFilter, transactionManager, ssgActiveConnectorManager, securityZoneManager);
    }

    @Override
    protected ActiveConnectorMO asResource(final SsgActiveConnector entity) {
        ActiveConnectorMO ssgActiveConnectorMO = ManagedObjectFactory.createActiveConnector();

        ssgActiveConnectorMO.setName(entity.getName());
        ssgActiveConnectorMO.setId(entity.getId());
        ssgActiveConnectorMO.setVersion(entity.getVersion());
        ssgActiveConnectorMO.setEnabled(entity.isEnabled());
        ssgActiveConnectorMO.setType(entity.getType());
        ssgActiveConnectorMO.setHardwiredId(entity.getHardwiredServiceGoid()==null?null: Goid.toString(entity.getHardwiredServiceGoid()));

        Map<String,String> properties = new HashMap<String,String>();
        for (String propertyName : entity.getPropertyNames()) {
            properties.put(propertyName, entity.getProperty(propertyName));
        }
        ssgActiveConnectorMO.setProperties(properties);

        // handle securityZone
        doSecurityZoneAsResource( ssgActiveConnectorMO, entity );

        return ssgActiveConnectorMO;
    }

    @Override
    protected SsgActiveConnector fromResource(final Object resource) throws InvalidResourceException {
        if (!(resource instanceof ActiveConnectorMO)) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected active connector");
        }
        final ActiveConnectorMO connectionResource = (ActiveConnectorMO) resource;
        final SsgActiveConnector activeConnector = new SsgActiveConnector();

        activeConnector.setEnabled(connectionResource.isEnabled());
        activeConnector.setName(connectionResource.getName());
        activeConnector.setHardwiredServiceGoid(connectionResource.getHardwiredId()==null?null:Goid.parseGoid(connectionResource.getHardwiredId()));
        activeConnector.setType(connectionResource.getType());

        // handle securityZone
        doSecurityZoneFromResource( connectionResource, activeConnector );

        for (Map.Entry<String, String> entry : connectionResource.getProperties().entrySet()) {

            activeConnector.setProperty(entry.getKey(), entry.getValue());
        }

        return activeConnector;
    }

    @Override
    protected void updateEntity(SsgActiveConnector oldEntity, SsgActiveConnector newEntity) throws InvalidResourceException {
        oldEntity.setEnabled(newEntity.isEnabled());
        oldEntity.setName(newEntity.getName());
        oldEntity.setHardwiredServiceGoid(newEntity.getHardwiredServiceGoid());
        oldEntity.setType(newEntity.getType());
        for (String name : newEntity.getPropertyNames()) {
            oldEntity.setProperty(name, newEntity.getProperty(name));
        }
        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }
}
