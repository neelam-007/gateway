/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.security.token;

import com.l7tech.util.SoapConstants;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A typesafe enum that lists the different types of WS-Security Security Tokens we support
 */
public class SecurityTokenType implements Serializable {
    public static final String SAML_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String SAML2_NS = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String SAML_ELEMENT = "Assertion";
    public static final String SECURECONVESATIONTOKEN_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";

    /**
     * Get a SecurityTokenType by its num.
     *
     * @param num The num to get.
     * @return The SecurityTokenType or null if not found
     */
    public static SecurityTokenType getByNum(int num) {
        SecurityTokenType securityTokenType = null;

        if (num >=0 && num < VALUES.length)
            securityTokenType = VALUES[num];

        return securityTokenType;
    }

    public int getNum() {
        return num;
    }

    public String getName() {
        return name;
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
            new SecurityTokenType(n++, "SAML Assertion", SecurityTokenType.SAML_NS + "#Assertion", SAML_NS, SAML_ELEMENT, SamlSecurityToken.class);
    public static final SecurityTokenType WSSC_CONTEXT =
            new SecurityTokenType(n++, "WS-SC SecurityContextToken", SECURECONVESATIONTOKEN_URI, SoapConstants.WSSC_NAMESPACE, SoapConstants.SECURITY_CONTEXT_TOK_EL_NAME, SecurityContextToken.class);
    public static final SecurityTokenType WSSC_DERIVED_KEY =
            new SecurityTokenType(n++, "WS-SC DerivedKeyToken", null, SoapConstants.WSSC_NAMESPACE, SoapConstants.WSSC_DK_EL_NAME, DerivedKeyToken.class);
    public static final SecurityTokenType WSS_USERNAME =
            new SecurityTokenType(n++, "WS-S UsernameToken", SoapConstants.SECURITY_NAMESPACE + "#UsernameToken", SoapConstants.SECURITY_NAMESPACE, "UsernameToken", UsernameToken.class);

    public static final SecurityTokenType WSS_X509_BST =
            new SecurityTokenType(n++, "WS-S X.509 BinarySecurityToken",
                    "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3",
                    SoapConstants.SECURITY_NAMESPACE,
                    "BinarySecurityToken",
                    X509SecurityToken.class);// TODO look up proper token type URI

    public static final SecurityTokenType WSS_ENCRYPTEDKEY =
            new SecurityTokenType(n++, "WS-S EncryptedKey", SoapConstants.XMLENC_NS + "EncryptedKey", SoapConstants.XMLENC_NS,  "EncryptedKey", EncryptedKey.class);

    public static final SecurityTokenType WSS_KERBEROS_BST =
            new SecurityTokenType(n++, "Kerberos BinarySecurityToken",
                    SoapConstants.VALUETYPE_KERBEROS_GSS_AP_REQ,
                    SoapConstants.SECURITY_NAMESPACE,
                    "BinarySecurityToken",
                    KerberosSecurityToken.class);

    public static final SecurityTokenType HTTP_BASIC =
            new SecurityTokenType(n++, "HTTP Basic", null, null, null, UsernameToken.class);
    public static final SecurityTokenType HTTP_DIGEST =
            new SecurityTokenType(n++, "HTTP Digest", null, null, null, UsernameToken.class);
    public static final SecurityTokenType HTTP_CLIENT_CERT =
            new SecurityTokenType(n++, "HTTPS Client Cert", null, null, null, X509SecurityToken.class);

    public static final SecurityTokenType UNKNOWN =
            new SecurityTokenType(n++, "Unknown", null, null, null, SecurityToken.class);

    public static final SecurityTokenType SAML2_ASSERTION =
            new SecurityTokenType(n++, "SAML2 Assertion", SecurityTokenType.SAML2_NS + "#Assertion", SAML2_NS, SAML_ELEMENT, SamlSecurityToken.class);

    public static final SecurityTokenType XPATH_CREDENTIALS =
            new SecurityTokenType(n++, "XPath Credentials", null, null, null, UsernameToken.class);

    public static final SecurityTokenType HTTP_KERBEROS =
            new SecurityTokenType(n++, "Windows Integrated", null, null, null, KerberosSecurityToken.class);

    public static final SecurityTokenType FTP_CREDENTIAL =
            new SecurityTokenType(n++, "FTP Credentials", null, null, null, UsernameToken.class);

    public static final SecurityTokenType X509_ISSUER_SERIAL =
                new SecurityTokenType(n++, "X509 Issuer Serial", null, null, null, X509SecurityToken.class);

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
        X509_ISSUER_SERIAL
    };

    private SecurityTokenType(int num, String name, String tokenTypeUri, String prototypeElementNs, String prototypeElementName, Class<? extends SecurityToken> interfaceClass) {
        this.num = num;
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
    private final String wstTokenTypeUri;
    private final String wstPrototypeElementNs;
    private final String wstPrototypeElementName;
    private final Class interfaceClass;
}
