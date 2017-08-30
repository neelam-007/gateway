package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.EntityType;

import java.util.ArrayList;
import java.util.List;

/**
 * Helps to create {@link Bundle}s for more readable unit tests.
 */
public class BundleBuilder {
    private ItemBuilder<ServiceMO> serviceItemBuilder = new ItemBuilder<ServiceMO>("serviceMOBuilder", EntityType.SERVICE.toString());
    private ItemBuilder<ServiceAliasMO> serviceAliasBuilder = new ItemBuilder<ServiceAliasMO>("serviceAliasBuilder", EntityType.SERVICE_ALIAS.toString());
    private List<Mapping> mappings =  new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    public BundleBuilder addServiceAlias(final PublishedService service, final PublishedServiceAlias alias) {
        final ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(serviceMO.getId());
        final ServiceDetail detail = ManagedObjectFactory.createServiceDetail();
        detail.setName(service.getName());
        serviceMO.setServiceDetail(detail);
        items.add(serviceItemBuilder.setContent(serviceMO).build());
        mappings.add(new MappingBuilder().withType(EntityType.SERVICE.toString()).
                withAction(Mapping.Action.NewOrUpdate).
                withSrcId(service.getId()).build());

        final ServiceAliasMO aliasMO = ManagedObjectFactory.createServiceAlias();
        aliasMO.setId(alias.getId());
        aliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, service.getId()));
        items.add(serviceAliasBuilder.setContent(aliasMO).build());
        mappings.add(new MappingBuilder().withType(EntityType.SERVICE_ALIAS.toString()).
                withAction(Mapping.Action.NewOrUpdate).
                withSrcId(alias.getId()).build());
        return this;
    }

    public Bundle build() {
        final Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setMappings(mappings);
        bundle.setReferences(items);
        return bundle;
    }
}