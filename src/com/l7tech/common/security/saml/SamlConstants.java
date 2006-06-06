package com.l7tech.common.security.saml;

/**
 * SAML constants
 *
 * @author emil
 * @version 27-Jul-2004
 */
public class SamlConstants {
    /** namespaces */
    public static final String NS_SAML  = "urn:oasis:names:tc:SAML:1.0:assertion";
    public static final String NS_SAML2 = "urn:oasis:names:tc:SAML:2.0:assertion";
    public static final String NS_SAML_PREFIX = "saml";
    public static final String NS_SAMLP = "urn:oasis:names:tc:SAML:1.0:protocol";
    public static final String NS_SAMLP_PREFIX = "samlp";

    /** confirmations */
    public static final String CONFIRMATION_SENDER_VOUCHES = "urn:oasis:names:tc:SAML:1.0:cm:sender-vouches";
    public static final String CONFIRMATION_HOLDER_OF_KEY = "urn:oasis:names:tc:SAML:1.0:cm:holder-of-key";
    public static final String CONFIRMATION_BEARER = "urn:oasis:names:tc:SAML:1.0:cm:bearer";

    /** confirmations saml 2 */
    public static final String CONFIRMATION_SAML2_SENDER_VOUCHES = "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches";
    public static final String CONFIRMATION_SAML2_HOLDER_OF_KEY = "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key";
    public static final String CONFIRMATION_SAML2_BEARER = "urn:oasis:names:tc:SAML:2.0:cm:bearer";

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
    /** <b>NOTE:</b> this RFC has been obsoleted by RFC3275, let's see if OASIS notices! */
    public static final String XML_DSIG_AUTHENTICATION = "urn:ietf:rfc:3075";
    public static final String UNSPECIFIED_AUTHENTICATION = "urn:oasis:names:tc:SAML:1.0:am:unspecified";

    public static final String[] ALL_AUTHENTICATIONS = new String[] {
        PASSWORD_AUTHENTICATION,
        KERBEROS_AUTHENTICATION,
        SRP_AUTHENTICATION,
        HARDWARE_TOKEN_AUTHENTICATION,
        SSL_TLS_CERTIFICATE_AUTHENTICATION,
        X509_PKI_AUTHENTICATION,
        PGP_AUTHENTICATION,
        SPKI_AUTHENTICATION,
        XKMS_AUTHENTICATION,
        XML_DSIG_AUTHENTICATION,
        UNSPECIFIED_AUTHENTICATION,
    };

    /** authentications saml 2 */
    public static final String AUTHENTICATION_SAML2_TELEPHONY_AUTH = "urn:oasis:names:tc:SAML:2.0:ac:classes:AuthenticatedTelephony";
    public static final String AUTHENTICATION_SAML2_IP = "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocol";
    public static final String AUTHENTICATION_SAML2_IPPASSWORD = "urn:oasis:names:tc:SAML:2.0:ac:classes:InternetProtocolPassword";
    public static final String AUTHENTICATION_SAML2_KERBEROS = "urn:oasis:names:tc:SAML:2.0:ac:classes:Kerberos";
    public static final String AUTHENTICATION_SAML2_MOBILE_1FACTOR_CONTRACT = "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorContract";
    public static final String AUTHENTICATION_SAML2_MOBILE_1FACTOR_UNREG = "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileOneFactorUnregistered";
    public static final String AUTHENTICATION_SAML2_MOBILE_2FACTOR_CONTRACT = "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorContract";
    public static final String AUTHENTICATION_SAML2_MOBILE_2FACTOR_UNREG = "urn:oasis:names:tc:SAML:2.0:ac:classes:MobileTwoFactorUnregistered";
    public static final String AUTHENTICATION_SAML2_TELEPHONY_NOMAD = "urn:oasis:names:tc:SAML:2.0:ac:classes:NomadTelephony";
    public static final String AUTHENTICATION_SAML2_PASSWORD = "urn:oasis:names:tc:SAML:2.0:ac:classes:Password";
    public static final String AUTHENTICATION_SAML2_PASSWORD_PROTECTED = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    public static final String AUTHENTICATION_SAML2_TELEPHONY_PERSONALIZED = "urn:oasis:names:tc:SAML:2.0:ac:classes:PersonalizedTelephony";
    public static final String AUTHENTICATION_SAML2_PGP = "urn:oasis:names:tc:SAML:2.0:ac:classes:PGP";
    public static final String AUTHENTICATION_SAML2_SESSION = "urn:oasis:names:tc:SAML:2.0:ac:classes:PreviousSession";
    public static final String AUTHENTICATION_SAML2_PASSWORD_SECURE = "urn:oasis:names:tc:SAML:2.0:ac:classes:SecureRemotePassword";
    public static final String AUTHENTICATION_SAML2_SMARTCARD = "urn:oasis:names:tc:SAML:2.0:ac:classes:Smartcard";
    public static final String AUTHENTICATION_SAML2_SMARTCARD_PKI = "urn:oasis:names:tc:SAML:2.0:ac:classes:SmartcardPKI";
    public static final String AUTHENTICATION_SAML2_SOFTWARE_PKI = "urn:oasis:names:tc:SAML:2.0:ac:classes:SoftwarePKI";
    public static final String AUTHENTICATION_SAML2_SPKI = "urn:oasis:names:tc:SAML:2.0:ac:classes:SPKI";
    public static final String AUTHENTICATION_SAML2_TELEPHONY = "urn:oasis:names:tc:SAML:2.0:ac:classes:Telephony";
    public static final String AUTHENTICATION_SAML2_TIME_SYNC_TOKEN = "urn:oasis:names:tc:SAML:2.0:ac:classes:TimeSyncToken";
    public static final String AUTHENTICATION_SAML2_TLS_CERT = "urn:oasis:names:tc:SAML:2.0:ac:classes:TLSClient";
    public static final String AUTHENTICATION_SAML2_X509 = "urn:oasis:names:tc:SAML:2.0:ac:classes:X509";
    public static final String AUTHENTICATION_SAML2_XMLDSIG = "urn:oasis:names:tc:SAML:2.0:ac:classes:XMLDSig";
    public static final String AUTHENTICATION_SAML2_UNSPECIFIED = "urn:oasis:names:tc:SAML:2.0:ac:classes:unspecified";

