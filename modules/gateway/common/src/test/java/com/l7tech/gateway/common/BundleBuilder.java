package com.l7tech.gateway.common;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Helps to create {@link Bundle}s for more readable unit tests.
 */
public class BundleBuilder {
    private final String ENTITY_TYPE_SERVICE = "SERVICE";
    private final String ENTITY_TYPE_SERVICE_ALIAS = "SERVICE_ALIAS";
    private ItemBuilder<ServiceMO> serviceItemBuilder = new ItemBuilder<ServiceMO>("serviceMOBuilder", ENTITY_TYPE_SERVICE);
    private ItemBuilder<ServiceAliasMO> serviceAliasBuilder = new ItemBuilder<ServiceAliasMO>("serviceAliasBuilder", ENTITY_TYPE_SERVICE_ALIAS);
    private List<Mapping> mappings =  new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private final Bundle bundle = ManagedObjectFactory.createBundle();

    public BundleBuilder addServiceAlias(final String serviceName,
                                         final String serviceId,
                                         final String aliasId) {
        final ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(serviceMO.getId());
        final ServiceDetail detail = ManagedObjectFactory.createServiceDetail();
        detail.setName(serviceName);
        serviceMO.setServiceDetail(detail);
        items.add(serviceItemBuilder.setContent(serviceMO).build());
        mappings.add(new MappingBuilder().withType(ENTITY_TYPE_SERVICE).
                withAction(Mapping.Action.NewOrUpdate).
                withSrcId(serviceId).build());

        final ServiceAliasMO aliasMO = ManagedObjectFactory.createServiceAlias();
        aliasMO.setId(aliasId);
        aliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, serviceId));
        items.add(serviceAliasBuilder.setContent(aliasMO).build());
        mappings.add(new MappingBuilder().withType(ENTITY_TYPE_SERVICE_ALIAS).
                withAction(Mapping.Action.NewOrUpdate).
                withSrcId(aliasId).build());
        return this;
    }

    public BundleBuilder name(final String name) {
        bundle.setName(name);
        return this;
    }

    public Bundle build() {
        bundle.setMappings(mappings);
        bundle.setReferences(items);
        return bundle;
    }
}