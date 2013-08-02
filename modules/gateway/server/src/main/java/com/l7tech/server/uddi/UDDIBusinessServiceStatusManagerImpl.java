package com.l7tech.server.uddi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 *
 */
public class UDDIBusinessServiceStatusManagerImpl extends HibernateEntityManager<UDDIBusinessServiceStatus, EntityHeader> implements UDDIBusinessServiceStatusManager {

    //- PUBLIC

    @Override
    public Collection<UDDIBusinessServiceStatus> findByRegistryAndMetricsStatus( final long registryOid,
                                                                                 final UDDIBusinessServiceStatus.Status status ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        if ( status != null ) {
            matchMap.put( "uddiMetricsReferenceStatus", status );
        }
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIBusinessServiceStatus> findByRegistryAndWsPolicyPublishStatus( final long registryOid,
                                                                                         final UDDIBusinessServiceStatus.Status status ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryOid", registryOid );
        if ( status != null ) {
            matchMap.put( "uddiPolicyStatus", status );
        }
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIBusinessServiceStatus> findByPublishedService(Goid publishedServiceGoid) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "publishedServiceGoid", publishedServiceGoid );
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIBusinessServiceStatus.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

}
