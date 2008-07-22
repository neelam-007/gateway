/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.identity.mapping;

import com.l7tech.security.saml.SamlConstants;

import java.io.Serializable;

/**
 * Typesafe enum for name formats.
 *
 * The values are a very slight superset of the formats known from SAML 1.1 and 2.0.
 *
 * Notes on SAML URIs:
 * <ul>
 * <li>A null URI in the {@link #saml11Uri} field will be reported as "unspecified"
 * (see {@link SamlConstants#NAMEIDENTIFIER_UNSPECIFIED}).
 * <li>A null URI in the {@link #saml20Uri} field will result in falling back to the {@link #getSaml11Uri()} value.
 * </ul>
 */
public final class NameFormat implements Serializable {
    private static int n = 0;

    public static final NameFormat LOGIN = new NameFormat(n++, "Login", null, null);

    public static final NameFormat EMAIL = new NameFormat(n++, "Email Address",
            SamlConstants.NAMEIDENTIFIER_EMAIL, null);

    public static final NameFormat KERBEROS = new NameFormat(n++, "Kerberos",
            null, "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos");

    public static final NameFormat X500_DN = new NameFormat(n++, "X.500 Distinguished Name",
            SamlConstants.NAMEIDENTIFIER_X509_SUBJECT, null);

    public static final NameFormat WINDOWS = new NameFormat(n++, "Windows Domain",
            SamlConstants.NAMEIDENTIFIER_WINDOWS, null);

    public static final NameFormat SAML2_ENTITY_ID = new NameFormat(n++, "SAML Entity Identitifer",
            null, "urn:oasis:names:tc:SAML:2.0:nameid-format:entity");

    public static final NameFormat SAML2_PERSISTENT_ID = new NameFormat(n++, "SAML Persistent Identifier",
            null, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");

    public static final NameFormat SAML2_TRANSIENT_ID = new NameFormat(n++, "SAML Transient Identifier",
            null, "urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

    public static final NameFormat OTHER = new NameFormat(n++, "Other",
            null, null);

    private final int num;
    private final String name;

    /** Null is equivalent to {@link SamlConstants#NAMEIDENTIFIER_UNSPECIFIED} */
    private final String saml11Uri;

    /** Null indicates unchanged from SAML 1.1 (use {@link #saml11Uri} */
    private final String saml20Uri;

    private static final NameFormat[] VALUES = {
        LOGIN,
        EMAIL,
        KERBEROS,
        X500_DN,
        WINDOWS,
        SAML2_ENTITY_ID,
        SAML2_PERSISTENT_ID,
        SAML2_TRANSIENT_ID,
        OTHER
    };

    protected Object readResolve() {
        return VALUES[num];
    }

    public static NameFormat fromInt(int i) {
        return VALUES[i];
    }

    public int toInt() {
        return num;
    }

    private NameFormat(int num, String name, String saml11Uri, String saml20Uri) {
        this.num = num;
        this.name = name;
        this.saml11Uri = saml11Uri;
        this.saml20Uri = saml20Uri;
    }

    public String getName() {
        return name;
    }

    /**
     * @return the SAML 1.1 NameIdentitifer Format URI
     */
    public String getSaml11Uri() {
        if (saml11Uri != null) return saml11Uri;

        return SamlConstants.NAMEIDENTIFIER_UNSPECIFIED;
    }

    /**
     * @return the SAML 2.0 NameID Format URI
     */
    public String getSaml20Uri() {
        if (saml20Uri != null) return saml20Uri;
        return getSaml11Uri();
    }
}
