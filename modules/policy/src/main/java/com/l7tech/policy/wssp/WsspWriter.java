/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.PrimitiveAssertion;
import org.apache.ws.policy.util.PolicyFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a layer 7 policy into a WS-SecurityPolicy tree.
 *
 * <p>The currently supported assertions are:</p>
 *
 * <ul>
 *   <li>TLS with or without client certificate.</li>
 *   <li>WSS Username Token</li>
 *   <li>WSS Digest</li>
 *   <li>WSS Timestamp</li>
 *   <li>WSS Signature</li>
 *   <li>WSS Integrity (Header / Body only)</li>
 *   <li>WSS Confidentiality (Header / Body only)</li>
 * <ul>
 *
 * <p>Currently all generated policies will use the Lax layout. The algorithm
 * suite defaults to Basic256Rsa15 but will use whatever is specififed in an
 * Integrity assertion (note that if ANY integrity assertion uses a non-default
 * algorithm then ALL assertions MUST use the same algorithm).</p>
 *
 * <p>Note that that ONLY signing/encryption of the Body is supported.</p>
 */
@SuppressWarnings({"JavaDoc"})
public class WsspWriter {

    //- PUBLIC

    /**
     * Decorate the given WSDL using the given Layer 7 policy.
     *
     * @param wsdl the wsdl DOM
     * @param layer7Root the layer 7 policy tree to use. Must not be null.
     * @param overrideBasePolicyXml   Optional.  if specified, will be used in place of base policy XML.
     * @param overrideInputPolicyXml  Optional.  if specified, will be used in place of input policy XML.
     * @param overrideOutputPolicyXml Optional.  if specified, will be used in place of output policy XML.
     * @throws PolicyAssertionException if the policy cannot be converted to WS-SP
     * @throws SAXException if specified override XML is not well formed
     * @throws InvalidDocumentFormatException if the document contains duplicate ID attributes.
     */
    public static void decorate(Document wsdl, Assertion layer7Root, boolean isWss11, String overrideBasePolicyXml, String overrideInputPolicyXml, String overrideOutputPolicyXml) throws PolicyAssertionException, SAXException, InvalidDocumentFormatException {
        addPreferredNamespacesIfAvailable(wsdl.getDocumentElement());
        WsspWriter wsspWriter = new WsspWriter();

        final String wsspId;
        final Element wsspElement;
        if (overrideBasePolicyXml == null) {
            Policy wssp = wsspWriter.convertFromLayer7(layer7Root, isWss11);
            wssp.setId(SoapUtil.generateUniqueId("policy", 1));
            wsspElement = toElement(wsdl, wssp);
            wsspId = wssp.getId();
        } else {
            wsspElement = toElement(wsdl, overrideBasePolicyXml);
            wsspId = SoapUtil.getOrCreateElementWsuId(wsspElement);
        }

        final String inputWsspId;
        final Element inputWsspElement;
        if (overrideInputPolicyXml == null) {
            Policy inputWssp = wsspWriter.convertFromLayer7(layer7Root, isWss11, true);
            inputWssp.setId( SoapUtil.generateUniqueId("policy", 2));
            inputWsspElement = toElement(wsdl, inputWssp);
            inputWsspId = inputWssp.getId();
        } else {
            inputWsspElement = toElement(wsdl, overrideInputPolicyXml);
            inputWsspId = SoapUtil.getOrCreateElementWsuId(inputWsspElement);
        }

        final String outputWsspId;
        final Element outputWsspElement;
        if (overrideOutputPolicyXml == null) {
            Policy outputWssp = wsspWriter.convertFromLayer7(layer7Root, isWss11, false);
            outputWssp.setId(SoapUtil.generateUniqueId("policy", 3));
            outputWsspElement = toElement(wsdl, outputWssp);
            outputWsspId = outputWssp.getId();
        } else {
            outputWsspElement = toElement(wsdl, overrideOutputPolicyXml);
            outputWsspId = SoapUtil.getOrCreateElementWsuId(outputWsspElement);
        }

        // add in reverse order
        Element wsdlDocEle = wsdl.getDocumentElement();
        if (outputWsspElement.hasChildNodes()) wsdlDocEle.insertBefore(outputWsspElement, wsdlDocEle.getFirstChild());
        if (inputWsspElement.hasChildNodes()) wsdlDocEle.insertBefore(inputWsspElement, wsdlDocEle.getFirstChild());
        wsdlDocEle.insertBefore(wsspElement, wsdlDocEle.getFirstChild());

        // add the main ref
        NodeList bindingNodeList = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "binding");
        for (int i = 0; i < bindingNodeList.getLength(); i++) {
            Element binding = (Element)bindingNodeList.item(i);
            binding.insertBefore(buildPolicyReference(wsdl, wsspId), binding.getFirstChild());

            // add all the input refs
            if (inputWsspElement.hasChildNodes()) {
                NodeList inputNodeList = binding.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
                for (int m = 0; m < inputNodeList.getLength(); m++) {
                    Element input = (Element)inputNodeList.item(m);
                    input.insertBefore(buildPolicyReference(wsdl, inputWsspId), input.getFirstChild());
                }
            }

            // add all the output refs
            if (outputWsspElement.hasChildNodes()) {
                NodeList outputNodeList = binding.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
                for (int m = 0; m < outputNodeList.getLength(); m++) {
                    Element output = (Element)outputNodeList.item(m);
                    output.insertBefore(buildPolicyReference(wsdl, outputWsspId), output.getFirstChild());
                }
            }
        }

