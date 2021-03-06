package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ActiveConnectorMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.transport.SsgActiveConnector;
import com.l7tech.gateway.common.transport.SsgActiveConnectorHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.transport.SsgActiveConnectorManager;
import com.l7tech.util.GoidUpgradeMapper;
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
    public ActiveConnectorMO asResource(final SsgActiveConnector entity) {
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
    public SsgActiveConnector fromResource(final Object resource, boolean strict) throws InvalidResourceException {
        if (!(resource instanceof ActiveConnectorMO)) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected active connector");
        }
        final ActiveConnectorMO connectionResource = (ActiveConnectorMO) resource;
        final SsgActiveConnector activeConnector = new SsgActiveConnector();

        activeConnector.setEnabled(connectionResource.isEnabled());
        activeConnector.setName(connectionResource.getName());

        try{
            Goid serviceGoid = connectionResource.getHardwiredId()== null? null : GoidUpgradeMapper.mapId(EntityType.SERVICE, connectionResource.getHardwiredId());
            activeConnector.setHardwiredServiceGoid(serviceGoid);
        } catch( IllegalArgumentException e){
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid hardwired id: "+e.getMessage());
        }

        activeConnector.setType(connectionResource.getType());

        // handle securityZone
        doSecurityZoneFromResource( connectionResource, activeConnector, strict );

        Map<String, String> props = connectionResource.getProperties();
        if(props!=null){
            for (Map.Entry<String, String> entry : props.entrySet()) {
                activeConnector.setProperty(entry.getKey(), entry.getValue());
            }
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
        for (String name : oldEntity.getPropertyNames()) {
            if(!newEntity.getPropertyNames().contains(name)){
                oldEntity.setProperty(name, null);
            }
        }
        oldEntity.setSecurityZone(newEntity.getSecurityZone());
    }
}
