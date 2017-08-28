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
    private ItemBuilder<ServiceMO> serviceItemBuilder = new ItemBuilder<ServiceMO>("serviceMOBuilder", EntityType.SERVICE.toString());;
    private ItemBuilder<ServiceAliasMO> serviceAliasBuilder = new ItemBuilder<ServiceAliasMO>("serviceAliasBuilder", EntityType.SERVICE_ALIAS.toString());;
    private List<Mapping> mappings =  new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    public BundleBuilder addServiceAlias(final PublishedService service, final PublishedServiceAlias alias) {
        final ServiceMO serviceMO = ManagedObjectFactory.createService();
        serviceMO.setId(serviceMO.getId());
        ServiceDetail detail = ManagedObjectFactory.createServiceDetail();
        detail.setName(service.getName());
        serviceMO.setServiceDetail(detail);
        items.add(serviceItemBuilder.setContent(serviceMO).build());
        mappings.add(createMapping(EntityType.SERVICE.toString(), Mapping.Action.NewOrUpdate, service.getId()));

        final ServiceAliasMO aliasMO = ManagedObjectFactory.createServiceAlias();
        aliasMO.setId(alias.getId());
        aliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, service.getId()));
        items.add(serviceAliasBuilder.setContent(aliasMO).build());
        mappings.add(createMapping(EntityType.SERVICE_ALIAS.toString(), Mapping.Action.NewOrUpdate, alias.getId()));
        return this;
    }

    public Bundle build() {
        final Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setMappings(mappings);
        bundle.setReferences(items);
        return bundle;
    }

    private Mapping createMapping(final String type, final Mapping.Action action, final String id) {
        final Mapping mappingForTest = ManagedObjectFactory.createMapping();
        mappingForTest.setType(type);
        mappingForTest.setAction(action);
        mappingForTest.setSrcId(id);
        return mappingForTest;
    }
}