    public static final String[] ALL_AUTHENTICATIONS_SAML2 = new String[] {
        AUTHENTICATION_SAML2_TELEPHONY_AUTH,
        AUTHENTICATION_SAML2_IP,
        AUTHENTICATION_SAML2_IPPASSWORD,
        AUTHENTICATION_SAML2_KERBEROS,
        AUTHENTICATION_SAML2_MOBILE_1FACTOR_CONTRACT,
        AUTHENTICATION_SAML2_MOBILE_1FACTOR_UNREG,
        AUTHENTICATION_SAML2_MOBILE_2FACTOR_CONTRACT,
        AUTHENTICATION_SAML2_MOBILE_2FACTOR_UNREG,
        AUTHENTICATION_SAML2_TELEPHONY_NOMAD,
        AUTHENTICATION_SAML2_PASSWORD,
        AUTHENTICATION_SAML2_PASSWORD_PROTECTED,
        AUTHENTICATION_SAML2_TELEPHONY_PERSONALIZED,
        AUTHENTICATION_SAML2_PGP,
        AUTHENTICATION_SAML2_SESSION,
        AUTHENTICATION_SAML2_PASSWORD_SECURE,
        AUTHENTICATION_SAML2_SMARTCARD,
        AUTHENTICATION_SAML2_SMARTCARD_PKI,
        AUTHENTICATION_SAML2_SOFTWARE_PKI,
        AUTHENTICATION_SAML2_SPKI,
        AUTHENTICATION_SAML2_TELEPHONY,
        AUTHENTICATION_SAML2_TIME_SYNC_TOKEN,
        AUTHENTICATION_SAML2_TLS_CERT,
        AUTHENTICATION_SAML2_X509,
        AUTHENTICATION_SAML2_XMLDSIG,
        AUTHENTICATION_SAML2_UNSPECIFIED,
    };

    /** name identifiers */
    public static final String NAMEIDENTIFIER_X509_SUBJECT = "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";
    public static final String NAMEIDENTIFIER_EMAIL = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
    public static final String NAMEIDENTIFIER_WINDOWS = "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName";
    public static final String NAMEIDENTIFIER_UNSPECIFIED = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";

    public static final String[] ALL_NAMEIDENTIFIERS = {
        NAMEIDENTIFIER_X509_SUBJECT,
        NAMEIDENTIFIER_EMAIL,
        NAMEIDENTIFIER_WINDOWS,
        NAMEIDENTIFIER_UNSPECIFIED,
    };

    /** SAML response status codes see saml core spec section 3.4.3.1 */
    public static final String STATUS_SUCCESS = "Success";

    /**
     * The request could not be performed due to an error on the part
     * of the SAML responder or SAML authority.
     */
    public static final String STATUS_RESPONDER = "Responder";

    public static final String ELEMENT_ASSERTION = "Assertion";
    public static final String ELEMENT_ISSUER = "Issuer";

    public static final String ATTR_ASSERTION_ID = "AssertionID";
    public static final String ATTR_SAML2_ASSERTION_ID = "ID";

    public static final String ELEMENT_AUDIENCE = "Audience";

    /**
     * Cannot instantiate this class
     */
    private SamlConstants() {
    }

}
