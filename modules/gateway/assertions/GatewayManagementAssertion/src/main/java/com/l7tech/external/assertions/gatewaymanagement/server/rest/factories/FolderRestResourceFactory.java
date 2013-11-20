package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories;

import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.folder.Folder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class FolderRestResourceFactory extends WsmanBaseResourceFactory<FolderMO, com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory> {

    @Override
    @Inject
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public FolderMO getResourceTemplate() {
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setName("Folder Template");
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        return folderMO;
    }
}