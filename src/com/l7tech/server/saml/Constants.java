package com.l7tech.server.saml;

/**
 * SAML constants
 *
 * @author emil
 * @version 27-Jul-2004
 */
public class Constants {
    /** namespaces */
    public static final String NS_SAML = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String NS_SAML_PREFIX = "saml";
    public static final String NS_SAMLP = "urn:oasis:names:tc:SAML:1.0:protocol";
    public static final String NS_SAMLP_PREFIX = "samlp";

    /** confirmations */
    public static final String CONFIRMATION_SENDER_VOUCHES = "urn:oasis:names:tc:SAML:1.0:cm:sender-vouches";
    public static final String CONFIRMATION_HOLDER_OF_KEY = "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";

    /** authentications */
    public static final String PASSWORD_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:password";
    public static final String KERBEROS_AUTHENTICATION = "urn:ietf:rfc:1510";
    public static final String SRP_AUTHENTICATION = "urn:ietf:rfc:2945";
    public static final String HARDWARE_TOKEN_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:HardwareToken";
    public static final String SSL_TLS_CERTIFICATE_AUTHENTICATION = "urn:ietf:rfc:2246";
    public static final String X509_PKI_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:X509-PKI";
    public static final String PGP_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:PGP";
    public static final String SPKI_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:SPKI";
    public static final String XKMS_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:XKMS";
    public static final String XML_DSIG_AUTHENTICATION = "urn:ietf:rfc:3075";
    public static final String UNSPECIFIED_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:unspecified";


    /** name identifiers */
    public static final String NAMEIDENTIFIER_X509_SUBJECT = "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";
    public static final String NAMEIDENTIFIER_EMAIL = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
    public static final String NAMEIDENTIFIER_WINDOWS = "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName";
    public static final String NAMEIDENTIFIER_UNSPECIFIED = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";

    /** SAML response status codes see saml core spec section 3.4.3.1 */
    public static final String STATUS_SUCCESS = "Success";

    /**
     * The request could not be performed due to an error on the part
     * of the SAML responder or SAML authority.
     */
    public static final String STATUS_RESPONDER = "Responder";

    public static final String ELEMENT_ASSERTION = "Assertion";

    /**
     * Cannot instantiate this class
     */
    private Constants() {
    }
}
