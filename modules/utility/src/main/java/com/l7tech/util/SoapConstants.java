package com.l7tech.util;

import javax.xml.soap.SOAPConstants;
import java.util.*;

import static com.l7tech.util.CollectionUtils.list;

/**
 * @author steve
 */
public class SoapConstants {
    public static final List<String> ENVELOPE_URIS = list(
        SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE,
        SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE,
        "http://www.w3.org/2001/06/soap-envelope",
        "http://www.w3.org/2001/09/soap-envelope",
        "urn:schemas-xmlsoap-org:soap.v1"
    );

    public static final String XMLNS = "xmlns";
    public static final String SECURITY_NAMESPACE_PREFIX = "wsse";// Namespace constants
    public static final String SECURITY_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    public static final String SECURITY_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/12/secext";
    public static final String SECURITY_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2002/07/secext";
    public static final String SECURITY_NAMESPACE4 = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    public static final String SECURITY_NAMESPACE5 = "http://schemas.xmlsoap.org/ws/2003/06/secext";
    public static final String SECURITY11_NAMESPACE = "http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd";//public static final String OLD_SECURITY11_NAMESPACE = "http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd";
    public static final List<String> WS_SECURITY_NAMESPACE_LIST = list(
        SECURITY_NAMESPACE,
        SECURITY_NAMESPACE2,
        SECURITY_NAMESPACE3,
        SECURITY_NAMESPACE4,
        SECURITY_NAMESPACE5,
        SECURITY11_NAMESPACE
    );

    public static final String WSSE_SECURITY_TOKEN_REFERENCE = "SecurityTokenReference";
    public static final String WSSE_REFERENCE = "Reference";
    public static final String WSSE_REFERENCE_ATTR_URI = "URI";
    public static final String WSSE_REFERENCE_ATTR_VALUE_TYPE = "ValueType";

    public static final String ENCRYPTION_METHOD = "EncryptionMethod";
    public static final String ATTRIBUTE_ALGORITHM = "Algorithm";

    public static final String XMLENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    public static final String XMLENC11_NS = "http://www.w3.org/2009/xmlenc11#";
    public static final String DIGSIG_URI = "http://www.w3.org/2000/09/xmldsig#";
    public static final String DIGSIG11_URI = "http://www.w3.org/2009/xmldsig11#";
    public static final String WSU_NAMESPACE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    public static final String WSU_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2002/07/utility";
    public static final String WSU_NAMESPACE3 = "http://schemas.xmlsoap.org/ws/2003/06/utility";
    public static final String WSSC_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    public static final String WSSC_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/sc";
    public static final String WSSC_NAMESPACE3 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512";
    public static final String[] WSSC_NAMESPACE_ARRAY = {
        WSSC_NAMESPACE,
        WSSC_NAMESPACE2,
        WSSC_NAMESPACE3,
    };

    public static final String WSC_RST_SCT_ACTION = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/RST/SCT";
    public static final String WSC_RST_SCT_ACTION2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/SCT";
    public static final String WSC_RST_SCT_ACTION3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/SCT";
    public static final List<String> WSC_RST_SCT_ACTION_LIST = list(
        WSC_RST_SCT_ACTION,
        WSC_RST_SCT_ACTION2,
        WSC_RST_SCT_ACTION3
    );

    public static final String WSC_RSTR_SCT_ACTION = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/RSTR/SCT";
    public static final String WSC_RSTR_SCT_ACTION2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/SCT";
    public static final String WSC_RSTR_SCT_ACTION3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/SCT";
    public static final List<String> WSC_RSTR_SCT_ACTION_LIST = list(
        WSC_RSTR_SCT_ACTION,
        WSC_RSTR_SCT_ACTION2,
        WSC_RSTR_SCT_ACTION3
    );

    public static final String WSC_RST_SCT_TOKEN_TYPE ="http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";
    public static final String WSC_RST_SCT_TOKEN_TYPE2 ="http://schemas.xmlsoap.org/ws/2005/02/sc/sct";
    public static final String WSC_RST_SCT_TOKEN_TYPE3 ="http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/sct";
    public static final List<String> WSC_RST_SCT_TOKEN_TYPE_LIST = list(
        WSC_RST_SCT_TOKEN_TYPE,
        WSC_RST_SCT_TOKEN_TYPE2,
        WSC_RST_SCT_TOKEN_TYPE3
    );

