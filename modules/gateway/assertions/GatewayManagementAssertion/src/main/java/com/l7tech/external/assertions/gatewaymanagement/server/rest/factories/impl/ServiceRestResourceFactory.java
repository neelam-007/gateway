package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class ServiceRestResourceFactory extends WsmanBaseResourceFactory<ServiceMO, ServiceResourceFactory> {

    public ServiceRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").put("parentFolder.id", "parentFolder.id").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("guid", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("guid", RestResourceFactoryUtils.stringConvert))
                        .put("soap", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("soap", RestResourceFactoryUtils.booleanConvert))
                        .put("parentFolder.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("folder.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.SERVICE;
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ServiceMO getResourceTemplate() {
        ServiceMO serviceMO = ManagedObjectFactory.createService();

        ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();
        serviceDetail.setEnabled(true);
        serviceDetail.setFolderId("Folder ID");
        serviceDetail.setName("Service Name");

        serviceMO.setServiceDetail(serviceDetail);
        return serviceMO;
    }
}