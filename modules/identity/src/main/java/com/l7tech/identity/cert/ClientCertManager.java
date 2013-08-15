/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.cert;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * DAO interface for {@link CertEntryRow}s.
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
    boolean userCanGenCert(User user, Certificate requestCert) throws FindException;

    /**
     * Check if the specified user certificate looks like it was probably signed by an earlier version of the
     * cluster CA cert.  This check will return true unless all of the following are true:
     * <ul>
     * <li>The specified cert DN matches the cluster CA DN;
     * <li>The specified cert has an authority key id (AKI);
     * <li>The cluster CA cert has a subject key identifier (SKI);
     * <li>The specified cert AKI differs from the cluster cert SKI.
     * </ul>
     *
     * @param userCert the certificate to examine
     * @return true if and only if the cert was confirmed to be probably stale.
     */
    boolean isCertPossiblyStale(X509Certificate userCert);

    /**
     * Records new cert for the user (if user is allowed)
     *
     * @param cert the cert to record
     * @throws UpdateException if user was not in a state that allowes the creation
     * of a cert or if an internal error occurs
     */
    void recordNewUserCert(User user, Certificate cert, boolean oldCertWasStale) throws UpdateException;

    /**
     * retrieves existing cert for this user
     */
    Certificate getUserCert(User user) throws FindException;

    /**
     * revokes the cert (if applicable) for this user
     */
    void revokeUserCert(User user) throws UpdateException, ObjectNotFoundException;

    /**
     * revokes the cert (if applicable) for this user and the certificate issuer matches
     * the given value.
     */
    boolean revokeUserCertIfIssuerMatches(User user, X500Principal issuer) throws UpdateException, ObjectNotFoundException;

    /**
     * record the fact that the a user cert was used successfully in an authentication operation
     * this will prevent the user to regen his cert until the administrator revokes the cert
     *
     * @param user owner of the cert
     * @throws UpdateException
     */
    void forbidCertReset(User user) throws UpdateException;

    /**
     * @return {@link com.l7tech.security.cert.TrustedCert}s with the matching base64'd SHA-1 thumbprint. Never null, but may be empty.
     * @param thumbprint the base64'd SHA-1 thumbprint value to search for. May be null.
     */
    List<CertEntryRow> findByThumbprint(String thumbprint) throws FindException;

    List<CertEntryRow> findBySki(String ski) throws FindException;

    List<CertEntryRow> findByIssuerAndSerial(X500Principal issuer, BigInteger serial) throws FindException;

    List<CertEntryRow> findBySubjectDn(X500Principal subjectDn) throws FindException;

    /**
     * Get information on all existing keys.
     *
     * @return The list of key information (may be empty)
     * @throws FindException If an error occurs
     */
    List<CertInfo> findAll() throws FindException;

    /**
     * Thrown by a {@link com.l7tech.identity.UserManager} if it doesn't like the cert
     */
    public static class VetoSave extends Exception {
        public VetoSave() {
        }

        public VetoSave(String message) {
            super(message);
        }

        public VetoSave(String message, Throwable cause) {
            super(message, cause);
        }

        public VetoSave(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Information for a Certificate
     *
     * @see ClientCertManager#findAll()
     */
    interface CertInfo {
        Goid getProviderId();
        String getUserId();
        String getLogin();
    }
}
