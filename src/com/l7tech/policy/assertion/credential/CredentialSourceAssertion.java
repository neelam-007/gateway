/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.policy.assertion.xmlsec.SecureConversation;

/**
 * Asserts that the requester's credentials were found, and using a particular authentication mechanism.
 *
 * @author alex
 * @version $Revision$
 */
public abstract class CredentialSourceAssertion extends Assertion {
    public static final CredentialSourceAssertion[] ALL_CREDENTIAL_ASSERTIONS_TYPES = new CredentialSourceAssertion[] {
        new SamlSecurity(),
        new RequestWssX509Cert(),
        new SecureConversation(),
        new HttpClientCert(),
        new HttpDigest(),
        new WssBasic(),
        new HttpBasic()
    };
}
