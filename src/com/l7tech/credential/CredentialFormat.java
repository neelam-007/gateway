/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

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
    public static final CredentialFormat CLEARTEXT  = new CredentialFormat( "Cleartext" );

    /**
     * The credentials are a hexadecimal-encoded H(A1) (i.e. MD5(user:pass[:realm])
     */
    public static final CredentialFormat DIGEST     = new CredentialFormat( "Digest" );

    /**
     * The credentials are a client's certificate (TODO: encoded how?)
     */
    public static final CredentialFormat CLIENTCERT = new CredentialFormat( "Client Certificate" );

    public String toString() {
        return "<CredentialFormat name='" + _name + "'/>";
    }

    protected CredentialFormat( String name ) {
        _name = name;
    }

    protected String _name;
}
