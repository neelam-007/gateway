/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

/**
 * Typesafe enum.
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
    public static final CredentialFormat CLIENTCERT = new CredentialFormat( "Client Certificate" );
    public static final CredentialFormat CLIENTCERT_X509_ASN1_DER = new CredentialFormat( "ASN.1 DER-encoded X.509 Client Certificate" );

    public String toString() {
        return "<CredentialFormat name='" + _name + "'/>";
    }

    protected CredentialFormat( String name ) {
        _name = name;
    }

    protected String _name;
}