    public static final String WSC_RST_CANCEL_ACTION = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/SCT/Cancel";
    public static final String WSC_RST_CANCEL_ACTION2 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/SCT/Cancel";
    public static final List<String> WSC_RST_CANCEL_ACTION_LIST = list(
        WSC_RST_CANCEL_ACTION,
        WSC_RST_CANCEL_ACTION2
    );

    public static final String WSC_RSTR_CANCEL_ACTION = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/SCT/Cancel";
    public static final String WSC_RSTR_CANCEL_ACTION2 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/SCT/Cancel";
    public static final List<String> WSC_RSTR_CANCEL_ACTION_LIST = list(
        WSC_RSTR_CANCEL_ACTION,
        WSC_RSTR_CANCEL_ACTION2
    );

    public static final String WST_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/04/trust";
    public static final String WST_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2005/02/trust"; // FIM
    public static final String WST_NAMESPACE3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
    public static final String WST_NAMESPACE4 = "http://docs.oasis-open.org/ws-sx/ws-trust/200802";
    public static final String[] WST_NAMESPACE_ARRAY = {
        WST_NAMESPACE,
        WST_NAMESPACE2,   // Seen in Tivoli Fim example messages
        WST_NAMESPACE3,
        WST_NAMESPACE4,
    };

    public static final String WST_RST_ISSUE_ACTION = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/RST/Issue";
    public static final String WST_RST_ISSUE_ACTION2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue";
    public static final String WST_RST_ISSUE_ACTION3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue";
    public static final List<String> WST_RST_ISSUE_ACTION_LIST = list(
        WST_RST_ISSUE_ACTION,
        WST_RST_ISSUE_ACTION2,
        WST_RST_ISSUE_ACTION3
    );

    public static final String WST_RSTR_ISSUE_ACTION = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/RSTR/Issue";
    public static final String WST_RSTR_ISSUE_ACTION2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/RSTR/Issue";
    public static final String WST_RSTR_ISSUE_ACTION3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/Issue";
    public static final List<String> WST_RSTR_ISSUE_ACTION_LIST = list(
        WST_RSTR_ISSUE_ACTION,
        WST_RSTR_ISSUE_ACTION2,
        WST_RSTR_ISSUE_ACTION3
    );

    public static final String WST_RST_ISSUE_REQUEST_TYPE = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue";
    public static final String WST_RST_ISSUE_REQUEST_TYPE2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue";
    public static final String WST_RST_ISSUE_REQUEST_TYPE3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue";
    public static final List<String> WST_RST_ISSUE_REQUEST_TYPE_LIST = list(
        WST_RST_ISSUE_REQUEST_TYPE,
        WST_RST_ISSUE_REQUEST_TYPE2,
        WST_RST_ISSUE_REQUEST_TYPE3
    );

    public static final String WST_RST_CANCEL_REQUEST_TYPE = "http://schemas.xmlsoap.org/ws/2005/02/trust/Cancel";
    public static final String WST_RST_CANCEL_REQUEST_TYPE2 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Cancel";
    public static final List<String> WST_RST_CANCEL_REQUEST_TYPE_LIST = list(
        WST_RST_CANCEL_REQUEST_TYPE,
        WST_RST_CANCEL_REQUEST_TYPE2
    );

    public static final String WST_BINARY_SECRET_NONCE_TYPE = "Nonce";

    public static final String WST_BINARY_SECRET_NONCE_TYPE_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Nonce";
    public static final String WST_BINARY_SECRET_NONCE_TYPE_URI2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/Nonce";
    public static final String WST_BINARY_SECRET_NONCE_TYPE_URI3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Nonce";
    public static final List<String> WST_BINARY_SECRET_NONCE_TYPE_URI_LIST = list(
        WST_BINARY_SECRET_NONCE_TYPE_URI,
        WST_BINARY_SECRET_NONCE_TYPE_URI2,
        WST_BINARY_SECRET_NONCE_TYPE_URI3
    );

