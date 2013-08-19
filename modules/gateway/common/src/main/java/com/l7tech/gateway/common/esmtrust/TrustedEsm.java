package com.l7tech.gateway.common.esmtrust;

import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.security.cert.TrustedCert;
import org.hibernate.annotations.Proxy;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents an Enterprise Manager Server instance that is trusted to manage this Gateway cluster.
 */
@Entity
@Proxy(lazy=false)
@Table(name="trusted_esm")
public class TrustedEsm extends NamedGoidEntityImp {
    private TrustedCert trustedCert;

    @ManyToOne(optional=false)
    @JoinColumn(name="trusted_cert_goid", nullable=false)
    public TrustedCert getTrustedCert() {
        return trustedCert;
    }

    public void setTrustedCert(TrustedCert trustedCert) {
        this.trustedCert = trustedCert;
    }
}
