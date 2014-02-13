package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ListenPortResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ListenPortMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
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
public class ListenPortAPIResourceFactory extends WsmanBaseResourceFactory<ListenPortMO, ListenPortResourceFactory> {

    public ListenPortAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .put("enabled", "enabled")
                        .put("protocol", "scheme")
                        .put("port", "port")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("enabled", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("enabled", RestResourceFactoryUtils.booleanConvert))
                        .put("protocol", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("scheme", RestResourceFactoryUtils.stringConvert))
                        .put("port", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("port", RestResourceFactoryUtils.intConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

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
