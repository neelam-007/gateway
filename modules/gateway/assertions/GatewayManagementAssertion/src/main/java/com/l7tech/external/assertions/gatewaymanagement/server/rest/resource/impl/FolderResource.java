package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl.FolderRestResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.resource.DependentRestEntityResource;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.rest.SpringBean;
import com.l7tech.objectmodel.EntityType;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

/**
 * The folder resource
 *
 * @author Victor Kazakov
 */
@Provider
@Path(FolderResource.FOLDERS_URI)
public class FolderResource extends DependentRestEntityResource<FolderMO, FolderRestResourceFactory> {

    protected static final String FOLDERS_URI = "folders";

    @Override
    @SpringBean
    public void setFactory( FolderRestResourceFactory factory) {
        super.factory = factory;
    }

    public EntityType getEntityType(){
        return EntityType.FOLDER;
    }

    @Override
    protected Reference toReference(FolderMO resource) {
        return toReference(resource.getId(), resource.getName());
    }
}
