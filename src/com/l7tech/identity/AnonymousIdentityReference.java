/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.identity;

import com.l7tech.objectmodel.AnonymousEntityReference;

/**
 * @author alex
 */
public class AnonymousIdentityReference extends AnonymousEntityReference implements Identity {
    private final long providerOid;

    public AnonymousIdentityReference(Class<? extends Identity> entityClass, String uniqueId, long providerOid, String name) {
        super(entityClass, uniqueId, name);
        this.providerOid = providerOid;
    }

    public long getProviderId() {
        return providerOid;
    }

    public boolean isEquivalentId(Object thatId) {
        return uniqueId != null && uniqueId.equals(thatId);
    }

    public String getId() {
        return uniqueId;
    }
}
