/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author alex
 */
public class ManagerPolicyCache implements EntityInvalidationListener, ReadOnlyEntityManager<Policy, EntityHeader>, GuidBasedEntityManager<Policy>, ApplicationListener {
    private final Map<Goid, Policy> cache = new ConcurrentHashMap<Goid, Policy>();
    private final Map<String, Goid> guidToOidMap = new ConcurrentHashMap<String, Goid>();

    public ManagerPolicyCache() {
    }

    @Override
    public Policy findByPrimaryKey(long oid) throws FindException {
        throw new UnsupportedOperationException("Oids are not supported here.");
    }

    @Override
    public Policy findByPrimaryKey(Goid goid) throws FindException {
        Policy p = cache.get(goid);
        if (p != null) return p;

        p = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(goid);
        if (p != null) {
            cache.put(goid, p);
            guidToOidMap.put(p.getGuid(), p.getGoid());
        }
        return p;
    }

    @Override
    public Policy findByGuid(String guid) throws FindException {
        Goid goid = guidToOidMap.get(guid);
        if(goid == null) {
            Policy p = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(guid);
            if(p != null) {
                cache.put(p.getGoid(), p);
                guidToOidMap.put(p.getGuid(), p.getGoid());
            }
            return p;
        } else {
            Policy p = findByPrimaryKey(goid);
            if(p == null) {
                guidToOidMap.remove(guid);
            }
            return p;
        }
    }

    @Override
    public Collection<EntityHeader> findAllHeaders() throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Policy> findAll() throws FindException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void notifyDelete( final EntityHeader entityHeader ) {
        invalidate( entityHeader );
    }

    @Override
    public void notifyUpdate( final EntityHeader entityHeader ) {
        invalidate( entityHeader );
    }

    @Override
    public Class<Policy> getImpClass() {
        return Policy.class;
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if ( applicationEvent instanceof LogonEvent ) {
            cache.clear();
            guidToOidMap.clear();
        }
    }

    private void invalidate( final EntityHeader entityHeader ) {
        if ( entityHeader.getType() == EntityType.POLICY ) {
            Policy removed = cache.remove( entityHeader.getOid() );
            if(removed != null) {
                guidToOidMap.remove(removed.getGuid());
            }
        }
    }
}
