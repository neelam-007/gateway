package com.l7tech.identity.cert;

import com.l7tech.identity.User;

/**
 * User: flascell
 * Date: Jul 29, 2003
 * Time: 11:35:46 AM
 *
 * Manages the client_cert data
 */
public interface ClientCertManager {

    /**
     * checks whether the user passwd is authorized to generate a new cert.
     * if the user already has a cert and it has been consumed, this will return false;
     * if the user has a non-consumed cert and that cert has been regenerated 10 times,
     * it will also return false.
     */
    boolean userCanGenCert(String userid);

    /**
     * records new cert
     */
    void recordNewCert(String userid, byte[] cert);

    /**
     *
     */
    byte[] getCert(String userid);
}
