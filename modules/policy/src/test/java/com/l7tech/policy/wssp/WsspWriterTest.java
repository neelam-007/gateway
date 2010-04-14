package com.l7tech.policy.wssp;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.xml.DOMResultXMLStreamWriter;
import com.l7tech.util.ArrayUtils;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.StAXPolicyWriter;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsspWriterTest {
    private static final Logger logger = Logger.getLogger(WsspWriterTest.class.getName());
    private static final AssertionRegistry assertionRegistry = new AssertionRegistry();

    @BeforeClass
    public static void initWspReader() {
        WspConstants.setTypeMappingFinder(assertionRegistry);
        for (Assertion assertion : AllAssertions.SERIALIZABLE_EVERYTHING)
            assertionRegistry.registerAssertion(assertion.getClass());
    }

    @Test
    public void testWriteA11() throws Exception {
        test(L7_POLICY_A11, EXPECTED_WSSPWRITER_OUTPUT_A11);
    }

    @Test
    public void testWriteA12() throws Exception {
        test(L7_POLICY_A12, EXPECTED_WSSPWRITER_OUTPUT_A12);
    }

    @Test
    public void testWriteT1() throws Exception {
        test(L7_POLICY_T1, EXPECTED_WSSPWRITER_OUTPUT_T1);
    }

    @Test
    public void testWriteExample_2113() throws Exception {
        test(L7_POLICY_EXAMPLE_2113, EXPECTED_WSSPWRITER_OUTPUT_2113);
    }

    @Test
    public void testWriteExample_2131() throws Exception {
        test(L7_POLICY_EXAMPLE_2131, EXPECTED_WSSPWRITER_OUTPUT_2131);
    }

    @Test
    public void testWriteExample_222() throws Exception {
        test(L7_POLICY_EXAMPLE_222, EXPECTED_WSSPWRITER_OUTPUT_222);
    }

    @Test
    public void testWriteExample_214() throws Exception {
        test(L7_POLICY_EXAMPLE_214, EXPECTED_WSSPWRITER_OUTPUT_214);
    }

    @Test
    public void testWriteExample_241() throws Exception {
        test(L7_POLICY_EXAMPLE_241, EXPECTED_WSSPWRITER_OUTPUT_241);
    }

    @Test
    public void testWriteRaytheon() throws Exception {
        test(prefilter(L7_POLICY_RAYTHEON), EXPECTED_WSSPWRITER_OUTPUT_RAYTHEON);
    }

    private String prefilter(String l7policy) throws IOException {
        Assertion assertion = WspReader.getDefault().parseStrictly(l7policy, WspReader.Visibility.omitDisabled);
        assertion = new ClientAssertionFilter().filter(assertion);
        assertion = Assertion.simplify(assertion, true, false);
        logger.info("Filtered and simplified policy: " + assertion);
        return WspWriter.getPolicyXml(assertion);
    }

    @Test
    public void testDOMWrite() throws Exception {
        Policy wssp = new WsspWriter().convertFromLayer7(WspReader.getDefault().parsePermissively( XmlUtil.stringToDocument(L7_POLICY_T1).getDocumentElement(), WspReader.INCLUDE_DISABLED), false);
        StAXPolicyWriter pw = (StAXPolicyWriter) PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER);
        pw.writePolicy(wssp, (XMLStreamWriter)Proxy.newProxyInstance(WsspWriterTest.class.getClassLoader(), new Class[]{XMLStreamWriter.class}, new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                System.out.println(method.getName() + " - " + method.getParameterTypes().length);
                return null;
            }
        }));
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        pw.writePolicy(wssp, new DOMResultXMLStreamWriter(new DOMResult(document)));
        System.out.println(XmlUtil.nodeToFormattedString(document));
    }

    @Test
    public void testDecorateWsdl() throws Exception {
        final Document doc = XmlUtil.stringToDocument(PING_WSDL);
        WsspWriter.decorate(doc, parseL7(L7_POLICY_T1), false, null, null, null);
        System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(XmlUtil.nodeToString(doc))));
    }

    private void test(String l7policyXmlStr, String expectedOutput) throws Exception {
        Assertion ass = parseL7(l7policyXmlStr);
        Policy p = new WsspWriter().convertFromLayer7(ass, false);
        String policyDocStr = WsspWriter.policyToXml(p);
        final String got = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(policyDocStr));
        System.out.println(got);
        assertEquals(expectedOutput, got);
    }

    private Assertion parseL7(String l7policyXmlStr) throws InvalidPolicyStreamException, SAXException {
        return WspReader.getDefault().parsePermissively(XmlUtil.stringToDocument(l7policyXmlStr).getDocumentElement(), WspReader.INCLUDE_DISABLED);
    }


    private static class ClientAssertionFilter {
        public Assertion filter( final Assertion assertionTree ) {
            if (assertionTree == null) return null;
            applyRules(assertionTree, null);
            return assertionTree;
        }

        private boolean applyRules( final Assertion arg, final Iterator parentIterator ){
            // apply rules on this one
            if (arg instanceof CompositeAssertion) {
                // apply rules to children
                CompositeAssertion root = (CompositeAssertion)arg;
                Iterator i = root.getChildren().iterator();
                while (i.hasNext()) {
                    Assertion kid = (Assertion)i.next();
                    if (kid.isEnabled()) {
                        applyRules(kid, i);
                    }
                    else {
                        // If it is disabled, then ignore it.
                        i.remove();
                    }
                }
                // if all children of this composite were removed, we have to remove it from it's parent
                if (root.getChildren().isEmpty() && parentIterator != null) {
                    parentIterator.remove();
                    return true;
                }
            } else {
                if ( ( !arg.isEnabled() ||
                        (!Assertion.isRequest(arg) && !Assertion.isResponse(arg)) ||
                        (Assertion.isRequest(arg) && !ArrayUtils.contains((String[])arg.meta().get(AssertionMetadata.CLIENT_ASSERTION_TARGETS), "request")) ||
                        (Assertion.isResponse(arg) && !ArrayUtils.contains((String[])arg.meta().get(AssertionMetadata.CLIENT_ASSERTION_TARGETS), "response")))
                        && parentIterator != null) {
                    parentIterator.remove();
                    return true;
                }

                if (Boolean.TRUE.equals(arg.meta().get(AssertionMetadata.USED_BY_CLIENT)))
                    return false;

                if (parentIterator == null) {
                    throw new RuntimeException("Invalid policy, all policies must have a composite assertion at the root");
                }

                parentIterator.remove();
                return true;
            }
            return false;
        }
    }


    static final String L7_POLICY_T1 =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SslAssertion/>\n" +
            "        <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:TokenType>http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd#UsernameToken</wsse:TokenType>\n" +
            "            <L7p:Properties/>\n" +
            "        </wsse:SecurityToken>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_T1 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:TransportBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:TransportToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:HttpsToken RequireClientCertificate=\"false\"/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:TransportToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic256Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:TransportBinding>\n" +
                    "    <sp:SignedSupportingTokens xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:UsernameToken sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:WssUsernameToken10/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:UsernameToken>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:SignedSupportingTokens>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    static final String WSSP_POLICY_T1 =
            "  <wsp:Policy wsu:Id=\"T1Endpoint\">\n" +
            "    <!-- Policy alternative T1 - Anonymous client -->\n" +
            "    <sp:TransportBinding>\n" +
            "      <wsp:Policy>\n" +
            "        <sp:TransportToken>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:HttpsToken RequireClientCertificate=\"false\"/>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:TransportToken>\n" +
            "        <sp:AlgorithmSuite>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:Basic256Rsa15/>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:AlgorithmSuite>\n" +
            "        <sp:Layout>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:Lax/>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:Layout>\n" +
            "        <sp:IncludeTimestamp/>\n" +
            "      </wsp:Policy>\n" +
            "    </sp:TransportBinding>\n" +
            "  </wsp:Policy>";

    static final String L7_POLICY_T3 =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SslAssertion/>\n" +
            "        <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:TokenType>http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd#UsernameToken</wsse:TokenType>\n" +
            "            <L7p:Properties/>\n" +
            "        </wsse:SecurityToken>\n" +
            "        <L7p:RequestWssTimestamp>\n" +
            "            <L7p:SignatureRequired booleanValue=\"false\"/>\n" +
            "        </L7p:RequestWssTimestamp>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String L7_POLICY_A11 =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" " +
            "            xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:TokenType>http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3</wsse:TokenType>\n" +
            "            <L7p:Properties/>\n" +
            "        </wsse:SecurityToken>\n" +
            "        <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:MessageParts\n" +
            "                Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "        </wsse:Integrity>\n" +
            "        <wsse:Confidentiality wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:MessageParts\n" +
            "                Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "            <wsse:Algorithm URI=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "        </wsse:Confidentiality>\n" +
            "        <L7p:ResponseWssIntegrity/>\n" +
            "        <L7p:ResponseWssConfidentiality>\n" +
            "            <L7p:XEncAlgorithm stringValue=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "        </L7p:ResponseWssConfidentiality>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_A11 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:AsymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:RecipientToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:RecipientToken>\n" +
                    "            <sp:InitiatorToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:InitiatorToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic256Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "            <sp:IncludeTimestamp/>\n" +
                    "            <sp:OnlySignEntireHeadersAndBody/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:AsymmetricBinding>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    static final String WSSP_POLICY_A11 =
            "<wsp:Policy wsu:Id=\"A11Endpoint\">\n" +
            "    <!-- Asymmetric Policy A11 - X509 with mutual authentication and AES 256 -->\n" +
            "    <sp:AsymmetricBinding>\n" +
            "      <wsp:Policy>\n" +
            "        <sp:RecipientToken>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never\">\n" +
            "              <wsp:Policy>\n" +
            "                <sp:WssX509V3Token10/>\n" +
            "              </wsp:Policy>\n" +
            "            </sp:X509Token>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:RecipientToken>\n" +
            "        <sp:InitiatorToken>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
            "              <wsp:Policy>\n" +
            "                <sp:WssX509V3Token10/>\n" +
            "              </wsp:Policy>\n" +
            "            </sp:X509Token>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:InitiatorToken>\n" +
            "        <sp:AlgorithmSuite>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:Basic256Rsa15/>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:AlgorithmSuite>\n" +
            "        <sp:Layout>\n" +
            "          <wsp:Policy>\n" +
            "            <sp:Lax/>\n" +
            "          </wsp:Policy>\n" +
            "        </sp:Layout>\n" +
            "        <sp:IncludeTimestamp/>\n" +
            "        <sp:OnlySignEntireHeadersAndBody/>\n" +
            "      </wsp:Policy>\n" +
            "    </sp:AsymmetricBinding>\n" +
            "    <sp:Wss10>\n" +
            "      <wsp:Policy>\n" +
            "        <sp:MustSupportRefKeyIdentifier/>\n" +
            "        <sp:MustSupportRefIssuerSerial/>\n" +
            "      </wsp:Policy>\n" +
            "    </sp:Wss10>\n" +
            "  </wsp:Policy>";

    static final String L7_POLICY_A12 =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" " +
            "            xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:TokenType>http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3</wsse:TokenType>\n" +
            "            <L7p:Properties/>\n" +
            "        </wsse:SecurityToken>\n" +
            "        <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:MessageParts\n" +
            "                Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "        </wsse:Integrity>\n" +
            "        <wsse:Confidentiality wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:MessageParts\n" +
            "                Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "            <wsse:Algorithm URI=\"http://www.w3.org/2001/04/xmlenc#tripledes-cbc\"/>\n" +
            "        </wsse:Confidentiality>\n" +
            "        <L7p:ResponseWssIntegrity/>\n" +
            "        <L7p:ResponseWssConfidentiality>\n" +
            "            <L7p:XEncAlgorithm stringValue=\"http://www.w3.org/2001/04/xmlenc#tripledes-cbc\"/>\n" +
            "        </L7p:ResponseWssConfidentiality>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_A12 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:AsymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:RecipientToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:RecipientToken>\n" +
                    "            <sp:InitiatorToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:InitiatorToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:TripleDesRsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "            <sp:IncludeTimestamp/>\n" +
                    "            <sp:OnlySignEntireHeadersAndBody/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:AsymmetricBinding>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    static final String PING_WSDL =
            "<wsdl:definitions xmlns:tns=\"http://example.com/ws/2008/09/securitypolicy\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:wsoap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" targetNamespace=\"http://example.com/ws/2008/09/securitypolicy\">\n" +
            "  <wsdl:types>\n" +
            "    <xs:schema targetNamespace=\"http://example.com/ws/2008/09/securitypolicy\" blockDefault=\"#all\" elementFormDefault=\"qualified\">\n" +
            "      <xs:element name=\"EchoRequest\" type=\"xs:string\"/>\n" +
            "      <xs:element name=\"EchoResponse\" type=\"xs:string\"/>\n" +
            "    </xs:schema>\n" +
            "  </wsdl:types>\n" +
            "  <wsdl:message name=\"EchoInMessage\">\n" +
            "    <wsdl:part name=\"Body\" element=\"tns:EchoRequest\"/>\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:message name=\"EchoOutMessage\">\n" +
            "    <wsdl:part name=\"Body\" element=\"tns:EchoResponse\"/>\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:portType name=\"Test\">\n" +
            "    <wsdl:operation name=\"Echo\">\n" +
            "      <wsdl:input message=\"tns:EchoInMessage\"/>\n" +
            "      <wsdl:output message=\"tns:EchoOutMessage\"/>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:portType>\n" +
            "  <wsdl:binding name=\"NoSecurityBinding\" type=\"tns:Test\">\n" +
            "    <wsoap12:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "    <wsdl:operation name=\"Echo\">\n" +
            "      <wsdl:input>\n" +
            "        <wsoap12:body use=\"literal\"/>\n" +
            "      </wsdl:input>\n" +
            "      <wsdl:output>\n" +
            "        <wsoap12:body use=\"literal\"/>\n" +
            "      </wsdl:output>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:binding>\n" +
            "</wsdl:definitions>";

    private static final String L7_POLICY_EXAMPLE_2113 =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:WssDigest>\n" +
            "            <L7p:RequireNonce booleanValue=\"true\"/>\n" +
            "            <L7p:RequireTimestamp booleanValue=\"true\"/>\n" +
            "            <L7p:RequiredPassword stringValue=\"ecilA\"/>\n" +
            "        </L7p:WssDigest>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_2113 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\"/>\n";

    private static final String L7_POLICY_EXAMPLE_241 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "            <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                <wsse:TokenType>http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct</wsse:TokenType>\n" +
            "                <L7p:Properties/>\n" +
            "            </wsse:SecurityToken>\n" +
            "            <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                <wsse:MessageParts\n" +
            "                    Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "            </wsse:Integrity>\n" +
            "            <L7p:ResponseWssIntegrity/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_241 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:SymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:ProtectionToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:SecureConversationToken>\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:RequireDerivedKeys/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:SecureConversationToken>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:ProtectionToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic256Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:SymmetricBinding>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    private static final String L7_POLICY_EXAMPLE_2131 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:EncryptedUsernameToken/>\n" +
            "            <L7p:RequestWssConfidentiality/>\n" +
            "            <L7p:ResponseWssIntegrity/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_2131 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:SymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic128Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:SymmetricBinding>\n" +
                    "    <sp:SupportingTokens xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:UsernameToken sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:WssUsernameToken10/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:UsernameToken>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:SupportingTokens>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    private static final String L7_POLICY_EXAMPLE_214 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:EncryptedUsernameToken/>\n" +
            "            <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                <wsse:MessageParts\n" +
            "                    Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "            </wsse:Integrity>\n" +
            "            <L7p:RequestWssConfidentiality/>\n" +
            "            <L7p:ResponseWssIntegrity/>\n" +
            "            <L7p:ResponseWssConfidentiality/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_214 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:SymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic128Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:SymmetricBinding>\n" +
                    "    <sp:SupportingTokens xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:UsernameToken sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:WssUsernameToken10/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:UsernameToken>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:SupportingTokens>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    private static final String L7_POLICY_EXAMPLE_222 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "            <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                <wsse:TokenType>http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3</wsse:TokenType>\n" +
            "                <L7p:Properties/>\n" +
            "            </wsse:SecurityToken>\n" +
            "            <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                <wsse:MessageParts\n" +
            "                    Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "            </wsse:Integrity>\n" +
            "            <L7p:RequestWssConfidentiality>\n" +
            "                <L7p:XEncAlgorithm stringValue=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "                <L7p:XEncAlgorithmList stringListValue=\"included\">\n" +
            "                    <L7p:item stringValue=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "                </L7p:XEncAlgorithmList>\n" +
            "                <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                    <L7p:Expression stringValue=\"/soapenv:Envelope/soapenv:Body\"/>\n" +
            "                    <L7p:Namespaces mapValue=\"included\">\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"soapenv\"/>\n" +
            "                            <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"wsp\"/>\n" +
            "                            <L7p:value stringValue=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"tns\"/>\n" +
            "                            <L7p:value stringValue=\"http://example.com/ws/2008/09/securitypolicy\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"L7p\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.layer7tech.com/ws/policy\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"s12\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2003/05/soap-envelope\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"xsd\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"env\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2003/05/soap-envelope\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"xsi\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" +
            "                        </L7p:entry>\n" +
            "                    </L7p:Namespaces>\n" +
            "                </L7p:XpathExpression>\n" +
            "            </L7p:RequestWssConfidentiality>\n" +
            "            <L7p:ResponseWssIntegrity/>\n" +
            "            <L7p:ResponseWssConfidentiality>\n" +
            "                <L7p:XEncAlgorithm stringValue=\"http://www.w3.org/2001/04/xmlenc#aes256-cbc\"/>\n" +
            "                <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                    <L7p:Expression stringValue=\"/soapenv:Envelope/soapenv:Body\"/>\n" +
            "                    <L7p:Namespaces mapValue=\"included\">\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"soapenv\"/>\n" +
            "                            <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"wsp\"/>\n" +
            "                            <L7p:value stringValue=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"tns\"/>\n" +
            "                            <L7p:value stringValue=\"http://example.com/ws/2008/09/securitypolicy\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"L7p\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.layer7tech.com/ws/policy\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"s12\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2003/05/soap-envelope\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"xsd\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"env\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2003/05/soap-envelope\"/>\n" +
            "                        </L7p:entry>\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"xsi\"/>\n" +
            "                            <L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/>\n" +
            "                        </L7p:entry>\n" +
            "                    </L7p:Namespaces>\n" +
            "                </L7p:XpathExpression>\n" +
            "            </L7p:ResponseWssConfidentiality>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_222 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:AsymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:RecipientToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:RecipientToken>\n" +
                    "            <sp:InitiatorToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:InitiatorToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic256Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "            <sp:IncludeTimestamp/>\n" +
                    "            <sp:OnlySignEntireHeadersAndBody/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:AsymmetricBinding>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "</wsp:Policy>\n";

    private static final String L7_POLICY_RAYTHEON =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<exp:Export Version=\"3.0\"\n" +
                    "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                    "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\"\n" +
                    "xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "    <exp:References/>\n" +
                    "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                    "xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "        <wsp:All wsp:Usage=\"Required\">\n" +
                    "            <L7p:WsspAssertion/>\n" +
                    "            <L7p:SslAssertion>\n" +
                    "                <L7p:RequireClientAuthentication booleanValue=\"true\"/>\n" +
                    "            </L7p:SslAssertion>\n" +
                    "            <wsse:SecurityToken\n" +
                    "xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
                    "               \n" +
                    "<wsse:TokenType>http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3</wsse:TokenType>\n" +
                    "                <L7p:Properties/>\n" +
                    "            </wsse:SecurityToken>\n" +
                    "            <L7p:HttpRoutingAssertion>\n" +
                    "                <L7p:ProtectedServiceUrl\n" +
                    "stringValue=\"http://hugh/ACMEWarehouseWS/Service1.asmx\"/>\n" +
                    "                <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                    "                    <L7p:Rules httpPassthroughRules=\"included\">\n" +
                    "                        <L7p:item httpPassthroughRule=\"included\">\n" +
                    "                            <L7p:Name stringValue=\"Cookie\"/>\n" +
                    "                        </L7p:item>\n" +
                    "                        <L7p:item httpPassthroughRule=\"included\">\n" +
                    "                            <L7p:Name stringValue=\"SOAPAction\"/>\n" +
                    "                        </L7p:item>\n" +
                    "                    </L7p:Rules>\n" +
                    "                </L7p:RequestHeaderRules>\n" +
                    "                <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
                    "                    <L7p:ForwardAll booleanValue=\"true\"/>\n" +
                    "                    <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
                    "                </L7p:RequestParamRules>\n" +
                    "                <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
                    "                    <L7p:Rules httpPassthroughRules=\"included\">\n" +
                    "                        <L7p:item httpPassthroughRule=\"included\">\n" +
                    "                            <L7p:Name stringValue=\"Set-Cookie\"/>\n" +
                    "                        </L7p:item>\n" +
                    "                    </L7p:Rules>\n" +
                    "                </L7p:ResponseHeaderRules>\n" +
                    "            </L7p:HttpRoutingAssertion>\n" +
                    "        </wsp:All>\n" +
                    "    </wsp:Policy>\n" +
                    "</exp:Export>";

    static final String EXPECTED_WSSPWRITER_OUTPUT_RAYTHEON =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n" +
                    "    <sp:AsymmetricBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:RecipientToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/Never\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:RecipientToken>\n" +
                    "            <sp:InitiatorToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:X509Token sp:IncludeToken=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy/IncludeToken/AlwaysToRecipient\">\n" +
                    "                        <wsp:Policy>\n" +
                    "                            <sp:WssX509V3Token10/>\n" +
                    "                        </wsp:Policy>\n" +
                    "                    </sp:X509Token>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:InitiatorToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic256Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "            <sp:IncludeTimestamp/>\n" +
                    "            <sp:OnlySignEntireHeadersAndBody/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:AsymmetricBinding>\n" +
                    "    <sp:Wss10 xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:MustSupportRefKeyIdentifier/>\n" +
                    "            <sp:MustSupportRefIssuerSerial/>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:Wss10>\n" +
                    "    <sp:TransportBinding xmlns:sp=\"http://schemas.xmlsoap.org/ws/2005/07/securitypolicy\">\n" +
                    "        <wsp:Policy>\n" +
                    "            <sp:TransportToken>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:HttpsToken RequireClientCertificate=\"true\"/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:TransportToken>\n" +
                    "            <sp:AlgorithmSuite>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Basic256Rsa15/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:AlgorithmSuite>\n" +
                    "            <sp:Layout>\n" +
                    "                <wsp:Policy>\n" +
                    "                    <sp:Lax/>\n" +
                    "                </wsp:Policy>\n" +
                    "            </sp:Layout>\n" +
                    "        </wsp:Policy>\n" +
                    "    </sp:TransportBinding>\n" +
                    "</wsp:Policy>\n";
}
