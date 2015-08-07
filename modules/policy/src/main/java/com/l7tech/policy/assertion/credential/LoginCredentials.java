/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.xmlsec.SecureConversation;
import com.l7tech.security.token.*;
import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.security.token.http.HttpDigestToken;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.Disposable;
import com.l7tech.util.ResourceUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.assertion.Assertion;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
public final class LoginCredentials implements Disposable {

    /**
     * Create a LoginCredentials for the given SecurityToken.
     *
     * @param securityToken The token for the credentials.
     * @param credentialSource The source assertion
     * @return The LoginCredentials or null.
     */
    public static LoginCredentials makeLoginCredentials( final SecurityToken securityToken,
                                                         final Class<? extends Assertion> credentialSource ) {
        return makeLoginCredentials(securityToken, true, credentialSource);
    }

    /**
     * Create a LoginCredentials for the given SecurityToken.
     *
     * @param securityToken The token for the credentials.
     * @param credentialSource The source assertion
     * @param supportingSecurityTokens The supporting security tokens (if any)
     * @return The LoginCredentials or null.
     */
    public static LoginCredentials makeLoginCredentials( final SecurityToken securityToken,
                                                         final Class<? extends Assertion> credentialSource,
                                                         final SecurityToken... supportingSecurityTokens ) {
        return makeLoginCredentials(securityToken, true, credentialSource, supportingSecurityTokens);
    }

    /**
     * Create a LoginCredentials for the given SecurityToken.
     *
     * @param securityToken The token for the credentials.
     * @param isTokenPresent Is the token from the the message/transport.
     * @param credentialSource The source assertion
     * @param supportingSecurityTokens The supporting security tokens (if any)
     * @return The LoginCredentials or null.
     */
    public static LoginCredentials makeLoginCredentials( final SecurityToken securityToken,
                                                         final boolean isTokenPresent,
                                                         final Class<? extends Assertion> credentialSource,
                                                         final SecurityToken... supportingSecurityTokens ) {
        LoginCredentials loginCredentials;

        if ( securityToken instanceof HasUsernameAndPassword ) {
            HasUsernameAndPassword huap = (HasUsernameAndPassword) securityToken;
            char[] password = huap.getPassword();
            if (password == null) password = new char[0];
            loginCredentials = LoginCredentials.makePasswordCredentials(
                    huap.getUsername(),
                    password,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource );
        } else if ( securityToken instanceof TlsClientCertToken) {
            TlsClientCertToken sxst = (TlsClientCertToken) securityToken;
            loginCredentials = new LoginCredentials(
                    sxst.getCertCn(),
                    null,
                    CredentialFormat.CLIENTCERT,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    null,
                    sxst.getCertificate() );
        } else if ( securityToken instanceof SamlAssertion ) {
            SamlAssertion samlAssertion = (SamlAssertion) securityToken;
            loginCredentials = makeSamlCredentials(
                    samlAssertion,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource );
        } else if ( securityToken instanceof X509SigningSecurityToken ) {
            X509SigningSecurityToken xsst = (X509SigningSecurityToken) securityToken;
            if ( !xsst.isPossessionProved() ) throw new IllegalArgumentException("Credential with unproved certificate!");
            String certCN = CertUtils.extractFirstCommonNameFromCertificate(xsst.getMessageSigningCertificate());
            loginCredentials = new LoginCredentials(
                    certCN,
                    null,
                    CredentialFormat.CLIENTCERT,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    null,
                    xsst.getMessageSigningCertificate() );
        } else if ( securityToken instanceof OpaqueSecurityToken ) {
            OpaqueSecurityToken ost = (OpaqueSecurityToken) securityToken;
            loginCredentials = new LoginCredentials(
                    ost.getUsername(),
                    ost.getCredential(),
                    CredentialFormat.OPAQUETOKEN,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource, null, null );
        } else if ( securityToken instanceof HttpDigestToken ) {
            HttpDigestToken hdt = (HttpDigestToken) securityToken;
            loginCredentials = new LoginCredentials(
                    hdt.getUsername(),
                    hdt.getHa1Hex().toCharArray(),
                    CredentialFormat.DIGEST,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    hdt.getRealm(),
                    hdt.getParams() );
          } else if ( securityToken instanceof NtlmToken ) {
            NtlmToken ntlmt = (NtlmToken) securityToken;
            loginCredentials = new LoginCredentials(
                    null,
                    ntlmt.getNtlmData().toCharArray(),
                    CredentialFormat.NTLMTOKEN,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    ntlmt.getRealm(),
                    ntlmt.getParams() );
         } else if ( securityToken instanceof KerberosSecurityToken ) {
            KerberosSecurityToken kst = (KerberosSecurityToken) securityToken;
            loginCredentials = new LoginCredentials(
                    null,
                    null,
                    CredentialFormat.KERBEROSTICKET,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    null,
                    kst.getServiceTicket() );
        } else if ( securityToken instanceof SessionSecurityToken ) {
            SessionSecurityToken ist = (SessionSecurityToken) securityToken;
            loginCredentials = new LoginCredentials(
                    ist.getLogin(),
                    null,
                    CredentialFormat.SESSIONTOKEN,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    null,
                    null );
        } else if (securityToken instanceof SshSecurityToken) {
            SshSecurityToken sshSecurityToken = (SshSecurityToken) securityToken;
            loginCredentials = new LoginCredentials(
                    sshSecurityToken.getUsername(),
                    null,
                    CredentialFormat.SSHTOKEN,
                    securityToken,
                    isTokenPresent,
                    supportingSecurityTokens,
                    credentialSource,
                    null,
                    null );
        } else {
            throw new IllegalArgumentException("Unsupported security token '"+securityToken.getClass()+"' of type '"+securityToken.getType()+"'");
        }

        return loginCredentials;    
    }

