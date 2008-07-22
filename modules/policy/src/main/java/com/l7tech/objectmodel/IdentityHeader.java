/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

/**
 * Extends EntityHeader to include an Identity Provider OID.
 */
public class IdentityHeader extends EntityHeader {
    private final long providerOid;

    public IdentityHeader(long providerOid, String identityId, EntityType type, String name, String description) {
        super(identityId, type, name, description);
        if (type != EntityType.USER && type != EntityType.GROUP && type != EntityType.MAXED_OUT_SEARCH_RESULT) throw new IllegalArgumentException("EntityType must be USER or GROUP");
        this.providerOid = providerOid;
    }

    public IdentityHeader(long providerOid, EntityHeader header) {
        this(providerOid, header.getStrId(), header.getType(), header.getName(), header.getDescription());
    }

    public long getProviderOid() {
        return providerOid;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final IdentityHeader that = (IdentityHeader)o;

        return providerOid == that.providerOid;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (int)(providerOid ^ (providerOid >>> 32));
        return result;
    }
}
