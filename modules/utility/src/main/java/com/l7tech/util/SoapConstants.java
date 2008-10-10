package com.l7tech.util;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPConstants;
import java.util.List;
import java.util.ArrayList;

/**
 * @author steve
 */
public class SoapConstants {
    public static final List<String> ENVELOPE_URIS = new ArrayList<String>();
    static {
        ENVELOPE_URIS.add( SOAPConstants.URI_NS_SOAP_ENVELOPE);
        ENVELOPE_URIS.add("http://www.w3.org/2001/06/soap-envelope");
        ENVELOPE_URIS.add("http://www.w3.org/2001/09/soap-envelope");
        ENVELOPE_URIS.add("http://www.w3.org/2003/05/soap-envelope");
        ENVELOPE_URIS.add("urn:schemas-xmlsoap-org:soap.v1");
    }
    protected static final QName[] EMPTY_QNAME_ARRAY = new QName[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];// Namespace prefix constants
    public static final String SOAP_ENV_PREFIX = "soapenv";
    public static final String SOAP_1_2_ENV_PREFIX = "s12";
    public static final String XMLNS = "xmlns";
    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";// Namespace constants
    public static final String SECURITY_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";
    public static final String SECURITY_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2002/07/secext";
    public static final String SECURITY_NAMESPACE4 = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE5 = "http://schemas.xmlsoap.org/ws/2003/06/secext";
    public static final String SECURITY11_NAMESPACE = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";//public static final String OLD_SECURITY11_NAMESPACE = "http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd";
    public static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";
    public static final String WSU_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WSU_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String WSU_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2003/06/utility";
    public static final String WSSC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    public static final String WSSC_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/sc";
    public static final String[] WSSC_NAMESPACE_ARRAY = {
        WSSC_NAMESPACE,
        WSSC_NAMESPACE2,
    };
    public static final String WST_NAMESPACE  = "http://schemas.xmlsoap.org/ws/2004/04/trust";
    public static final String WST_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/trust"; // FIM
    public static final String[] WST_NAMESPACE_ARRAY = {
        WST_NAMESPACE,
        WST_NAMESPACE2,   // Seen in Tivoli Fim example messages
    };
    public static final String WST_REQUESTSECURITYTOKEN = "RequestSecurityToken";
    public static final String WST_TOKENTYPE = "TokenType";
    public static final String WST_REQUESTTYPE = "RequestType";
    public static final String WSX_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/mex";
    public static final String WSP_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/12/policy";
    public static final String WSP_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/09/policy"; // FIM
    public static final String[] WSP_NAMESPACE_ARRAY = {
        WSP_NAMESPACE,
        WSP_NAMESPACE2,    // Seen in Tivoli Fim example messages
    };
    public static final String WSA_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/addressing";
    public static final String WSA_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/08/addressing"; // FIM
    public static final String WSA_NAMESPACE_10 = "http://www.w3.org/2005/08/addressing";
    public static final String WSA_NAMESPACE_200303 = "http://schemas.xmlsoap.org/ws/2003/03/addressing";
    public static final String[] WSA_NAMESPACE_ARRAY = {
        WSA_NAMESPACE,
        WSA_NAMESPACE2,
        WSA_NAMESPACE_10,
        WSA_NAMESPACE_200303
    };
    public static final String L7_MESSAGEID_NAMESPACE = "http://www.layer7tech.com/ws/addr";
    public static final String L7_MESSAGEID_PREFIX = "L7a";
    public static final String L7_SERVICEID_ELEMENT = "ServiceId";
    public static final String L7_POLICYVERSION_ELEMENT = "PolicyVersion";
    public static final List<String> SECURITY_URIS = new ArrayList<String>();
    static {
        SECURITY_URIS.add(SECURITY_NAMESPACE);
        SECURITY_URIS.add(SECURITY_NAMESPACE2);
        SECURITY_URIS.add(SECURITY_NAMESPACE3);
        SECURITY_URIS.add(SECURITY_NAMESPACE4);
        SECURITY_URIS.add(SECURITY_NAMESPACE5);
    }
    public static final String[] SECURITY_URIS_ARRAY = (String[])SECURITY_URIS.toArray(EMPTY_STRING_ARRAY);
    public static final String WSU_PREFIX = "wsu";
    public static final List<String> WSU_URIS = new ArrayList<String>();
    static {
        WSU_URIS.add(WSU_NAMESPACE);
        WSU_URIS.add(WSU_NAMESPACE2);
        WSU_URIS.add(WSU_NAMESPACE3);
    }
    public static final String[] WSU_URIS_ARRAY = (String[])WSU_URIS.toArray(EMPTY_STRING_ARRAY);// Attribute names
    public static final String ID_ATTRIBUTE_NAME = "Id";
    public static final String ACTOR_ATTR_NAME = "actor";  // SOAP 1.1
    public static final String ROLE_ATTR_NAME = "role";    // SOAP 1.2
    public static final String MUSTUNDERSTAND_ATTR_NAME = "mustUnderstand"; // SOAP 1.1+
    public static final String REFERENCE_URI_ATTR_NAME = "URI";
    public static final String UNTOK_PSSWD_TYPE_ATTR_NAME = "Type";// Element names
    public static final String ENVELOPE_EL_NAME = "Envelope";
    public static final String BODY_EL_NAME = "Body";
    public static final String HEADER_EL_NAME = "Header";
    public static final String SECURITY_EL_NAME = "Security";
    public static final String SIGNATURE_EL_NAME = "Signature";
    public static final String SIGNED_INFO_EL_NAME = "SignedInfo";
    public static final String REFERENCE_EL_NAME = "Reference";
    public static final String SECURITY_CONTEXT_TOK_EL_NAME = "SecurityContextToken";
    public static final String SECURITYTOKENREFERENCE_EL_NAME = "SecurityTokenReference";
    public static final String BINARYSECURITYTOKEN_EL_NAME = "BinarySecurityToken";
    public static final String KEYIDENTIFIER_EL_NAME = "KeyIdentifier";
    public static final String ENCRYPTEDKEY_EL_NAME = "EncryptedKey";
    public static final String TIMESTAMP_EL_NAME = "Timestamp";
    public static final String CREATED_EL_NAME = "Created";
    public static final String EXPIRES_EL_NAME = "Expires";
    public static final String USERNAME_TOK_EL_NAME = "UsernameToken";
    public static final String UNTOK_USERNAME_EL_NAME = "Username";
    public static final String UNTOK_PASSWORD_EL_NAME = "Password";
    public static final String MESSAGEID_EL_NAME = "MessageID";
    public static final String RELATESTO_EL_NAME = "RelatesTo";
    public static final String WSSC_ID_EL_NAME = "Identifier";
    public static final String WSSC_DK_EL_NAME = "DerivedKeyToken";
    public static final String REFLIST_EL_NAME = "ReferenceList";
    public static final String DATAREF_EL_NAME = "DataReference";
    public static final String KINFO_EL_NAME = "KeyInfo";// Misc
    public static final String SOAPACTION = "SOAPAction";
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";
    public static final String SUPPORTED_ENCRYPTEDKEY_ALGO_2 = "http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p";
    public static final String VALUETYPE_SKI_SUFFIX = "X509SubjectKeyIdentifier";
    public static final String VALUETYPE_SKI = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#" + VALUETYPE_SKI_SUFFIX;
    public static final String VALUETYPE_SKI_2 = SECURITY_NAMESPACE_PREFIX + ":" + VALUETYPE_SKI_SUFFIX;
    public static final String VALUETYPE_X509_SUFFIX = "X509v3";
    public static final String VALUETYPE_X509 = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#" + VALUETYPE_X509_SUFFIX;
    public static final String VALUETYPE_X509_2 = SECURITY_NAMESPACE_PREFIX + ":" + VALUETYPE_X509_SUFFIX;
    public static final String VALUETYPE_X509_THUMB_SHA1_SUFFIX = "ThumbprintSHA1";
    public static final String VALUETYPE_X509_THUMB_SHA1 =        "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1";
    public static final String VALUETYPE_ENCRYPTED_KEY = "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKey";
    public static final String VALUETYPE_ENCRYPTED_KEY_SHA1_SUFFIX = "EncryptedKeySHA1";
    public static final String VALUETYPE_ENCRYPTED_KEY_SHA1 = "http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#EncryptedKeySHA1";
    public static final String VALUETYPE_SAML = "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-saml-token-profile-1.0#SAMLAssertion-1.0"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML2 = "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-saml-token-profile-1.0#SAMLAssertion-1.1"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML3 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertion-1.1";
    public static final String VALUETYPE_SAML4 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV1.1";
    public static final String VALUETYPE_SAML5 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    public static final String[] VALUETYPE_SAML_ARRAY = {
        VALUETYPE_SAML,
        VALUETYPE_SAML2,
        VALUETYPE_SAML3, // SAML 1.1 assertion from STP 1.0
        VALUETYPE_SAML4, // SAML 1.1 assertion from STP 1.1
        VALUETYPE_SAML5, // SAML 2.0 assertion
    };
    public static final String VALUETYPE_SAML_ASSERTIONID = "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-saml-token-profile-1.0#SAMLAssertionID"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML_ASSERTIONID2 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID";
    public static final String VALUETYPE_SAML_ASSERTIONID3 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";
    public static final String[] VALUETYPE_SAML_ASSERTIONID_ARRAY = {
        VALUETYPE_SAML_ASSERTIONID,
        VALUETYPE_SAML_ASSERTIONID2, // SAML 1.1 assertion id ref (STP 1.0 and 1.1)
        VALUETYPE_SAML_ASSERTIONID3, // SAML 2.0 assertion id ref (STP 1.1)
    };
    public static final String VALUETYPE_DERIVEDKEY = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk";
    public static final String VALUETYPE_DERIVEDKEY2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk";
    public static final String VALUETYPE_SECURECONV = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";
    public static final String VALUETYPE_SECURECONV2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/sct";
    public static final String VALUETYPE_KERBEROS_GSS_AP_REQ = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#GSS_Kerberosv5_AP_REQ";
    public static final String VALUETYPE_KERBEROS_APREQ_SHA1 = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#Kerberosv5APREQSHA1";
    public static final String ENCODINGTYPE_BASE64BINARY_SUFFIX = "Base64Binary";
    public static final String ENCODINGTYPE_BASE64BINARY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#" + ENCODINGTYPE_BASE64BINARY_SUFFIX;
    public static final String ENCODINGTYPE_BASE64BINARY_2 = SECURITY_NAMESPACE_PREFIX + ":" + ENCODINGTYPE_BASE64BINARY_SUFFIX;
    public static final String ALGORITHM_PSHA = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk/p_sha1";
    public static final String ALGORITHM_PSHA2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";
    public static final String TRANSFORM_STR = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform";
    public static final String TRANSFORM_ATTACHMENT_COMPLETE = "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Transform";
    public static final String TRANSFORM_ATTACHMENT_CONTENT = "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Only-Transform";// Well-known actors (SOAP 1.1)
    public static final String ACTOR_VALUE_NEXT = "http://schemas.xmlsoap.org/soap/actor/next";
    public static final String ACTOR_LAYER7_WRAPPED = "http://www.layer7tech.com/ws/actor-wrapped";// Well-known roles (SOAP 1.2)
    public static final String ROLE_VALUE_NONE = "http://www.w3.org/2003/05/soap-envelope/role/none";
    public static final String ROLE_VALUE_NEXT = "http://www.w3.org/2003/05/soap-envelope/role/next";
    public static final String ROLE_VALUE_ULTIMATE = "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";
    /**
     * soap envelope xpath '/soapenv:Envelope'
     */
    public static final String SOAP_ENVELOPE_XPATH = "/" + SOAP_ENV_PREFIX + ":" + ENVELOPE_EL_NAME;
    public static final String SOAP_1_2_ENVELOPE_XPATH = "/" + SOAP_1_2_ENV_PREFIX + ":" + ENVELOPE_EL_NAME;
    
    /**
     * soap body xpath '/soapenv:Envelope/soapenv:Body'
     */
    public static final String SOAP_BODY_XPATH = SOAP_ENVELOPE_XPATH + "/" + SOAP_ENV_PREFIX + ":Body";
    /**
     * soap header xpath '/soapenv:Envelope/soapenv:Header'
     */
    public static final String SOAP_HEADER_XPATH = SOAP_ENVELOPE_XPATH + "/" + SOAP_ENV_PREFIX + ":Header";
    public static final String C14N_EXCLUSIVE = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String C14N_EXCLUSIVEWC = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";


    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";    
}
