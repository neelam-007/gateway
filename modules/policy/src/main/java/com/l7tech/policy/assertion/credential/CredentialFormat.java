/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Typesafe enum. Not currently persistent.
 *
 * @author alex
 * @version $Revision$
 */
public class CredentialFormat {
    /**
     * The credentials are assumed to be a UTF-8 encoded String.
     */
    public static final CredentialFormat CLEARTEXT = new CredentialFormat( "Cleartext" );

    /**
     * The credentials are a base64 encoded (basic auth)
     */
    public static final CredentialFormat BASIC = new CredentialFormat( "Basic" );

    /**
     * The credentials are a hexadecimal-encoded H(A1) (i.e. MD5(user:pass[:realm])
     */
    public static final CredentialFormat DIGEST = new CredentialFormat( "Digest" );

    /**
     * The credentials are a client's certificate
     */
    public static final CredentialFormat CLIENTCERT = new CredentialFormat( "Client Certificate", true );

    /**
     * Credentials are a SAML token.  It might have a reference to an X.509 cert as well.
     */
    public static final CredentialFormat SAML = new CredentialFormat( "SAML Token");

    /**
     * The credentials are a kerberos ticket
     */
    public static final CredentialFormat KERBEROSTICKET = new CredentialFormat( "Kerberos Ticket");

    /**
     * An opaque token such as a single sign on session cookie.
     */
    public static final CredentialFormat OPAQUETOKEN = new CredentialFormat("Opaque Token");

    public static final Set<CredentialFormat> POLICY_MANAGER_ADMINISTRATION =
            Collections.unmodifiableSet(new HashSet<CredentialFormat>(Arrays.asList(CLEARTEXT, CLIENTCERT)));

    public static final Set<CredentialFormat> ESM_ADMINISTRATION =
            Collections.unmodifiableSet(new HashSet<CredentialFormat>(Arrays.asList(CLEARTEXT)));

    public String toString() {
        return "<CredentialFormat name='" + _name + "'/>";
    }

    public boolean isClientCert() {
        return _clientCert;
    }

    private CredentialFormat( String name ) {
        this(name, false);
    }

    private CredentialFormat( String name, boolean clientCert ) {
        _name = name;
        _clientCert = clientCert;
    }

    public String getName() {
        return _name;
    }

    private String _name;
    private boolean _clientCert;
}
