/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.CredentialsUnavailableException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;

import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default CredentialManager implementation.  This version requires that the credentials already be
 * configured in the Ssg object; it takes no action.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:39:29 AM
 */
public class CredentialManagerImpl extends CredentialManager {
    private static final Logger log = Logger.getLogger(CredentialManagerImpl.class.getName());
    private static CredentialManagerImpl INSTANCE = new CredentialManagerImpl();

    public CredentialManagerImpl() {}

    public static CredentialManagerImpl getInstance() {
        return INSTANCE;
    }

    public PasswordAuthentication getCredentials(Ssg ssg)
            throws OperationCanceledException
    {
        PasswordAuthentication pw = ssg.getRuntime().getCredentials();
        if (pw != null)
            return pw;

        log.log(Level.WARNING, "Headless CredentialManager: unable to obtain new credentials");
        throw new CredentialsUnavailableException("Unable to obtain new credentials");
    }

    public PasswordAuthentication getCredentialsWithReasonHint(Ssg ssg,
                                                               CredentialManager.ReasonHint hint,
                                                               boolean disregardExisting,
                                                               boolean reportBadPassword)
            throws OperationCanceledException
    {
        if (disregardExisting || reportBadPassword)
            return getNewCredentials(ssg, reportBadPassword);
        else
            return getCredentials(ssg);
    }

    public PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException {
        log.log(Level.WARNING, "Headless CredentialManager: unable to obtain new credentials");
        throw new CredentialsUnavailableException("Unable to obtain new credentials");
    }

    public void notifyLengthyOperationStarting(Ssg ssg, String message) {
        log.info("Starting lengthy operation for Gateway " + ssg + ": " + message);
    }

    public void notifyLengthyOperationFinished(Ssg ssg) {
        log.info("Lengthy operation for Gateway " + ssg + " has completed");
    }

    public void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException {
        log.log(Level.SEVERE, "Key store for Gateway " + ssg + " has been damaged -- aborting");
        throw new OperationCanceledException("Unable to authorize deletion of corrupt keystore");
    }

    /**
     * Notify the user that a client certificate has already been issued for his account.
     * At this point there is nothing the user can do except try a different account, or contact
     * his Gateway administrator and beg to have the lost certificate revoked from the database.
     *
     * @param ssg
     */
    public void notifyCertificateAlreadyIssued(Ssg ssg) {
        log.log(Level.SEVERE, "Certificate has already been issued for this account on Gateway " + ssg + "; the server refuses to give us a new one until the Gateway admin revokes our old one");
    }

    /**
     * Notify the user that an SSL connection to the SSG could not be established because the hostname did not match
     * the one in the certificate.
     *
     * @param ssg
     * @param whatWeWanted  the expected hostname, equal to ssg.getSsgAddress()
     * @param whatWeGotInstead  the hostname in the peer's certificate
     */
    public void notifySsgHostnameMismatch(Ssg ssg, String whatWeWanted, String whatWeGotInstead) {
        log.log(Level.SEVERE, "Gateway hostname " + whatWeWanted + " does not match hostname in peer certificate: \"" + whatWeGotInstead + "\"");
    }

    public void notifySsgCertificateUntrusted(Ssg ssg, X509Certificate certificate) throws OperationCanceledException {
        String msg = "The downloaded Gateway server certificate could not be verified with the current user name and password.";
        log.log(Level.SEVERE, msg);
        throw new OperationCanceledException(msg);
    }
}
