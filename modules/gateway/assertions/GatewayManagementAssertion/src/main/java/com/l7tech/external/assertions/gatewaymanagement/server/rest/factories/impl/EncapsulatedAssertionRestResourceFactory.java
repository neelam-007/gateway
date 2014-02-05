package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EncapsulatedAssertionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
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
public class EncapsulatedAssertionRestResourceFactory extends WsmanBaseResourceFactory<EncapsulatedAssertionMO, EncapsulatedAssertionResourceFactory> {

    public EncapsulatedAssertionRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("policy.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("policy.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public EntityType getEntityType(){
        return EntityType.ENCAPSULATED_ASSERTION;
    }

    @Override
    @Inject
    public void setFactory(EncapsulatedAssertionResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public EncapsulatedAssertionMO getResourceTemplate() {
        EncapsulatedAssertionMO encapsulatedAssertionMO = ManagedObjectFactory.createEncapsulatedAssertion();
        encapsulatedAssertionMO.setName("Template Cluster Property Name");
        encapsulatedAssertionMO.setGuid("Encapsulated Assertion Guid");
        return encapsulatedAssertionMO;
    }
}