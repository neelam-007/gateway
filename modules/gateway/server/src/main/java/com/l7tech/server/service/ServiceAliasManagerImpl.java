package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;

/**
 * @author darmstrong
 */
public class ServiceAliasManagerImpl
    extends AliasManagerImpl<PublishedServiceAlias, PublishedService, ServiceHeader>
    implements ServiceAliasManager
{
    public Class getImpClass() {
        return PublishedServiceAlias.class;
    }

    public Class getInterfaceClass() {
        return PublishedServiceAlias.class;
    }

    public String getTableName() {
        return "published_service_alias";
    }

    public ServiceHeader getNewEntityHeader(ServiceHeader sh) {
        return new ServiceHeader(sh);
    }
}
