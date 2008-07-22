package com.l7tech.policy.wssp;

import com.l7tech.xml.DOMResultXMLStreamWriter;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.ws.policy.Policy;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.StAXPolicyWriter;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.dom.DOMResult;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class WsspWriterTest extends TestCase {

    public static Test suite() {
        return new TestSuite(WsspWriterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testWriteA11() throws Exception {
        test(L7_POLICY_A11);
    }

    public void testWriteA12() throws Exception {
        test(L7_POLICY_A12);
    }

    public void testWriteT1() throws Exception {
        test(L7_POLICY_T1);
    }

    public void testDOMWrite() throws Exception {
        Policy wssp = new WsspWriter().convertFromLayer7(WspReader.getDefault().parsePermissively( XmlUtil.stringToDocument(L7_POLICY_T1).getDocumentElement()));
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

    private void test(String l7policyXmlStr) throws Exception {
        Assertion ass = WspReader.getDefault().parsePermissively(XmlUtil.stringToDocument(l7policyXmlStr).getDocumentElement());
        Policy p = new WsspWriter().convertFromLayer7(ass);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER).writePolicy(p, baos);
        String policyDocStr = new String(baos.toByteArray(), "UTF-8");
        System.out.println(XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(policyDocStr)));
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
}
