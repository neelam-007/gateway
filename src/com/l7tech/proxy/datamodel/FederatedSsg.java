/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.datamodel;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.net.PasswordAuthentication;

/**
 * @author alex
 * @version $Revision$
 */
public class FederatedSsg extends Ssg {
    public FederatedSsg(Ssg trustedSsg) {
        super();
        this.trustedSsg = trustedSsg;
    }

    public FederatedSsg(Ssg trustedSsg, long id ) {
        super( id );
        this.trustedSsg = trustedSsg;
    }

    public FederatedSsg(Ssg trustedSsg, long id, String serverAddress ) {
        super( id, serverAddress );
        this.trustedSsg = trustedSsg;
    }

    // ***********************************************************************
    // Delegated methods
    // ***********************************************************************
    public synchronized String getKeyStorePath() {
        return trustedSsg.getKeyStorePath();
    }

    File getKeyStoreFile() {
        return trustedSsg.getKeyStoreFile();
    }

    KeyStore keyStore() {
        return trustedSsg.keyStore();
    }

    Boolean haveClientCert() {
        return trustedSsg.haveClientCert();
    }

    public String getUsername() {
        return trustedSsg.getUsername();
    }

    PrivateKey privateKey() {
        return trustedSsg.privateKey();
    }

    boolean passwordWorkedForPrivateKey() {
        return trustedSsg.passwordWorkedForPrivateKey();
    }

    public synchronized X509Certificate clientCert() {
        return trustedSsg.clientCert();
    }

    // ***********************************************************************
    // Hardcoded methods
    // ***********************************************************************
    public boolean isSavePasswordToDisk() {
        return false;
    }

    public boolean passwordWorkedWithSsg() {
        return false;
    }

    public boolean promptForUsernameAndPassword() {
        return false;
    }

    // ***********************************************************************
    // Unsupported methods
    // ***********************************************************************
    public void setUsername( String username ) {
        throw new UnsupportedOperationException("Federated SSGs don't have their own usernames");
    }

    public char[] cmPassword() {
        throw new UnsupportedOperationException("Federated SSGs don't have passwords");
    }

    public void cmPassword( char[] password ) {
        throw new UnsupportedOperationException("Federated SSGs don't have passwords");
    }

    public void setSavePasswordToDisk( boolean savePasswordToDisk ) {
        throw new UnsupportedOperationException("Federated SSGs don't have passwords");
    }

    public void passwordWorkedWithSsg( boolean worked ) {
        throw new UnsupportedOperationException("Federated SSGs don't have passwords");
    }

    public void promptForUsernameAndPassword( boolean promptForUsernameAndPassword ) {
        throw new UnsupportedOperationException("Federated SSGs don't have passwords");
    }

    public void setKeyStorePath( String keyStorePath ) {
        throw new UnsupportedOperationException("Federated SSGs don't have their own keystores");
    }

    void keyStore( KeyStore keyStore ) {
        throw new UnsupportedOperationException("Federated SSGs don't have their own keystores");
    }

    void haveClientCert( Boolean haveClientCert ) {
        throw new UnsupportedOperationException("Federated SSGs don't have their own keystores");
    }

    void privateKey( PrivateKey privateKey ) {
        throw new UnsupportedOperationException("Federated SSGs don't have their own private keys");
    }

    void passwordWorkedForPrivateKey( boolean worked ) {
        throw new UnsupportedOperationException("Federated SSGs don't have their own private keys");
    }

    public byte[] getPersistPassword() {
        throw new UnsupportedOperationException("Federated SSGs cannot get the trusted SSG's password");
    }

    public void setPersistPassword( byte[] persistPassword ) {
        throw new UnsupportedOperationException("Federated SSGs cannot set the trusted SSG's password");
    }

    public PasswordAuthentication getCredentials() {
        throw new UnsupportedOperationException("Federated SSGs cannot get the trusted SSG's password");
    }

    public synchronized void clientCert( X509Certificate cert ) {
        throw new UnsupportedOperationException("Federated SSGs cannot set the trusted SSG's client cert");
    }

    private Ssg trustedSsg;
}
