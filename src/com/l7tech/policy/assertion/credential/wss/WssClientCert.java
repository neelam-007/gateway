/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

/**
 * This assertion verifies that the soap envelope of a request is digitally signed by a user.
 * The signature is validated and the X509 cert is extracted in and wrapped in a PrincipalCredentials
 * object.
 *
 * @author alex, flascell
 * @version $Revision$
 *
 * todo remove this class
 */
public class WssClientCert extends WssCredentialSourceAssertion {
}
