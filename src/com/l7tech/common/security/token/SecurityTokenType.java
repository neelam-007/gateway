/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A typesafe enum that lists the different types of WS-Security Security Tokens we support
 */
public class SecurityTokenType implements Serializable {
    public int getNum() {
        return num;
    }

    public String getName() {
        return name;
    }

    public Class getInterfaceClass() {
        return interfaceClass;
    }

    private static int n = 0;
    public static final SecurityTokenType SAML = new SecurityTokenType(n++, "SAML Assertion", SamlSecurityToken.class);
    public static final SecurityTokenType WSSC = new SecurityTokenType(n++, "WS-SC SecurityContextToken", SecurityContextToken.class);
    public static final SecurityTokenType USERNAME = new SecurityTokenType(n++, "WS-S UsernameToken", UsernameToken.class);
    public static final SecurityTokenType X509 = new SecurityTokenType(n++, "WS-S X.509 BinarySecurityToken", X509SecurityToken.class);

    private static final SecurityTokenType[] VALUES = { SAML, WSSC, USERNAME, X509 };

    private SecurityTokenType(int num, String name, Class interfaceClass) {
        this.num = num;
        this.name = name;
        this.interfaceClass = interfaceClass;
        if (!SecurityToken.class.isAssignableFrom(interfaceClass) || SecurityToken.class == interfaceClass)
            throw new IllegalArgumentException("interfaceClass must be derived from " + SecurityToken.class.getName());
    }

    protected Object readResolve() throws ObjectStreamException {
        return VALUES[num];
    }

    private final int num;
    private final String name;
    private final Class interfaceClass;
}
