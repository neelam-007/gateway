package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JMSDestinationRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The jms destination resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + JMSDestinationResource.jmsDestination_URI)
@Singleton
public class JMSDestinationResource extends RestEntityResource<JMSDestinationMO, JMSDestinationRestResourceFactory> {

    protected static final String jmsDestination_URI = "jmsDestinations";

    @Override
    @SpringBean
    public void setFactory(JMSDestinationRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Reference<JMSDestinationMO> toReference(JMSDestinationMO resource) {
        return toReference(resource.getId(), resource.getJmsDestinationDetail().getName());
    }
}
