package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class ServiceAliasRestResourceFactory extends WsmanBaseResourceFactory<ServiceAliasMO, ServiceAliasResourceFactory> {

    public ServiceAliasRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("service.id", "entityGoid")
                        .put("folder.id", "folder.id")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("service.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("entityGoid", RestResourceFactoryUtils.goidConvert))
                        .put("folder.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("folder.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.SERVICE_ALIAS;
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
        serviceAliasMO.setServiceReference(new ManagedObjectReference());

        return serviceAliasMO;

    }
}
