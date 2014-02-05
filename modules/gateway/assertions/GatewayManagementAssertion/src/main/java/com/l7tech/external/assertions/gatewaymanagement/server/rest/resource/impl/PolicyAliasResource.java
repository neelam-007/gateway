package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.PolicyAliasRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.PolicyAliasMO;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;

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
    protected Item<PolicyAliasMO> toReference(PolicyAliasMO resource) {
        return toReference(resource.getId(), resource.getPolicyReference().getId());
    }
    @Override
    public Item<PolicyAliasMO> toReference(EntityHeader entityHeader) {
        if (entityHeader instanceof AliasHeader) {
            return toReference(entityHeader.getStrId(),((AliasHeader) entityHeader).getAliasedEntityId().toString());
        } else {
            return super.toReference(entityHeader);
        }
    }
}
