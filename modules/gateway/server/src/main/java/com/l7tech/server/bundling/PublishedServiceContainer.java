package com.l7tech.server.bundling;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.PersistentEntity;
import org.apache.commons.collections.IteratorUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * For holding jms entities.
 */
public class PublishedServiceContainer extends PersistentEntityContainer<PublishedService> {
    private final List<ServiceDocument> serviceDocuments;

    public PublishedServiceContainer(final PublishedService service, final List<ServiceDocument> serviceDocuments) {
        super(service);
        this.serviceDocuments = serviceDocuments;
    }

    public PublishedServiceContainer(final PublishedService service, final Iterator<PersistentEntity> serviceDocsIterator) {
        super(service);
        serviceDocuments = IteratorUtils.toList(serviceDocsIterator);
    }

    public PublishedService getPublishedService() {
        return entity;
    }

    public List<ServiceDocument> getServiceDocuments() {
        return serviceDocuments;
    }

    @Override
    public List getEntities() {
        ArrayList list = new ArrayList();
        list.add(entity);
        list.addAll(serviceDocuments);
        return list;
    }
}