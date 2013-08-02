/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.objectmodel.*;
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
    public Collection<UDDIProxiedServiceInfo> findByUDDIRegistryAndMetricsState( final long registryOid,
                                                                             final boolean metricsEnabled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        matchMap.put( "metricsEnabled", metricsEnabled );
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> findByUDDIRegistryOid(long registryOid) throws FindException {
        return findByPropertyMaybeNull("uddiRegistryOid", registryOid);
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

    //- PRIVATE
}