    public static final String WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE = "AsymmetricKey";

    public static final String WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/AsymmetricKey";
    public static final String WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/AsymmetricKey";
    public static final String WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/AsymmetricKey";
    public static final List<String> WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI_LIST = list(
        WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI,
        WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI2,
        WST_BINARY_SECRET_ASYMMETRIC_KEY_TYPE_URI3
    );

    public static final String WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE = "SymmetricKey";
    
    public static final String WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/SymmetricKey";
    public static final String WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/SymmetricKey";
    public static final String WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/SymmetricKey";
    public static final List<String> WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI_LIST = list(
        WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI,
        WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI2,
        WST_BINARY_SECRET_SYMMETRIC_KEY_TYPE_URI3
    );

    
    public static final String WST_REQUESTSECURITYTOKEN = "RequestSecurityToken";
    public static final String WST_TOKENTYPE = "TokenType";
    public static final String WST_REQUESTTYPE = "RequestType";
    public static final String WST_CANCELTARGET = "CancelTarget";
    public static final String WST_ISSUER = "Issuer";
    public static final String ENTROPY = "Entropy";
    public static final String BINARY_SECRET = "BinarySecret";
    public static final String BINARY_SECRET_ATTR_TYPE ="Type";
    public static final String KEY_SIZE = "KeySize";
    
    public static final String WSX_NAMESPACE = "http://schemas.xmlsoap.org/ws/2004/03/mex";
    public static final String WSP_NAMESPACE = "http://schemas.xmlsoap.org/ws/2002/12/policy";
    public static final String WSP_NAMESPACE2 = "http://schemas.xmlsoap.org/ws/2004/09/policy"; // FIM
    public static final String WSP_NAMESPACE3 = "http://www.w3.org/ns/ws-policy";
    public static final String[] WSP_NAMESPACE_ARRAY = {
        WSP_NAMESPACE,
        WSP_NAMESPACE2,    // Seen in Tivoli Fim example messages
        WSP_NAMESPACE3,
    };

    public static final String WSA_ACTION = "Action";
    public static final String WSA_ADDRESS = "Address";
    public static final String WSA_ENDPOINTREFERENCE = "EndpointReference";

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

    public static final String WSA_WSDL_NAMESPACE_2006_05 = "http://www.w3.org/2006/05/addressing/wsdl";
    public static final String WSA_WSDL_NAMESPACE_2006_02 = "http://www.w3.org/2006/02/addressing/wsdl";
    public static final String WSA_WSDL_NAMESPACE_2005_03 = "http://www.w3.org/2005/03/addressing/wsdl";

    /**
     * Listed in order of preference (newest to oldest).
     */
    public static final Collection<String> WSA_WSDL_NAMESPACES = list(
                    WSA_WSDL_NAMESPACE_2006_05,
                    WSA_WSDL_NAMESPACE_2006_02,
                    WSA_WSDL_NAMESPACE_2005_03
    );
    
    public static final String WSA_WSDL_LATEST = WSA_WSDL_NAMESPACE_2006_05;

    public static final String WSA_ANONYMOUS_ADDRESS = "http://www.w3.org/2005/08/addressing/anonymous";
    public static final String WSA_NO_ADDRESS = "http://www.w3.org/2005/08/addressing/none";

