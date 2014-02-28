package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyAliasAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyAliasTransformer;
import com.l7tech.gateway.api.PolicyAliasMO;
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
public class PolicyAliasResource extends RestEntityResource<PolicyAliasMO, PolicyAliasAPIResourceFactory, PolicyAliasTransformer> {

    protected static final String policyAlias_URI = "policyAliases";

    @Override
    @SpringBean
    public void setFactory(PolicyAliasAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PolicyAliasTransformer transformer) {
        super.transformer = transformer;
    }
}
