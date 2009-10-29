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
