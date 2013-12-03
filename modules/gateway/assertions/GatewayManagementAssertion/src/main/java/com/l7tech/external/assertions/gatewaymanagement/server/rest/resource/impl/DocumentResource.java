package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.DocumentRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResourceUtils;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The resource document resource
 */
@Provider
@Path(DocumentResource.document_URI)
public class DocumentResource extends RestEntityResource<ResourceDocumentMO, DocumentRestResourceFactory> {

    protected static final String document_URI = "resources";

    @Override
    @SpringBean
    public void setFactory(DocumentRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.RESOURCE_ENTRY;
    }

    @Override
    protected Reference toReference(ResourceDocumentMO resource) {
        return toReference(resource.getId(), resource.getResource().getSourceUrl());
    }

    @Override
    public Reference toReference(EntityHeader entityHeader) {
        if(entityHeader instanceof ResourceEntryHeader){
            return ManagedObjectFactory.createReference(RestEntityResourceUtils.createURI(uriInfo.getAbsolutePath(), entityHeader.getStrId()), entityHeader.getStrId(), getEntityType().name(), ((ResourceEntryHeader) entityHeader).getUri());
        } else {
            return super.toReference(entityHeader);
        }
    }
}