    /**
     * Build LoginCredentials for the given SAML assertion and source.
     *
     * <p>The login for the credentials will be the certificates common name if
     * one is available, else the SAML assertions name identifier value.</p>
     *
     * @param samlAssertion The SAML assertion (must not be null)
     * @param credentialSource The source credential assertion (must not be null)
     * @return The new LoginCredentials
     */
    private static LoginCredentials makeSamlCredentials( final SamlAssertion samlAssertion,
                                                         final boolean isTokenPresent,
                                                         final SecurityToken[] supportingSecurityTokens,
                                                         final Class<? extends Assertion> credentialSource ) {
        String login;
        X509Certificate cert = samlAssertion.getSubjectCertificate();
        if (cert != null) {
            login = CertUtils.extractFirstCommonNameFromCertificate(cert);
        } else if (SamlConstants.NAMEIDENTIFIER_X509_SUBJECT.equals(samlAssertion.getNameIdentifierFormat())) {
            login = CertUtils.extractFirstCommonNameFromDN(samlAssertion.getNameIdentifierValue());
        } else {
            login = samlAssertion.getNameIdentifierValue();
        }

        return new LoginCredentials(login, null, CredentialFormat.SAML, samlAssertion, isTokenPresent, supportingSecurityTokens, credentialSource, null, samlAssertion);
    }

