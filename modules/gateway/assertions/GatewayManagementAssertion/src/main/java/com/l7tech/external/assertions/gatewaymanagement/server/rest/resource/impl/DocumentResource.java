package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.DocumentRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The resource document resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + DocumentResource.document_URI)
@Singleton
public class DocumentResource extends RestEntityResource<ResourceDocumentMO, DocumentRestResourceFactory> {

    protected static final String document_URI = "resources";

    @Override
    @SpringBean
    public void setFactory(DocumentRestResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    protected Item<ResourceDocumentMO> toReference(ResourceDocumentMO resource) {
        return toReference(resource.getId(), resource.getResource().getSourceUrl());
    }

    @Override
    public Item<ResourceDocumentMO> toReference(EntityHeader entityHeader) {
        if (entityHeader instanceof ResourceEntryHeader) {
            return toReference(entityHeader.getStrId(),((ResourceEntryHeader) entityHeader).getUri());
        } else {
            return super.toReference(entityHeader);
        }
    }
}
