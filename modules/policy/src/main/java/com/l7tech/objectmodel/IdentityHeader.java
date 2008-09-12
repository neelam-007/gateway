/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

/**
 * Extends EntityHeader to include an Identity Provider OID.
 */
public class IdentityHeader extends EntityHeader {
    private final long providerOid;

    public IdentityHeader( final IdentityHeader header ) {
        super(header.getStrId(), header.getType(), header.getName(), header.getDescription());
        this.providerOid = header.getProviderOid();
    }

    public IdentityHeader( final long providerOid, final String identityId, final EntityType type, final String name, final String description) {
        super(identityId, type, name, description);
        if (type != EntityType.USER && type != EntityType.GROUP && type != EntityType.MAXED_OUT_SEARCH_RESULT) throw new IllegalArgumentException("EntityType must be USER or GROUP");
        this.providerOid = providerOid;
    }

    public IdentityHeader( final long providerOid, final EntityHeader header) {
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

    @Override
    public int compareTo(Object o) {
        int compareResult = super.compareTo(o);
        if ( compareResult == 0 ) {
            final IdentityHeader that = (IdentityHeader)o;
            compareResult = Long.valueOf( providerOid ).compareTo( that.providerOid );          
        }
        return compareResult;
    }
}
