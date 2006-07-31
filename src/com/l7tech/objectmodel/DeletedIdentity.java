/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import com.l7tech.identity.Identity;

/**
 * A {@link AnonymousEntityReference} with a {@link #providerId}.
 * @author alex
 */
public class DeletedIdentity extends AnonymousEntityReference implements Identity {
    private final long providerId;

    public DeletedIdentity(Class entityClass, long providerOid, String identityId) {
        super(entityClass, identityId);
        this.providerId = providerOid;
    }

    public long getProviderId() {
        return providerId;
    }

    public String getUniqueIdentifier() {
        return uniqueId;
    }
}
