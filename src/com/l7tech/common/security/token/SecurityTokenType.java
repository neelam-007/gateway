/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import com.l7tech.common.util.SoapUtil;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A typesafe enum that lists the different types of WS-Security Security Tokens we support
 */
public class SecurityTokenType implements Serializable {
    public static final String SAML_NS = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String SAML_ELEMENT = "Assertion";
    public static final String SECURECONVESATIONTOKEN_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";

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
            new SecurityTokenType(n++, "SAML Assertion", "saml:Assertion", SAML_NS, SAML_ELEMENT, SamlSecurityToken.class);
    public static final SecurityTokenType WSSC_CONTEXT =
            new SecurityTokenType(n++, "WS-SC SecurityContextToken", SECURECONVESATIONTOKEN_URI, SoapUtil.WSSC_NAMESPACE, SoapUtil.SECURITY_CONTEXT_TOK_EL_NAME, SecurityContextToken.class);
    public static final SecurityTokenType WSSC_DERIVED_KEY =
            new SecurityTokenType(n++, "WS-SC DerivedKeyToken", null, SoapUtil.WSSC_NAMESPACE, SoapUtil.WSSC_DK_EL_NAME, DerivedKeyToken.class);
    public static final SecurityTokenType USERNAME =
            new SecurityTokenType(n++, "WS-S UsernameToken", null, SoapUtil.SECURITY_NAMESPACE, "UsernameToken", UsernameToken.class); // TODO look up proper token type URI
    public static final SecurityTokenType X509 =
            new SecurityTokenType(n++, "WS-S X.509 BinarySecurityToken", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3", SoapUtil.SECURITY_NAMESPACE, "BinarySecurityToken", X509SecurityToken.class);// TODO look up proper token type URI

    private static final SecurityTokenType[] VALUES = { SAML_ASSERTION, WSSC_CONTEXT, USERNAME, X509 };

    private SecurityTokenType(int num, String name, String tokenTypeUri, String prototypeElementNs, String prototypeElementName, Class interfaceClass) {
        this.num = num;
        this.name = name;
        this.wstTokenTypeUri = tokenTypeUri;
        this.wstPrototypeElementNs = prototypeElementNs;
        this.wstPrototypeElementName = prototypeElementName;
        this.interfaceClass = interfaceClass;
        if (!SecurityToken.class.isAssignableFrom(interfaceClass) || SecurityToken.class == interfaceClass)
            throw new IllegalArgumentException("interfaceClass must be derived from " + SecurityToken.class.getName());
    }

    protected Object readResolve() throws ObjectStreamException {
        return VALUES[num];
    }

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
