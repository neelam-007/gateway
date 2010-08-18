package com.l7tech.external.assertions.saml2attributequery.server;

import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 21-Jan-2009
 * Time: 7:04:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class AssertionMessages extends com.l7tech.gateway.common.audit.AssertionMessages {
    public static final M REQUEST_DIGSIG_NO_SIG = m(4807, Level.WARNING, "No signature found for element.");
    public static final M REQUEST_DIGSIG_VAR_UNUSABLE = m(4808, Level.WARNING, "The input variable was not set properly.");
    public static final M REQUEST_SAML_ATTR_FORBIDDEN = m(4809, Level.WARNING, "Requester tried to access a forbidden attribute");
    public static final M REQUEST_SAML_ATTR_UNKNOWN = m(4810, Level.WARNING, "Requester tried to access an unknown attribute");
    public static final M RESPONSE_ENCRYPT_SAML_ASSERTION_VAR_UNUSABLE = m(4811, Level.WARNING, "The variable saml2.encrypt.cert.subjectDN was not set properly.");
    public static final M RESPONSE_ENCRYPT_SAML_ASSERTION_CERT_NOT_FOUND = m(4812, Level.WARNING, "The certificate \"{0}\" was not found.");
    public static final M RESPONSE_ENCRYPT_SAML_ASSERTION_PK_NOT_FOUND = m(4812, Level.WARNING, "The private key \"{0}\" was not found.");
}
