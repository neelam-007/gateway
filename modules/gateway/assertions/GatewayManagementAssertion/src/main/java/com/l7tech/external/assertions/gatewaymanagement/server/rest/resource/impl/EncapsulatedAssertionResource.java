package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.EncapsulatedAssertionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The Encapsulated Assertion resource
 *
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + EncapsulatedAssertionResource.ENCAPSULATED_ASSERTION_URI)
@Singleton
public class EncapsulatedAssertionResource extends DependentRestEntityResource<EncapsulatedAssertionMO, EncapsulatedAssertionRestResourceFactory> {

    protected static final String ENCAPSULATED_ASSERTION_URI = "encapsulatedAssertions";

    @Override
    @SpringBean
    public void setFactory( EncapsulatedAssertionRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Item<EncapsulatedAssertionMO> toReference(EncapsulatedAssertionMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
