/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.CredentialsUnavailableException;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.SslPeer;

import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default CredentialManager implementation.  This version requires that the credentials already be
 * configured in the Ssg object; it takes no action.
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
        return pw;
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

    public void notifyCertificateAlreadyIssued(Ssg ssg) {
        log.log(Level.SEVERE, "Certificate has already been issued for this account on Gateway " + ssg + "; the server refuses to give us a new one until the Gateway admin revokes our old one");
    }

    public void notifySslHostnameMismatch(String server, String whatWeWanted, String whatWeGotInstead) {
        log.log(Level.SEVERE, "Gateway hostname " + whatWeWanted + " does not match hostname in peer certificate: \"" + whatWeGotInstead + "\"");
    }

    public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
        String msg = "The authenticity of the SSL server certificate could not be established automatically.";
        log.log(Level.SEVERE, msg);
        throw new OperationCanceledException(msg);
    }

    public void saveSsgChanges(Ssg ssg) {
        log.log(Level.SEVERE, "Unable to save changed Ssg information for Ssg " + ssg + "; ignoring the save request.");
    }
}
