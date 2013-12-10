package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.JMSDestinationRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.JMSDestinationMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The jms destination resource
 */
@Provider
@Path(JMSDestinationResource.jmsDestination_URI)
public class JMSDestinationResource extends RestEntityResource<JMSDestinationMO, JMSDestinationRestResourceFactory> {

    protected static final String jmsDestination_URI = "jmsDestinations";

    @Override
    @SpringBean
    public void setFactory(JMSDestinationRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.JMS_ENDPOINT;
    }

    @Override
    protected Reference toReference(JMSDestinationMO resource) {
        return toReference(resource.getId(), resource.getJmsDestinationDetail().getName());
    }
}
