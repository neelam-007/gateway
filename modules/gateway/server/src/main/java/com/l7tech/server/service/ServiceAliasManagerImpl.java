package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityAlias;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 10:13:14 AM
 */
public class ServiceAliasManagerImpl extends AliasManagerImpl<PublishedServiceAlias, ServiceHeader>
        implements ServiceAliasManager{
    
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
