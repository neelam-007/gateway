package com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ServiceAliasResourceFactory;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.APIResourceWsmanBaseTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemBuilder;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.service.ServiceManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class ServiceAliasTransformer extends APIResourceWsmanBaseTransformer<ServiceAliasMO, PublishedServiceAlias, AliasHeader<PublishedService>, ServiceAliasResourceFactory> {

    @Inject
    private ServiceManager serviceManager;

    @Override
    @Inject
    protected void setFactory(ServiceAliasResourceFactory factory) {
        super.factory = factory;
    }

    @Override
    public Item<ServiceAliasMO> convertToItem(ServiceAliasMO m) {
        return new ItemBuilder<ServiceAliasMO>(findServiceAliasName(Goid.parseGoid(m.getServiceReference().getId())), m.getId(), factory.getType().name())
                .setContent(m)
                .build();
    }

    @Override
    public Item<ServiceAliasMO> convertToItem(EntityHeader header) {
        if (header instanceof AliasHeader) {
            return new ItemBuilder<ServiceAliasMO>(findServiceAliasName(((AliasHeader) header).getAliasedEntityId()), header.getStrId(), factory.getType().name())
                    .build();
        } else {
            return super.convertToItem(header);
        }
    }

    /**
     * Finds the service alias name by looking for the service with the given id. If this service cannot be found the service
     * id is returned.
     *
     * @param serviceID The id of the service to search for
     * @return The name of the service alias
     */
    private String findServiceAliasName(Goid serviceID) {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceID);
            if (service != null) {
                return service.getName() + " alias";
            }
        } catch (Throwable t) {
            //we do not want to throw here and default to using the id if a service cannot be found
        }
        return serviceID.toString();
    }
}
