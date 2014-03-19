package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
    public void setFactory(PolicyAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public PolicyAliasMO getResourceTemplate() {
        PolicyAliasMO policyAliasMO = ManagedObjectFactory.createPolicyAlias();
        policyAliasMO.setFolderId("Folder ID");
        policyAliasMO.setPolicyReference(new ManagedObjectReference());
        return policyAliasMO;

    }
}
