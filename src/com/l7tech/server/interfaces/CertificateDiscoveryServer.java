/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * This is the interface provided by the Gateway that clients (including the Bridge and Console) can use to obtain
 * the Gateway's server certificate.  If the client provides a valid username and the Gateway has access
 * to that user's password, a hash of the password with the server certificate will be returned along with the
 * certificate; assuming the client knows the same password, it can recompute the hash and thereby verify the
 * integrity of the server certificate.
 * @author mike
 */
public interface CertificateDiscoveryServer extends Remote {
    class CertificateInfo {
        String encodedX509Certificate;
    }

    /**
     * Obtain this Gateway's server certificate.  Barring a serious configuration problem or
     * internal error, this call will always succeed; however, it may not be possible
     * for the client to establish the authenticity of the certificate if the provided username
     * is not known to the Gateway, or the corresponding password is not available. 
     * @param username
     * @param nonce
     * @return
     * @throws RemoteException
     */
    CertificateInfo getServerCertificate(String username, String nonce) throws RemoteException;
}
