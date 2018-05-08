package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.LogSinkTransformer;
import com.l7tech.gateway.api.LogSinkMO;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.SsgKeyHeader;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.log.SinkManager;
import com.l7tech.util.Functions;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class LogSinkAPIResourceFactory extends
        EntityManagerAPIResourceFactory<LogSinkMO, SinkConfiguration, EntityHeader> {

    @Inject
    private LogSinkTransformer transformer;
    @Inject
    private SinkManager logSinkManager;
    @Inject
    private EntityCrud entityCrud;

    @NotNull
    @Override
    public EntityType getResourceEntityType() {
        return EntityType.LOG_SINK;
    }

    @Override
    protected SinkConfiguration convertFromMO(LogSinkMO resource) throws ResourceFactory.InvalidResourceException {
        return transformer.convertFromMO(resource, null).getEntity();
    }

    @Override
    protected LogSinkMO convertToMO(SinkConfiguration entity) {
        return transformer.convertToMO(entity, null);
    }

    @Override
    protected SinkManager getEntityManager() {
        return logSinkManager;
    }

    @Override
    protected void beforeUpdateEntity(@NotNull SinkConfiguration entity, @NotNull SinkConfiguration oldEntity) throws ObjectModelException {
        checkPermissions(entity);
    }

    @Override
    protected void beforeCreateEntity(SinkConfiguration entity) throws ObjectModelException {
        checkPermissions(entity);
    }

    private void checkPermissions(SinkConfiguration sinkConfiguration) throws ObjectModelException {

        Functions.Binary<EntityHeader,String,EntityType>  entityHeaderResolver = new Functions.Binary<EntityHeader, String, EntityType>() {
            @Override
            public EntityHeader call(String id, EntityType entityType)  {
                return new EntityHeader(Goid.parseGoid(id), entityType, null, null);
            }
        };

        checkPermissions(sinkConfiguration, EntityType.SERVICE, GatewayDiagnosticContextKeys.SERVICE_ID, entityHeaderResolver);
        checkPermissions(sinkConfiguration, EntityType.SSG_CONNECTOR, GatewayDiagnosticContextKeys.LISTEN_PORT_ID, entityHeaderResolver);
        checkPermissions(sinkConfiguration, EntityType.EMAIL_LISTENER, GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID, entityHeaderResolver);
        checkPermissions(sinkConfiguration, EntityType.JMS_ENDPOINT, GatewayDiagnosticContextKeys.JMS_LISTENER_ID, entityHeaderResolver);
        checkPermissions(sinkConfiguration, EntityType.POLICY, GatewayDiagnosticContextKeys.POLICY_ID, entityHeaderResolver);
        checkPermissions(sinkConfiguration, EntityType.FOLDER, GatewayDiagnosticContextKeys.FOLDER_ID, entityHeaderResolver);
        checkPermissions(sinkConfiguration, EntityType.USER, GatewayDiagnosticContextKeys.USER_ID, new Functions.Binary<EntityHeader, String, EntityType>() {
            @Override
            public EntityHeader call(String id, EntityType entityType)  {
                String[] split = id.split(":");
                if(split.length==2) {
                    return new IdentityHeader(Goid.parseGoid(split[0]), new EntityHeader(split[1], entityType, null, null));
                }
                return null;
            }
        });

        // check RBAC for keystore
        if( sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID)!=null &&
                sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS)!=null){
            rbacAccessService.validatePermitted(EntityType.SSG_KEY_ENTRY, OperationType.READ);
            EntityHeader header = new SsgKeyHeader(null, Goid.parseGoid(sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID)), sinkConfiguration.getProperty(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS), "");
            Entity entity = entityCrud.find(header);
            if(entity!=null){
                rbacAccessService.validatePermitted(entity,OperationType.READ);
            }else{
                throw new FindException("Private key not found: Type: " + header.toString());
            }
        }


    }

    private void checkPermissions(SinkConfiguration sinkConfiguration, EntityType entityType, String key, Functions.Binary<EntityHeader, String, EntityType> getEntityHeader)  throws ObjectModelException {
        if(sinkConfiguration.getFilters().containsKey(key)){
            rbacAccessService.validatePermitted(entityType, OperationType.READ);
            for(String id: sinkConfiguration.getFilters().get(key)){
                EntityHeader entityHeader = getEntityHeader.call(id,entityType);
                if(entityHeader != null ) {
                    Entity entity = entityCrud.find(entityHeader);
                    if(entity != null) {
                        rbacAccessService.validatePermitted(entity, OperationType.READ);
                    }else{
                        throw new FindException("Filter entity not found: Type: " + entityType + " id:" + id);
                    }
                }
            }
        }
    }
}
