package com.l7tech.proxy.policy.assertion.xmlsec;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy)
 *
 * On the server side, this decorates a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope.
 *
 * @author flascell
 */
public class ClientXmlDsigResAssertion {
}
