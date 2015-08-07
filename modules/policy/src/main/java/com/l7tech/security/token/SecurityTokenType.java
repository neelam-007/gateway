/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.security.token;

import com.l7tech.security.token.http.TlsClientCertToken;
import com.l7tech.util.SoapConstants;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.token.http.HttpDigestToken;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A typesafe enum that lists the different types of WS-Security Security Tokens we support
 */
public class SecurityTokenType implements Serializable {
    public static final String SAML_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String SAML2_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String SAML_ELEMENT = "Assertion";
    public static final String SECURECONVERSATIONTOKEN_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";

    /**
     * Get a SecurityTokenType by its num.
     *
     * @param num The num to get.
     * @return The SecurityTokenType or null if not found
     */
    public static SecurityTokenType getByNum(int num) {
        SecurityTokenType securityTokenType = null;

        if (num >=0 && num < VALUES.length) {
            securityTokenType = VALUES[num];
            assert num == securityTokenType.getNum() : "Token " +num+ " in wrong position in VALUES array";
        }

        return securityTokenType;
    }

    /**
     * Look up a security token type by its WS-Trust token type URI.  This is the URI that would be used with a wsse:TokenType element to
     * identify a type of security token.
     *
     * @param tokenTypeUri the token URI, eg "http://www.w3.org/2001/04/xmlenc#EncryptedKey".  Required.
     * @return the first SecurityTokenType with a matching token URI, or null if none was found.
     */
    public static SecurityTokenType getByWstTokenTypeUri(String tokenTypeUri) {
        for (SecurityTokenType value : VALUES) {
            if (tokenTypeUri.equals(value.getWstTokenTypeUri()))
                return value;
        }
        return null;
    }

    public static SecurityTokenType getByName(String name) {
        if(name == null)
            return null;
        for (SecurityTokenType value : VALUES) {
            if (name.equals(value.getName()))
                return value;
        }
        return null;
    }

    public int getNum() {
        return num;
    }

    public String getName() {
        return name;
    }

    public String getCategory(){
        return category;
    }

    public Class getInterfaceClass() {
        return interfaceClass;
    }

    public String getWstTokenTypeUri() {
        return wstTokenTypeUri;
    }

    public String getWstPrototypeElementNs() {
        return wstPrototypeElementNs;
    }

    public String getWstPrototypeElementName() {
        return wstPrototypeElementName;
    }

    private static int n = 0;

    // TODO look up proper token type URIs for RequestSecurityToken messages
    public static final SecurityTokenType SAML_ASSERTION =
            new SecurityTokenType(n++, "SAML Assertion", "SAML", SecurityTokenType.SAML_NS + "#Assertion", SAML_NS, SAML_ELEMENT, SamlSecurityToken.class);
    public static final SecurityTokenType WSSC_CONTEXT =
            new SecurityTokenType(n++, "WS-SC SecurityContextToken", "SymmetricKey", SECURECONVERSATIONTOKEN_URI, SoapConstants.WSSC_NAMESPACE, SoapConstants.SECURITY_CONTEXT_TOK_EL_NAME, SecurityContextToken.class);
    public static final SecurityTokenType WSSC_DERIVED_KEY =
            new SecurityTokenType(n++, "WS-SC DerivedKeyToken", "SymmetricKey", null, SoapConstants.WSSC_NAMESPACE, SoapConstants.WSSC_DK_EL_NAME, DerivedKeyToken.class);
    public static final SecurityTokenType WSS_USERNAME =
            new SecurityTokenType(n++, "WS-S UsernameToken", "Password", SoapConstants.SECURITY_NAMESPACE + "#UsernameToken", SoapConstants.SECURITY_NAMESPACE, "UsernameToken", UsernameToken.class);

    public static final SecurityTokenType WSS_X509_BST =
            new SecurityTokenType(n++, "WS-S X.509 BinarySecurityToken",
                    "X.509",
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3",
                    SoapConstants.SECURITY_NAMESPACE,
                    "BinarySecurityToken",
                    X509SigningSecurityToken.class);// TODO look up proper token type URI

    public static final SecurityTokenType WSS_ENCRYPTEDKEY =
            new SecurityTokenType(n++, "WS-S EncryptedKey", "SymmetricKey", SoapConstants.XMLENC_NS + "EncryptedKey", SoapConstants.XMLENC_NS,  "EncryptedKey", EncryptedKey.class);

