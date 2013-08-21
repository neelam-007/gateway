package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class UDDIBusinessServiceStatusManagerImpl extends HibernateEntityManager<UDDIBusinessServiceStatus, EntityHeader> implements UDDIBusinessServiceStatusManager {

    //- PUBLIC

    @Override
    public Collection<UDDIBusinessServiceStatus> findByRegistryAndMetricsStatus( final Goid registryGoid,
                                                                                 final UDDIBusinessServiceStatus.Status status ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryGoid", registryGoid );
        if ( status != null ) {
            matchMap.put( "uddiMetricsReferenceStatus", status );
        }
        return findMatching( Collections.singletonList( matchMap ) );
    }

    @Override
    public Collection<UDDIBusinessServiceStatus> findByRegistryAndWsPolicyPublishStatus( final Goid registryGoid,
                                                                                         final UDDIBusinessServiceStatus.Status status ) throws FindException {
        final Map<String,Object> matchMap = new HashMap<String,Object>();
        matchMap.put( "uddiRegistryGoid", registryGoid );
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
