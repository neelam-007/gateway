/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfoHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.*;

public class UDDIProxiedServiceInfoManagerImpl extends HibernateEntityManager<UDDIProxiedServiceInfo, EntityHeader>
implements UDDIProxiedServiceInfoManager{

    //- PUBLIC

    public UDDIProxiedServiceInfoManagerImpl() {
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedServiceInfo.class;
    }

    @Override
    public UDDIProxiedServiceInfo findByPublishedServiceGoid( final Goid publishedServiceGoid ) throws FindException {
        return findByUniqueKey( "publishedServiceGoid", publishedServiceGoid );
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> findByUDDIRegistryAndMetricsState( final Goid registryGoid,
                                                                             final boolean metricsEnabled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryGoid", registryGoid );
        matchMap.put( "metricsEnabled", metricsEnabled );
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> findByUDDIRegistryOid(Goid registryGoid) throws FindException {
        return findByPropertyMaybeNull("uddiRegistryGoid", registryGoid);
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIProxiedServiceInfo proxiedServiceInfo) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("publishedServiceGoid", proxiedServiceInfo.getPublishedServiceGoid());
        return Arrays.asList(serviceOidMap);
    }

    @Override
    protected EntityHeader newHeader(final UDDIProxiedServiceInfo entity) {
        EntityHeader header = super.newHeader(entity);
        if (entity != null) {
            header = new UDDIProxiedServiceInfoHeader(entity.getGoid(), null, null, entity.getVersion(),
                    entity.getSecurityZone() ==  null ? null : entity.getSecurityZone().getGoid(), entity.getPublishedServiceGoid());
        }
        return header;
    }

    //- PRIVATE
}
