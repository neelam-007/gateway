/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.HttpChallengeRequiredException;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.security.token.SecurityTokenType;

import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * A CredentialManager that, on its own, delegates every call to another credential manager.
 * Subclasses can override particular methods.
 */
public class DelegatingCredentialManager extends CredentialManager {
    private final CredentialManager delegate;

    public DelegatingCredentialManager(CredentialManager delegate) {
        if (delegate == null) throw new IllegalArgumentException("delegate must be non-null");
        this.delegate = delegate;
    }

    protected CredentialManager getDelegate() {
        return delegate;
    }

    public PasswordAuthentication getCredentials(Ssg ssg) throws OperationCanceledException, HttpChallengeRequiredException {
        return getDelegate().getCredentials(ssg);
    }

    public PasswordAuthentication getCredentialsWithReasonHint(Ssg ssg, ReasonHint hint, boolean disregardExisting, boolean reportBadPassword) throws OperationCanceledException, HttpChallengeRequiredException {
        return getDelegate().getCredentialsWithReasonHint(ssg, hint, disregardExisting, reportBadPassword);
    }

    public PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException, HttpChallengeRequiredException {
        return getDelegate().getNewCredentials(ssg, displayBadPasswordMessage);
    }

    public PasswordAuthentication getAuxiliaryCredentials(Ssg ssg, SecurityTokenType tokenType, String stsHostname, ReasonHint hint, boolean reportBadPassword) throws OperationCanceledException {
        return getDelegate().getAuxiliaryCredentials(ssg, tokenType, stsHostname, hint, reportBadPassword);
    }

    public void notifyLengthyOperationStarting(Ssg ssg, String message) {
        getDelegate().notifyLengthyOperationStarting(ssg, message);
    }

    public void notifyLengthyOperationFinished(Ssg ssg) {
        getDelegate().notifyLengthyOperationFinished(ssg);
    }

    public void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException {
        getDelegate().notifyKeyStoreCorrupt(ssg);
    }

    public void notifyCertificateAlreadyIssued(Ssg ssg) {
        getDelegate().notifyCertificateAlreadyIssued(ssg);
    }

    public void notifyFeatureNotAvailable(Ssg ssg, String feature) {
        getDelegate().notifyFeatureNotAvailable(ssg, feature);
    }

    public void notifySslHostnameMismatch(String server, String whatWeWanted, String whatWeGotInstead) {
        getDelegate().notifySslHostnameMismatch(server, whatWeWanted, whatWeGotInstead);
    }

    public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
        getDelegate().notifySslCertificateUntrusted(sslPeer, serverDesc, untrustedCertificate);
    }

    public void saveSsgChanges(Ssg ssg) {
        getDelegate().saveSsgChanges(ssg);
    }
}