    public static final String L7_MESSAGEID_NAMESPACE = "http://www.layer7tech.com/ws/addr";
    public static final String L7_MESSAGEID_PREFIX = "L7a";
    public static final String L7_SERVICEID_ELEMENT = "ServiceId";
    public static final String L7_CLIENTVERSION_ELEMENT = "ClientVersion";
    public static final String L7_POLICYVERSION_ELEMENT = "PolicyVersion";
    public static final String L7_SOAP_ACTOR = "secure_span";
    // This is a URI actor/role value, sent by default by version 4.6 SSBs and SSGs.  See Bug #5829
    public static final String L7_SOAP_ACTOR_URI  = "http://www.layer7tech.com/ws/policy";
    public static final String L7_SOAP_ACTORS = L7_SOAP_ACTOR + " " + L7_SOAP_ACTOR_URI;
    public static final List<String> SECURITY_URIS = list(
        SECURITY_NAMESPACE,
        SECURITY_NAMESPACE2,
        SECURITY_NAMESPACE3,
        SECURITY_NAMESPACE4,
        SECURITY_NAMESPACE5
    );
    public static final String[] SECURITY_URIS_ARRAY = (String[]) SECURITY_URIS.toArray(new String[SECURITY_URIS.size()]);
    public static final String WSU_PREFIX = "wsu";
    public static final List<String> WSU_URIS = list(
            WSU_NAMESPACE,
            WSU_NAMESPACE2,
            WSU_NAMESPACE3
    );
    public static final String[] WSU_URIS_ARRAY = (String[]) WSU_URIS.toArray(new String[WSU_URIS.size()]);// Attribute names
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
    public static final String REFERENCE_DIGEST_METHOD_EL_NAME = "DigestMethod";
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
    public static final String UNTOK_CREATED_EL_NAME = "Created";
    public static final String UNTOK_NONCE_EL_NAME = "Nonce";
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
    public static final String VALUETYPE_SAML_ASSERTIONID_DRAFT_2004 = "http://www.docs.oasis-open.org/wss/2004/01/oasis-200401-wss-saml-token-profile-1.0#SAMLAssertionID"; // from a DRAFT spec?
    public static final String VALUETYPE_SAML_ASSERTIONID_SAML11 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID";
    public static final String VALUETYPE_SAML_ASSERTIONID_SAML20 = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLID";
    public static final String[] VALUETYPE_SAML_ASSERTIONID_ARRAY = {
            VALUETYPE_SAML_ASSERTIONID_DRAFT_2004,
            VALUETYPE_SAML_ASSERTIONID_SAML11, // SAML 1.1 assertion id ref (STP 1.0 and 1.1)
            VALUETYPE_SAML_ASSERTIONID_SAML20, // SAML 2.0 assertion id ref (STP 1.1)
    };
    
    public static final String SAML_NAMESPACE = "urn:oasis:names:tc:SAML:1.0:assertion";   // SAML 1.1
    public static final String SAML_NAMESPACE2 = "urn:oasis:names:tc:SAML:2.0:assertion";  // SAML 2.0
    public static final List<String> SAML_NAMESPACE_LIST = list(
        SAML_NAMESPACE,
        SAML_NAMESPACE2
    );

    public static final String VALUETYPE_DERIVEDKEY = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk";
    public static final String VALUETYPE_DERIVEDKEY2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk";
    public static final String VALUETYPE_DERIVEDKEY3 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/dk";
    public static final String VALUETYPE_SECURECONV = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct";
    public static final String VALUETYPE_SECURECONV2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/sct";
    public static final String VALUETYPE_SECURECONV3 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/sct";
    public static final String VALUETYPE_KERBEROS_GSS_AP_REQ = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#GSS_Kerberosv5_AP_REQ";
    public static final String VALUETYPE_KERBEROS_APREQ_SHA1 = "http://docs.oasis-open.org/wss/oasis-wss-kerberos-token-profile-1.1#Kerberosv5APREQSHA1";
    public static final String ENCODINGTYPE_BASE64BINARY_SUFFIX = "Base64Binary";
    public static final String ENCODINGTYPE_BASE64BINARY = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#" + ENCODINGTYPE_BASE64BINARY_SUFFIX;
    public static final String ENCODINGTYPE_BASE64BINARY_2 = SECURITY_NAMESPACE_PREFIX + ":" + ENCODINGTYPE_BASE64BINARY_SUFFIX;
    public static final String ALGORITHM_PSHA = "http://schemas.xmlsoap.org/ws/2004/04/security/sc/dk/p_sha1";
    public static final String ALGORITHM_PSHA2 = "http://schemas.xmlsoap.org/ws/2005/02/sc/dk/p_sha1";
    public static final String ALGORITHM_PSHA3 = "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/dk/p_sha1";
    public static final String TRANSFORM_STR = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform";
    public static final String TRANSFORM_ATTACHMENT_COMPLETE = "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Transform";
    public static final String TRANSFORM_ATTACHMENT_CONTENT = "http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Only-Transform";// Well-known actors (SOAP 1.1)
    public static final String ACTOR_VALUE_NEXT = "http://schemas.xmlsoap.org/soap/actor/next";
    public static final String ACTOR_LAYER7_WRAPPED = "http://www.layer7tech.com/ws/actor-wrapped";// Well-known roles (SOAP 1.2)
    public static final String ROLE_VALUE_NONE = "http://www.w3.org/2003/05/soap-envelope/role/none";
    public static final String ROLE_VALUE_NEXT = "http://www.w3.org/2003/05/soap-envelope/role/next";
    public static final String ROLE_VALUE_ULTIMATE = "http://www.w3.org/2003/05/soap-envelope/role/ultimateReceiver";

