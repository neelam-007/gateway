package com.l7tech.security;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.net.URL;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;
import javax.xml.xpath.XPathFunctionResolver;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.l7tech.common.io.XmlUtil;

/**
 * Check a Document for compliance with WS-I Basic Security Profile 1.0
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiBSPValidator {

    //- PUBLIC

    /**
     * Utility for performing software XPath validation of an XML document to
     * check for WSI-BSP compliance.
     */
    public static void main(String[] args) throws Exception {
        if(args.length < 1) {
            System.out.println("\nUsage:\n\n\tjava ...WsiBSPValidator <filepath> [<filepath>]*\n");
        }
        else {
            WsiBSPValidator wsiBSPValidator = new WsiBSPValidator();
            for (int i = 0; i < args.length; i++) {
                String filePath = args[i];
                InputStream xmlIn = null;

                try {
                    xmlIn = new FileInputStream(filePath);
                    Document testDoc = XmlUtil.parse(xmlIn);

                    boolean valid = wsiBSPValidator.isValid(testDoc);
                    String message = null;
                    if(valid) {
                        message = "Document is valid '" + filePath + "'.";
                    }
                    else {
                        message = "Document is invalid '" + filePath + "'.";
                    }
                    System.out.println(message);
                }
                catch(FileNotFoundException fnfe) {
                    System.out.println("Document not found '" + filePath + "'.");
                }
                finally {
                    if(xmlIn!=null) try{xmlIn.close();}catch(IOException e){/*ignore*/};
                }
            }
        }
    }

    public WsiBSPValidator() {
        this(true);
    }

    /**
     *
     */
    public WsiBSPValidator(boolean allowNonStandardFunctions) {
        loadRules();
        XPathFactory xpf = XPathFactory.newInstance();
        xpf.setXPathFunctionResolver(getXPathFunctionResolver());
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(getNamespaceContext());
        xpes = new XPathExpression[rules.length];
        for (int i = 0; i < xpes.length; i++) {
            try {
                // hacked detection of XPaths with calls to non-standard functions
                if(allowNonStandardFunctions || !(rules[i].getXPath().indexOf("l7:")>=0)) {
                    XPathExpression xpe = xpath.compile(rules[i].getXPath());
                    xpes[i] = xpe;
                }
                else {
                    XPathExpression xpe = xpath.compile("0=0");
                    xpes[i] = xpe;
                }
            }
            catch(XPathExpressionException xpee) {
                xpee.printStackTrace();
            }
        }
    }

    /**
     *
     * @param document
     */
    public boolean isValid(Document document) {
        boolean valid = true;

        for(int i = 0; i < xpes.length; i++) {
            XPathExpression xpe = xpes[i];
            if(xpe!=null) {
                boolean passed = false;
                try {
                    passed = Boolean.parseBoolean(xpe.evaluate(document));
                }
                catch(XPathExpressionException xpee) {
                    xpee.printStackTrace();
                }
                if(!passed) {
                    System.err.println("WSI-BSP_FAILED: " + rules[i].getId() + ": " + rules[i].getDescription());
                    valid = false;
                }
            }
        }

        return valid;
    }

    //- PRIVATE

    /**
     * RULES resource
     */
    private static final String RESOURCE_RULES = "ServerWsiBspAssertion.rules.properties";

    /**
     * Prefix to name mappings
     */
    private String[][] prefixNamespaceMappings;

    /**
     * Uncompiled rules
     */
    private XPathRule[] rules;

    /**
     * Compiled expressions.
     */
    private final XPathExpression[] xpes;

    /**
     *
     */
    private void loadRules() {
        Properties rulesProps = new Properties();
        try {
            URL resourceUrl = WsiBSPValidator.class.getResource(RESOURCE_RULES);
            System.out.println(resourceUrl);
            rulesProps.load(resourceUrl.openStream());
        }
        catch(IOException ioe) {
            System.out.println("WARNING, could not load BSP rules! (resource "+RESOURCE_RULES+")");
            ioe.printStackTrace();
        }

        Map<String, String> namespaces = new HashMap();
        Set<XPathRule> rules = new HashSet();
        for (String name : (Collection<String>) Collections.list(rulesProps.propertyNames())) {
            if (name.startsWith("Namespace.")) {
                namespaces.put(name.substring(10), rulesProps.getProperty(name));
            }
            else if (name.endsWith(".path")) {
                String id = id = name.substring(0, name.length()-5);
                String path = rulesProps.getProperty(name);
                String desc = rulesProps.getProperty(name.replace("path", "rule"));
                rules.add(new XPathRule(id, path, desc));
            }
        }

        this.rules = rules.toArray(new XPathRule[rules.size()]);
        this.prefixNamespaceMappings = new String[namespaces.size()][];
        int i = 0;
        for (Map.Entry<String, String> entry : namespaces.entrySet()) {
            this.prefixNamespaceMappings[i++] = new String[]{ entry.getKey(), entry.getValue() };
        }
    }

    /**
     *
     */
    private String map(String match, int colFrom, int colTo) {
        for (int i = 0; i < prefixNamespaceMappings.length; i++) {
            String[] prefixNamespaceMapping = prefixNamespaceMappings[i];
            if(prefixNamespaceMapping[colFrom].equals(match)) return prefixNamespaceMapping[colTo];
        }

        return null;
    }

    /**
     * Custom XPath expressions.
     */
    private static XPathFunctionResolver getXPathFunctionResolver() {
        return new XPathFunctionResolver() {
            public XPathFunction resolveFunction(QName functionName, int arity) {

                /**
                 * Currently we only have one XPath function so we don't bother with name resolution ...
                 */
                return new XPathFunction() { // distinct element checker
                    public Object evaluate(List args) throws XPathFunctionException {
                        boolean result = false;

                        if(args!=null) {
                            Iterator iterator = args.iterator();
                            if (iterator.hasNext()) {
                                Object o =  iterator.next();
                                if(o instanceof NodeList) {
                                    NodeList nl = (NodeList) o;

                                    Set values = new HashSet();
                                    result = true;

                                    for(int l=0; l<nl.getLength(); l++) {
                                        Node node = nl.item(l);
                                        if(node instanceof Attr) {
                                            String value = ((Attr)node).getValue();
                                            if(values.contains(value)) {
                                                result = false;
                                                break;
                                            }
                                            else {
                                                values.add(value);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        return Boolean.valueOf(result);
                    }
                };
            }
        };
    }

    private NamespaceContext getNamespaceContext() {
        return new NamespaceContext(){
            public String getNamespaceURI(String prefix) {
                return map(prefix,0,1);
            }

            public String getPrefix(String namespaceURI) {
                return map(namespaceURI,0,1);
            }

            public Iterator getPrefixes(String namespaceURI) {
                return Collections.singletonList(getPrefix(namespaceURI)).iterator();
            }
        };
    }

//    private static String tokenCompare(String attr, String compareOp, String joinOp) {
//        StringBuffer tokBuffer = new StringBuffer();
//
//        for(int t=0; t<tokenUris.length; t++) {
//            String tokenUri = tokenUris[t];
//            if(t>0) {
//                tokBuffer.append(' ');
//                tokBuffer.append(joinOp);
//                tokBuffer.append(' ');
//            }
//            tokBuffer.append(attr);
//            tokBuffer.append(compareOp);
//            tokBuffer.append('\'');
//            tokBuffer.append(tokenUri);
//            tokBuffer.append('\'');
//        }
//
//        return tokBuffer.toString();
//    }

//    private static final String[][] prefixNamespaceMappings = {
//        { "soap", "http://schemas.xmlsoap.org/soap/envelope/" }
//    ,   { "xsd", "http://www.w3.org/2001/XMLSchema" }
//    ,   { "wsi", "http://www.ws-i.org/schemas/conformanceClaim" }
//    ,   { "ds", "http://www.w3.org/2000/09/xmldsig#" }
//    ,   { "xenc", "http://www.w3.org/2001/04/xmlenc#" }
//    ,   { "c14n", "http://www.w3.org/2001/10/xml-exc-c14n#" }
//    ,   { "wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd" }
//    ,   { "wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd" }
//    ,   { "b10", "http://www.ws-i.org/Profiles/Basic/2003-08/BasicProfile-1.0a.htm" }
//    ,   { "bp11", "http://www.ws-i.org/Profiles/BasicProfile-1.1.html" }
//    ,   { "l7", "http://www.layer7tech.com/"}
//    };
//
//    //7.1.1 Certificate Path Format ??
//    //7.1.2 Key Identifier for External References
//    //8.1.2 Signature Types
//
//    /**
//     * Known (accepted standard) token profile URIs
//     */
//    private static final String[] tokenUris = {
//            // UT
//            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#UsernameToken"
//
//            // X509
//        ,   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#PKCS7"
//        ,   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3"
//        ,   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1"
//        ,   "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier"
//
//            // SAML
//        ,   "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.0#SAMLAssertionID"
//
//            // REL
//        ,   "http://docs.oasis-open.org/wss/oasis-wss-rel-token-profile-1.0.pdf#license"
//
//            // Kerberos
//        ,   "http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-kerberos-token-profile-1.1#GSS_Kerberosv5_AP_REQ"
//    };
//
//    /*
//#Base64Binary               http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary
//#STR-Transform              http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform
//    */
//
//    /**
//     * The XPaths to test against (with the rules they match).
//     *
//     * TODO some XPaths only work when there's one reference ...
//     */
//    private static final XPathRule[] rules = {
//        /* Not actual WS-I BSP rules, but still required */
//        new XPathRule("E0001", "0=count(//*[namespace-uri()='http://www.w3.org/2001/06/soap-envelope' or namespace-uri()='http://www.w3.org/2001/09/soap-envelope' or namespace-uri()='http://www.w3.org/2003/05/soap-envelope' or namespace-uri()='urn:schemas-xmlsoap-org:soap.v1'])", "Invalid SOAP namespace")
//     ,  new XPathRule("E0002", "0=count(//*[namespace-uri()='http://schemas.xmlsoap.org/ws/2002/12/secext' or namespace-uri()='http://schemas.xmlsoap.org/ws/2002/07/secext' or namespace-uri()='http://schemas.xmlsoap.org/ws/2002/xx/secext' or namespace-uri()='http://schemas.xmlsoap.org/ws/2003/06/secext' or namespace-uri()='http://docs.oasis-open.org/wss/oasis-wss-wssecurity-secext-1.1.xsd'])", "Invalid WS-Security Namespace")
//     ,  new XPathRule("E0003", "0=count(//*[namespace-uri()='http://schemas.xmlsoap.org/ws/2002/07/utility' or namespace-uri()='http://schemas.xmlsoap.org/ws/2003/06/utility'])", "Invalid Utility Namespace")
//
//        /* 5.1 Security Tokens */
//    ,   new XPathRule("R3029", "0=count(//wsse:BinarySecurityToken[not(@EncodingType)])", "A SECURITY_TOKEN named wsse:BinarySecurityToken MUST specify an EncodingType attribute.")
//    ,   new XPathRule("R3030", "0=count(//wsse:BinarySecurityToken[@EncodingType!='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary'])", "An EncodingType attribute of a SECURITY_TOKEN named wsse:BinarySecurityToken MUST have a value of \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\".")
//    ,   new XPathRule("R3031", "0=count(//wsse:BinarySecurityToken[not(@ValueType)])", "A SECURITY_TOKEN named wsse:BinarySecurityToken MUST specify a ValueType attribute.")
//    ,   new XPathRule("R3032", "0=count(//wsse:BinarySecurityToken["+tokenCompare("@ValueType", "!=", "and")+"])", "A ValueType attribute of a SECURITY_TOKEN named wsse:BinarySecurityToken MUST have a value specified by the related security token profile.")
////    ,   new XPathRule("R3033", "//wsse:BinarySecurityToken//", "A SECURITY_TOKEN named wsse:BinarySecurityToken containing a single X.509 Certificate MUST specify a ValueType attribute with the value \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\".")
//
//        /* 5.2 SecurityTokenReferences */
////    ,   new XPathRule("R3022", "", "When a SECURITY_TOKEN_REFERENCE references an INTERNAL_SECURITY_TOKEN which has a wsu:Id attribute, the reference MUST use either a Direct Reference or an Embedded Reference.")
////    ,   new XPathRule("R5204", "", "When a SECURITY_TOKEN_REFERENCE uses a Direct Reference to an INTERNAL_SECURITY_TOKEN, it MUST use a Shorthand XPointer Reference.")
////    ,   new XPathRule("R5205", "", "An INTERNAL_SECURITY_TOKEN that is not an embedded security token MUST precede the first SECURITY_TOKEN_REFERENCE that references it.")
////    ,   new XPathRule("R3023", "", "Each SECURITY_TOKEN_REFERENCE that refers to an INTERNAL_SECURITY_TOKEN that is referenced several times SHOULD be a direct reference rather than an embedded reference.")
////    ,   new XPathRule("R3024", "", "When a SECURITY_TOKEN_REFERENCE references an EXTERNAL_SECURITY_TOKEN that can be referred to using a Direct Reference, a Direct Reference MUST be used.")
////    ,   new XPathRule("R3025", "", "When a wsse:Embedded element in a SECURITY_TOKEN_REFERENCE is used to specify an INTERNAL_SECURITY_TOKEN, its format MUST be the same as if the token were a child of a SECURITY_HEADER.")
//    ,   new XPathRule("R3027", "0=count(//wsse:SecurityTokenReference/ds:KeyName)", "A SECURITY_TOKEN_REFERENCE MUST NOT use a Key Name to reference a SECURITY_TOKEN.")
//    ,   new XPathRule("R3054", "0=count(//wsse:SecurityTokenReference//wsse:KeyIdentifier[not(@ValueType)])", "A wsse:KeyIdentifier element in a SECURITY_TOKEN_REFERENCE MUST specify a ValueType attribute.")
//    ,   new XPathRule("R3063", "0=count(//wsse:SecurityTokenReference//wsse:KeyIdentifier["+tokenCompare("@ValueType", "!=", "and")+"])", "A ValueType attribute on a wsse:KeyIdentifier element in a SECURITY_TOKEN_REFERENCE MUST contain a value specified within the security token profile associated with the referenced SECURITY_TOKEN.")
//    ,   new XPathRule("R3070", "0=count(//wsse:SecurityTokenReference//wsse:KeyIdentifier[not(@EncodingType)])", "A wsse:KeyIdentifier element in a SECURITY_TOKEN_REFERENCE MUST specify an EncodingType attribute.")
//    ,   new XPathRule("R3071", "0=count(//wsse:SecurityTokenReference//wsse:KeyIdentifier[@EncodingType!='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary'])", "An EncodingType attribute on a wsse:KeyIdentifier element in a SECURITY_TOKEN_REFERENCE MUST have a value of \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\".")
//    ,   new XPathRule("R3055", "0=count(//wsse:Embedded/wsse:SecurityTokenReference)", "A wsse:Embedded element in a SECURE_ENVELOPE MUST NOT contain a wsse:SecurityTokenReference child element.")
//          // also what are the defined tokens ??
//    ,   new XPathRule("R3060", "0=count(//wsse:Embedded[count(*)!=1])", "A wsse:Embedded element in a SECURE_ENVELOPE MUST contain a single child element containing a security token defined in a security token profile.")
////    ,   new XPathRule("R3056", "", "A SECURITY_TOKEN_REFERENCE MUST NOT use a Direct Reference to another SECURITY_TOKEN_REFERENCE that does not have a wsse:Embedded child element.")
////    ,   new XPathRule("R3064", "", "When a SECURITY_TOKEN_REFERENCE uses a Direct Reference to an INTERNAL_SECURITY_TOKEN contained within an wsse:Embedded element of another SECURITY_TOKEN_REFERENCE, the reference MUST be to the contained INTERNAL_SECURITY_TOKEN not to the wsse:Embedded element.")
//    ,   new XPathRule("R3059", "0=count(//wsse:SecurityTokenReference//wsse:Reference[not(@ValueType)])", "A wsse:Reference element in a SECURITY_TOKEN_REFERENCE MUST specify a ValueType attribute.")
//        // what are the token profile value types??
//    ,   new XPathRule("R3058", "0=count(//wsse:SecurityTokenReference//wsse:Reference["+tokenCompare("@ValueType", "!=", "and")+"])", "A wsse:Reference element in a SECURITY_TOKEN_REFERENCE MUST specify a ValueType attribute that matches a ValueType specified by a security token profile for the the referenced SECURITY_TOKEN.")
//    ,   new XPathRule("R3061", "0=count(//wsse:SecurityTokenReference[count(*)!=1])", "A SECURITY_TOKEN_REFERENCE MUST have exactly one child element.")
//    ,   new XPathRule("R3062", "0=count(//wsse:SecurityTokenReference//wsse:Reference[not(@URI)])", "A wsse:Reference element in a SECURITY_TOKEN_REFERENCE MUST specify a URI attribute.")
////    ,   new XPathRule("R3065", "", "When a SIGNATURE uses the SecurityTokenReference Dereferencing Transform, the ds:CanonicalizationMethod element MUST be present and wrapped in a wsse:TransformationParameters element.")
////    ,   new XPathRule("R3066", "", "A SECURITY_TOKEN_REFERENCE MUST NOT use a Shorthand XPointer Reference to refer to an INTERNAL_SECURITY_TOKEN located in another SECURITY_HEADER.")
//        /* 5.3 Timestamps */
//    ,   new XPathRule("R3203", "0=count(//wsu:Timestamp[count(wsu:Created)!=1])", "A TIMESTAMP MUST have exactly one wsu:Created element child.")
//    ,   new XPathRule("R3224", "0=count(//wsu:Timestamp[count(wsu:Expires) > 1])", "A TIMESTAMP MUST NOT contain more than one wsu:Expires element.")
//    ,   new XPathRule("R3221", "0=count(//wsu:Timestamp/wsu:Created/preceding-sibling::wsu:Expires)", "If a TIMESTAMP contains a wsu:Expires element it MUST appear after the wsu:Created element.")
////    ,   new XPathRule("R3213", "", "A TIMESTAMP MUST NOT include wsu:Created or wsu:Expires values that specify leap seconds.")
//    ,   new XPathRule("R3225", "0=count(//wsu:Timestamp//wsu:Created[@ValueType])", "A wsu:Created element within a TIMESTAMP MUST NOT include a ValueType attribute.")
//    ,   new XPathRule("R3226", "0=count(//wsu:Timestamp//wsu:Expires[@ValueType])", "A wsu:Expires element within a TIMESTAMP MUST NOT include a ValueType attribute.")
////    ,   new XPathRule("R3217", "", "A TIMESTAMP MUST only contain time values in UTC format as specified by the XML Schema type (dateTime).")
////    ,   new XPathRule("R3220", "", "A RECEIVER MUST be capable of processing wsu:Created and wsu:Expires values upto and including milliseconds.")
//    ,   new XPathRule("R3218", "0=count(//wsse:Security/*//wsu:Timestamp)", "A SECURITY_HEADER MUST NOT contain a wsu:timestamp that is not its immediate child.")
//    ,   new XPathRule("R3219", "0=count(//wsse:Security/wsu:Timestamp[position()>1])", "A SECURITY_HEADER MUST NOT contain more than one TIMESTAMP.")
//        /* 5.4 wsu:Id References */
//    ,   new XPathRule("R3204", "l7:distinct(//@wsu:Id)", "Two wsu:Id attributes within any SECURE_ENVELOPE MUST NOT have the same value.")
//        /* 5.5 wsse:Security Processing Order */
////    ,   new XPathRule("R3212", "", "Within a SECURITY_HEADER, all SIGNATURE, ENCRYPTED_KEY, and ENCRYPTION_REFERENCE_LIST elements MUST be ordered so a receiver will get the correct result by processing the elements in the order they appear.")
//        /* 5.6 SOAP Actor */
//    ,   new XPathRule("R3206", "1>=count(//wsse:Security[not(@soap:Actor)])", "Within a SECURE_ENVELOPE there MUST be at most one SECURITY_HEADER with the actor attribute omitted.")
//    ,   new XPathRule("R3210", "l7:distinct(//wsse:Security/@Actor)", "Within a SECURE_ENVELOPE there MUST NOT be more than one SECURITY_HEADER with the same actor attribute value.")
//        /* 6.1 Token Usage */
//    ,   new XPathRule("R4201", "0=count(//wsse:Security//wsse:UsernameToken/wsse:Password[not(@Type)])", "A wsse:UsernameToken/wsse:Password element in a SECURITY_HEADER MUST specify a Type attribute.")
////    ,   new XPathRule("R4212", "", "When a wsse:Password element with a Type attribute with a value of \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest\" is used within a SECURITY_TOKEN, its value MUST be computed using the following formula, where \"+\" indicates concatenation: Password_Digest = Base64 ( SHA-1 ( nonce + created + password ) ). That is, concatenate the text forms of the nonce, creation timestamp, and the password (or shared secret or password equivalent), digest the combination using the SHA-1 hash algorithm, then include the Base64 encoding of that result as the password (digest). Any elements that are not present are simply omitted from the concatenation.")
////    ,   new XPathRule("R4214", "", "Any SECURITY_TOKEN_REFERENCE which directly references a SECURITY_TOKEN named wsse:UsernameToken MUST have a wsse:Reference/@ValueType attribute with a value of \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#UsernameToken\".")
//    ,   new XPathRule("R4220", "0=count(//wsse:Security//wsse:UsernameToken[not(@EncodingType)])", "A SECURITY_TOKEN named wsse:UsernameToken MUST specify an EncodingType attribute.")
//    ,   new XPathRule("R4221", "0=count(//wsse:Security//wsse:UsernameToken[@EncodingType!='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary'])", "An EncodingType attribute of a SECURITY_TOKEN named wsse:UsernameToken MUST have a value of \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\".")
////    ,   new XPathRule("R4215", "", "When a SECURITY_TOKEN_REFERENCE refers to a wsse:UsernameToken, a KeyIdentifier reference MUST NOT be used.")
//        /* 7.1 Token Types */
////    ,   new XPathRule("R5201", "", "When certificate path information is provided, a SENDER MUST provide one of the X509PKIPathv1 or PKCS7 token types.")
////    ,   new XPathRule("R5202", "", "When certificate path information is provided, a SENDER SHOULD provide the X509PKIPathv1 token type.")
////    ,   new XPathRule("R5203", "", "When certificate path information is provided, a RECEIVER MUST accept X509PKIPathv1 and PKCS7 token types.")
////    ,   new XPathRule("R5209", "", "When a SECURITY_TOKEN_REFERENCE references an EXTERNAL_SECURITY_TOKEN that cannot be referred to using a Direct Reference but can be referred to using a Key Identifier or ds:X509Data/ds:X509IssuerSerial, a Key Identifier or ds:X509Data/ds:X509IssuerSerial MUST be used.")
//    ,   new XPathRule("R5206", "0=count(//wsse:SecurityTokenReference/wsse:KeyIdentifier[@ValueType!='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier'])", "When the wsse:KeyIdentifier element is used within a SECURITY_TOKEN_REFERENCE to specify a reference to an X.509 Certificate Token, the wsse:KeyIdentifier element MUST have a ValueType attribute with the value \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\"")
////    ,   new XPathRule("R5208", "", "When the wsse:KeyIdentifier element is used within a SECURITY_TOKEN_REFERENCE to specify a reference to an X.509 Certificate Token its contents MUST be the value of the certificate's X.509 SubjectKeyIdentifier extension.")
//    ,   new XPathRule("R5207", "0=count(//wsse:SecurityTokenReference/wsse:KeyIdentifier[@ValueType='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier' and @EncodingType!='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary'])", "")
//        /* 8.1 General Constraints on XML Signature */
////    ,   new XPathRule("R3105", "", "SENDERs and RECEIVERs can agree in out of band fashion on required and allowed signed message content.")
////    ,   new XPathRule("R3102", "", "A SIGNATURE MUST NOT be an Enveloping Signature as defined by the XML Signature specification.")
////    ,   new XPathRule("R3103", "", "A SIGNATURE SHOULD be a Detached Signature as defined by the XML Signature specification.")
////    ,   new XPathRule("R3104", "", "A SIGNATURE SHOULD NOT be an Enveloped Signature as defined by the XML Signature Specification.")
//        /* 8.2 Element References in XML Signature */
////TODO only works when there's one reference
////    ,   new XPathRule("R3001", "count(//ds:Signature//ds:Reference)=count(//*[@wsu:Id=(substring(//ds:Signature//ds:Reference/@URI,2,1000)) or @id=(substring(//ds:Signature//ds:Reference/@URI,2,1000))])", "When a ds:Reference in a SIGNATURE refers to an element that carries a wsu:Id attribute or a Local ID attribute defined by XML Signature or XML Encryption, a Shorthand XPointer Reference MUST be used to refer to that element.")
//    ,   new XPathRule("R3003", "0=count(//*[@wsu:Id=(substring(//ds:Signature//ds:Reference/@URI,2,1000)) and namespace-uri(.)='http://www.w3.org/2000/09/xmldsig#'])", "When a ds:Reference/@URI in a SIGNATURE is a Shorthand XPointer Reference to an XML Signature element, the reference value MUST be a Local ID defined by XML Signature.")
//    ,   new XPathRule("R3004", "0=count(//*[@wsu:Id=(substring(//ds:Signature//ds:Reference/@URI,2,1000)) and namespace-uri(.)='http://www.w3.org/2001/04/xmlenc#'])", "When a ds:Reference/@URI in a SIGNATURE is a Shorthand XPointer Reference to an XML Encryption element, the reference value MUST be a Local ID defined by XML Encryption.")
////    ,   new XPathRule("R3005", "", "When a ds:Reference/@URI in a SIGNATURE is a Shorthand XPointer Reference to an element not defined by XML Signature or XML Encryption, the reference value SHOULD be a wsu:Id.")
//    ,   new XPathRule("R3002", "0=count(//ds:Signature//ds:Reference/ds:Transform[@Algorithm!='http://www.w3.org/2002/06/xmldsig-filter2'])", "When referring to an element in a SECURE_ENVELOPE that does NOT carry an attribute of type ID from a ds:Reference in a SIGNATURE the XPath Filter 2.0 Transform (http://www.w3.org/2002/06/xmldsig-filter2) MUST be used to refer to that element.")
//        /* 8.3 XML Signature Algorithms */
//    ,   new XPathRule("R5404", "0=count(//ds:Signature//ds:CanonicalizationMethod[@Algorithm!='http://www.w3.org/2001/10/xml-exc-c14n#'])", "Any ds:CanonicalizationMethod/@Algorithm attribute in a SIGNATURE MUST have a value of \"http://www.w3.org/2001/10/xml-exc-c14n#\" indicating that is uses Exclusive C14N without comments for canonicalization.")
//
//
////    ,   new XPathRule("R5406", "count(//ds:Signature//ds:CanonicalizationMethod[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#'])=count(//ds:Signature//ds:CanonicalizationMethod[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#']/c14n:InclusiveNamespaces)", "Any ds:CanonicalizationMethod element within a SIGNATURE that has an @Algorithm attribute whose value is \"http://www.w3.org/2001/10/xml-exc-c14n#\" MUST have a c14n:InclusiveNamespaces child element with an @PrefixList attribute unless the PrefixList is empty.")
//    ,   new XPathRule("R5407", "count(/ds:Signature//ds:Transform[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#'])=count(/ds:Signature//ds:Transform[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#']/c14n:InclusiveNamespaces)", "Any ds:Transform element within a SIGNATURE that has an @Algorithm attribute whose value is \"http://www.w3.org/2001/10/xml-exc-c14n#\" MUST have a c14n:InclusiveNamespaces child element with an @PrefixList attribute unless the PrefixList is empty.")
//    ,   new XPathRule("R5409", "0=count(//ds:Signature//ds:CanonicalizationMethod[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#']/c14n:InclusiveNamespaces[@PrefixList=''])", "Any ds:CanonicalizationMethod element within a SIGNATURE that has an @Algorithm attribute whose value is \"http://www.w3.org/2001/10/xml-exc-c14n#\" MUST have an c14n:InclusiveNamespaces child element without an @PrefixList attribute if the PrefixList would be empty.")
//    ,   new XPathRule("R5410", "0=count(//ds:Signature//ds:Transform[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#']/c14n:InclusiveNamespaces[@PrefixList=''])", "Any ds:Transform element within a SIGNATURE that has an @Algorithm attribute whose value is \"http://www.w3.org/2001/10/xml-exc-c14n#\" MUST have a c14n:InclusiveNamespaces child element without an @PrefixList attribute if the PrefixList is would be empty.")
////    ,   new XPathRule("R5405", "", "Any c14n:InclusiveNamespaces/@PrefixList attribute within a SIGNATURE MUST contain the prefix of all in-scope namespaces for the element being signed and its descendants that are not visibly utilized, per Exclusive XML Canonicalization Version 1.0.")
////    ,   new XPathRule("R5408", "0=count(//ds:Signature//c14n:InclusiveNamespaces[contains(@PrefixList,'#default')!=(count(//*[(@wsu:Id=(substring(//ds:Signature//ds:Reference/@URI,2,1000)) or @id=(substring(//ds:Signature//ds:Reference/@URI,2,1000)))]/descendant-or-self::*[namespace-uri(.)='']) > 0)])", "Any c14n:InclusiveNamespaces/@PrefixList attribute within a SIGNATURE MUST contain the string \"#default\" if a default namespace is in-scope for the element being signed but is not visibly utilized, per Exclusive XML Canonicalization Version 1.0.")
//    ,   new XPathRule("R5413", "count(//ds:Transform[@Algorithm='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform'])=count(//ds:Transform[@Algorithm='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform']/wsse:TransformationParameters/ds:CanonicalizationMethod/c14n:InclusiveNamespaces)", "When the STR-Transform is used the ds:Transform/wsse:TransformationParameters/ds:CanonicalizationMethod/c14n:InclusiveNamespaces element MUST be provided.")
////    ,   new XPathRule("R5414", "", "A RECEIVER MUST be capable of accepting and processing a ec:InclusiveNamespaces/@PrefixList containing prefixes in any order within the string.")
////    ,   new XPathRule("R5415", "", "A RECEIVER MUST be capable of accepting and processing a ec:InclusiveNamespaces/@PrefixList containing arbitrary whitespace before, after and between the prefixes within the string.")
//    ,   new XPathRule("R5410", "count(//ds:Signature//ds:Reference)=count(//ds:Signature//ds:Reference/ds:Transforms)", "Any ds:Reference element in a SIGNATURE MUST have a ds:Transforms child element.")
//    ,   new XPathRule("R5411", "count(//ds:Signature//ds:Transforms/ds:Transform[1])=count(//ds:Signature//ds:Transforms)", "Any ds:Transforms element in a SIGNATURE MUST have at least one ds:Transform child element.")
//    ,   new XPathRule("R5412", "count(//ds:Signature//ds:Transforms)=count(//ds:Signature//ds:Transforms/*[position()=last()]/self::ds:Transform[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#' or @Algorithm='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform' or @Algorithm='http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Signature-Transform' or @Algorithm='http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Signature-Transform'])", "Any ds:Transforms element in a SIGNATURE MUST have as its last child a ds:Transform element which specifies \"http://www.w3.org/2001/10/xml-exc-c14n#\" or \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform\" or \"http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Signature-Transform\" or \"http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Signature-Transform\"")
//    ,   new XPathRule("R5420", "0=count(//ds:Signature//ds:DigestMethod[@Algorithm!='http://www.w3.org/2000/09/xmldsig#sha1'])", "Any ds:DigestMethod/@Algorithm element in a SIGNATURE MUST have the value \"http://www.w3.org/2000/09/xmldsig#sha1\"")
//    ,   new XPathRule("R5421", "0=count(//ds:Signature//ds:SignatureMethod[@Algorithm!='http://www.w3.org/2000/09/xmldsig#hmac-sha1' and @Algorithm!='http://www.w3.org/2000/09/xmldsig#rsa-sha1'])", "Any ds:SignatureMethod/@Algorithm element in a SIGNATURE MUST have a value of \"http://www.w3.org/2000/09/xmldsig#hmac-sha1\" or \"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"")
//    ,   new XPathRule("R5423", "0=count(//ds:Signature//ds:Transform[@Algorithm!='http://www.w3.org/2001/10/xml-exc-c14n#' and @Algorithm!='http://www.w3.org/2002/06/xmldsig-filter2' and @Algorithm!='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform' and @Algorithm!='http://www.w3.org/2000/09/xmldsig#enveloped-signature' and @Algorithm!='http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Signature-Transform' and @Algorithm!='http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Signature-Transform'])", "Any ds:Transform/@Algorithm attribute in a SIGNATURE MUST have a value of \"http://www.w3.org/2001/10/xml-exc-c14n#\" or \"http://www.w3.org/2002/06/xmldsig-filter2\" or \"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#STR-Transform\" or \"http://www.w3.org/2000/09/xmldsig#enveloped-signature\" or \"http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Content-Signature-Transform\" or \"http://docs.oasis-open.org/wss/2004/XX/oasis-2004XX-wss-swa-profile-1.0#Attachment-Complete-Signature-Transform\"")
//        /* 8.4 XML Signature Syntax */
//    ,   new XPathRule("R5401", "0=count(//ds:Signature//ds:HMACOutputLength)", "The ds:HMACOutputLength element MUST NOT appear in a SIGNATURE.")
//    ,   new XPathRule("R5402", "0=count(//ds:Signature//ds:KeyInfo[count(*)!=1])", "A ds:KeyInfo element in a SIGNATURE MUST have exactly one child element.")
//    ,   new XPathRule("R5409", "0=count(//ds:Signature//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])", "The child element of a ds:KeyInfo element in a SIGNATURE MUST be a SECURITY_TOKEN_REFERENCE element.")
//    ,   new XPathRule("R5403", "0=count(//ds:Signature//ds:Manifest)", "A SIGNATURE MUST NOT contain a ds:Manifest element.")
//    ,   new XPathRule("R5440", "0=count(//ds:Signature//xenc:EncryptedData)", "A SIGNATURE MUST NOT have any xenc:EncryptedData elements amongst its descendants.")
//
//        /* 9.1 XML Encryption Processing Model */
//    //,   new XPathRule("R3205", "", "Each ENCRYPTION_REFERENCE_LIST produced as part of an encryption step MUST use a single key.")
//    //,   new XPathRule("R3215", "", "An ENCRYPTION_REFERENCE_LIST MUST contain an xenc:DataReference element for each ENCRYPTED_DATA produced in the associated encryption step.")
//    //,   new XPathRule("R3214", "", "An ENCRYPTED_KEY_REFERENCE_LIST MUST contain a xenc:DataReference for each ENCRYPTED_DATA produced in the associated encryption step.")
//    //,   new XPathRule("R3208", "", "An ENCRYPTED_KEY MUST precede any ENCRYPTED_DATA in the same SECURITY_HEADER referenced by the associated ENCRYPTED_KEY_REFERENCE_LIST.")
//        /* 9.2 XML Encryption Syntax */
//    //,   new XPathRule("R3216", "", "Any ENCRYPTED_KEY that is used in an encryption step MUST contain a ENCRYPTED_KEY_REFERENCE_LIST.")
//    ,   new XPathRule("R3209", "0=count(//xenc:EncryptedKey[@Type])", "Any ENCRYPTED_KEY MUST NOT specify a Type attribute.")
//    ,   new XPathRule("R5622", "0=count(//xenc:EncryptedKey[@MimeType])", "Any ENCRYPTED_KEY MUST NOT specify a MimeType attribute.")
//    ,   new XPathRule("R5623", "0=count(//xenc:EncryptedKey[@Encoding])", "Any ENCRYPTED_KEY MUST NOT specify a Encoding attribute.")
//    //,   new XPathRule("R5629", "count(//xenc:EncryptedData[])=count(//xenc:EncryptedData[]/ds:KeyInfo[1])", "An ENCRYPTED_DATA which is not referenced from an ENCRYPTED_KEY MUST contain a ds:KeyInfo")
//    ,   new XPathRule("R5624", "0=count(//xenc:EncryptedData[not(@Id)])", "Any ENCRYPTED_DATA MUST have an Id attribute.")
//    //,   new XPathRule("R3211", "0=count(//xenc:EncryptedData//wsse:SecurityTokenReference/wsse:Reference[substring(@URI,2,1000)=//ds:KeyInfo/@Id > 0])", "Any SECURITY_TOKEN_REFERENCE inside an ENCRYPTED_DATA MUST NOT reference a ds:KeyInfo element.")
//    ,   new XPathRule("R5601", "count(//xenc:EncryptedData)=count(//xenc:EncryptedData/xenc:EncryptionMethod[1])", "Any ENCRYPTED_DATA MUST have an xenc:EncryptionMethod child element.")
//    ,   new XPathRule("R5603", "count(//xenc:EncryptedKey)=count(//xenc:EncryptedKey/xenc:EncryptionMethod[1])", "Any ENCRYPTED_KEY MUST have an xenc:EncryptionMethod child element.")
//    ,   new XPathRule("R5602", "0=count(//xenc:EncryptedKey[@Recipient])", "Any ENCRYPTED_KEY MUST NOT contain a Recipient attribute.")
//    ,   new XPathRule("R5424", "0=count(//xenc:EncryptedKey//ds:KeyInfo[count(*)!=1])", "A ds:KeyInfo element in an ENCRYPTED_KEY MUST have exactly one child element.")
//    ,   new XPathRule("R5425", "0=count(//xenc:EncryptedData//ds:KeyInfo[count(*)!=1])", "A ds:KeyInfo element in an ENCRYPTED_DATA MUST have exactly one child element.")
//    ,   new XPathRule("R5426", "0=count(//xenc:EncryptedKey//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])", "The child element of a ds:KeyInfo element in an ENCRYPTED_KEY MUST be a SECURITY_TOKEN_REFERENCE.")
//    ,   new XPathRule("R5427", "0=count(//xenc:EncryptedData//ds:KeyInfo[count(wsse:SecurityTokenReference)!=1])", "The child element of a ds:KeyInfo element in an ENCRYPTED_DATA MUST be a SECURITY_TOKEN_REFERENCE.")
//    //,   new XPathRule("R5606", "", "Within a SECURE_ENVELOPE, encrypted element or element content encrypted as a result of an encryption step MUST be replaced by a corresponding ENCRYPTED_DATA.")
//    ,   new XPathRule("R5607", "count(/soap:Envelope/wsse:Security/xenc:EncryptedKey//xenc:ReferenceList)=0 or (1=count(/soap:Envelope) and 1=count(/soap:Envelope/soap:Header) and 1=count(/soap:Envelope/soap:Body))", "When encryption is used, the SECURE_ENVELOPE MUST still be a valid SOAP Envelope. Specifically, the Envelope, Header, or Body elements MUST NOT be encrypted.")
//    /* Section 9.3 Element References in XML Encryption */
//    //,   new XPathRule("R5608", "", "When referring from an xenc:DataReference or xenc:KeyReference in an ENCRYPTION_REFERENCE_LIST to an element that carries a wsu:Id attribute or a Local ID attribute defined by either XML Signature or XML Encryption, a Shorthand XPointer Reference MUST be used to refer to that element.")
//    //,   new XPathRule("R5613", "", "When referring from a an xenc:DataReference or xenc:KeyReference in an ENCRYPTED_KEY_REFERENCE_LIST to an element that carries a wsu:Id attribute or a Local ID attribute defined by either XML Signature or XML Encryption, a Shorthand XPointer Reference MUST be used to refer to that element.")
//    //,   new XPathRule("R3006", "", "When a xenc:DataReference/@URI or xenc:KeyReference/@URI in a ENCRYPTION_REFERENCE_LIST is a Shorthand XPointer Reference to an XML Signature element, the reference value MUST be a Local ID defined by XML Signature.")
//    //,   new XPathRule("R3007", "", "When a xenc:DataReference/@URI or xenc:KeyReference in a ENCRYPTED_KEY_REFERENCE_LIST is a Shorthand XPointer Reference to an XML Signature element, the reference value MUST be a Local ID defined by XML Signature.")
//    //,   new XPathRule("R5609", "", "When an xenc:DataReference/@URI or xenc:KeyReference/@URI in an ENCRYPTION_REFERENCE_LIST is a Shorthand XPointer Reference to an XML Encryption element, the reference value MUST be a Local ID defined by XML Encryption.")
//    //,   new XPathRule("R5610", "", "When an xenc:DataReference/@URI or xenc:KeyReference/@URI in an ENCRYPTED_KEY_REFERENCE_LIST is a Shorthand XPointer Reference to an XML Encryption element, the reference value MUST be a Local ID defined by XML Encryption.")
//    //,   new XPathRule("R5611", "", "When an xenc:DataReference/@URI or xenc:KeyReference/@URI in an ENCRYPTION_REFERENCE_LIST is a Shorthand XPointer Reference to an element not defined by XML Signature or XML Encryption, the reference value SHOULD be a wsu:Id.")
//    //,   new XPathRule("R5612", "", "When an xenc:DataReference/@URI or xenc:KeyReference/@URI in an ENCRYPTED_KEY_REFERENCE_LIST is a Shorthand XPointer Reference to an element not defined by XML Signature or XML Encryption, the reference value SHOULD be a wsu:Id.")
//    /* 9.4 XML Encryption Algorithms */
//    ,   new XPathRule("R5620", "0=count(//xenc:EncryptedData//xenc:EncryptionMethod[@Algorithm!='http://www.w3.org/2001/04/xmlenc#tripledes-cbc' and @Algorithm!='http://www.w3.org/2001/04/xmlenc#aes128-cbc' and @Algorithm!='http://www.w3.org/2001/04/xmlenc#aes256-cbc'])", "Any xenc:EncryptionMethod/@Algorithm attribute in an ENCRYPTED_DATA MUST have a value of \"http://www.w3.org/2001/04/xmlenc#tripledes-cbc\", \"http://www.w3.org/2001/04/xmlenc#aes128-cbc\" or \"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"")
//    // for rules below how do we know whether key transport or key wrapping is in effect?
//    //,   new XPathRule("R5621", "0=count(//xenc:EncryptedKey//xenc:EncryptionMethod[@Algorithm!='http://www.w3.org/2001/04/xmlenc#rsa-1_5' and @Algorithm!='http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p'])", "When used for Key Transport, any xenc:EncryptionMethod/@Algorithm attribute in an ENCRYPTED_KEY MUST have a value of \"http://www.w3.org/2001/04/xmlenc#rsa-1_5\" or \"http://www.w3.org/2001/04/xmlenc#rsa-oaep-mgf1p\"")
//    //,   new XPathRule("R5625", "0=count(//xenc:EncryptedKey//xenc:EncryptionMethod[@Algorithm!='http://www.w3.org/2001/04/xmlenc#kw-tripledes' and @Algorithm!='http://www.w3.org/2001/04/xmlenc#kw-aes128' and @Algorithm!='http://www.w3.org/2001/04/xmlenc#kw-aes256')", "When used for Key Wrap, any xenc:EncryptionMethod/@Algorithm attribute in an ENCRYPTED_KEY MUST have a value of \"http://www.w3.org/2001/04/xmlenc#kw-tripledes\", \"http://www.w3.org/2001/04/xmlenc#kw-aes128\", or \"http://www.w3.org/2001/04/xmlenc#kw-aes256\"")
//    };

    /**
     * XPath data object
     */
    private static final class XPathRule
    {
        private final String name;
        private final String path;
        private final String description;

        private XPathRule(String name, String path, String description) {
            this.name = name;
            this.path = path;
            this.description = description;
        }

        private String getId() {
            return name;
        }

        private String getXPath() {
            return path;
        }

        private String getDescription() {
            return description;
        }
    }
}
