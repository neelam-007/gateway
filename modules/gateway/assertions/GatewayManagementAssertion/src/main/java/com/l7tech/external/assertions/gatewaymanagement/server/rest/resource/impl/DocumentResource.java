package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.DocumentRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.rest.SpringBean;

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
}
