/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Extends EntityHeader to include an Identity Provider OID.
 */
@XmlRootElement
public class IdentityHeader extends EntityHeader {
    private long providerOid;
    private String commonName = "";

    @Deprecated // use for serialization only
    protected IdentityHeader() {}

    public IdentityHeader(long providerOid, long identityOid, EntityType type, String loginName, String description, String commonName, int version) {
        this(providerOid, Long.toString(identityOid), type, loginName, description, commonName, version);
    }

    //added for bug #5321
    public IdentityHeader(long providerOid, String identityId, EntityType type, String loginName, String description, String commonName, Integer version) {
        super(identityId, type, loginName, description, version);
        if (type != EntityType.USER && type != EntityType.GROUP)
            throw new IllegalArgumentException("EntityType must be USER or GROUP");
        this.providerOid = providerOid;
        this.commonName = commonName;
    }

    public IdentityHeader( long providerOid, EntityHeader header ) {
        this(providerOid, header.getStrId(), header.getType(), header.getName(), header.getDescription(), getCommonName(header), header.getVersion());
    }

    @XmlAttribute
    public long getProviderOid() {
        return providerOid;
    }

    @Deprecated // use for serialization only
    public void setProviderOid(long providerOid) {
        this.providerOid = providerOid;
    }

    // /added for bug #5321
    @XmlTransient
    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final IdentityHeader that = (IdentityHeader) o;

        return providerOid == that.providerOid;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + (int) (providerOid ^ (providerOid >>> 32));
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

    private static String getCommonName( final EntityHeader entityHeader ) {
        String commonName = null;

        if ( entityHeader instanceof IdentityHeader ) {
            IdentityHeader identityHeader = (IdentityHeader) entityHeader;
            commonName = identityHeader.getCommonName();
        }

        return commonName;
    }
}
