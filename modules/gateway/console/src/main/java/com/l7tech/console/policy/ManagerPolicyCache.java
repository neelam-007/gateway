/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy;

import com.l7tech.policy.Policy;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author alex
 */
public class ManagerPolicyCache implements EntityInvalidationListener, ReadOnlyEntityManager<Policy, EntityHeader>, GuidBasedEntityManager<Policy> {
    private final Map<Long, Policy> cache = new ConcurrentHashMap<Long, Policy>();
    private final Map<String, Long> guidToOidMap = new ConcurrentHashMap<String, Long>();

    public ManagerPolicyCache() {
    }

    public Policy findByPrimaryKey(long oid) throws FindException {
        Policy p = cache.get(oid);
        if (p != null) return p;

        p = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(oid);
        if (p != null) {
            cache.put(oid, p);
            guidToOidMap.put(p.getGuid(), p.getOid());
        }
        return p;
    }

    public Policy findByGuid(String guid) throws FindException {
        Long oid = guidToOidMap.get(guid);
        if(oid == null) {
            Policy p = Registry.getDefault().getPolicyAdmin().findPolicyByGuid(guid);
            if(p != null) {
                cache.put(p.getOid(), p);
                guidToOidMap.put(p.getGuid(), p.getOid());
            }
            return p;
        } else {
            Policy p = findByPrimaryKey(oid);
            if(p == null) {
                guidToOidMap.remove(guid);
            }
            return p;
        }
    }

    public Collection<EntityHeader> findAllHeaders() throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection<Policy> findAll() throws FindException {
        throw new UnsupportedOperationException();
    }

    public Collection<EntityHeader> findAllHeaders(int offset, int windowSize) {
        throw new UnsupportedOperationException();
    }

    public void notifyDelete( final EntityHeader entityHeader ) {
        invalidate( entityHeader );
    }

    public void notifyUpdate( final EntityHeader entityHeader ) {
        invalidate( entityHeader );
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
