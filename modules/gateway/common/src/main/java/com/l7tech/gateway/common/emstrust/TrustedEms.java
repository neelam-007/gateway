package com.l7tech.gateway.common.emstrust;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.security.cert.TrustedCert;

import javax.persistence.*;

/**
 * Represents an Enterprise Manager Server instance that is trusted to manage this Gateway cluster.
 */
@Entity
@Table(name="trusted_ems")
public class TrustedEms extends NamedEntityImp {
    private TrustedCert trustedCert;


    @ManyToOne(optional=false)
    @JoinColumn(name="trusted_cert_oid", nullable=false)
    public TrustedCert getTrustedCert() {
        return trustedCert;
    }

    public void setTrustedCert(TrustedCert trustedCert) {
        this.trustedCert = trustedCert;
    }
}
