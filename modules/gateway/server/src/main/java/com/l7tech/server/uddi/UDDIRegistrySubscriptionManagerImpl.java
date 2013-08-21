package com.l7tech.server.uddi;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.HibernateEntityManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class UDDIRegistrySubscriptionManagerImpl extends HibernateEntityManager<UDDIRegistrySubscription, EntityHeader> implements UDDIRegistrySubscriptionManager {

    //- PUBLIC

    @Override
    public UDDIRegistrySubscription findByUDDIRegistryGoid( final Goid uddiRegistryGoid ) throws FindException {
        return findByUniqueKey( "uddiRegistryGoid", uddiRegistryGoid );
    }

    @Override
    public Collection<UDDIRegistrySubscription> findBySubscriptionKey( final String subscriptionKey ) throws FindException {
        return findByPropertyMaybeNull( "subscriptionKey", subscriptionKey );
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return UDDIRegistrySubscription.class;
    }

    //- PROTECTED

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints( final UDDIRegistrySubscription uddiRegistrySubscription ) {
        Map<String,Object> serviceOidMap = new HashMap<String, Object>();
        serviceOidMap.put( "uddiRegistryGoid", uddiRegistrySubscription.getUddiRegistryGoid() );
        return Arrays.asList(serviceOidMap);
    }
}
