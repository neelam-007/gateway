package com.l7tech.external.assertions.gatewaymanagement.server.rest.resource;

import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.FolderRestResourceFactory;
import com.l7tech.gateway.api.FolderMO;
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

    protected EntityType getEntityType(){
        return EntityType.FOLDER;
    }
}
