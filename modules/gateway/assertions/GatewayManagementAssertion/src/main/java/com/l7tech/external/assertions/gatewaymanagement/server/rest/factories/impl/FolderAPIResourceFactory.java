package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * This was created: 11/18/13 as 4:30 PM
 *
 * @author Victor Kazakov
 */
@Component
public class FolderAPIResourceFactory extends WsmanBaseResourceFactory<FolderMO, FolderResourceFactory> {

    public FolderAPIResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("name", "name").put("parentFolder.id", "parentFolder.id").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("name", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("name", RestResourceFactoryUtils.stringConvert))
                        .put("parentFolder.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("folder.id", RestResourceFactoryUtils.goidConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
    }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.FOLDER.toString();
    }

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