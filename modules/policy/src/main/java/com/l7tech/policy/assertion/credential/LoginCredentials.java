/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ArrayUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.Assertion;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;

/**
 * Stores a reference to a User and its associated credentials (i.e. password).
 * <p/>
 * Immutable.
 * <p/>
 * Please don't ever make this class Serializable; we don't want to save anyone's password by accident.
 * <p/>
 *
 * @author alex
 */
public class LoginCredentials implements SecurityToken {

    /**
     * Build LoginCredentials for the given certificate and source
     *
     * <p>The login for the credentials will be the certificates common name.</p>
     *
     * @param cert The client certificate (proven, must not be null)
     * @param credentialSource The source credential assertion (must not be null)
     * @return The new LoginCredentials
     */
    public static LoginCredentials makeCertificateCredentials( final X509Certificate cert,
                                                               final Class<? extends Assertion> credentialSource ) {
        String login = CertUtils.extractFirstCommonNameFromCertificate(cert);

        return new LoginCredentials(login, null, CredentialFormat.CLIENTCERT, credentialSource, null, cert);
    }

    /**
     * Build LoginCredentials for the given SAML assertion and source.
     *
     * <p>The login for the credentials will be the certificates common name if
     * one is available, else the SAML assertions name identifier value.</p>
     *
     * @param assertion The SAML assertion (must not be null)
     * @param credentialSource The source credential assertion (must not be null)
     * @return The new LoginCredentials
     */
    public static LoginCredentials makeSamlCredentials( final SamlAssertion assertion,
                                                        final Class<? extends Assertion> credentialSource ) {
        String login;
        X509Certificate cert = assertion.getSubjectCertificate();
        if (cert != null) {
            login = CertUtils.extractFirstCommonNameFromCertificate(cert);
        } else {
            login = assertion.getNameIdentifierValue();
        }
        return new LoginCredentials(login, null, CredentialFormat.SAML, credentialSource, null, assertion);
    }

    /**
     * Build LoginCredentials for the given username/password and source.
     *
     * @param login The users login
     * @param credentials The users password
     * @param credentialSource The source credential assertion (must not be null)
     * @return The new LoginCredentials
     */
    public static LoginCredentials makePasswordCredentials( final String login,
                                                            final char[] credentials,
                                                            final Class<? extends Assertion> credentialSource ) {
        return new LoginCredentials(login, credentials, CredentialFormat.CLEARTEXT, credentialSource, null);
    }

    /**
     * Build LoginCredentials for the given username/password digest and source.
     *
     * @param login The users login
     * @param credentials The users password digest
     * @param realm The users authentication realm
     * @param credentialSource The source credential assertion (must not be null)
     * @return The new LoginCredentials
     */
    public static LoginCredentials makeDigestCredentials( final String login,
                                                          final char[] credentials,
                                                          final String realm,
                                                          final Class<? extends Assertion> credentialSource ) {
        return new LoginCredentials(login, credentials, CredentialFormat.DIGEST, credentialSource, realm, null);
    }

    /**
     * Construct a new LoginCredentials.
     *
     * @param login The users login
     * @param credentials The users credentials
     * @param format The credential format.
     * @param credentialSource The source credential assertion (must not be null)
     * @param realm The users authentication realm
     * @param payload The credential payload
     * @param type The type of the security token for these credentials
     */
    public LoginCredentials( final String login,
                             final char[] credentials,
                             final CredentialFormat format,
                             final Class<? extends Assertion> credentialSource,
                             final String realm,
                             final Object payload,
                             final SecurityTokenType type ) {
        this.login = login;
        this.credentials = credentials;
        this.realm = realm;
        this.format = format;
        this.credentialSourceAssertion = credentialSource;
        this.payload = payload;
        this.type = type;

        if (format.isClientCert() && !(payload instanceof X509Certificate))
            throw new IllegalArgumentException("Must provide a certificate when creating client cert credentials");
    }

    /**
     * Construct a new LoginCredentials.
     *
     * @param login The users login
     * @param credentials The users credentials
     * @param format The credential format.
     * @param credentialSource The source credential assertion (must not be null)
     * @param realm The users authentication realm
     * @param payload The credential payload
     */
    public LoginCredentials( final String login,
                             final char[] credentials,
                             final CredentialFormat format,
                             final Class<? extends Assertion> credentialSource,
                             final String realm,
                             final Object payload) {
        this(login, credentials, format, credentialSource, realm, payload, null);
    }

    /**
     * Construct a new LoginCredentials.
     *
     * @param login The users login
     * @param credentials The users credentials
     * @param format The credential format.
     * @param credentialSource The source credential assertion (must not be null)
     * @param realm The users authentication realm
     */
    public LoginCredentials( final String login,
                             final char[] credentials,
                             final CredentialFormat format,
                             final Class<? extends Assertion> credentialSource,
                             final String realm ) {
        this(login, credentials, format, credentialSource, realm, null);
    }

    /**
     * Construct a new LoginCredentials.
     *
     * @param login The users login
     * @param credentials The users credentials
     * @param format The credential format.
     * @param credentialSource The source credential assertion (must not be null)
     */
    public LoginCredentials( final String login,
                             final char[] credentials,
                             final CredentialFormat format,
                             final Class<? extends Assertion> credentialSource ) {
        this(login, credentials, format, credentialSource, null);
    }

    /**
     * Construct a new LoginCredentials.
     *
     * @param login The users login
     * @param credentials The users password
     * @param credentialSource The source credential assertion (must not be null)
     */
    public LoginCredentials( final String login,
                             final char[] credentials,
                             final Class<? extends Assertion> credentialSource ) {
        this(login, credentials, CredentialFormat.CLEARTEXT, credentialSource, null);
    }

