package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.FolderResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.objectmodel.folder.Folder;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class FolderTransformer extends APIResourceWsmanBaseTransformer<FolderMO, Folder, FolderResourceFactory> {

    @Override
    @Inject
    protected void setFactory(FolderResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<FolderMO> convertToItem(FolderMO m) {
        return new ItemBuilder<FolderMO>(m.getName(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
