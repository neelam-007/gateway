/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.util.ClientLogger;

import java.net.PasswordAuthentication;

/**
 * Default CredentialManager implementation.  This version requires that the credentials already be
 * configured in the Ssg object; it takes no action.
 *
 * User: mike
 * Date: Jun 27, 2003
 * Time: 10:39:29 AM
 */
public class CredentialManagerImpl extends CredentialManager {
    private static final ClientLogger log = ClientLogger.getInstance(CredentialManagerImpl.class);
    private static CredentialManagerImpl INSTANCE = new CredentialManagerImpl();

    private CredentialManagerImpl() {}

    public static CredentialManagerImpl getInstance() {
        return INSTANCE;
    }

    public PasswordAuthentication getCredentials(Ssg ssg)
            throws OperationCanceledException
    {
        PasswordAuthentication pw = ssg.getCredentials();
        if (pw != null)
            return pw;

        log.error("Headless CredentialManager: unable to obtain new credentials");
        throw new OperationCanceledException("Unable to obtain new credentials");
    }

    public PasswordAuthentication getNewCredentials(Ssg ssg) throws OperationCanceledException {
        log.error("Headless CredentialManager: unable to obtain new credentials");
        throw new OperationCanceledException("Unable to obtain new credentials");
    }

    public void notifyLengthyOperationStarting(Ssg ssg, String message) {
        log.info("Starting lengthy operation for Gateway " + ssg + ": " + message);
    }

    public void notifyLengthyOperationFinished(Ssg ssg) {
        log.info("Lengthy operation for Gateway " + ssg + " has completed");
    }

    public void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException {
        log.error("Key store for Gateway " + ssg + " has been damaged -- aborting");
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
        log.error("Certificate has already been issued for this account on Gateway " + ssg + "; the server refuses to give us a new one until the Gateway admin revokes our old one");
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
        log.error("Gateway hostname " + whatWeWanted + " does not match hostname in peer certificate: \"" + whatWeGotInstead + "\"");
    }
}
