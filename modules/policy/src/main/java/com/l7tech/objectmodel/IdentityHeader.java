package com.l7tech.objectmodel;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Extends EntityHeader to include an Identity Provider OID.
 */
@XmlRootElement
public class IdentityHeader extends ZoneableEntityHeader {
    private long providerOid;
    private final String commonName;
    private final boolean enabled;

    @Deprecated // use for serialization only
    public IdentityHeader() {
        commonName = "";
        enabled = true;
    }

    public IdentityHeader(long providerOid, long identityOid, EntityType type, String loginName, String description, String commonName, int version) {
        this(providerOid, Long.toString(identityOid), type, loginName, description, commonName, version, true);
    }

    public IdentityHeader(long providerOid, long identityOid, EntityType type, String loginName, String description, String commonName, int version, boolean enabled) {
        this(providerOid, Long.toString(identityOid), type, loginName, description, commonName, version, enabled);
    }

    //added for bug #5321
    public IdentityHeader(long providerOid, String identityId, EntityType type, String loginName, String description, String commonName, Integer version) {
        this(providerOid, identityId, type, loginName, description, commonName, version, true );
    }

    public IdentityHeader(long providerOid, String identityId, EntityType type, String loginName, String description, String commonName, Integer version, boolean enabled) {
        super(identityId, type, loginName, description, version);
        if (type != EntityType.USER && type != EntityType.GROUP)
            throw new IllegalArgumentException("EntityType must be USER or GROUP");
        this.providerOid = providerOid;
        this.commonName = commonName;
        this.enabled = enabled;
    }

    public IdentityHeader( long providerOid, EntityHeader header ) {
        this(providerOid, header.getStrId(), header.getType(), header.getName(), header.getDescription(), getCommonName(header), header.getVersion(),
                !(header instanceof IdentityHeader) || ((IdentityHeader) header).isEnabled() );
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

    @XmlTransient
    public boolean isEnabled() {
        return enabled;
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