        // for readability
        wsdl.normalize();
        try{
            DomUtils.stripWhitespace(wsdl.getDocumentElement());
        } catch (SAXException e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE, "Error stripping whitespace from WSDL.", ExceptionUtils.getDebugException(e));
        }
        XmlUtil.format(wsdl, true);
    }

    /**
     * Convert the specified layer 7 policy into WS-SecurityPolicy format.
     *
     * <p>The Layer 7 policy must already have been filtered to remove any
     * assertions that are not relevant to the client consuming the service.</p>
     *
     * <p>The returned policy will contains all assertions that are relevant to
     * then "Endpoint Policy Subject".</p>
     *
     * <p>Assertions that relate to a "Message Policy Subject" are ignored.
     * For example this means that any information on Signed/Encrypted elements
     * is lost.</p>
     *
     * @param layer7Root the layer 7 policy tree to convert. Must not be null.
     * @param isWss11 true to use WSS 1.1
     * @return  the converted Apache Policy.  Never null.
     */
    public Policy convertFromLayer7(Assertion layer7Root, boolean isWss11) throws PolicyAssertionException {
        if(!(layer7Root instanceof AllAssertion)) {
            throw new IllegalArgumentException("Assertion must be AllAssertion!");
        }

        // Sanity check
        AllAssertion l7p = (AllAssertion) layer7Root;
        //noinspection unchecked
        Collection<Assertion> l7Assertions = l7p.getChildren();
        checkConvertable(l7Assertions);
        int algorithmSuite = determineAlgorithmSuite(l7Assertions);

        // Policy stub
        Policy wssp = new Policy();

        // Construct the Policy
        if (isSymmetricBinding(l7Assertions)) {
            buildSymmetricBinding(wssp, algorithmSuite, isWss11, l7Assertions);
        } else if (isAsymmetricBinding(l7Assertions)) {
            buildAsymmetricBinding(wssp, algorithmSuite, isWss11);
        }

        if (isTransportBinding(l7Assertions)) {
            buildTransportBinding(wssp, algorithmSuite, isWss11, l7Assertions);
        }

        return wssp;
    }

    private void buildSupportingToken(org.apache.ws.policy.Assertion wssp, boolean ssl, boolean digest, boolean wss11) {
        String supportingTokenEleName = ssl ? SPELE_SIGNED_SUPPORTING_TOKENS : SPELE_SUPPORTING_TOKENS;
        PrimitiveAssertion supportingTokens = prim(supportingTokenEleName);
        Policy supportingTokensPolicy = new Policy();
        supportingTokens.addTerm(supportingTokensPolicy);

        supportingTokensPolicy.addTerm(buildUsernameToken(digest,wss11));

        wssp.addTerm(supportingTokens);
    }

    /**
     * Convert the specified layer 7 policy into WS-SecurityPolicy format.
     *
     * <p>The Layer 7 policy must already have been filtered to remove any
     * assertions that are not relevant to the client consuming the service.</p>
     *
     * <p>The returned policy will contains all assertions that are relevant to
     * then "Message Policy Subject" for the given type.</p>
     *
     * TODO support for faults
     * TODO policies for operation instances (rather than jus in / out)
     *
     * @param layer7Root  the layer 7 policy tree to convert.  Must not be null.
     * @param isWss11 true to use WSS 1.1
     * @param isInput true if this is the input message
     * @return  the converted Apache Policy.  Never null.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public Policy convertFromLayer7(Assertion layer7Root, boolean isWss11, boolean isInput) throws PolicyAssertionException {
        if(!(layer7Root instanceof AllAssertion)) {
            throw new IllegalArgumentException("Assertion must be AllAssertion!");
        }

        // Sanity check
        AllAssertion l7p = (AllAssertion) layer7Root;
        //noinspection unchecked
        Collection<Assertion> l7Assertions = l7p.getChildren();
        checkConvertable(l7Assertions);

        // Policy stub
        Policy wssp = new Policy();

        Collection<? extends XpathBasedAssertion> encryptionAssertions;
        Collection<? extends XpathBasedAssertion> signingAssertions;
        if (isInput) {
            encryptionAssertions = getInstancesOf(l7Assertions, RequireWssEncryptedElement.class);
            signingAssertions = getInstancesOf(l7Assertions, RequireWssSignedElement.class);
        }
        else {
            encryptionAssertions = getInstancesOf(l7Assertions, WssEncryptElement.class);
            signingAssertions = getInstancesOf(l7Assertions, WssSignElement.class);
        }

        if (!encryptionAssertions.isEmpty()) {
            PrimitiveAssertion encryptedParts = prim(SPELE_ENCRYPTED_PARTS);
            buildParts(encryptedParts, encryptionAssertions);
            wssp.addTerm(encryptedParts);
        }

        if (!signingAssertions.isEmpty()) {
            PrimitiveAssertion signedParts = prim(SPELE_SIGNED_PARTS);
            buildParts(signedParts, signingAssertions);
            wssp.addTerm(signedParts);
        }

        return wssp;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WsspWriter.class.getName());

    // algorithm suites
    private static final int ALGORITHM_SUITE_BASIC128_RSA15 = 1;
    private static final int ALGORITHM_SUITE_BASIC192_RSA15 = 2;
    private static final int ALGORITHM_SUITE_BASIC256_RSA15 = 3;
    private static final int ALGORITHM_SUITE_TRIPLEDES_RSA15 = 4;
    private static final int ALGORITHM_SUITE_BASIC128_RSAOAEP = 5;
    private static final int ALGORITHM_SUITE_BASIC192_RSAOAEP = 6;
    private static final int ALGORITHM_SUITE_BASIC256_RSAOAEP = 7;
    private static final int ALGORITHM_SUITE_TRIPLEDES_RSAOAEP = 8;

    // namespaces / prefix
    private static final String DEFAULT_WSSP_VERSION = SyspropUtil.getString("com.l7tech.policy.wssp.defaultVersion", "1.1");
    private static final String DEFAULT_WSP_VERSION = SyspropUtil.getString("com.l7tech.policy.wsp.defaultVersion", "1.2");

    private static final String SP12_NS = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702";
    private static final String SP11_NS = "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy";
    private static final String SP_PREFIX = "sp";

    public static final String WSP12_NS = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    public static final String WSP15_NS = "http://www.w3.org/ns/ws-policy";
    private static final String WSP_PREFIX = "wsp";

    private static final String WSU_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    private static final String WSU_PREFIX = "wsu";

    public static final String WSP_NS = DEFAULT_WSP_VERSION.equals("1.5") ? WSP15_NS : WSP12_NS;
    public static final String SP_NS = DEFAULT_WSSP_VERSION.equals("1.2") ? SP12_NS : SP11_NS;    

    private static final String[][] PREFERRED_NAMESPACE_PREFIXES = new String[][] {
            {WSP_PREFIX, WSP_NS},
            {WSU_PREFIX, WSU_NS},
            {SP_PREFIX, SP_NS},
    };

    // Security Policy Elements
    private static final String SPELE_TOKEN_RECIPIENT = "RecipientToken";
    private static final String SPELE_TOKEN_X509 = "X509Token";
    private static final String SPELE_TOKEN_X509_WSS10 = "WssX509V3Token10";
    private static final String SPELE_TOKEN_X509_WSS11 = "WssX509V3Token11";
    private static final String SPELE_TOKEN_TRANSPORT = "TransportToken";
    private static final String SPELE_TOKEN_PROTECTION = "ProtectionToken";
    private static final String SPELE_TOKEN_TRANSPORT_HTTPS = "HttpsToken";
    private static final String SPELE_TOKEN_USERNAME = "UsernameToken";
    private static final String SPELE_TOKEN_USERNAME10 = "WssUsernameToken10";
    private static final String SPELE_TOKEN_USERNAME11 = "WssUsernameToken11";
    private static final String SPELE_HASH_PASSWORD = "HashPassword";
    private static final String SPELE_TOKEN_INITIATOR = "InitiatorToken";
    private static final String SPELE_LAYOUT = "Layout";
    private static final String SPELE_LAYOUT_LAX = "Lax";
    private static final String SPELE_TIMESTAMP = "IncludeTimestamp";
    private static final String SPELE_SIGN_HEADERS_BODY = "OnlySignEntireHeadersAndBody";
    private static final String SPELE_BINDING_TRANSPORT = "TransportBinding";
    private static final String SPELE_BINDING_ASYMMETRIC = "AsymmetricBinding";
    private static final String SPELE_BINDING_SYMMETRIC = "SymmetricBinding";
    private static final String SPELE_WSS10 = "Wss10";
    private static final String SPELE_MUSTSUPPORT_REF_KEY_ID = "MustSupportRefKeyIdentifier";
    private static final String SPELE_MUSTSUPPORT_REF_ISSUER_SERIAL = "MustSupportRefIssuerSerial";
    private static final String SPELE_WSS11 = "Wss11";
    private static final String SPELE_REQUIRE_SIGNATURE_CONFIRMATION = "RequireSignatureConfirmation";
    private static final String SPELE_ALGORITHM_SUITE = "AlgorithmSuite";
    private static final String SPELE_ALGORITHMSUITE_BASIC128RSAOAEP = "Basic128";
    private static final String SPELE_ALGORITHMSUITE_BASIC192RSAOAEP = "Basic192";
    private static final String SPELE_ALGORITHMSUITE_BASIC256RSAOAEP = "Basic256";
    private static final String SPELE_ALGORITHMSUITE_TRIPLEDESRSAOAEP = "TripleDes";
    private static final String SPELE_ALGORITHMSUITE_BASIC128RSA15 = "Basic128Rsa15";
    private static final String SPELE_ALGORITHMSUITE_BASIC192RSA15 = "Basic192Rsa15";
    private static final String SPELE_ALGORITHMSUITE_BASIC256RSA15 = "Basic256Rsa15";
    private static final String SPELE_ALGORITHMSUITE_TRIPLEDESRSA15 = "TripleDesRsa15";
    private static final String SPELE_SIGNED_SUPPORTING_TOKENS = "SignedSupportingTokens";
    private static final String SPELE_SUPPORTING_TOKENS = "SupportingTokens";
    private static final String SPELE_SIGNED_PARTS = "SignedParts";
    private static final String SPELE_ENCRYPTED_PARTS = "EncryptedParts";
    private static final String SPELE_PART_BODY = "Body";
    private static final String SPELE_PART_HEADER = "Header";

    // Security Policy Attributes
    private static final String SPATTR_NAME = "Name";
    private static final String SPATTR_NAMESPACE = "Namespace";
    private static final String SPATTR_INCL_TOKEN = "IncludeToken";
    private static final String SPATTR_REQ_CLIENT_CERT = "RequireClientCertificate";

    // Security Policy Attribute Values
    private static final String SPVALUE_INCL_TOKEN_NEVER = "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never";
    private static final String SPVALUE_INCL_TOKEN_ALWAYSTORECIPIENT = "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient";

    // Pattern for Headers (Encryption/Signing)
    private static final String BODY_PATTERN_S11 = "/{http://schemas.xmlsoap.org/soap/envelope/}Envelope/{http://schemas.xmlsoap.org/soap/envelope/}Body";
    private static final String BODY_PATTERN_S12 = "/{http://www.w3.org/2003/05/soap-envelope}Envelope/{http://www.w3.org/2003/05/soap-envelope}Body";
    private static final Pattern HEADER_PATTERN_S11 = Pattern.compile("/\\{http://schemas.xmlsoap.org/soap/envelope/\\}Envelope/\\{http://schemas.xmlsoap.org/soap/envelope/\\}Header/\\{([^\\s}]{1,1024})}([^\\s}:/\\(\\){}\\[\\]]{1,1024})");
    private static final Pattern HEADER_PATTERN_S12 = Pattern.compile("/\\{http://www.w3.org/2003/05/soap-envelope\\}Envelope/\\{http://www.w3.org/2003/05/soap-envelope\\}Header/\\{([^\\s}]{1,1024})}([^\\s}:/\\(\\){}\\[\\]]{1,1024})");

    // WSS assertions
    private static final Collection<Class<? extends Assertion>> WSS_ASSERTIONS = Collections.unmodifiableCollection(Arrays.<Class<? extends Assertion>>asList(
        RequireWssX509Cert.class,
        RequireWssSignedElement.class,
        RequireWssEncryptedElement.class,
        WssSignElement.class,
        WssEncryptElement.class
    ));

    private static final Collection<Class<? extends Assertion>> SYMMETRIC_ASSERTIONS = Collections.<Class<? extends Assertion>>unmodifiableCollection(Arrays.asList(
        EncryptedUsernameTokenAssertion.class,
        SecureConversation.class
    ));

    private static final Collection<Class<? extends Assertion>> TRANSPORT_ASSERTIONS = Collections.unmodifiableCollection(Arrays.<Class<? extends Assertion>>asList(
        SslAssertion.class,
        HttpBasic.class,
        HttpDigest.class
    ));

    // All supported assertions
    private static final Collection SUPPORTED_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(
        SslAssertion.class,
        WssBasic.class,
        WssDigest.class,
        EncryptedUsernameTokenAssertion.class,
        SecureConversation.class,
        RequireWssTimestamp.class,
        RequireWssX509Cert.class,
        RequireWssSignedElement.class,
        RequireWssEncryptedElement.class,
        AddWssTimestamp.class,
        WssSignElement.class,
        WssEncryptElement.class
    ));

    /**
     * Ensure that the given assertions can be converted to WS-SP.
     *
     * @param assertions The Assertions to check
     */
    private void checkConvertable(Collection<Assertion> assertions) throws PolicyAssertionException {
        boolean sslCertIdentity = false;
        boolean usesWss = false;

        for (Assertion assertion : assertions) {

            if (!isSupportedAssertion(assertion)) {
                throw new PolicyAssertionException(assertion, "Assertion not supported: " + assertion);
            } else if (isWssAssertion(assertion)) {
                usesWss = true;

                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, RequireWssEncryptedElement.class));
                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, WssEncryptElement.class));
                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, RequireWssSignedElement.class));
                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, WssSignElement.class));
            } else if (assertion instanceof SslAssertion) {
                SslAssertion sslAssertion = (SslAssertion) assertion;
                if (sslAssertion.isRequireClientAuthentication()) {
                    sslCertIdentity = true;
                }
            }
        }