    public static final SecurityTokenType WSS_KERBEROS_BST =
            new SecurityTokenType(n++, "Kerberos BinarySecurityToken",
                    "Kerberos",
                    SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ,
                    SoapConstants.SECURITY_NAMESPACE,
                    "BinarySecurityToken",
                    KerberosSigningSecurityToken.class);

    public static final SecurityTokenType HTTP_BASIC =
            new SecurityTokenType(n++, "HTTP Basic", "Password", null, null, null, HttpBasicToken.class);
    public static final SecurityTokenType HTTP_DIGEST =
            new SecurityTokenType(n++, "HTTP Digest", "Password", null, null, null, HttpDigestToken.class);
    public static final SecurityTokenType HTTP_CLIENT_CERT =
            new SecurityTokenType(n++, "HTTPS Client Cert", "X.509", null, null, null, TlsClientCertToken.class);

    public static final SecurityTokenType UNKNOWN =
            new SecurityTokenType(n++, "Unknown", "Unknown", null, null, null, SecurityToken.class);

    public static final SecurityTokenType SAML2_ASSERTION =
            new SecurityTokenType(n++, "SAML2 Assertion", "SAML", SecurityTokenType.SAML2_NS + "#Assertion", SAML2_NS, SAML_ELEMENT, SamlSecurityToken.class);

    public static final SecurityTokenType XPATH_CREDENTIALS =
            new SecurityTokenType(n++, "XPath Credentials", "Password", null, null, null, UsernameToken.class);

    public static final SecurityTokenType HTTP_KERBEROS =
            new SecurityTokenType(n++, "Windows Integrated", "Kerberos", null, null, null, KerberosSecurityToken.class);

    public static final SecurityTokenType FTP_CREDENTIAL =
            new SecurityTokenType(n++, "FTP Credentials", "Password", null, null, null, UsernameToken.class);

    public static final SecurityTokenType X509_ISSUER_SERIAL =
                new SecurityTokenType(n++, "X509 Issuer Serial", "X.509", null, null, null, X509SigningSecurityToken.class);

    public static final SecurityTokenType HTTP_NTLM =
            new SecurityTokenType(n++, "NTLM Credentials", "Password", null, null, null, NtlmToken.class);

    public static final SecurityTokenType SSH_CREDENTIAL =
            new SecurityTokenType(n++, "SSH Credentials", "SSH", null, null, null, SshSecurityToken.class);

    private static final SecurityTokenType KERBEROS =
            new SecurityTokenType(n++, "Kerberos Credential", "Kerberos", null, null, null, KerberosAuthenticationSecurityToken.class);
    /**
     * NOTE: Order MUST equal declaration order above (see readResolve/getByNum)
     *       DO NOT reorder, these numbers must be the same between releases.
     */
    private static final SecurityTokenType[] VALUES = {
        SAML_ASSERTION,
        WSSC_CONTEXT,
        WSSC_DERIVED_KEY,
        WSS_USERNAME,
        WSS_X509_BST,
        WSS_ENCRYPTEDKEY,
        WSS_KERBEROS_BST,
        HTTP_BASIC,
        HTTP_DIGEST,
        HTTP_CLIENT_CERT,
        UNKNOWN,
        SAML2_ASSERTION,
        XPATH_CREDENTIALS,
        HTTP_KERBEROS,
        FTP_CREDENTIAL,
        X509_ISSUER_SERIAL,
        HTTP_NTLM,
        SSH_CREDENTIAL,
        KERBEROS,
    };

    private SecurityTokenType(int num, String name, String category, String tokenTypeUri, String prototypeElementNs, String prototypeElementName, Class<? extends SecurityToken> interfaceClass) {
        this.num = num;
        this.category = category;
        this.name = name;
        this.wstTokenTypeUri = tokenTypeUri;
        this.wstPrototypeElementNs = prototypeElementNs;
        this.wstPrototypeElementName = prototypeElementName;
        this.interfaceClass = interfaceClass;
    }

    protected Object readResolve() throws ObjectStreamException {
        return VALUES[num];
    }

    @Override
    public String toString() {
        return getName();
    }

    private final int num;
    private final String name;
    private final String category;
    private final String wstTokenTypeUri;
    private final String wstPrototypeElementNs;
    private final String wstPrototypeElementName;
    private final Class interfaceClass;
}
