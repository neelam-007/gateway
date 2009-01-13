/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wssp;

import com.l7tech.common.io.BufferPoolByteArrayOutputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
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
import java.io.UnsupportedEncodingException;
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
     */
    public static void decorate(Document wsdl, Assertion layer7Root) throws PolicyAssertionException {
        addPreferredNamespacesIfAvailable(wsdl.getDocumentElement());
        WsspWriter wsspWriter = new WsspWriter();
        Policy wssp = wsspWriter.convertFromLayer7(layer7Root);
        Policy inputWssp = wsspWriter.convertFromLayer7(layer7Root, true);
        Policy outputWssp = wsspWriter.convertFromLayer7(layer7Root, false);

        wssp.setId(SoapUtil.generateUniqueId("policy", 1));
        inputWssp.setId( SoapUtil.generateUniqueId("policy", 2));
        outputWssp.setId(SoapUtil.generateUniqueId("policy", 3));

        Element wsspElement = toElement(wsdl, wssp);
        Element inputWsspElement = toElement(wsdl, inputWssp);
        Element outputWsspElement = toElement(wsdl, outputWssp);

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
     * @return  the converted Apache Policy.  Never null.
     */
    public Policy convertFromLayer7(Assertion layer7Root) throws PolicyAssertionException {
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
        if (isAsymmetricBinding(l7Assertions)) {
            buildAsymmetricBinding(wssp, algorithmSuite);
        } else if (isTransportBinding(l7Assertions)) {
            buildTransportBinding(wssp, algorithmSuite, l7Assertions);
            wssp.addTerm(buildWss10());
        }

        boolean ssl = isSslPolicy(l7Assertions);

        if (containsInstanceOf(l7Assertions, WssBasic.class)) {
            buildSupportingToken(wssp, ssl, false);
        }

        if (containsInstanceOf(l7Assertions, WssDigest.class)) {
            buildSupportingToken(wssp, ssl, true);
        }


        return wssp;
    }

    private void buildSupportingToken(Policy wssp, boolean ssl, boolean digest) {
        String supportingTokenEleName = ssl ? SPELE_SIGNED_SUPPORTING_TOKENS : SPELE_SUPPORTING_TOKENS;
        PrimitiveAssertion supportingTokens = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, supportingTokenEleName, PREFIX_SECURITY_POLICY));
        Policy supportingTokensPolicy = new Policy();
        supportingTokens.addTerm(supportingTokensPolicy);

        supportingTokensPolicy.addTerm(buildUsernameToken(digest));

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
     * @param isInput true if this is the input message
     * @return  the converted Apache Policy.  Never null.
     */
    public Policy convertFromLayer7(Assertion layer7Root, boolean isInput) throws PolicyAssertionException {
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
            {"wsp",  "http://www.w3.org/ns/ws-policy"},
            {"wsu",  "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"},
            {"sp",   "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200702"},
            {"sp13", "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200802"}

    };

    private static final boolean USE_NEW_WSP_NS = SyspropUtil.getBoolean("com.l7tech.policy.wssp.useNewWsspNs", false);
    public static final String WSP_NS_OLD = "http://schemas.xmlsoap.org/ws/2004/09/policy";
    public static final String WSP_NS_NEW = "http://www.w3.org/ns/ws-policy";
    public static final String WSP_NS = USE_NEW_WSP_NS ? WSP_NS_NEW : WSP_NS_OLD;

    // Security Policy Elements
    private static final String SPELE_TOKEN_RECIPIENT = "RecipientToken";
    private static final String SPELE_TOKEN_X509 = "X509Token";
    private static final String SPELE_TOKEN_X509_WSS10 = "WssX509V3Token10";
    private static final String SPELE_TOKEN_TRANSPORT = "TransportToken";
    private static final String SPELE_TOKEN_TRANSPORT_HTTPS = "HttpsToken";
    private static final String SPELE_TOKEN_USERNAME = "UsernameToken";
    private static final String SPELE_HASH_PASSWORD = "HashPassword";
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
    private static final Collection<Class<? extends Assertion>> WSS_ASSERTIONS = Collections.unmodifiableCollection(Arrays.<Class<? extends Assertion>>asList(
        RequestWssX509Cert.class,
        RequestWssIntegrity.class,
        RequestWssConfidentiality.class,
        ResponseWssIntegrity.class,
        ResponseWssConfidentiality.class
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
    private void checkConvertable(Collection<Assertion> assertions) throws PolicyAssertionException {
        boolean sslCertIdentity = false;
        boolean usesWss = false;

        for (Assertion assertion : assertions) {

            if (!isSupportedAssertion(assertion)) {
                throw new PolicyAssertionException(assertion, "Assertion not supported: " + assertion);
            } else if (isWssAssertion(assertion)) {
                usesWss = true;

                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, RequestWssConfidentiality.class));
                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, ResponseWssConfidentiality.class));
                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, RequestWssIntegrity.class));
                ensureHeaderOrBodyXpathsOnly(this.<XpathBasedAssertion>getInstancesOf(assertions, ResponseWssIntegrity.class));
            } else if (assertion instanceof SslAssertion) {
                SslAssertion sslAssertion = (SslAssertion) assertion;
                if (sslAssertion.isRequireClientAuthentication()) {
                    sslCertIdentity = true;
                }
            }
        }

        if(sslCertIdentity && usesWss)
            throw new PolicyAssertionException(null, "Cannot use WSS and TLS with client cert.");
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
            if (assertion instanceof RequestWssConfidentiality) {
                RequestWssConfidentiality rwc = (RequestWssConfidentiality) assertion;
                algEncStr = rwc.getXEncAlgorithm();
                keyEncAlgStr = rwc.getKeyEncryptionAlgorithm();
            } else if (assertion instanceof ResponseWssConfidentiality) {
                ResponseWssConfidentiality rwc = (ResponseWssConfidentiality) assertion;
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
     * @param digest
     */
    private org.apache.ws.policy.Assertion buildUsernameToken(boolean digest) {
        PrimitiveAssertion usernameToken = new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_TOKEN_USERNAME, PREFIX_SECURITY_POLICY));
        usernameToken.addAttribute(new QName(NAMESPACE_SECURITY_POLICY, SPATTR_INCL_TOKEN, PREFIX_SECURITY_POLICY), SPVALUE_INCL_TOKEN_ALWAYSTORECIPIENT);
        if (digest)
            usernameToken.addTerm(new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_HASH_PASSWORD, PREFIX_SECURITY_POLICY)));
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

        assertion.addTerm(binding);
        assertion.addTerm(buildWss10());
    }

    /**
     * Build a transport binding assertion and siblings, attach to the given assertion
     */
    private void buildTransportBinding(org.apache.ws.policy.Assertion assertion, int algorithmSuite, Collection l7Assertions) {
        QName name = new QName(NAMESPACE_SECURITY_POLICY, SPELE_BINDING_TRANSPORT, PREFIX_SECURITY_POLICY);
        PrimitiveAssertion binding = new PrimitiveAssertion(name);

        Policy bindingPolicy = new Policy();
        binding.addTerm(bindingPolicy);

        if (containsInstanceOf(l7Assertions, SslAssertion.class)) {
            SslAssertion sslAssertion = getInstanceOf(l7Assertions, SslAssertion.class);
            bindingPolicy.addTerm(buildTransportToken(sslAssertion.isRequireClientAuthentication()));
        }

        bindingPolicy.addTerm(buildAlgorithmSuite(algorithmSuite));
        bindingPolicy.addTerm(buildLayout());
        if (containsInstanceOf(l7Assertions, RequestWssTimestamp.class) ||
            containsInstanceOf(l7Assertions, ResponseWssTimestamp.class)) {
            bindingPolicy.addTerm(buildTimestamp());
        }

        assertion.addTerm(binding);
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

            if (pattern.equals(BODY_PATTERN)) {
                if (!bodyDone) {
                    bodyDone = true;
                    assertion.addTerm(new PrimitiveAssertion(new QName(NAMESPACE_SECURITY_POLICY, SPELE_PART_BODY, PREFIX_SECURITY_POLICY)));
                }
            } else {
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
    private void ensureHeaderOrBodyXpathsOnly(Collection<XpathBasedAssertion> xpathAssertions) throws PolicyAssertionException {
        for (XpathBasedAssertion xpathBasedAssertion : xpathAssertions) {
            String pattern = xpathBasedAssertion.pattern();
            Map<String, String> namespaces = xpathBasedAssertion.namespaceMap();

            pattern = expandXpath(pattern, namespaces);

            if (!pattern.equals(BODY_PATTERN)) {
                Matcher headerMatcher = HEADER_PATTERN.matcher(pattern);
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
                        targetElement.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, namesp);
                    } else {
                        targetElement.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, namesp);
                    }
                }
            }
        }
    }

    private static Element buildPolicyReference(Document factory, Policy policy) {
        Element policyReference = factory.createElementNS(WSP_NS, "wsp:PolicyReference");
        policyReference.setAttribute("URI", "#"+policy.getId());
        policyReference.setAttribute("xmlns:wsp", WSP_NS);
        return policyReference;
    }

    private static Element toElement(Document target, Policy policy) throws PolicyAssertionException {
        return (Element)target.importNode(policyToElement(policy), true);
    }

    public static Element policyToElement(Policy p) throws PolicyAssertionException {
        try {
            return XmlUtil.stringToDocument(policyToXml(p)).getDocumentElement();
        } catch (SAXException e) {
            throw new PolicyAssertionException(null, e);
        }
    }

    public static String policyToXml(Policy p) throws PolicyAssertionException {
        if (USE_NEW_WSP_NS)
            return policyToXml(p, WSP_NS_OLD, WSP_NS_NEW);
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
        BufferPoolByteArrayOutputStream baos = new BufferPoolByteArrayOutputStream();
        try {
            PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER).writePolicy(p, baos);
            String xml = new String(baos.getPooledByteArray(), 0, baos.size(), "UTF-8");
            if (srcUri != null && dstUri != null) {
                // Translate namespace decls
                xml = xml.replaceAll("\\<\\?.*?\\?\\>", "");
                return xml.replaceAll("^([^\\>]+)" + srcUri, "$1" + dstUri);
            } else {
                return xml;
            }
        } catch (UnsupportedEncodingException e) {
            throw new PolicyAssertionException(null, e);
        } finally {
            ResourceUtils.closeQuietly(baos);
        }
    }
}