//        if(sslCertIdentity && usesWss)
//            throw new PolicyAssertionException(null, "Cannot use WSS and TLS with client cert.");
    }

    private boolean isSymmetricBinding(Collection<Assertion> assertions) {
        for (Assertion assertion : assertions) {
            if (isSymmetricAssertion(assertion))
                return true;
        }
        return false;
    }

    private boolean isSymmetricAssertion(Assertion assertion) {
        return isOneOf(SYMMETRIC_ASSERTIONS, assertion.getClass());
    }

    private boolean isAsymmetricBinding(Collection<Assertion> assertions) {
        boolean isAsymmetric = false;

        for (Assertion assertion : assertions) {
            if (isWssAssertion(assertion)) {
                isAsymmetric = true;
            }
        }

        return isAsymmetric;
    }

    private boolean isTransportBinding(Collection<Assertion> assertions) {
        for (Assertion assertion : assertions) {
            if (isTransportAssertion(assertion))
                return true;
        }
        return false;
    }

    private boolean isSslPolicy(Collection<Assertion> assertions) {
        for (Assertion assertion : assertions) {
            if (assertion instanceof SslAssertion) {
                SslAssertion sslAssertion = (SslAssertion) assertion;
                if (SslAssertion.REQUIRED == sslAssertion.getOption())
                    return true;
            }
        }
        return false;
    }

    /**
     * Work out the applicable algorithm suite for the given assertions.
     *
     * @throws PolicyAssertionException if there are inconsistent algorithms
     */
    private int determineAlgorithmSuite(Collection<Assertion> assertions) throws PolicyAssertionException {
        Integer suite = null;

        for (Assertion assertion : assertions) {
            String algEncStr = null;
            String keyEncAlgStr = null;
            if (assertion instanceof RequireWssEncryptedElement) {
                RequireWssEncryptedElement rwc = (RequireWssEncryptedElement) assertion;
                algEncStr = rwc.getXEncAlgorithm();
                keyEncAlgStr = rwc.getKeyEncryptionAlgorithm();
            } else if (assertion instanceof WssEncryptElement) {
                WssEncryptElement rwc = (WssEncryptElement) assertion;
                algEncStr = rwc.getXEncAlgorithm();
                keyEncAlgStr = rwc.getKeyEncryptionAlgorithm();
            }

            if (algEncStr != null) {
                int algorithm = ALGORITHM_SUITE_BASIC128_RSA15;
                boolean rsa15 = keyEncAlgStr == null || SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO.equals(keyEncAlgStr);
                if (XencUtil.AES_128_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_BASIC128_RSA15 : ALGORITHM_SUITE_BASIC128_RSAOAEP;
                } else if (XencUtil.AES_192_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_BASIC192_RSA15 : ALGORITHM_SUITE_BASIC192_RSAOAEP;
                } else if (XencUtil.AES_256_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_BASIC256_RSA15 : ALGORITHM_SUITE_BASIC256_RSAOAEP;
                } else if (XencUtil.TRIPLE_DES_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_TRIPLEDES_RSA15 : ALGORITHM_SUITE_TRIPLEDES_RSAOAEP;
                } else {
                    //TODO throw rather than default for unknown algorithms
                }
                if (suite != null && suite != algorithm) {
                    // conflicting algorithms specifed, not currently supported
                    throw new PolicyAssertionException(null, "Conflicting algorithms specified.");
                } else {
                    suite = algorithm;
                }
            }
        }

        if (suite == null) {
            suite = ALGORITHM_SUITE_BASIC256_RSA15;
        }

        return suite;
    }

    /**
     * Build a lax layout assertion
     */
    private org.apache.ws.policy.Assertion buildLayout() {
        QName layoutName = qname(SPELE_LAYOUT);
        QName layoutTypeName = qname(SPELE_LAYOUT_LAX);

        PrimitiveAssertion layout = new PrimitiveAssertion(layoutName);
        PrimitiveAssertion layoutType = new PrimitiveAssertion(layoutTypeName);

        Policy layoutPolicy = new Policy();
        layoutPolicy.addTerm(layoutType);
        layout.addTerm(layoutPolicy);

        return layout;
    }

    /**
     * Build an algorithm suite assertion
     */
    private org.apache.ws.policy.Assertion buildAlgorithmSuite(int suite) {
        QName algSuiteName = qname(SPELE_ALGORITHM_SUITE);
        String algSuiteTypeEleName = null;
        switch(suite) {
            case ALGORITHM_SUITE_BASIC128_RSA15:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_BASIC128RSA15;
                break;
            case ALGORITHM_SUITE_BASIC192_RSA15:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_BASIC192RSA15;
                break;
            case ALGORITHM_SUITE_BASIC256_RSA15:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_BASIC256RSA15;
                break;
            case ALGORITHM_SUITE_TRIPLEDES_RSA15:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_TRIPLEDESRSA15;
                break;
            case ALGORITHM_SUITE_BASIC128_RSAOAEP:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_BASIC128RSAOAEP;
                break;
            case ALGORITHM_SUITE_BASIC192_RSAOAEP:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_BASIC192RSAOAEP;
                break;
            case ALGORITHM_SUITE_BASIC256_RSAOAEP:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_BASIC256RSAOAEP;
                break;
            case ALGORITHM_SUITE_TRIPLEDES_RSAOAEP:
                algSuiteTypeEleName = SPELE_ALGORITHMSUITE_TRIPLEDESRSAOAEP;
                break;
        }

        QName algSuiteTypeName = qname(algSuiteTypeEleName);

        PrimitiveAssertion algorithmSuite = new PrimitiveAssertion(algSuiteName);
        PrimitiveAssertion algorithmSuiteType = new PrimitiveAssertion(algSuiteTypeName);

        Policy algSuitePolicy = new Policy();
        algSuitePolicy.addTerm(algorithmSuiteType);
        algorithmSuite.addTerm(algSuitePolicy);

        return algorithmSuite;
    }

    /**
     * Build an HTTPS transport token
     */
    private org.apache.ws.policy.Assertion buildTransportToken(boolean clientCert) {
        PrimitiveAssertion transportToken = prim(SPELE_TOKEN_TRANSPORT);
        Policy transportTokenPolicy = new Policy();
        transportToken.addTerm(transportTokenPolicy);

        PrimitiveAssertion httpsToken = prim(SPELE_TOKEN_TRANSPORT_HTTPS);
        httpsToken.addAttribute(new QName(SPATTR_REQ_CLIENT_CERT), Boolean.toString(clientCert));

        transportTokenPolicy.addTerm(httpsToken);

        return transportToken;
    }

    /**
     * Build an X509 token
     */
    private org.apache.ws.policy.Assertion buildX509Token(String tokenSubject, String tokenType, String includeTokenValue) {
        PrimitiveAssertion subjectToken = prim(tokenSubject);
        Policy subjectPolicy = new Policy();
        subjectToken.addTerm(subjectPolicy);

        PrimitiveAssertion x509Token = prim(SPELE_TOKEN_X509);
        x509Token.addAttribute(qname(SPATTR_INCL_TOKEN), includeTokenValue);
        Policy x509TokenPolicy = new Policy();
        x509Token.addTerm(x509TokenPolicy);

        x509TokenPolicy.addTerm(prim(tokenType));
        subjectPolicy.addTerm(x509Token);

        return subjectToken;
    }

    private QName qname(String localPart) {
        return qname(SP_NS, localPart, SP_PREFIX);
    }

    private QName qname(String nsUri, String localPart, String nsPrefix) {
        return new QName(nsUri, localPart, nsPrefix);
    }

    /**
     * Build a username token
     * @param digest
     */
    private org.apache.ws.policy.Assertion buildUsernameToken(boolean digest, boolean wss11) {
        PrimitiveAssertion usernameToken = prim(SPELE_TOKEN_USERNAME);
        usernameToken.addAttribute(qname(SPATTR_INCL_TOKEN), SPVALUE_INCL_TOKEN_ALWAYSTORECIPIENT);

        Policy usernameTokenPolicy = new Policy();        
        if (digest)
            usernameTokenPolicy.addTerm(prim(SPELE_HASH_PASSWORD));
        usernameTokenPolicy.addTerm(prim(wss11 ? SPELE_TOKEN_USERNAME11 : SPELE_TOKEN_USERNAME10));
        usernameToken.addTerm(usernameTokenPolicy);

        return usernameToken;
    }

    /**
     * Build a Wss 1.0 assertion
     */
    private org.apache.ws.policy.Assertion buildWss10() {
        PrimitiveAssertion wss10 = prim(SPELE_WSS10);
        Policy wss10Policy = new Policy();
        wss10.addTerm(wss10Policy);

        wss10Policy.addTerm(prim(SPELE_MUSTSUPPORT_REF_KEY_ID));
        wss10Policy.addTerm(prim(SPELE_MUSTSUPPORT_REF_ISSUER_SERIAL));

        return wss10;
    }

    /**
     * Build a Wss 1.1 assertion
     */
    private org.apache.ws.policy.Assertion buildWss11() {
        PrimitiveAssertion wss11 = prim(SPELE_WSS11);
        Policy wss11Policy = new Policy();
        wss11.addTerm(wss11Policy);

        wss11Policy.addTerm(prim(SPELE_MUSTSUPPORT_REF_KEY_ID));
        wss11Policy.addTerm(prim(SPELE_MUSTSUPPORT_REF_ISSUER_SERIAL));
        wss11Policy.addTerm(prim(SPELE_REQUIRE_SIGNATURE_CONFIRMATION));

        return wss11;
    }

    /**
     * Build an asymmetric binding assertion and siblings, attach to the given assertion
     */
    private void buildAsymmetricBinding(org.apache.ws.policy.Assertion assertion, int algorithmSuite, boolean wss11) {
        QName name = qname(SPELE_BINDING_ASYMMETRIC);
        PrimitiveAssertion binding = new PrimitiveAssertion(name);

        Policy bindingPolicy = new Policy();
        binding.addTerm(bindingPolicy);

        bindingPolicy.addTerm(buildX509Token(SPELE_TOKEN_RECIPIENT, wss11 ? SPELE_TOKEN_X509_WSS11 : SPELE_TOKEN_X509_WSS10, SPVALUE_INCL_TOKEN_NEVER));
        bindingPolicy.addTerm(buildX509Token(SPELE_TOKEN_INITIATOR, wss11 ? SPELE_TOKEN_X509_WSS11 : SPELE_TOKEN_X509_WSS10, SPVALUE_INCL_TOKEN_ALWAYSTORECIPIENT));
        bindingPolicy.addTerm(buildAlgorithmSuite(algorithmSuite));
        bindingPolicy.addTerm(buildLayout());
        bindingPolicy.addTerm(prim(SPELE_TIMESTAMP));
        bindingPolicy.addTerm(prim(SPELE_SIGN_HEADERS_BODY));

        assertion.addTerm(binding);
        assertion.addTerm( wss11? buildWss11() : buildWss10());
    }

    /**
     * Build a symmetric binding assertion and siblings, attach to given assertion
     */
    private void buildSymmetricBinding(org.apache.ws.policy.Assertion assertion, int algorithmSuite, boolean wss11, Collection<Assertion> l7Assertions) {
        QName name = qname(SPELE_BINDING_SYMMETRIC);
        PrimitiveAssertion binding = new PrimitiveAssertion(name);

        Policy bindingPolicy = new Policy();
        binding.addTerm(bindingPolicy);

        boolean isSc = false;
        if (containsInstanceOf(l7Assertions, SecureConversation.class)) {
            isSc = true;
            bindingPolicy.addTerm(buildProtectionToken());
            // TODO add sp:Trust13 assertion as next-sibling of SymmetricBinding
        }

        bindingPolicy.addTerm(buildAlgorithmSuite(algorithmSuite));
        bindingPolicy.addTerm(buildLayout());
        if (containsInstanceOf(l7Assertions, RequireWssTimestamp.class) ||
            containsInstanceOf(l7Assertions, AddWssTimestamp.class)) {
            bindingPolicy.addTerm(prim(SPELE_TIMESTAMP));
        }

        assertion.addTerm(binding);

        if ( !isSc ) {
            boolean ssl = isSslPolicy(l7Assertions);
            if (containsInstanceOf(l7Assertions, WssBasic.class)) {
                buildSupportingToken(assertion, ssl, false, wss11);
            } else if (containsInstanceOf(l7Assertions, WssDigest.class)) {
                buildSupportingToken(assertion, ssl, true, wss11);
            }
        }

        assertion.addTerm(wss11 ? buildWss11() : buildWss10());
    }

    private PrimitiveAssertion prim(String name) {
        return prim(SP_NS, name, SP_PREFIX);
    }

    private PrimitiveAssertion prim(String nsUri, String name, String nsPrefix) {
        return new PrimitiveAssertion(new QName(nsUri, name, nsPrefix));
    }

    private PrimitiveAssertion buildProtectionToken() {
        PrimitiveAssertion protToken = prim(SPELE_TOKEN_PROTECTION);
        Policy protTokenPolicy = new Policy();
        protToken.addTerm(protTokenPolicy);

        final PrimitiveAssertion scTok = prim("SecureConversationToken");
        protTokenPolicy.addTerm(scTok);

        Policy scTokPolicy = new Policy();
        scTok.addTerm(scTokPolicy);

        scTokPolicy.addTerm(prim("RequireDerivedKeys"));
        // TODO add bootstrap policy
        
        return protToken;
    }

    /**
     * Build a transport binding assertion and siblings, attach to the given assertion
     */
    private void buildTransportBinding(org.apache.ws.policy.Assertion assertion, int algorithmSuite, boolean wss11, Collection<Assertion> l7Assertions) {
        PrimitiveAssertion binding = prim(SPELE_BINDING_TRANSPORT);

        Policy bindingPolicy = new Policy();
        binding.addTerm(bindingPolicy);

        if (containsInstanceOf(l7Assertions, SslAssertion.class)) {
            SslAssertion sslAssertion = getInstanceOf(l7Assertions, SslAssertion.class);
            bindingPolicy.addTerm(buildTransportToken(sslAssertion.isRequireClientAuthentication()));
        }

        bindingPolicy.addTerm(buildAlgorithmSuite(algorithmSuite));
        bindingPolicy.addTerm(buildLayout());
        if (containsInstanceOf(l7Assertions, RequireWssTimestamp.class) ||
            containsInstanceOf(l7Assertions, AddWssTimestamp.class)) {
            bindingPolicy.addTerm(prim(SPELE_TIMESTAMP));
        }

        assertion.addTerm(binding);

        boolean ssl = isSslPolicy(l7Assertions);
        boolean wss = false;
        if (containsInstanceOf(l7Assertions, WssBasic.class)) {
            wss = true;
            buildSupportingToken(assertion, ssl, false, wss11);
        } else if (containsInstanceOf(l7Assertions, WssDigest.class)) {
            wss = true;
            buildSupportingToken(assertion, ssl, true, wss11);
        }

        if ( wss ) assertion.addTerm(wss11 ? buildWss11() : buildWss10());
    }

    /**
     * Check if the given assertion is supported
     */
    private boolean isSupportedAssertion(Assertion assertion) {
        return isOneOf(SUPPORTED_ASSERTIONS, assertion.getClass());
    }

    /**
     * Check if the given assertion is a WSS assertion
     */
    private boolean isWssAssertion(Assertion assertion) {
        return isOneOf(WSS_ASSERTIONS, assertion.getClass());
    }

    private boolean isTransportAssertion(Assertion assertion) {
        return isOneOf(TRANSPORT_ASSERTIONS, assertion.getClass());
    }

    private void buildParts(org.apache.ws.policy.Assertion assertion, Collection<? extends XpathBasedAssertion> xpathAssertions) {
        boolean bodyDone = false;
        for (XpathBasedAssertion confAssertion : xpathAssertions) {
            String pattern = confAssertion.pattern();
            Map<String, String> namespaces = confAssertion.namespaceMap();
            pattern = expandXpath(pattern, namespaces);

            if (pattern.equals(BODY_PATTERN_S11) || pattern.equals(BODY_PATTERN_S12)) {
                if (!bodyDone) {
                    bodyDone = true;
                    assertion.addTerm(prim(SPELE_PART_BODY));
                }
            } else {
                Matcher headerMatcher = HEADER_PATTERN_S11.matcher(pattern);
                if (!headerMatcher.matches()) {
                    headerMatcher = HEADER_PATTERN_S12.matcher(pattern);
                }
                if (!headerMatcher.matches())
                    continue;

                PrimitiveAssertion headerAssertion = prim(SPELE_PART_HEADER);
                headerAssertion.addAttribute(new QName(SPATTR_NAMESPACE), headerMatcher.group(1));
                String elementName = headerMatcher.group(2);
                if (!"*".equals(elementName)) {
                    headerAssertion.addAttribute(new QName(SPATTR_NAME), elementName);
                }
                assertion.addTerm(headerAssertion);
            }
        }
    }

    /**
     * This just expands the assertions XPath to be QNameish then checks it is a simple
     * XPath for the body "/Envelope/Body" or a header "/Envelope/Header/{xx}yy"
     */
    private void ensureHeaderOrBodyXpathsOnly(Collection<XpathBasedAssertion> xpathAssertions) throws PolicyAssertionException {
        for (XpathBasedAssertion xpathBasedAssertion : xpathAssertions) {
            String pattern = xpathBasedAssertion.pattern();
            Map<String, String> namespaces = xpathBasedAssertion.namespaceMap();

            pattern = expandXpath(pattern, namespaces);

            if (!pattern.equals(BODY_PATTERN_S11) && !pattern.equals(BODY_PATTERN_S12)) {
                Matcher headerMatcher = HEADER_PATTERN_S11.matcher(pattern);
                if (!headerMatcher.matches())
                    headerMatcher = HEADER_PATTERN_S12.matcher(pattern);
                if (!headerMatcher.matches())
                    throw new PolicyAssertionException(xpathBasedAssertion, "Assertion XPath not supported (Body or Headers only '" + xpathBasedAssertion.pattern() + "')");
            }
        }
    }

    /**
     * Expands the XPath to be QNameish
     */
    private String expandXpath(String pattern, Map<String, String> namespaces) {
        for (Map.Entry entry : namespaces.entrySet()) {
            String prefix = (String) entry.getKey();
            String uri = (String) entry.getValue();

            pattern = pattern.replaceAll("(?!/)" + prefix + ":", "{" + uri + "}");
        }

        return pattern;
    }

    /**
     * Check if the given class is in the given collection
     */
    private boolean isOneOf(Collection items, Class clazz) {
        return items.contains(clazz);
    }

    /**
     * Check if an instance of the given class is in the given collection
     */
    private boolean containsInstanceOf(Collection<?> items, Class clazz) {
        boolean contains = false;

        for (Object object : items) {
            if (clazz.isInstance(object)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    /**
     * Get the first instance of the given class from the given collection
     */
    private <T> T getInstanceOf(Collection<?> items, Class<T> clazz) {
        T found = null;

        for (Object object : items) {
            if (clazz.isInstance(object)) {
                //noinspection unchecked
                found = (T) object;
                break;
            }
        }

        return found;
    }

    /**
     * Get all the instances of the given class from the given collection
     */
    private <T> Collection<T> getInstancesOf(Collection<?> items, Class<? extends T> clazz) {
        List<T> found = new ArrayList<T>();

        for (Object object : items) {
            if (clazz.isInstance(object)) {
                //noinspection unchecked
                found.add((T) object);
            }
        }

        return found;
    }

    /**
     * Add the preferred prefixes to the document element.
     *
     * Only add if the prefix is not already used and the namespace is not
     * already declared.
     */
    private static void addPreferredNamespacesIfAvailable(Element targetElement) {
        Map<String, String> nsMap = DomUtils.getNamespaceMap(targetElement);

        for (String[] prefixes : PREFERRED_NAMESPACE_PREFIXES) {
            String prefix = prefixes[0];
            String namesp = prefixes[1];

            if (!nsMap.containsKey(prefix)) {
                boolean found = false;

                for (Map.Entry entry : nsMap.entrySet()) {
                    String currentNS = (String) entry.getValue();
                    if (namesp.equals(currentNS)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    if (prefix == null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                        targetElement.setAttributeNS(DomUtils.XMLNS_NS, XMLConstants.XMLNS_ATTRIBUTE, namesp);
                    } else {
                        targetElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namesp);
                    }
                }
            }
        }
    }

    private static Element buildPolicyReference(Document factory, String policyId) {
        Element policyReference = factory.createElementNS(WSP_NS, "wsp:PolicyReference");
        policyReference.setAttribute("URI", "#"+policyId);
        policyReference.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:wsp", WSP_NS);
        return policyReference;
    }

    private static Element toElement(Document target, Policy policy) throws PolicyAssertionException {
        return (Element)target.importNode(policyToElement(policy), true);
    }

    private static Element toElement(Document target, String xml) throws SAXException {
        return (Element)target.importNode(XmlUtil.stringToDocument(xml).getDocumentElement(), true);
    }

    public static Element policyToElement(Policy p) throws PolicyAssertionException {
        try {
            return XmlUtil.stringToDocument(policyToXml(p)).getDocumentElement();
        } catch (SAXException e) {
            throw new PolicyAssertionException(null, e);
        }
    }

    public static String policyToXml(Policy p) throws PolicyAssertionException {
        if ( !WSP12_NS.equals( WSP_NS ) )
            return policyToXml(p, WSP12_NS, WSP_NS);
        else
            return policyToXml(p, null, null);
    }

    /**
     * Convert an Apache WSSP Policy to XML, converting namespaces on the fly.
     *
     * @param p the policy to serialize to XML.  Required.
     * @param srcUri a namespace URL to replace.  May not contain regex metacharacters.  Required.
     * @param dstUri What to replace it with.  Required.
     * @return
     * @throws PolicyAssertionException
     */
    public static String policyToXml(Policy p, String srcUri, String dstUri) throws PolicyAssertionException {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream();
        try {
            PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER).writePolicy(p, baos);
            String xml = baos.toString(Charsets.UTF8);
            if (srcUri != null && dstUri != null) {
                // Translate namespace decls
                xml = xml.replaceAll("\\<\\?.*?\\?\\>", "");
                return xml.replaceAll("^([^\\>]+)" + srcUri, "$1" + dstUri);
            } else {
                return xml;
            }
        } finally {
            ResourceUtils.closeQuietly(baos);
        }
    }
}
