package com.l7tech.gateway.common.emstrust;

import com.l7tech.objectmodel.imp.PersistentEntityImp;

import javax.persistence.*;

/**
 * Represents a mapping of an EMS user ID (on some Trusted EMS) to a local user ID on this Gateway cluster.
 */
@Entity
@Table(name="trusted_ems_user")
public class TrustedEmsUser extends PersistentEntityImp {
    private TrustedEms trustedEms;
    private long providerOid;
    private String ssgUserId;
    private String emsUserId;

    @ManyToOne(optional=false)
    @JoinColumn(name="trusted_ems_oid", nullable=false)
    public TrustedEms getTrustedEms() {
        return trustedEms;
    }

    public void setTrustedEms(TrustedEms trustedEms) {
        this.trustedEms = trustedEms;
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

    @Column(name="ems_user_id", length=128)
    public String getEmsUserId() {
        return emsUserId;
    }

    public void setEmsUserId(String emsUserId) {
        this.emsUserId = emsUserId;
    }
}
