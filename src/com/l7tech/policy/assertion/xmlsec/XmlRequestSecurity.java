package com.l7tech.policy.assertion.xmlsec;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 2:08:45 PM
 * $Id$
 *
 * Enforces XML digital signature on the entire envelope of the response and maybe XML encryption on the body
 * element of the response.
 *
 * Whether XML encryption is used depends on the property encryption
 */
public class XmlRequestSecurity extends XmlSecurityAssertion {
}
