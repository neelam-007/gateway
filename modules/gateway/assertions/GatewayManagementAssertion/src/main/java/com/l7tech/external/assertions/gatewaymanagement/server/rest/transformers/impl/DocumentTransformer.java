package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class DocumentTransformer extends APIResourceWsmanBaseTransformer<ResourceDocumentMO, ResourceEntry, ResourceEntryHeader, DocumentResourceFactory> {

    @Override
    @Inject
    protected void setFactory(DocumentResourceFactory factory) {
        super.factory = factory;
    }

    @NotNull
    @Override
    public Item<ResourceDocumentMO> convertToItem(@NotNull ResourceDocumentMO m) {
        return new ItemBuilder<ResourceDocumentMO>(m.getResource().getSourceUrl(), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }
}
