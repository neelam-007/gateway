package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RevocationCheckingPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class RevocationCheckingPolicyAPIResourceFactory extends WsmanBaseResourceFactory<RevocationCheckingPolicyMO, RevocationCheckingPolicyResourceFactory> {

    public RevocationCheckingPolicyAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.REVOCATION_CHECK_POLICY.toString();
    }

    @Override
    @Inject
    public void setFactory(RevocationCheckingPolicyResourceFactory factory) {
        super.factory = factory;
    }
}
