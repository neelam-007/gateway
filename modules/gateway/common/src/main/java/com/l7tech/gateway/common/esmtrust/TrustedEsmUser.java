package com.l7tech.gateway.common.esmtrust;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;

import org.hibernate.annotations.Proxy;

/**
 * Represents a mapping of an ESM user ID (on some Trusted ESM) to a local user ID on this Gateway cluster.
 */
@Entity
@Proxy(lazy=false)
@Table(name="trusted_esm_user")
public class TrustedEsmUser extends PersistentEntityImp {
    private transient TrustedEsm trustedEsm;
    private long providerOid;
    private String ssgUserId;
    private String esmUserId;
    private String esmUserDisplayName;

    @Override
    @Version
    @Column(name="version")
    public int getVersion() {
        return super.getVersion();
    }

    @ManyToOne(optional=false)
    @JoinColumn(name="trusted_esm_oid", nullable=false)
    public TrustedEsm getTrustedEsm() {
        return trustedEsm;
    }

    public void setTrustedEsm(TrustedEsm trustedEsm) {
        this.trustedEsm = trustedEsm;
    }

    @Column(name="provider_oid")
    public long getProviderOid() {
        return providerOid;
    }

    public void setProviderOid(long providerOid) {
        this.providerOid = providerOid;
    }

    @Column(name="user_id", length=128)
    public String getSsgUserId() {
        return ssgUserId;
    }

    public void setSsgUserId(String ssgUserId) {
        this.ssgUserId = ssgUserId;
    }

    @Column(name="esm_user_id", length=128)
    public String getEsmUserId() {
        return esmUserId;
    }

    public void setEsmUserId(String esmUserId) {
        this.esmUserId = esmUserId;
    }

    @Column(name="esm_user_display_name", length=128)
    public String getEsmUserDisplayName() {
        return esmUserDisplayName;
    }

    public void setEsmUserDisplayName(String esmUserDisplayName) {
        this.esmUserDisplayName = esmUserDisplayName;
    }

    @Transient
    public String getName() {
        return esmUserId + " -> " + ssgUserId;
    }
}
