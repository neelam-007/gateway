package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * This assertion verifies that the soap request contained
 * an xml digital signature. The cert used for the signature is
 * remembered to identify the user. This cert can later
 * be used for comparaison in an identity assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class RequestWssX509Cert extends CredentialSourceAssertion {
}
