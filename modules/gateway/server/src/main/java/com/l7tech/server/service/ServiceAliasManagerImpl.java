package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.AliasHeader;

/**
 * @author darmstrong
 */
public class ServiceAliasManagerImpl
    extends AliasManagerImpl<PublishedServiceAlias, PublishedService, ServiceHeader>
    implements ServiceAliasManager
{
    //- PUBLIC

    @Override
    public Class<PublishedServiceAlias> getImpClass() {
        return PublishedServiceAlias.class;
    }

    @Override
    public Class<PublishedServiceAlias> getInterfaceClass() {
        return PublishedServiceAlias.class;
    }

    @Override
    public String getTableName() {
        return "published_service_alias";
    }

    @Override
    public ServiceHeader getNewEntityHeader( final ServiceHeader sh ) {
        return new ServiceHeader(sh);
    }

    //- PROTECTED

    @Override
    protected AliasHeader<PublishedService> newHeader( final PublishedServiceAlias entity ) {
        final AliasHeader<PublishedService> header = new AliasHeader<>(entity);
        header.setSecurityZoneGoid(entity.getSecurityZone() == null ? null : entity.getSecurityZone().getGoid());
        return header;
    }
}
