package com.l7tech.identity.cert;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;

import java.security.cert.Certificate;

/**
 * This is our internal CA. It manages the client_cert data.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Jul 29, 2003<br/>
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
     * @throws UpdateException if user was not in a state that allowes the creation
     * of a cert or if an internal error occurs
     */
    void recordNewUserCert(User user, Certificate cert) throws UpdateException;

    /**
     * retrieves existing cert for this user
     */
    Certificate getUserCert(User user) throws FindException;

    /**
     * revokes the cert (if applicable) for this user
     */
    void revokeUserCert(User user) throws UpdateException, ObjectNotFoundException;

    /**
     * record the fact that the a user cert was used successfully in an authentication operation
     * this will prevent the user to regen his cert until the administrator revokes the cert
     *
     * @param user owner of the cert
     * @throws UpdateException
     */
    void forbidCertReset(User user) throws UpdateException;

    /**
     * Thrown by a {@link com.l7tech.identity.UserManager} if it doesn't like the cert
     */
    public static class VetoSave extends Exception {
        public VetoSave(String message) {
            super(message);
        }
    }
}
