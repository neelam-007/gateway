/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.proxy.datamodel.CredentialManagerImpl;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.SslPeer;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Credential manager for use in the interactive Bridge command line configurator.
 * This credential manager does not support multithreaded operation.
 */
class CommandSessionCredentialManager extends CredentialManagerImpl {
    private final CommandSession session;
    private List lastFailedServerCerts = new ArrayList();
    private List trustedCertFingerprints = new ArrayList();

    /**
     * Creates a credential manager that will use command line prompts for usernames and passwords.  When prompting
     * for a password, the specified output stream will be spammed with backspaces followed by asterisks to
     * attempt to mask the password.  (Sadly, that appears to be the best that can be done with the current JRE.)
     *
     * @param session  the command session we are attached to.  Must not be null.
     */
    CommandSessionCredentialManager(CommandSession session) {
        super();
        this.session = session;
    }

    public void notifyLengthyOperationStarting(Ssg ssg, String message) {
        session.getOut().print("Generating new RSA key pair... ");
    }

    public void notifyLengthyOperationFinished(Ssg ssg) {
        session.getOut().println("Done.");
    }

    static class BadKeystoreException extends OperationCanceledException {
        public BadKeystoreException(String message) {
            super(message);
        }
    }

    public void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException {
        throw new BadKeystoreException("Unable to authorize deletion of corrupt keystore");
    }

    public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate cert)
            throws OperationCanceledException
    {
        final String gotPrintMD5 = getThumbprint(cert, CertUtils.ALG_MD5).toLowerCase();
        final String gotPrintSHA = getThumbprint(cert, CertUtils.ALG_SHA1).toLowerCase();

        for (Iterator i = trustedCertFingerprints.iterator(); i.hasNext();) {
            String printPrefix = ((String)i.next()).toLowerCase();
            if (gotPrintMD5.startsWith(printPrefix)) {
                session.getOut().println("Manually trusting server certificate with MD5 fingerprint " + gotPrintMD5);
                return; // Trust this certificate
            }
            if (gotPrintSHA.startsWith(printPrefix)) {
                session.getOut().println("Manually trusting server certificate with SHA1 fingerprint " + gotPrintSHA);
                return; // Trust this certificate
            }
        }

        lastFailedServerCerts.add(cert);
        throw new OperationCanceledException("Unable to automatically trust server certificate.");
    }

    private String getThumbprint(X509Certificate cert, String alg) throws OperationCanceledException {
        try {
            return CertUtils.getCertificateFingerprint(cert, alg, CertUtils.FINGERPRINT_RAW_HEX).toLowerCase();
        } catch (CertificateEncodingException e) {
            throw new OperationCanceledException("Invalid server certificate: " + ExceptionUtils.getMessage(e), e); // can't happen, would have been caught earlier
        } catch (NoSuchAlgorithmException e) {
            throw new OperationCanceledException("Unable to compute cert thumbprint: " + ExceptionUtils.getMessage(e), e); // can't happen, misconfigured VM
        }
    }

    public void saveSsgChanges(Ssg ssg) {
        session.onChangesMade(); // mark the session as dirty so any changed state will get saved
    }

    /**
     * Get the list of X509Certificate objects that have been passed to {@link #notifySslCertificateUntrusted} since
     * the last time {@link #clearLastFailedServerCerts} was called.
     *
     * @return the list of recently failed server certs.  May be empty, but never null.
     */
    public List getLastFailedServerCerts() {
        return Collections.unmodifiableList(lastFailedServerCerts);
    }

    /**
     * Clear the collection of failed server certificates so we can begin collecting more.
     */
    public void clearLastFailedServerCerts() {
        lastFailedServerCerts.clear();
    }

    /**
     * Manually trust any future X.509 certificate whose SHA-1 or MD5 fingerprint, when encoded as hex, starts
     * with the specified thumbprint string or prefix.
     *
     * @param thumbPrint the thumbprint to trust, or some prefix of the thumbprint.  Must not be null or empty.
     */
    public void addTrustedServerCertThumbprint(String thumbPrint) {
        if (thumbPrint == null || thumbPrint.length() < 1)
            throw new IllegalArgumentException("A non-empty thumbprint is needed");
        trustedCertFingerprints.add(thumbPrint);
    }
}
