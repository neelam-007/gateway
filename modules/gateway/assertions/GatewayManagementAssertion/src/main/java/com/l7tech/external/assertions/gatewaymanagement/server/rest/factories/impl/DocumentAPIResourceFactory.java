package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

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
    public void setFactory(com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public ResourceDocumentMO getResourceTemplate() {
        ResourceDocumentMO docMO = ManagedObjectFactory.createResourceDocument();
        docMO.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ConnectorProperty", "PropertyValue").map());
        Resource resource = ManagedObjectFactory.createResource();
        resource.setId("TemplateId");
        resource.setContent("TemplateContent");
        resource.setType("dtd");
        docMO.setResource(resource);
        return docMO;

    }
}
