package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ListenPortResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class ListenPortAPIResourceFactory extends WsmanBaseResourceFactory<ListenPortMO, ListenPortResourceFactory> {

    public ListenPortAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SSG_CONNECTOR.toString();
    }

    @Override
    @Inject
    public void setFactory(ListenPortResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ListenPortMO getResourceTemplate() {
        ListenPortMO emailListenerMO = ManagedObjectFactory.createListenPort();
        emailListenerMO.setName("TemplateListenPort");
        emailListenerMO.setPort(1234);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ConnectorProperty", "PropertyValue").map());
        return emailListenerMO;

    }
}
