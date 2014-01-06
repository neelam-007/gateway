package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.RevocationCheckingPolicyRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ListingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.ReadingResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.Functions;

import javax.inject.Singleton;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.util.List;

/**
 * The active connector resource
 */
@Provider
@Path(RevocationCheckingPolicyResource.revocationCheckingPolicies_URI)
@Singleton
public class RevocationCheckingPolicyResource implements ReadingResource, ListingResource {

    protected static final String revocationCheckingPolicies_URI = "revocationCheckingPolicies";
    private RevocationCheckingPolicyRestResourceFactory factory;
    @Context
    protected UriInfo uriInfo;

    @SpringBean
    public void setFactory(RevocationCheckingPolicyRestResourceFactory factory) {
        this.factory = factory;
    }

    protected Reference<RevocationCheckingPolicyMO> toReference(RevocationCheckingPolicyMO resource) {
        return new ReferenceBuilder<RevocationCheckingPolicyMO>(resource.getName(), resource.getId(), factory.getEntityType().name())
                .addLink(ManagedObjectFactory.createLink("self", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(),  resource.getId())))
                .build();
    }

    @Override
    public Reference<References> listResources(final int offset, final int count, final String sort, final String order) {
        final String sortKey = factory.getSortKey(sort);
        if (sort != null && sortKey == null) {
            throw new IllegalArgumentException("Invalid sort. Cannot sort by: " + sort);
        }

        List<Reference> references = Functions.map(factory.listResources(offset, count, sortKey, RestEntityResourceUtils.convertOrder(order), RestEntityResourceUtils.createFiltersMap(factory.getFiltersInfo(), uriInfo.getQueryParameters())), new Functions.Unary<Reference, RevocationCheckingPolicyMO>() {
            @Override
            public Reference call(RevocationCheckingPolicyMO resource) {
                return toReference(resource);
            }
        });
        return new ReferenceBuilder<References>(factory.getEntityType() + " list", "List").setContent(ManagedObjectFactory.createReferences(references))
                .addLink(ManagedObjectFactory.createLink("self", uriInfo.getRequestUri().toString()))
                .build();
    }

    @Override
    public Reference<RevocationCheckingPolicyMO> getResource(String id) throws ResourceFactory.ResourceNotFoundException {
        RevocationCheckingPolicyMO resource = factory.getResource(id);
        return new ReferenceBuilder<>(toReference(resource))
                .setContent(resource)
                .addLink(ManagedObjectFactory.createLink("template", RestEntityResourceUtils.createURI(uriInfo.getBaseUriBuilder().path(this.getClass()).build(), "template")))
                .addLink(ManagedObjectFactory.createLink("list", uriInfo.getBaseUriBuilder().path(this.getClass()).build().toString()))
                .build();
    }
}
