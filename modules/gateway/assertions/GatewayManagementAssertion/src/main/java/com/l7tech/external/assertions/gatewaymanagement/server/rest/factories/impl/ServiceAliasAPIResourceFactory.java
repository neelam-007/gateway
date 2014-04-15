package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class ServiceAliasAPIResourceFactory extends WsmanBaseResourceFactory<ServiceAliasMO, ServiceAliasResourceFactory> {

    public ServiceAliasAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SERVICE_ALIAS.toString();
    }

    @Override
    @Inject
    public void setFactory(ServiceAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ServiceAliasMO getResourceTemplate() {
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId("Folder ID");
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, new Goid(3,1).toString()));

        return serviceAliasMO;

    }
}
