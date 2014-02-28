package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JMSDestinationAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.JMSDestinationTransformer;
import com.l7tech.gateway.api.JMSDestinationMO;
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
public class JMSDestinationResource extends RestEntityResource<JMSDestinationMO, JMSDestinationAPIResourceFactory, JMSDestinationTransformer> {

    protected static final String jmsDestination_URI = "jmsDestinations";

    @Override
    @SpringBean
    public void setFactory(JMSDestinationAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(JMSDestinationTransformer transformer) {
        super.transformer = transformer;
    }
}
