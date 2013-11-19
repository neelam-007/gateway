package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class ServiceRestResourceFactory extends WsmanBaseResourceFactory<ServiceMO, com.l7tech.external.assertions.gatewaymanagement.server.ServiceResourceFactory> {

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