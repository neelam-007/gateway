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
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ArrayUtils;
import com.l7tech.xml.saml.SamlAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

import java.security.MessageDigest;
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
 * TODO move this to a better package
 *
 * @author alex
 */
public class LoginCredentials implements SecurityToken {

    public static LoginCredentials makeCertificateCredentials(X509Certificate cert, Class credentialSource) {
        String login = CertUtils.extractFirstCommonNameFromCertificate(cert);

        return new LoginCredentials(login, null, CredentialFormat.CLIENTCERT, credentialSource, null, cert);
    }

    public static LoginCredentials makeSamlCredentials(SamlAssertion assertion, Class credentialSource) {
        String login;
        X509Certificate cert = assertion.getSubjectCertificate();
        if (cert != null) {
            login = CertUtils.extractFirstCommonNameFromCertificate(cert);
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
                            Class credentialSource, String realm, Object payload, SecurityTokenType type) {
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

    public LoginCredentials(String login, char[] credentials, CredentialFormat format,
                            Class credentialSource, String realm, Object payload) {
        this(login, credentials, format, credentialSource, realm, payload, null);
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
     */
    public String getRealm() {
        return realm;
    }

    public CredentialFormat getFormat() {
        return format;
    }

    /**
     * Could be null.
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
        if (cachedToString == null) {
            StringBuffer sb = new StringBuffer("<LoginCredentials format=\"");
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

    public byte[] getDigest() {
        if (cachedDigest == null) {
            try {
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                sha1.update(format.toString().getBytes("UTF-8"));
                sha1.update(login.getBytes("UTF-8"));
                sha1.update(new String(credentials).getBytes("UTF-8"));
                if (payload instanceof X509Certificate) {
                    X509Certificate cert = (X509Certificate)payload;
                    sha1.update(cert.getEncoded());
                } else if (payload instanceof SamlAssertion) {
                    SamlAssertion samlAssertion = (SamlAssertion)payload;
                    String s = XmlUtil.nodeToString(samlAssertion.asElement());
                    sha1.update(s.getBytes("UTF-8"));
                }
                cachedDigest = sha1.digest();
            } catch (Exception e) {
                throw new RuntimeException(e); // Can't happen
            }
        }
        return cachedDigest;
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

    private long authInstant;

    private transient final char[] credentials;
    private transient Class credentialSourceAssertion;

    private transient volatile byte[] cachedDigest;
    private transient volatile String cachedToString;
}
