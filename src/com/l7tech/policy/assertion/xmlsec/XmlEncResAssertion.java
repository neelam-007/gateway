package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.ConfidentialityAssertion;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 11:09:10 AM
 * $Id$
 *
 * Server side: encrypts the response's body element.
 * Proxy side: validates that the response was encrypted and decypher it.
 */
public class XmlEncResAssertion extends ConfidentialityAssertion {
}
