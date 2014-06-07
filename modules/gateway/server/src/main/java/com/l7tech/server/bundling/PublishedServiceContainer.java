package com.l7tech.server.bundling;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.objectmodel.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Entity container for holding a published service and its related service documents.
 */
public class PublishedServiceContainer extends EntityContainer<PublishedService> {
    // The service documents
    @NotNull
    private final Collection<ServiceDocument> serviceDocuments;

    public PublishedServiceContainer(@NotNull final PublishedService service, @NotNull final Collection<ServiceDocument> serviceDocuments) {
        super(service);
        this.serviceDocuments = serviceDocuments;
    }

    @NotNull
    public PublishedService getPublishedService() {
        return getEntity();
    }

    @NotNull
    public Collection<ServiceDocument> getServiceDocuments() {
        return serviceDocuments;
    }

    @NotNull
    @Override
    public List<Entity> getEntities() {
        final ArrayList<Entity> list = new ArrayList<>();
        list.add(getEntity());
        list.addAll(serviceDocuments);
        return list;
    }
}