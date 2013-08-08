package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateGoidEntityManager;

import java.util.*;

/**
 *
 */
public class UDDIServiceControlManagerImpl extends HibernateGoidEntityManager<UDDIServiceControl,EntityHeader> implements UDDIServiceControlManager {

    //- PUBLIC

    @Override
    public UDDIServiceControl findByPublishedServiceGoid( final Goid serviceGoid ) throws FindException {
        return findByUniqueKey( "publishedServiceGoid", serviceGoid );
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryGoid( final Goid registryGoid ) throws FindException {
        return findByPropertyMaybeNull( "uddiRegistryGoid", registryGoid);
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIServiceKey( final String serviceKey ) throws FindException {
        return findByPropertyMaybeNull( "uddiServiceKey", serviceKey);
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryAndServiceKey( final Goid registryGoid,
                                                                           final String serviceKey,
                                                                           final Boolean uddiControlled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryGoid", registryGoid );
        matchMap.put( "uddiServiceKey", serviceKey );
        if ( uddiControlled != null ) {
            matchMap.put( "underUddiControl", uddiControlled );
        }
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIServiceControl> findByUDDIRegistryAndMetricsState( final Goid registryGoid,
                                                                             final boolean metricsEnabled ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryGoid", registryGoid );
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
