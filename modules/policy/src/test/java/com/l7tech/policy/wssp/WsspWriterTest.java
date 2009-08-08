package com.l7tech.policy.wssp;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.xml.DOMResultXMLStreamWriter;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.StAXPolicyWriter;
import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsspWriterTest {
    private static final AssertionRegistry assertionRegistry = new AssertionRegistry();

    @BeforeClass
    public static void initWspReader() {
        WspConstants.setTypeMappingFinder(assertionRegistry);
        for (Assertion assertion : AllAssertions.SERIALIZABLE_EVERYTHING)
            assertionRegistry.registerAssertion(assertion.getClass());
    }

    @Test
    public void testWriteA11() throws Exception {
        test(L7_POLICY_A11);
    }

    @Test
    public void testWriteA12() throws Exception {
        test(L7_POLICY_A12);
    }

    @Test
    public void testWriteT1() throws Exception {
        test(L7_POLICY_T1);
    }

    @Test
    public void testWriteExample_2113() throws Exception {
        test(L7_POLICY_EXAMPLE_2113);
    }

    @Test
    public void testWriteExample_2131() throws Exception {
        test(L7_POLICY_EXAMPLE_2131);
    }

    @Test
    public void testWriteExample_222() throws Exception {
        test(L7_POLICY_EXAMPLE_222);
    }
    
    @Test
    public void testWriteExample_214() throws Exception {
        test(L7_POLICY_EXAMPLE_214);
    }

    @Test
    public void testWriteExample_241() throws Exception {
        test(L7_POLICY_EXAMPLE_241);
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

    private void test(String l7policyXmlStr) throws Exception {
        Assertion ass = parseL7(l7policyXmlStr);
        Policy p = new WsspWriter().convertFromLayer7(ass, false);
        String policyDocStr = WsspWriter.policyToXml(p);
        System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(policyDocStr)));
    }

    private Assertion parseL7(String l7policyXmlStr) throws InvalidPolicyStreamException, SAXException {
        return WspReader.getDefault().parsePermissively(XmlUtil.stringToDocument(l7policyXmlStr).getDocumentElement(), WspReader.INCLUDE_DISABLED);
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

    private static final String L7_POLICY_EXAMPLE_2131 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:EncryptedUsernameToken/>\n" +
            "            <L7p:RequestWssConfidentiality/>\n" +
            "            <L7p:ResponseWssIntegrity/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

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
}
