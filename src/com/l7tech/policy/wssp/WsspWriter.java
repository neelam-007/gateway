/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMResult;

import org.apache.ws.policy.Policy;
import org.apache.ws.policy.PrimitiveAssertion;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.StAXPolicyWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.l7tech.common.security.xml.XencUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.xml.DOMResultXMLStreamWriter;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssTimestamp;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssTimestamp;

/**
 * Converts a layer 7 policy into a WS-SecurityPolicy tree.
 *
 * <p>The currently supported assertions are:</p>
 *
 * <ul>
 *   <li>TLS with or without client certificate.</li>
 *   <li>WSS Username Token</li>
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
public class WsspWriter {

    //- PUBLIC

    /**
     * Decorate the given WSDL using the given Layer 7 policy.
     *
     * @param wsdl the wsdl DOM
     * @param layer7Root the layer 7 policy tree to use. Must not be null.
     */
    public static void decorate(Document wsdl, Assertion layer7Root) throws PolicyAssertionException {
        addPreferredNamespacesIfAvailable(wsdl.getDocumentElement());
        WsspWriter wsspWriter = new WsspWriter();
        Policy wssp = wsspWriter.convertFromLayer7(layer7Root);
        Policy inputWssp = wsspWriter.convertFromLayer7(layer7Root, true);
        Policy outputWssp = wsspWriter.convertFromLayer7(layer7Root, false);

        wssp.setId(SoapUtil.generateUniqueId("policy", 1));
        inputWssp.setId(SoapUtil.generateUniqueId("policy", 2));
        outputWssp.setId(SoapUtil.generateUniqueId("policy", 3));

        StAXPolicyWriter pw = (StAXPolicyWriter) PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER);
        Element wsspElement = toElement(wsdl, pw, wssp);
        Element inputWsspElement = toElement(wsdl, pw, inputWssp);
        Element outputWsspElement = toElement(wsdl, pw, outputWssp);

        // add in reverse order
        Element wsdlDocEle = wsdl.getDocumentElement();
        if (outputWsspElement.hasChildNodes()) wsdlDocEle.insertBefore(outputWsspElement, wsdlDocEle.getFirstChild());
        if (inputWsspElement.hasChildNodes()) wsdlDocEle.insertBefore(inputWsspElement, wsdlDocEle.getFirstChild());
        wsdlDocEle.insertBefore(wsspElement, wsdlDocEle.getFirstChild());

        // add the main ref
        NodeList bindingNodeList = wsdl.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "binding");
        for (int i = 0; i < bindingNodeList.getLength(); i++) {
            Element binding = (Element)bindingNodeList.item(i);
            binding.insertBefore(buildPolicyReference(wsdl, wssp), binding.getFirstChild());

            // add all the input refs
            if (inputWsspElement.hasChildNodes()) {
                NodeList inputNodeList = binding.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "input");
                for (int m = 0; m < inputNodeList.getLength(); m++) {
                    Element input = (Element)inputNodeList.item(m);
                    input.insertBefore(buildPolicyReference(wsdl, inputWssp), input.getFirstChild());
                }
            }

            // add all the output refs
            if (outputWsspElement.hasChildNodes()) {
                NodeList outputNodeList = binding.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/", "output");
                for (int m = 0; m < outputNodeList.getLength(); m++) {
                    Element output = (Element)outputNodeList.item(m);
                    output.insertBefore(buildPolicyReference(wsdl, outputWssp), output.getFirstChild());
                }
            }
        }

        // for readability
        wsdl.normalize();
        try{
            XmlUtil.stripWhitespace(wsdl.getDocumentElement());
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
     * @return  the converted Apache Policy.  Never null.
     */
    public Policy convertFromLayer7(Assertion layer7Root) throws PolicyAssertionException {
        if(!(layer7Root instanceof AllAssertion)) {
            throw new IllegalArgumentException("Assertion must be AllAssertion!");
        }

        // Sanity check
        AllAssertion l7p = (AllAssertion) layer7Root;
        Collection l7Assertions = l7p.getChildren();
        checkConvertable(l7Assertions);
        int algorithmSuite = determineAlgorithmSuite(l7Assertions);

        // Policy stub
        Policy wssp = new Policy();

        // Construct the Policy
        if(isAsymmetricBinding(l7Assertions)) {
            buildAsymmetricBinding(wssp, algorithmSuite);
        }
        else { // Transport Binding
            buildTransportBinding(wssp, algorithmSuite, l7Assertions);
        }

        return wssp;
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
     * @param isInput true if this is the input message
     * @return  the converted Apache Policy.  Never null.
     */
    public Policy convertFromLayer7(Assertion layer7Root, boolean isInput) throws PolicyAssertionException {
        if(!(layer7Root instanceof AllAssertion)) {
            throw new IllegalArgumentException("Assertion must be AllAssertion!");
        }

        // Sanity check
        AllAssertion l7p = (AllAssertion) layer7Root;
        Collection l7Assertions = l7p.getChildren();
        checkConvertable(l7Assertions);

        // Policy stub
        Policy wssp = new Policy();

        Collection encryptionAssertions = null;
        Collection signingAssertions = null;
        if (isInput) {
            encryptionAssertions = getInstancesOf(l7Assertions, RequestWssConfidentiality.class);
            signingAssertions = getInstancesOf(l7Assertions, RequestWssIntegrity.class);
        }
        else {
            encryptionAssertions = getInstancesOf(l7Assertions, ResponseWssConfidentiality.class);
            signingAssertions = getInstancesOf(l7Assertions, ResponseWssIntegrity.class);
        }

        if (!encryptionAssertions.isEmpty()) {
            PrimitiveAssertion encryptedParts = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_ENCRYPTED_PARTS, PREFIX_SECURITY_POLICY));
            buildParts(encryptedParts, encryptionAssertions);
            wssp.addTerm(encryptedParts);
        }

        if (!signingAssertions.isEmpty()) {
            PrimitiveAssertion signedParts = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_SIGNED_PARTS, PREFIX_SECURITY_POLICY));
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
    private static final String NAMESPACE_SECURITY_POLICY = "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy";

    private static final String PREFIX_SECURITY_POLICY = "sp";

    private static final String[][] PREFERRED_NAMESPACE_PREFIXES = new String[][] {
            {"wsp", "http://schemas.xmlsoap.org/ws/2004/09/policy"},
            {"wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"},
            {"sp", "http://schemas.xmlsoap.org/ws/2005/07/securitypolicy"}
    };

    // Security Policy Elements
    private static final String SPELE_TOKEN_RECIPIENT = "RecipientToken";
    private static final String SPELE_TOKEN_X509 = "X509Token";
    private static final String SPELE_TOKEN_X509_WSS10 = "WssX509V3Token10";
    private static final String SPELE_TOKEN_TRANSPORT = "TransportToken";
    private static final String SPELE_TOKEN_TRANSPORT_HTTPS = "HttpsToken";
    private static final String SPELE_TOKEN_USERNAME = "UsernameToken";
    private static final String SPELE_TOKEN_INITIATOR = "InitiatorToken";
    private static final String SPELE_LAYOUT = "Layout";
    private static final String SPELE_LAYOUT_LAX = "Lax";
    private static final String SPELE_TIMESTAMP = "IncludeTimestamp";
    private static final String SPELE_SIGN_HEADERS_BODY = "OnlySignEntireHeadersAndBody";
    private static final String SPELE_BINDING_TRANSPORT = "TransportBinding";
    private static final String SPELE_BINDING_ASYMMETRIC = "AsymmetricBinding";
    private static final String SPELE_WSS10 = "Wss10";
    private static final String SPELE_MUSTSUPPORT_REF_KEY_ID = "MustSupportRefKeyIdentifier";
    private static final String SPELE_MUSTSUPPORT_REF_ISSUER_SERIAL = "MustSupportRefIssuerSerial";
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
    private static final String BODY_PATTERN = "/{http://schemas.xmlsoap.org/soap/envelope/}Envelope/{http://schemas.xmlsoap.org/soap/envelope/}Body";
    private static final Pattern HEADER_PATTERN = Pattern.compile("/\\{http://schemas.xmlsoap.org/soap/envelope/\\}Envelope/\\{http://schemas.xmlsoap.org/soap/envelope/\\}Header/\\{([^\\s}]{1,1024})}([^\\s}:/\\(\\){}\\[\\]]{1,1024})");

    // WSS assertions
    private static final Collection WSS_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(
        RequestWssX509Cert.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class
    ));

    // All supported assertions
    private static final Collection SUPPORTED_ASSERTIONS = Collections.unmodifiableCollection(Arrays.asList(
        SslAssertion.class,
        WssBasic.class,
        RequestWssTimestamp.class,
        RequestWssX509Cert.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssTimestamp.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class
    ));

    /**
     * Ensure that the given assertions can be converted to WS-SP.
     *
     * @param assertions The Assertions to check
     */
    private void checkConvertable(Collection assertions) throws PolicyAssertionException {
        boolean sslCertIdentity = false;
        boolean usesWss = false;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();

            if(!isSupportedAssertion(assertion)) {
                throw new PolicyAssertionException(assertion, "Assertion not supported: " + assertion);
            }
            else if(isWssAssertion(assertion)) {
                usesWss = true;

                ensureHeaderOrBodyXpathsOnly(getInstancesOf(assertions, RequestWssConfidentiality.class));
                ensureHeaderOrBodyXpathsOnly(getInstancesOf(assertions, ResponseWssConfidentiality.class));
                ensureHeaderOrBodyXpathsOnly(getInstancesOf(assertions, RequestWssIntegrity.class));
                ensureHeaderOrBodyXpathsOnly(getInstancesOf(assertions, ResponseWssIntegrity.class));
            }
            else if(assertion instanceof SslAssertion) {
                SslAssertion sslAssertion = (SslAssertion) assertion;
                if(sslAssertion.isRequireClientAuthentication()) {
                    sslCertIdentity = true;
                }
            }
        }

        if(sslCertIdentity && usesWss)
            throw new PolicyAssertionException(null, "Cannot use WSS and TLS with client cert.");
    }

    private boolean isAsymmetricBinding(Collection assertions) {
        boolean isAsymmetric = false;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();
            if(isWssAssertion(assertion)) {
                isAsymmetric = true;
            }
        }

        return isAsymmetric;
    }

    /**
     * Work out the applicable algorithm suite for the given assertions.
     *
     * @throws PolicyAssertionException if there are inconsistent algorithms
     */
    private int determineAlgorithmSuite(Collection assertions) throws PolicyAssertionException {
        Integer suite = null;;

        for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
            Assertion assertion = (Assertion) iterator.next();
            String algEncStr = null;
            String keyEncAlgStr = null;
            if (assertion instanceof RequestWssConfidentiality) {
                RequestWssConfidentiality rwc = (RequestWssConfidentiality) assertion;
                algEncStr = rwc.getXEncAlgorithm();
                keyEncAlgStr = rwc.getKeyEncryptionAlgorithm();
            }
            else if (assertion instanceof ResponseWssConfidentiality) {
                ResponseWssConfidentiality rwc = (ResponseWssConfidentiality) assertion;
                algEncStr = rwc.getXEncAlgorithm();
                keyEncAlgStr = rwc.getKeyEncryptionAlgorithm();
            }

            if (algEncStr != null) {
                int algorithm = ALGORITHM_SUITE_BASIC128_RSA15;
                boolean rsa15 = keyEncAlgStr == null || SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO.equals(keyEncAlgStr);
                if (XencUtil.AES_128_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_BASIC128_RSA15 : ALGORITHM_SUITE_BASIC128_RSAOAEP;
                }
                else if (XencUtil.AES_192_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_BASIC192_RSA15 : ALGORITHM_SUITE_BASIC192_RSAOAEP;
                }
                else if (XencUtil.AES_256_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_BASIC256_RSA15 : ALGORITHM_SUITE_BASIC256_RSAOAEP;
                }
                else if (XencUtil.TRIPLE_DES_CBC.equals(algEncStr)) {
                    algorithm = rsa15 ? ALGORITHM_SUITE_TRIPLEDES_RSA15 : ALGORITHM_SUITE_TRIPLEDES_RSAOAEP;
                }
                else {
                    //TODO throw rather than default for unknown algorithms
                }
                if (suite != null && suite.intValue()!=algorithm) {
                    // conflicting algorithms specifed, not currently supported
                    throw new PolicyAssertionException(null, "Conflicting algorithms specified.");
                }
                else {
                    suite = Integer.valueOf(algorithm);
                }
            }
        }

        if (suite == null) {
            suite = Integer.valueOf(ALGORITHM_SUITE_BASIC256_RSA15);
        }

        return suite.intValue();
    }

    /**
     * Build a lax layout assertion
     */
    private org.apache.ws.policy.Assertion buildLayout() {
        QName layoutName = new QName(NAMESPACE_SECURITY_POLICY, SPELE_LAYOUT, PREFIX_SECURITY_POLICY);
        QName layoutTypeName = new QName(NAMESPACE_SECURITY_POLICY, SPELE_LAYOUT_LAX, PREFIX_SECURITY_POLICY);

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
        QName algSuiteName = new QName(NAMESPACE_SECURITY_POLICY, SPELE_ALGORITHM_SUITE, PREFIX_SECURITY_POLICY);
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

        QName algSuiteTypeName = new QName(NAMESPACE_SECURITY_POLICY, algSuiteTypeEleName, PREFIX_SECURITY_POLICY);

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
        PrimitiveAssertion transportToken = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_TOKEN_TRANSPORT, PREFIX_SECURITY_POLICY));
        Policy transportTokenPolicy = new Policy();
        transportToken.addTerm(transportTokenPolicy);

        PrimitiveAssertion httpsToken = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_TOKEN_TRANSPORT_HTTPS, PREFIX_SECURITY_POLICY));
        httpsToken.addAttribute(new QName(SPATTR_REQ_CLIENT_CERT), Boolean.toString(clientCert));

        transportTokenPolicy.addTerm(httpsToken);

        return transportToken;
    }

    /**
     * Build an X509 token
     */
    private org.apache.ws.policy.Assertion buildX509Token(String tokenSubject, String tokenType, String includeTokenValue) {
        PrimitiveAssertion subjectToken = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, tokenSubject, PREFIX_SECURITY_POLICY));
        Policy subjectPolicy = new Policy();
        subjectToken.addTerm(subjectPolicy);

        PrimitiveAssertion x509Token = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_TOKEN_X509, PREFIX_SECURITY_POLICY));
        x509Token.addAttribute(new QName(NAMESPACE_SECURITY_POLICY, SPATTR_INCL_TOKEN, PREFIX_SECURITY_POLICY), includeTokenValue);
        Policy x509TokenPolicy = new Policy();
        x509Token.addTerm(x509TokenPolicy);

        x509TokenPolicy.addTerm(new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, tokenType, PREFIX_SECURITY_POLICY)));
        subjectPolicy.addTerm(x509Token);

        return subjectToken;
    }

    /**
     * Build a username token
     */
    private org.apache.ws.policy.Assertion buildUsernameToken() {
        PrimitiveAssertion usernameToken = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_TOKEN_USERNAME, PREFIX_SECURITY_POLICY));
        usernameToken.addAttribute(new QName(NAMESPACE_SECURITY_POLICY, SPATTR_INCL_TOKEN, PREFIX_SECURITY_POLICY), SPVALUE_INCL_TOKEN_ALWAYSTORECIPIENT);
        return usernameToken;
    }

    /**
     * Build a timestamp assertion
     */
    private org.apache.ws.policy.Assertion buildTimestamp() {
        return new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_TIMESTAMP, PREFIX_SECURITY_POLICY));
    }

    /**
     * Build a sign headers body property assertion
     */
    private org.apache.ws.policy.Assertion buildDefaultSignature() {
        return new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_SIGN_HEADERS_BODY, PREFIX_SECURITY_POLICY));
    }

    /**
     * Build a Wss 1.0 assertion
     */
    private org.apache.ws.policy.Assertion buildWss10() {
        PrimitiveAssertion wss10 = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_WSS10, PREFIX_SECURITY_POLICY));
        Policy wss10Policy = new Policy();
        wss10.addTerm(wss10Policy);

        wss10Policy.addTerm(new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_MUSTSUPPORT_REF_KEY_ID, PREFIX_SECURITY_POLICY)));
        wss10Policy.addTerm(new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_MUSTSUPPORT_REF_ISSUER_SERIAL, PREFIX_SECURITY_POLICY)));

        return wss10;
    }

    /**
     * Build an asymmetric binding assertion and siblings, attach to the given assertion
     */
    private void buildAsymmetricBinding(org.apache.ws.policy.Assertion assertion, int algorithmSuite) {
        QName name = new QName(NAMESPACE_SECURITY_POLICY, SPELE_BINDING_ASYMMETRIC, PREFIX_SECURITY_POLICY);
        PrimitiveAssertion binding = new PrimitiveAssertion(name);

        Policy bindingPolicy = new Policy();
        binding.addTerm(bindingPolicy);

        bindingPolicy.addTerm(buildX509Token(SPELE_TOKEN_RECIPIENT, SPELE_TOKEN_X509_WSS10, SPVALUE_INCL_TOKEN_NEVER));
        bindingPolicy.addTerm(buildX509Token(SPELE_TOKEN_INITIATOR, SPELE_TOKEN_X509_WSS10, SPVALUE_INCL_TOKEN_ALWAYSTORECIPIENT));
        bindingPolicy.addTerm(buildAlgorithmSuite(algorithmSuite));
        bindingPolicy.addTerm(buildLayout());
        bindingPolicy.addTerm(buildTimestamp());
        bindingPolicy.addTerm(buildDefaultSignature());

        org.apache.ws.policy.Assertion outerPolicy = assertion;
        outerPolicy.addTerm(binding);
        outerPolicy.addTerm(buildWss10());
    }

    /**
     * Build a transport binding assertion and siblings, attach to the given assertion
     */
    private void buildTransportBinding(org.apache.ws.policy.Assertion assertion, int algorithmSuite, Collection l7Assertions) {
        QName name = new QName(NAMESPACE_SECURITY_POLICY, SPELE_BINDING_TRANSPORT, PREFIX_SECURITY_POLICY);
        PrimitiveAssertion binding = new PrimitiveAssertion(name);

        Policy bindingPolicy = new Policy();
        binding.addTerm(bindingPolicy);

        boolean ssl = false;
        if (containsInstanceOf(l7Assertions, SslAssertion.class)) {
            ssl = true;
            SslAssertion sslAssertion = (SslAssertion) getInstanceOf(l7Assertions, SslAssertion.class);
            bindingPolicy.addTerm(buildTransportToken(sslAssertion.isRequireClientAuthentication()));
        }

        bindingPolicy.addTerm(buildAlgorithmSuite(algorithmSuite));
        bindingPolicy.addTerm(buildLayout());
        if (containsInstanceOf(l7Assertions, RequestWssTimestamp.class) ||
            containsInstanceOf(l7Assertions, ResponseWssTimestamp.class)) {
            bindingPolicy.addTerm(buildTimestamp());
        }

        org.apache.ws.policy.Assertion outerPolicy = assertion;
        outerPolicy.addTerm(binding);

        if (containsInstanceOf(l7Assertions, WssBasic.class)) {
            String supportingTokenEleName = ssl ? SPELE_SIGNED_SUPPORTING_TOKENS : SPELE_SUPPORTING_TOKENS;
            PrimitiveAssertion supportingTokens = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, supportingTokenEleName, PREFIX_SECURITY_POLICY));
            Policy supportingTokensPolicy = new Policy();
            supportingTokens.addTerm(supportingTokensPolicy);

            supportingTokensPolicy.addTerm(buildUsernameToken());

            outerPolicy.addTerm(supportingTokens);
            outerPolicy.addTerm(buildWss10());
        }
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

    private void buildParts(org.apache.ws.policy.Assertion assertion, Collection xpathAssertions) {
        boolean bodyDone = false;
        for (Iterator assertionIter=xpathAssertions.iterator(); assertionIter.hasNext();) {
            XpathBasedAssertion confAssertion = (XpathBasedAssertion) assertionIter.next();

            String pattern = confAssertion.pattern();
            Map namespaces = confAssertion.namespaceMap();
            pattern = expandXpath(pattern, namespaces);

            if(pattern.equals(BODY_PATTERN)) {
                if (!bodyDone) {
                    bodyDone = true;
                    assertion.addTerm(new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_PART_BODY, PREFIX_SECURITY_POLICY)));
                }
            }
            else {
                Matcher headerMatcher = HEADER_PATTERN.matcher(pattern);
                if (headerMatcher.matches()) {
                    PrimitiveAssertion headerAssertion = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_PART_HEADER, PREFIX_SECURITY_POLICY));
                    headerAssertion.addAttribute(new QName(SPATTR_NAMESPACE), headerMatcher.group(1));
                    String elementName = headerMatcher.group(2);
                    if (!"*".equals(elementName)) {
                        headerAssertion.addAttribute(new QName(SPATTR_NAME), elementName);
                    }
                    assertion.addTerm(headerAssertion);
                }
            }
        }
    }

    /**
     * This just expands the assertions XPath to be QNameish then checks it is a simple
     * XPath for the body "/Envelope/Body" or a header "/Envelope/Header/{xx}yy"
     */
    private void ensureHeaderOrBodyXpathsOnly(Collection xpathAssertions) throws PolicyAssertionException {
        for (Iterator iterator = xpathAssertions.iterator(); iterator.hasNext();) {
            XpathBasedAssertion xpathBasedAssertion = (XpathBasedAssertion) iterator.next();
            String pattern = xpathBasedAssertion.pattern();
            Map namespaces = xpathBasedAssertion.namespaceMap();

            pattern = expandXpath(pattern, namespaces);

            if(!pattern.equals(BODY_PATTERN)) {
                Matcher headerMatcher = HEADER_PATTERN.matcher(pattern);
                if (!headerMatcher.matches())
                    throw new PolicyAssertionException(xpathBasedAssertion, "Assertion XPath not supported (Body or Headers only '"+xpathBasedAssertion.pattern()+"')");
            }
        }
    }

    /**
     * Expands the XPath to be QNameish
     */
    private String expandXpath(String pattern, Map namespaces) {

        for(Iterator iterator1 = namespaces.entrySet().iterator(); iterator1.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator1.next();
            String prefix = (String) entry.getKey();
            String uri = (String) entry.getValue();

            pattern = pattern.replaceAll("(?!/)"+prefix+":", "{"+uri+"}");
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
    private boolean containsInstanceOf(Collection items, Class clazz) {
        boolean contains = false;

        for (Iterator iterator = items.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if(clazz.isInstance(object)) {
                contains = true;
                break;
            }
        }

        return contains;
    }

    /**
     * Get the first instance of the given class from the given collection
     */
    private Object getInstanceOf(Collection items, Class clazz) {
        Object found = null;

        for (Iterator iterator = items.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if(clazz.isInstance(object)) {
                found = object;
                break;
            }
        }

        return found;
    }

    /**
     * Get all the instances of the given class from the given collection
     */
    private Collection getInstancesOf(Collection items, Class clazz) {
        List found = new ArrayList();

        for (Iterator iterator = items.iterator(); iterator.hasNext();) {
            Object object = iterator.next();
            if(clazz.isInstance(object)) {
                found.add(object);
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
        Map nsMap = XmlUtil.getNamespaceMap(targetElement);

        for (int n=0; n<PREFERRED_NAMESPACE_PREFIXES.length; n++) {
            String prefix = (String) PREFERRED_NAMESPACE_PREFIXES[n][0];
            String namesp = (String) PREFERRED_NAMESPACE_PREFIXES[n][1];

            if (!nsMap.containsKey(prefix)) {
                boolean found = false;

                for (Iterator iterator = nsMap.entrySet().iterator(); iterator.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    String currentNS = (String) entry.getValue();
                    if (namesp.equals(currentNS)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    if (prefix == null || XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                        targetElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, namesp);
                    }
                    else {
                        targetElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namesp);
                    }
                }
            }
        }
    }

    private static Element buildPolicyReference(Document factory, Policy policy) {
        Element policyReference = factory.createElementNS("http://schemas.xmlsoap.org/ws/2004/09/policy", "wsp:PolicyReference");
        policyReference.setAttribute("URI", "#"+policy.getId());
        policyReference.setAttribute("xmlns:wsp", "http://schemas.xmlsoap.org/ws/2004/09/policy");
        return policyReference;
    }

    /**
     * TODO ensure there are no issues with the DOMResultXMLStreamWriter (see backup below ...)
     */
    private static Element toElement(Document target, StAXPolicyWriter pw, Policy policy) throws PolicyAssertionException {
        try {
            Map nsMap = XmlUtil.getNamespaceMap(target.getDocumentElement());
            Element container = target.createElement("fragmentWithNamespaces");
            for(Iterator iterator = nsMap.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String prefix = (String) entry.getKey();
                String namesp = (String) entry.getValue();

                if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    container.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, namesp);
                }
                else {
                    container.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namesp);
                }
            }
            pw.writePolicy(policy, new DOMResultXMLStreamWriter(new DOMResult(container)));
            return (Element) container.getFirstChild();
        }
        catch(XMLStreamException xse) {
            throw new PolicyAssertionException(null, "Could not create DOM from WS-SecurityPolicy", xse);
        }
    }

    /**
     * Alternative policy -> DOM method
     * /
    private static Element toElement2(Document target, StAXPolicyWriter pw, Policy policy) throws PolicyAssertionException {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            pw.writePolicy(policy, baos);
            return (Element) target.importNode(XmlUtil.stringToDocument(HexUtils.decodeUtf8(baos.toByteArray())).getDocumentElement(), true);
        }
        catch(org.xml.sax.SAXException se) {
            throw new PolicyAssertionException(null, "Could not create DOM from WS-SecurityPolicy", se);
        }
    } */

}
