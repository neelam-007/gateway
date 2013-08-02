package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.Map;
import java.util.Collection;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;

/**
 *
 */
public class UDDIServiceControlManagerImpl extends HibernateEntityManager<UDDIServiceControl,EntityHeader> implements UDDIServiceControlManager {

    //- PUBLIC

    @Override
    public UDDIServiceControl findByPublishedServiceGoid( final Goid serviceGoid ) throws FindException {
        return findByUniqueKey( "publishedServiceGoid", serviceGoid );
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryOid( final long registryOid ) throws FindException {
        return findByPropertyMaybeNull( "uddiRegistryOid", registryOid);
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIServiceKey( final String serviceKey ) throws FindException {
        return findByPropertyMaybeNull( "uddiServiceKey", serviceKey);
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryAndServiceKey( final long registryOid,
                                                                           final String serviceKey,
                                                                           final Boolean uddiControlled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        matchMap.put( "uddiServiceKey", serviceKey );
        if ( uddiControlled != null ) {
            matchMap.put( "underUddiControl", uddiControlled );
        }
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryAndMetricsState( final long registryOid,
                                                                             final boolean metricsEnabled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        matchMap.put( "metricsEnabled", metricsEnabled );
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIServiceControl.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final UDDIServiceControl uddiServiceControl ) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put( "publishedServiceGoid", uddiServiceControl.getPublishedServiceGoid() );
        return Arrays.asList(serviceOidMap);
    }

}
