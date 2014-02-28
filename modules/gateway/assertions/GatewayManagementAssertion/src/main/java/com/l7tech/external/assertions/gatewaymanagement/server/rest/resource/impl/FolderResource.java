package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.FolderAPIResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.RestEntityResource;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.FolderTransformer;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.rest.SpringBean;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The folder resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(RestEntityResource.RestEntityResource_version_URI + FolderResource.FOLDERS_URI)
@Singleton
public class FolderResource extends DependentRestEntityResource<FolderMO, FolderAPIResourceFactory, FolderTransformer> {

    protected static final String FOLDERS_URI = "folders";

    @Override
    @SpringBean
    public void setFactory( FolderAPIResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    @SpringBean
    public void setTransformer(FolderTransformer transformer) {
        super.transformer = transformer;
    }
}
