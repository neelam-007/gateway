package com.l7tech.identity.cert;

import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

import java.security.cert.Certificate;

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
     *
     * if the user has no current cert or a cert that has not been used, this will
     * return true
     *
     */
    boolean userCanGenCert(User user);

    /**
     * Records new cert for the user (if user is allowed)
     *
     * @param cert the cert to record
     * @throws IllegalStateException if user was not in a state that allowes the creation
     * of a cert
     */
    void recordNewUserCert(User user, Certificate cert) throws IllegalStateException;

    /**
     * retrieves existing cert for this user
     */
    Certificate getUserCert(User user) throws FindException;

    /**
     * revokes the cert (if applicable) for this user
     */
    void revokeUserCert(User user);
}
