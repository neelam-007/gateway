package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.objectmodel.EntityType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 */
@Component
public class DocumentAPIResourceFactory extends WsmanBaseResourceFactory<ResourceDocumentMO, DocumentResourceFactory> {

    public DocumentAPIResourceFactory() { }

    @NotNull
    @Override
    public String getResourceType(){
        return EntityType.RESOURCE_ENTRY.toString();
    }

    @Override
    @Inject
    @Named("documentResourceFactory")
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory factory) {
        super.factory = factory;
    }
}
