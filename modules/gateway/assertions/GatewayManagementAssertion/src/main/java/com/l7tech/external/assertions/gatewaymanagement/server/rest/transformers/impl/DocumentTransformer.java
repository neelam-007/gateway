package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.objectmodel.EntityHeader;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DocumentTransformer extends APIResourceWsmanBaseTransformer<ResourceDocumentMO, ResourceEntry, ResourceEntryHeader, DocumentResourceFactory> {

    @Override
    @Inject
    protected void setFactory(DocumentResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<ResourceDocumentMO> convertToItem(ResourceDocumentMO m) {
        return new ItemBuilder<ResourceDocumentMO>(m.getResource().getSourceUrl(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    public Item<ResourceDocumentMO> convertToItem(EntityHeader header){
        if (header instanceof ResourceEntryHeader) {
            return new ItemBuilder<ResourceDocumentMO>(((ResourceEntryHeader) header).getUri(), header.getStrId(), factory.getType().name())
                    .build();
        } else {
            return super.convertToItem(header);
        }
    }
}
