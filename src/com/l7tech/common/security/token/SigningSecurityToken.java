/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import java.security.cert.X509Certificate;

/**
 * @author alex
 * @version $Revision$
 */
public interface SigningSecurityToken extends SecurityToken {
    /**
     * @return true if the sender has proven its possession of the private key corresponding to this security token.
     * This is done by signing one or more elements of the message with it.
     */
    boolean isPossessionProved();

    /**
     * Mark this token as having proved the possession of its corresponding private key.
     * <p>
     * Normally, only {@link com.l7tech.common.security.xml.processor.WssProcessorImpl} should call this.
     */
    void onPossessionProved();

    /**
     * @return the certificate that was used to sign one or more elements of the message that contained this token
     */
    X509Certificate getMessageSigningCertificate();
}
