package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap request sent from a requestor (probably proxy) to the ssg server
 *
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope.
 * On the proxy side, this must decorate a request with an xml d-sig
 *
 * This extends CredentialSourceAssertion because once the validity of the signature if confirmed, the cert is used
 * as credentials.
 *
 * @author flascell
 */
public class XmlDsigReqAssertion extends CredentialSourceAssertion {
}