    /** @return the login name, or null if there isn't one. */
    public String getLogin() {
        return login;
    }

    /**
     * Get the credentials.
     *
     * @return The credentials or null if there are none for this type.
     * @see #getFormat()
     */
    public char[] getCredentials() {
        return credentials;
    }

    /**
     * Get the realm for these credentials (may be null)
     *
     * @return The realm or null.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Get the format for these credentials.
     *
     * @return The credential format.
     */
    public CredentialFormat getFormat() {
        return format;
    }

    /**
     * Get the payload for these credentials.
     *
     * @return the credential payload or null.
     */
    public Object getPayload() {
        return payload;
    }

    /**
     * @return the Class of the {@link com.l7tech.policy.assertion.Assertion} that found this set of credentials.
     */
    public Class<? extends Assertion> getCredentialSourceAssertion() {
        return credentialSourceAssertion;
    }

    @Override
    public String toString() {
        if (cachedToString == null) {
            StringBuilder sb = new StringBuilder("<LoginCredentials format=\"");
            sb.append(format.getName());
            sb.append("\" ");

            if (login != null) {
                sb.append("login=\"");
                sb.append(login);
            } else if (payload instanceof X509Certificate) {
                sb.append("subjectDn=\"");
                sb.append(((X509Certificate)payload).getSubjectDN().getName());
            } else if (payload instanceof SamlAssertion) {
                SamlAssertion samlAssertion = (SamlAssertion)payload;
                sb.append("nameIdentifier=\"");
                sb.append(samlAssertion.getNameIdentifierValue());
            } else if (payload instanceof KerberosServiceTicket) {
                KerberosServiceTicket kerberosServiceTicket = (KerberosServiceTicket)payload;
                sb.append("kerberosPrincipal=\"");
                sb.append(kerberosServiceTicket.getClientPrincipalName());
            }
            sb.append("\"/>");
            cachedToString = sb.toString();
        }
        return cachedToString;
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

    @Override
    public SecurityTokenType getType() {
        // Use given type if available
        SecurityTokenType securityTokenType = type;

        // Check for specific type based on source assertion
        if (securityTokenType == null) {
            securityTokenType =
                    (SecurityTokenType) CREDENTIAL_SOURCE_TO_TOKEN_TYPE.get(credentialSourceAssertion);
        }

        // Else use default type for format
        if (securityTokenType == null) {
            if (format == CredentialFormat.CLIENTCERT) {
                securityTokenType = SecurityTokenType.HTTP_CLIENT_CERT;
            } else if (format == CredentialFormat.SAML) {
                securityTokenType = SecurityTokenType.SAML_ASSERTION;
            } else if (format == CredentialFormat.DIGEST) {
                securityTokenType = SecurityTokenType.HTTP_DIGEST;
            } else if (format == CredentialFormat.CLEARTEXT) {
                securityTokenType = SecurityTokenType.HTTP_BASIC;
            } else if (format == CredentialFormat.KERBEROSTICKET) {
                securityTokenType = SecurityTokenType.WSS_KERBEROS_BST;
            } else {
                securityTokenType = SecurityTokenType.UNKNOWN;
            }
        }

        return securityTokenType;
    }

    @SuppressWarnings({"RedundantIfStatement"})
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final LoginCredentials that = (LoginCredentials)o;

        if (credentialSourceAssertion != null ? !credentialSourceAssertion.equals(that.credentialSourceAssertion) : that.credentialSourceAssertion != null)
            return false;
        if (!Arrays.equals(credentials, that.credentials)) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;
        if (login != null ? !login.equals(that.login) : that.login != null) return false;
        if (payload != null ? !payload.equals(that.payload) : that.payload != null) return false;
        if (realm != null ? !realm.equals(that.realm) : that.realm != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = (login != null ? login.hashCode() : 0);
        result = 31 * result + (credentials != null ? Arrays.hashCode(credentials) : 0);
        result = 31 * result + (realm != null ? realm.hashCode() : 0);
        result = 31 * result + (credentialSourceAssertion != null ? credentialSourceAssertion.hashCode() : 0);
        result = 31 * result + (format != null ? format.hashCode() : 0);
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        return result;
    }

    /**
     * Returns the best username available given the {@link #format}.
     */
    public String getName() {
        if (format == CredentialFormat.CLEARTEXT || format == CredentialFormat.DIGEST || format == CredentialFormat.BASIC) {
            return getLogin();
        } else if (format == CredentialFormat.CLIENTCERT) {
            X509Certificate cert = (X509Certificate)payload;
            return cert.getSubjectDN().getName();
        } else if (format == CredentialFormat.SAML) {
            SamlAssertion ass = (SamlAssertion)payload;
            return ass.getNameIdentifierValue();
        } else if (format == CredentialFormat.KERBEROSTICKET) {
            KerberosServiceTicket tick = (KerberosServiceTicket) payload;
            return tick.getClientPrincipalName();
        } else {
            return null;
        }
    }

    // Mappings from assertions to token types
    private static final Map CREDENTIAL_SOURCE_TO_TOKEN_TYPE = ArrayUtils.asMap(new Object[][]{
            {RequestWssX509Cert.class, SecurityTokenType.WSS_X509_BST},
            {WssBasic.class, SecurityTokenType.WSS_USERNAME},
            {XpathCredentialSource.class, SecurityTokenType.XPATH_CREDENTIALS},
            {HttpNegotiate.class, SecurityTokenType.HTTP_KERBEROS},
    }, true);

    private final String login;
    private final String realm;
    private final CredentialFormat format;
    private final Object payload;
    private final SecurityTokenType type;

    private transient final char[] credentials;
    private transient final Class<? extends Assertion> credentialSourceAssertion;

    private transient volatile String cachedToString;
}
