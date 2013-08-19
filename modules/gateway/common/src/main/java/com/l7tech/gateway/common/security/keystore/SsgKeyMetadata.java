package com.l7tech.gateway.common.security.keystore;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.imp.ZoneableGoidEntityImp;
import org.hibernate.annotations.Proxy;
import org.hibernate.annotations.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Represents metadata that is associated with a key in a keystore accessible to the Gateway, indexed by
 * keystore OID and key alias.
 * <p/>
 * TODO Use this mechanism to track special purpose keys (default SSL, default CA, audit signer, audit viewer, etc) instead of using a bunch of cluster properties
 */
@Entity
@Proxy(lazy=false)
@Table(name="keystore_key_metadata")
public class SsgKeyMetadata extends ZoneableGoidEntityImp {

    Goid keystoreGoid;
    String alias;

    public SsgKeyMetadata() {
    }

    public SsgKeyMetadata(Goid keystoreGoid, @NotNull String alias, @Nullable SecurityZone securityZone) {
        setKeystoreGoid(keystoreGoid);
        setAlias(alias);
        setSecurityZone(securityZone);
    }

    @Column(name = "keystore_file_goid", nullable=false)
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getKeystoreGoid() {
        return keystoreGoid;
    }

    public void setKeystoreGoid(Goid keystoreGoid) {
        this.keystoreGoid = keystoreGoid;
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

        if (!Goid.equals(keystoreGoid, metadata.keystoreGoid)) return false;
        if (alias != null ? !alias.equals(metadata.alias) : metadata.alias != null) return false;
        if (securityZone != null ? !securityZone.equals(metadata.securityZone) : metadata.securityZone != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (keystoreGoid != null ? keystoreGoid.hashCode() : 0);
        result = 31 * result + (alias != null ? alias.hashCode() : 0);
        result = 31 * result + (securityZone != null ? securityZone.hashCode() : 0);
        return result;
    }
}
