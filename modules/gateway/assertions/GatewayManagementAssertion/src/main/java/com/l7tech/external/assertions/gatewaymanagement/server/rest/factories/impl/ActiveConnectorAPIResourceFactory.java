package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ActiveConnectorResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ActiveConnectorMO;
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
public class ActiveConnectorAPIResourceFactory extends WsmanBaseResourceFactory<ActiveConnectorMO, ActiveConnectorResourceFactory> {

    public ActiveConnectorAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("enabled", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("enabled", RestResourceFactoryUtils.booleanConvert))
                        .put("type", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("type", RestResourceFactoryUtils.stringConvert))
                        .put("hardwiredServiceGoid", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("hardwiredServiceGoid", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.SSG_ACTIVE_CONNECTOR.toString();
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.ActiveConnectorResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ActiveConnectorMO getResourceTemplate() {
        ActiveConnectorMO activeConnectorMO = ManagedObjectFactory.createActiveConnector();
        activeConnectorMO.setName("TemplateActiveConnector");
        activeConnectorMO.setType("SFTP");
        activeConnectorMO.setEnabled(true);
        activeConnectorMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder().put("ConnectorProperty", "PropertyValue").map());
        return activeConnectorMO;
    }
}
