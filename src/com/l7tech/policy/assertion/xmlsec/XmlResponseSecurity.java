package com.l7tech.policy.assertion.xmlsec;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 27, 2003
 * Time: 2:08:56 PM
 * $Id$
 *
 * Enforces XML digital signature on the entire envelope of the request and maybe XML encryption on the body
 * element of the request.
 *
 * Whether XML encryption is used depends on the property encryption
 */
public class XmlResponseSecurity extends XmlSecurityAssertion {
    public static final String XML_ENC_HEADER_NAME = "ssg_xml_encryption_session_id";
}
