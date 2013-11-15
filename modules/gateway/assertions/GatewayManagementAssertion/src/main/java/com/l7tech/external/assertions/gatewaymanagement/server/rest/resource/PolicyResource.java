package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.PolicyResourceFactory;
import com.l7tech.gateway.api.PolicyMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.service.ServiceManager;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The policy resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(PolicyResource.POLICIES_URI)
public class PolicyResource extends DependentRestEntityResource<PolicyMO, PolicyResourceFactory> {

    protected static final String POLICIES_URI = "policies";

    @SpringBean
    ServiceManager serviceManager;

    @Override
    @SpringBean
    public void setFactory( PolicyResourceFactory factory) {
        super.factory = factory;
    }

    protected EntityType getEntityType(){
        return EntityType.POLICY;
    }
}
