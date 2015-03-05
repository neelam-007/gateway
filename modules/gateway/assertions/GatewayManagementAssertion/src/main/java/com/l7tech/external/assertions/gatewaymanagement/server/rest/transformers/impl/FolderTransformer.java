package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

@Component
public class FolderTransformer extends APIResourceWsmanBaseTransformer<FolderMO, Folder,FolderHeader, FolderResourceFactory> {

    @Override
    @Inject
    @Named("folderResourceFactory")
    protected void setFactory(FolderResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<FolderMO> convertToItem(@NotNull FolderMO m) {
        return new ItemBuilder<FolderMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
