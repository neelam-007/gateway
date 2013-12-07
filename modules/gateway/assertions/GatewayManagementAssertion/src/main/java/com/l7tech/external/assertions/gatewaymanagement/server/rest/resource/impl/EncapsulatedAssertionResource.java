package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.EncapsulatedAssertionRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.gateway.api.EncapsulatedAssertionMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The Encapsulated Assertion resource
 *
 */
@Provider
@Path(EncapsulatedAssertionResource.ENCAPSULATED_ASSERTION_URI)
public class EncapsulatedAssertionResource extends DependentRestEntityResource<EncapsulatedAssertionMO, EncapsulatedAssertionRestResourceFactory> {

    protected static final String ENCAPSULATED_ASSERTION_URI = "encapsulatedAssertions";

    @Override
    @SpringBean
    public void setFactory( EncapsulatedAssertionRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.ENCAPSULATED_ASSERTION;
    }

    @Override
    protected Reference toReference(EncapsulatedAssertionMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
