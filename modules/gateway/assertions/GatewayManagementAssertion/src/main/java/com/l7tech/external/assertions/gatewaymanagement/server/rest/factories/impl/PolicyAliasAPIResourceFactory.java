package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class PolicyAliasAPIResourceFactory extends WsmanBaseResourceFactory<PolicyAliasMO, PolicyAliasResourceFactory> {

    public PolicyAliasAPIResourceFactory() {}

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.POLICY_ALIAS.toString();
    }

    @Override
    @Inject
    @Named("policyAliasResourceFactory")
    public void setFactory(PolicyAliasResourceFactory factory) {
        super.factory = factory;
    }
}
