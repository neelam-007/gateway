package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.PolicyTransformer;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.rest.SpringBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

/**
 * The policy resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + PolicyResource.POLICIES_URI)
@Singleton
public class PolicyResource extends DependentRestEntityResource<PolicyMO, PolicyAPIResourceFactory, PolicyTransformer> {

    protected static final String POLICIES_URI = "policies";

    @Context
    private ResourceContext resourceContext;

    @Override
    @SpringBean
    public void setFactory(PolicyAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(PolicyTransformer transformer) {
        super.transformer = transformer;
    }

    @NotNull
    @Override
    public String getUrl(@NotNull PolicyMO policyMO) {
        return getUrlString(policyMO.getId());
    }

    /**
     * Shows the policy versions
     *
     * @param id The policy id
     * @return The policyVersion resource for handling policy version requests.
     * @throws ResourceFactory.ResourceNotFoundException
     *
     */
    @Path("{id}/versions")
    public PolicyVersionResource versions(@PathParam("id") String id) throws ResourceFactory.ResourceNotFoundException {
        return resourceContext.initResource(new PolicyVersionResource(id));
    }
}
