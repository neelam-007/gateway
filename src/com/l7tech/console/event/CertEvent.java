package com.l7tech.console.event;

import com.l7tech.common.security.TrustedCert;

import java.util.EventObject;

/**
 * Event describing a action for Certificate.
 * <p/>
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CertEvent extends EventObject {

    private TrustedCert cert;

    /**
     * create the Certificate event
     *
     * @param source the event source
     */
    public CertEvent(Object source, TrustedCert cert) {
        super(source);
        this.cert = cert;
    }

    /**
     * Get the certificate
     *
     * @return TrustedCert  The cert retrieved.
     */
    public TrustedCert getCert() {
        return cert;
    }

}
