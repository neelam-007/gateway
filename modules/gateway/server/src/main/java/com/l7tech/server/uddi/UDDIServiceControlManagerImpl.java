package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
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
    public UDDIServiceControl findByPublishedServiceOid( final long serviceOid ) throws FindException {
        return findByUniqueKey( "publishedServiceOid", serviceOid );
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
        return this.findMatching( Collections.singletonList( matchMap ) );
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
        serviceOidMap.put( "publishedServiceOid", uddiServiceControl.getPublishedServiceOid() );
        return Arrays.asList(serviceOidMap);
    }

}
