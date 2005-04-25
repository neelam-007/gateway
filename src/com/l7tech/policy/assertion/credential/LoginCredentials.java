/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.xml.saml.SamlAssertion;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

/**
 * Stores a reference to a User and its associated credentials (i.e. password).
 * <p/>
 * Immutable.
 * <p/>
 * Please don't ever make this class Serializable; we don't want to save anyone's password by accident.
 * <p/>
 * TODO move this to a better package
 *
 * @author alex
 */
public class LoginCredentials {
    public static LoginCredentials makeCertificateCredentials(X509Certificate cert, Class credentialSource) {
        String login = getCn(cert);

        return new LoginCredentials(login, null, CredentialFormat.CLIENTCERT, credentialSource, null, cert);
    }

    private static String getCn(X509Certificate cert) {
        Map dnMap = CertUtils.dnToAttributeMap(cert.getSubjectDN().getName());
        List cnValues = (List)dnMap.get("CN");
        String login = null;
        if (cnValues != null && cnValues.size() >= 1) {
            login = (String)cnValues.get(0);
        }
        return login;
    }

    public static LoginCredentials makeSamlCredentials(SamlAssertion assertion, Class credentialSource) {
        String login = null;
        X509Certificate cert = assertion.getSubjectCertificate();
        if (cert != null) {
            login = getCn(cert);
        } else {
            login = assertion.getNameIdentifierValue();
        }
        return new LoginCredentials(login, null, CredentialFormat.SAML, credentialSource, null, assertion);
    }

    public static LoginCredentials makePasswordCredentials(String login, char[] pass, Class credentialSource) {
        return new LoginCredentials(login, pass, CredentialFormat.CLEARTEXT, credentialSource, null);
    }

    public static LoginCredentials makeDigestCredentials(String login, char[] digest, String realm, Class credentialSource) {
        return new LoginCredentials(login, digest, CredentialFormat.DIGEST, credentialSource, realm, null);
    }

    public LoginCredentials(String login, char[] credentials, CredentialFormat format,
                            Class credentialSource, String realm, Object payload) {
        this.login = login;
        this.credentials = credentials;
        this.realm = realm;
        this.format = format;
        this.credentialSourceAssertion = credentialSource;
        this.payload = payload;
        if (format.isClientCert() && !(payload instanceof X509Certificate))
            throw new IllegalArgumentException("Must provide a certificate when creating client cert credentials");
    }

    public LoginCredentials(String login, char[] credentials, CredentialFormat format, Class credentialSource, String realm) {
        this(login, credentials, format, credentialSource, realm, null);
    }

    public LoginCredentials(String login, char[] credentials, CredentialFormat format, Class credentialSource) {
        this(login, credentials, format, credentialSource, null);
    }

    public LoginCredentials(String login, char[] credentials, Class credentialSource) {
        this(login, credentials, CredentialFormat.CLEARTEXT, credentialSource, null);
    }

    /** @return the login name, or null if there isn't one. */
    public String getLogin() {
        return login;
    }

    public char[] getCredentials() {
        return credentials;
    }

    /**
     * Could be null.
     *
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
     *
     * @return
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @return the Class of the {@link com.l7tech.policy.assertion.Assertion} that found this set of credentials.
     */
    public Class getCredentialSourceAssertion() {
        return credentialSourceAssertion;
    }

    /**
     * @param credentialSourceAssertion the Class of the {@link com.l7tech.policy.assertion.Assertion} that found this set of credentials.
     */
    public void setCredentialSourceAssertion(Class credentialSourceAssertion) {
        this.credentialSourceAssertion = credentialSourceAssertion;
    }

    public long getAuthInstant() {
        return authInstant;
    }

    public void onAuthentication() {
        this.authInstant = System.currentTimeMillis();
    }

    public String toString() {
        return getClass().getName() + "\n\t" +
          "format: " + format.toString() + "\n\t" +
          "login: " + login;
    }

    public X509Certificate getClientCert() {
        if (format == CredentialFormat.CLIENTCERT) {
            return (X509Certificate)payload;
        } else if (format == CredentialFormat.SAML) {
            SamlAssertion hok = (SamlAssertion)payload;
            return hok.getSubjectCertificate();
        }
        return null;
    }

    private final String login;
    private final char[] credentials;
    private final String realm;
    private Class credentialSourceAssertion;
    private CredentialFormat format;
    private Object payload;
    private long authInstant;
}
