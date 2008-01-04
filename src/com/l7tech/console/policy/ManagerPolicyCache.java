/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityInvalidationListener;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ReadOnlyEntityManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author alex
 */
public class ManagerPolicyCache implements EntityInvalidationListener, ReadOnlyEntityManager<Policy, EntityHeader> {
    private final Map<Long, Policy> cache = new ConcurrentHashMap<Long, Policy>();

    public ManagerPolicyCache() {
    }

    public Policy findByPrimaryKey(long oid) throws FindException {
        Policy p = cache.get(oid);
        if (p != null) return p;

        p = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(oid);
        if (p != null) cache.put(oid, p);
        return p;
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

    public void invalidate( final EntityHeader entityHeader ) {
        if ( entityHeader.getType() == EntityType.POLICY ) {
            cache.remove( entityHeader.getOid() );
        }
    }
}
