/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import java.security.cert.X509Certificate;

/**
 * Stores a reference to a User and its associated credentials (i.e. password).
 *
 * Immutable.
 *
 * @author alex
 */
public class LoginCredentials {
    public LoginCredentials( String login, byte[] credentials, CredentialFormat format,
                             Class credentialSource, String realm, Object payload ) {
        this.login = login;
        this.credentials = credentials;
        this.realm = realm;
        this.format = format;
        this.credentialSourceAssertion = credentialSource;
        this.payload = payload;
        if (format.isClientCert() && !(payload instanceof X509Certificate))
            throw new IllegalArgumentException("Must provide a certificate when creating client cert credentials");
    }

    public LoginCredentials( String login, byte[] credentials, CredentialFormat format, Class credentialSource, String realm ) {
        this( login, credentials, format, credentialSource, realm, null );
    }

    public LoginCredentials( String login, byte[] credentials, CredentialFormat format, Class credentialSource ) {
        this( login, credentials, format, credentialSource, null );
    }

    public LoginCredentials( String login, byte[] credentials, Class credentialSource ) {
        this( login, credentials, CredentialFormat.CLEARTEXT, credentialSource, null );
    }

    public String getLogin() {
        return login;
    }

    public byte[] getCredentials() {
        return credentials;
    }

    /**
     * Could be null.
     * @return
     */
    public String getRealm() {
        return realm;
    }

    public CredentialFormat getFormat() {
        return format;
    }

    /**
     * Could be null.
     * @return
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @return the Class of the {@link CredentialSourceAssertion} that found this set of credentials.
     */
    public Class getCredentialSourceAssertion() {
        return credentialSourceAssertion;
    }

    /**
     * @param credentialSourceAssertion the Class of the {@link CredentialSourceAssertion} that found this set of credentials. Must be derived from {@link CredentialSourceAssertion}.
     * @throws ClassCastException if the specified class is not derived from {@link CredentialSourceAssertion}.
     */
    public void setCredentialSourceAssertion( Class credentialSourceAssertion ) throws ClassCastException {
        if (credentialSourceAssertion == null ||
            CredentialSourceAssertion.class.isAssignableFrom(credentialSourceAssertion))
            this.credentialSourceAssertion = credentialSourceAssertion;
        else throw new ClassCastException(credentialSourceAssertion.getName() +
                                         " is not derived from " +
                                         CredentialSourceAssertion.class.getName() );
    }

    public String toString() {
        return getClass().getName() + "\n\t" +
                "format: " + format.toString() + "\n\t" +
                "login: " + login;
    }

    private final String login;
    private final byte[] credentials;
    private final String realm;
    private Class credentialSourceAssertion;
    private CredentialFormat format;
    private Object payload;
}