    /**
     * Build LoginCredentials for the given username/password and source.
     *
     * @param login The users login
     * @param credentials The users password
     * @param credentialSource The source credential assertion (must not be null)
     * @return The new LoginCredentials
     */
    private static LoginCredentials makePasswordCredentials( final String login,
                                                             final char[] credentials,
                                                             final SecurityToken securityToken,
                                                             final boolean isTokenPresent,
                                                             final SecurityToken[] supportingSecurityTokens,
                                                             final Class<? extends Assertion> credentialSource ) {
        return new LoginCredentials(login, credentials, CredentialFormat.CLEARTEXT, securityToken, isTokenPresent, supportingSecurityTokens, credentialSource, null, null);
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
    private LoginCredentials( final String login,
                              final char[] credentials,
                              final CredentialFormat format,
                              final SecurityToken securityToken,
                              final boolean isTokenPresent,
                              final SecurityToken[] supportingSecurityTokens,
                              final Class<? extends Assertion> credentialSource,
                              final String realm,
                              final Object payload ) {
        this.login = login;
        this.credentials = credentials;
        this.realm = realm;
        this.format = format;
        this.securityToken = securityToken;
        this.securityTokenIsPresent = isTokenPresent;
        this.supportingSecurityTokens = supportingSecurityTokens;
        this.credentialSourceAssertion = credentialSource;
        this.payload = payload;

        if (format.isClientCert() && !(payload instanceof X509Certificate))
            throw new IllegalArgumentException("Must provide a certificate when creating client cert credentials");
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
     * Get the security token use to generate these credentials.
     *
     * @return The security token
     */
    public SecurityToken getSecurityToken() {
        return securityToken;
    }

    /**
     * Get all the security tokens used to generate these credentials.
     *
     * @return The security tokens
     */
    public SecurityToken[] getSecurityTokens() {
        SecurityToken[] tokens;

        if ( supportingSecurityTokens != null ) {
            tokens = new SecurityToken[supportingSecurityTokens.length+1];
            tokens[0] = securityToken;
            System.arraycopy( supportingSecurityTokens, 0, tokens, 1, supportingSecurityTokens.length );
        } else {
            tokens = new SecurityToken[]{ securityToken };
        }

        return tokens;
    }

    /**
     * Is the given security token present.
     *
     * @param securityToken The security token to check
     * @return true if present, false if not presented
     */
    public boolean isSecurityTokenPresent( final SecurityToken securityToken ) {
        boolean requestToken = true;

        if ( securityToken == this.securityToken ) {
            requestToken = securityTokenIsPresent;
        }

        return requestToken;
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
    public void dispose() {
        Collection<Object> toDispose = new ArrayList<Object>();
        toDispose.add( payload );
        toDispose.add( securityToken );
        if ( supportingSecurityTokens != null ) {
            toDispose.addAll( Arrays.asList( supportingSecurityTokens ) );
        }
        ResourceUtils.dispose( toDispose );
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

    /**
     * This method SecurityTokenType for this credential
     *
     * @return a feasible token type for the credential.
     */
    public SecurityTokenType getType() {
        // Check for specific type based on source assertion
        SecurityTokenType securityTokenType =
                    (SecurityTokenType) CREDENTIAL_SOURCE_TO_TOKEN_TYPE.get(credentialSourceAssertion);

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
            } else if (format == CredentialFormat.SSHTOKEN) {
                securityTokenType = SecurityTokenType.SSH_CREDENTIAL;
            } else if (format == CredentialFormat.NTLMTOKEN) {
                securityTokenType = SecurityTokenType.HTTP_NTLM;
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
            {RequireWssX509Cert.class, SecurityTokenType.WSS_X509_BST},
            {WssBasic.class, SecurityTokenType.WSS_USERNAME},
            {XpathCredentialSource.class, SecurityTokenType.XPATH_CREDENTIALS},
            {HttpNegotiate.class, SecurityTokenType.HTTP_KERBEROS},
            {SecureConversation.class, SecurityTokenType.WSSC_CONTEXT},
    }, true);

    private final String login;
    private final String realm;
    private final CredentialFormat format;
    private final Object payload;

    private transient final SecurityToken securityToken; // Not part of equality since that would break auth caching
    private transient final boolean securityTokenIsPresent;
    private transient final SecurityToken[] supportingSecurityTokens; // Not part of equality since that would break auth caching
    private transient final char[] credentials;
    private transient final Class<? extends Assertion> credentialSourceAssertion;

    private volatile String cachedToString;
}
