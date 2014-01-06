package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyAliasRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The policy alias resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PolicyAliasResource.policyAlias_URI)
@Singleton
public class PolicyAliasResource extends RestEntityResource<PolicyAliasMO, PolicyAliasRestResourceFactory> {

    protected static final String policyAlias_URI = "policyAliases";

    @Override
    @SpringBean
    public void setFactory(PolicyAliasRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<PolicyAliasMO> toReference(PolicyAliasMO resource) {
        return toReference(resource.getId(), resource.getPolicyReference().getId());
    }
}
