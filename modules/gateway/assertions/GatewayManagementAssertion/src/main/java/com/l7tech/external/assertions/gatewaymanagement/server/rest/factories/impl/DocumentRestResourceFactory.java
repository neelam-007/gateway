package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.DocumentResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.RestResourceFactoryUtils;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.WsmanBaseResourceFactory;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 *
 */
@Component
public class DocumentRestResourceFactory extends WsmanBaseResourceFactory<ResourceDocumentMO, DocumentResourceFactory> {

    public DocumentRestResourceFactory() {
        super(
                CollectionUtils.MapBuilder.<String, String>builder()
                        .put("id", "id")
                        .put("uri", "uri").map(),
                CollectionUtils.MapBuilder.<String, Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>>builder()
                        .put("uri", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("uri", RestResourceFactoryUtils.stringConvert))
                        .put("description", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("description", RestResourceFactoryUtils.stringConvert))
                        .put("type", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("type", RestResourceFactoryUtils.stringConvert))
//                        .put("targetNamespace", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("resourceKey1", RestResourceFactoryUtils.stringConvert))
//                        .put("publicId", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("resourceKey1", RestResourceFactoryUtils.stringConvert))
                        .put("securityZone.id", new Pair<String, Functions.UnaryThrows<?, String, IllegalArgumentException>>("securityZone.id", RestResourceFactoryUtils.goidConvert))
                        .map());
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
        docMO.setResource(resource);
        return docMO;

    }
}
