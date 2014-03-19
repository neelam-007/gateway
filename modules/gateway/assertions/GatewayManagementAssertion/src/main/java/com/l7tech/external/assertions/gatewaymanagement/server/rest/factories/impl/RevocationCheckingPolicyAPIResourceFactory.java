package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.RevocationCheckingPolicyResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
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

    @Override
    public RevocationCheckingPolicyMO getResourceTemplate() {
        RevocationCheckingPolicyMO revocationCheckingPolicyMO = ManagedObjectFactory.createRevocationCheckingPolicy();
        revocationCheckingPolicyMO.setName("TemplateRevocationCheckPolicy");
        revocationCheckingPolicyMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("Property", "PropertyValue").map());
        return revocationCheckingPolicyMO;
    }
}
