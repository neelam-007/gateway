package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.ConfidentialityAssertion;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 11:08:54 AM
 * $Id$
 *
 * Server side: validates that the body is encrypted, fails otherwise and decyphers it.
 * Proxy side: encrypts the body of the request.
 */
public class XmlEncReqAssertion extends ConfidentialityAssertion {
}
