package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.EncapsulatedAssertionResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class EncapsulatedAssertionAPIResourceFactory extends WsmanBaseResourceFactory<EncapsulatedAssertionMO, EncapsulatedAssertionResourceFactory> {

    public EncapsulatedAssertionAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.ENCAPSULATED_ASSERTION.toString();
    }

    @Override
    @Inject
    @Named("encapsulatedAssertionResourceFactory")
    public void setFactory(EncapsulatedAssertionResourceFactory factory) {
        super.factory = factory;
    }
}