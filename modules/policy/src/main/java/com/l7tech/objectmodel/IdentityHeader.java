package com.l7tech.objectmodel;

import org.hibernate.annotations.Type;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Extends EntityHeader to include an Identity Provider OID.
 */
@XmlRootElement
public class IdentityHeader extends EntityHeader {
    private Goid providerGoid;
    private final String commonName;
    private final boolean enabled;

    @Deprecated // use for serialization only
    public IdentityHeader() {
        commonName = "";
        enabled = true;
    }

    public IdentityHeader(Goid providerGoid, Goid identityOid, EntityType type, String loginName, String description, String commonName, int version) {
        this(providerGoid, Goid.toString(identityOid), type, loginName, description, commonName, version, true);
    }

    public IdentityHeader(Goid providerGoid, Goid identityOid, EntityType type, String loginName, String description, String commonName, int version, boolean enabled) {
        this(providerGoid, Goid.toString(identityOid), type, loginName, description, commonName, version, enabled);
    }

    //added for bug #5321
    public IdentityHeader(Goid providerGoid, String identityId, EntityType type, String loginName, String description, String commonName, Integer version) {
        this(providerGoid, identityId, type, loginName, description, commonName, version, true );
    }

    public IdentityHeader(Goid providerGoid, String identityId, EntityType type, String loginName, String description, String commonName, Integer version, boolean enabled) {
        super(identityId, type, loginName, description, version);
        if (type != EntityType.USER && type != EntityType.GROUP)
            throw new IllegalArgumentException("EntityType must be USER or GROUP");
        this.providerGoid = providerGoid;
        this.commonName = commonName;
        this.enabled = enabled;
    }

    public IdentityHeader( Goid providerGoid, EntityHeader header ) {
        this(providerGoid, header.getStrId(), header.getType(), header.getName(), header.getDescription(), getCommonName(header), header.getVersion(),
                !(header instanceof IdentityHeader) || ((IdentityHeader) header).isEnabled() );
    }

    @XmlAttribute()
    @XmlJavaTypeAdapter(GoidAdapter.class)
    public Goid getProviderGoid() {
        return providerGoid;
    }

    @Deprecated // use for serialization only
    public void setProviderGoid(Goid providerGoid) {
        this.providerGoid = providerGoid;
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

        return providerGoid.equals(that.providerGoid);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + providerGoid.hashCode();
        return result;
    }

    @Override
    public int compareTo(Object o) {
        int compareResult = super.compareTo(o);
        if ( compareResult == 0 ) {
            final IdentityHeader that = (IdentityHeader)o;
            compareResult = providerGoid.compareTo(that.providerGoid);
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
