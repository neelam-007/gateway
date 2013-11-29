package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.GenericEntityMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class GenericEntityRestResourceFactory extends WsmanBaseResourceFactory<GenericEntityMO, GenericEntityResourceFactory> {

    public GenericEntityRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name")
                        .put("entityClassName", "entityClassName")
                        .map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("enabled", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("enabled", RestResourceFactoryUtils.booleanConvert))
                        .put("entityClassName", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("entityClassName", RestResourceFactoryUtils.stringConvert))
                        .map());
    }

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.GenericEntityResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public GenericEntityMO getResourceTemplate() {
        GenericEntityMO genericEntityMO = ManagedObjectFactory.createGenericEntity();
        genericEntityMO.setName("TemplateGenericEntity");
        genericEntityMO.setDescription("template description");
        genericEntityMO.setEntityClassName("com.foo");
        return genericEntityMO;
    }
}
