/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.uddi.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UDDIProxiedServiceInfoManagerImpl extends HibernateEntityManager<UDDIProxiedServiceInfo, EntityHeader>
implements UDDIProxiedServiceInfoManager{

    //- PUBLIC

    public UDDIProxiedServiceInfoManagerImpl( final UDDIHelper uddiHelper ) {
        this.uddiHelper = uddiHelper;
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIProxiedServiceInfo.class;
    }

    @Override
    public void saveUDDIProxiedServiceInfo(final UDDIProxiedServiceInfo uddiProxiedServiceInfo)
            throws SaveException{
        //todo delete this method, not needed
        save(uddiProxiedServiceInfo);
    }

    @Override
    public UDDIProxiedServiceInfo findByPublishedServiceOid( final long publishedServiceOid ) throws FindException {
        return findByUniqueKey( "publishedServiceOid", publishedServiceOid );
    }

    @Override
    public Collection<UDDIProxiedServiceInfo> findByUDDIRegistryAndMetricsState( final long registryOid,
                                                                             final boolean metricsEnabled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        matchMap.put( "metricsEnabled", metricsEnabled );
        return findMatching( Collections.singletonList( matchMap ) );
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final UDDIProxiedServiceInfo proxiedServiceInfo) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put("publishedServiceOid", proxiedServiceInfo.getPublishedServiceOid());
        return Arrays.asList(serviceOidMap);
    }

    //- PRIVATE

    private  static final Logger logger = Logger.getLogger(UDDIProxiedServiceInfoManagerImpl.class.getName());

    private final UDDIHelper uddiHelper;

}
