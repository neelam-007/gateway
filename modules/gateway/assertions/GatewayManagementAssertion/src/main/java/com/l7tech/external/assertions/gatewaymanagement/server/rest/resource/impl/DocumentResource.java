package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.DocumentAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.DocumentTransformer;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The resource document resource
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + DocumentResource.document_URI)
@Singleton
public class DocumentResource extends RestEntityResource<ResourceDocumentMO, DocumentAPIResourceFactory, DocumentTransformer> {

    protected static final String document_URI = "resources";

    @Override
    @SpringBean
    public void setFactory(DocumentAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(DocumentTransformer transformer) {
        super.transformer = transformer;
    }
}
