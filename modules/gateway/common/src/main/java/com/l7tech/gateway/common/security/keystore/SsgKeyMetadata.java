package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.imp.ZoneablePersistentEntityImp;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Proxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.*;

/**
 * Represents metadata that is associated with a key in a keystore accessible to the Gateway, indexed by
 * keystore OID and key alias.
 * <p/>
 * TODO Use this mechanism to track special purpose keys (default SSL, default CA, audit signer, audit viewer, etc) instead of using a bunch of cluster properties
 */
@Entity
@Proxy(lazy=false)
@Table(name="keystore_key_metadata")
public class SsgKeyMetadata extends ZoneablePersistentEntityImp {

    long keystoreOid;
    String alias;

    public SsgKeyMetadata() {
    }

    public SsgKeyMetadata(long keystoreOid, @NotNull String alias, @Nullable SecurityZone securityZone) {
        setKeystoreOid(keystoreOid);
        setAlias(alias);
        setSecurityZone(securityZone);
    }

    @Column(name = "keystore_file_oid", nullable=false)
    public long getKeystoreOid() {
        return keystoreOid;
    }

    public void setKeystoreOid(long keystoreOid) {
        this.keystoreOid = keystoreOid;
    }

    @Column(name = "alias", nullable=false)
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * Overridden because it is transient in mapped superclass.
     */
    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    /**
     * Copy payload values from the specified source to the specified destination.
     * <p/>
     * Payload values are any field other than oid, version, keystoreOid and alias.
     *
     * @param source source.  Required.
     * @param dest   destination.  Required.
     */
    public static void copyPayloadValues(@NotNull SsgKeyMetadata source, @NotNull SsgKeyMetadata dest) {
        dest.setSecurityZone(source.getSecurityZone());
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SsgKeyMetadata)) return false;
        if (!super.equals(o)) return false;

        SsgKeyMetadata metadata = (SsgKeyMetadata) o;

        if (keystoreOid != metadata.keystoreOid) return false;
        if (alias != null ? !alias.equals(metadata.alias) : metadata.alias != null) return false;
        if (securityZone != null ? !securityZone.equals(metadata.securityZone) : metadata.securityZone != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (keystoreOid ^ (keystoreOid >>> 32));
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (securityZone != null ? securityZone.hashCode() : 0);
        return result;
    }
}