    public static final String C14N_EXCLUSIVE = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String C14N_EXCLUSIVEWC = "http://www.w3.org/2001/10/xml-exc-c14n#WithComments";

    public static final String FC_CLIENT = "Client";
    public static final String FC_SERVER = "Server";

    /**
     * A permissive list of possible ID attribute names you might encounter when doing SOAP security processing.
     */
    public static final Set<FullQName> DEFAULT_ID_ATTRIBUTE_QNAMES = Collections.unmodifiableSet(new LinkedHashSet<FullQName>() {{
        add(new FullQName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2002/07/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("urn:oasis:names:tc:SAML:1.0:assertion", "local", "AssertionID"));
        add(new FullQName("urn:oasis:names:tc:SAML:2.0:assertion", "local", "ID"));
        add(new FullQName(null, null, "Id"));
    }});

    public static final IdAttributeConfig DEFAULT_ID_ATTRIBUTE_CONFIG = IdAttributeConfig.makeIdAttributeConfig(DEFAULT_ID_ATTRIBUTE_QNAMES);

    /**
     * An ID attribute configuration that does not recognize SAML assertions or local ID attributes.
     */
    public static final Set<FullQName> NOSAML_ID_ATTRIBUTE_QNAMES = Collections.unmodifiableSet(new LinkedHashSet<FullQName>() {{
        add(new FullQName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2002/07/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName("http://schemas.xmlsoap.org/ws/2003/06/utility", null, "Id"));
        add(new FullQName(null, null, "Id"));
    }});

    public static final IdAttributeConfig NOSAML_ID_ATTRIBUTE_CONFIG = IdAttributeConfig.makeIdAttributeConfig(NOSAML_ID_ATTRIBUTE_QNAMES);

    /**
     * A strict configuration that only recognizes wsu:Id (with the final wsu namespace) attributes.  
     */
    public static final Set<FullQName> STRICT_WSS_ID_ATTRIBUTE_QNAMES = Collections.unmodifiableSet(new LinkedHashSet<FullQName>() {{
        add(new FullQName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", null, "Id"));
    }});

    public static final IdAttributeConfig STRICT_WSS_ID_ATTRIBUTE_CONFIG = IdAttributeConfig.makeIdAttributeConfig(STRICT_WSS_ID_ATTRIBUTE_QNAMES);

    public static final String WSA_MSG_PROP_ACTION = "Action";
    public static final String WSA_MSG_PROP_DESTINATION = "To";
    public static final String WSA_MSG_PROP_SOURCE_ENDPOINT = "From";
    public static final String WSA_MSG_PROP_MESSAGE_ID = "MessageID";
    public static final String WSA_MSG_PROP_REPLY_TO = "ReplyTo";
    public static final String WSA_MSG_PROP_FAULT_TO = "FaultTo";
    public static final String WSA_MSG_PROP_RELATES_TO = "RelatesTo";
    public static final String WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE = "RelationshipType";
    public static final String WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE = "http://www.w3.org/2005/08/addressing/reply";

    public static final String P_SHA1_ALG_URI = "http://schemas.xmlsoap.org/ws/2004/04/security/trust/CK/PSHA1";
    public static final String P_SHA1_ALG_URI2 = "http://schemas.xmlsoap.org/ws/2005/02/trust/CK/PSHA1";
    public static final String P_SHA1_ALG_URI3 = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/CK/PSHA1";
}
