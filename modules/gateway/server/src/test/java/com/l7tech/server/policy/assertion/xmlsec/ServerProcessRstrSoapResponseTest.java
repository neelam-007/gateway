package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.ProcessRstrSoapResponse;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Element;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerProcessRstrSoapResponseTest {

    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    private static final String RSTR_MESSAGE =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "  <soap:Body>\n" +
            "    <wst:RequestSecurityTokenResponse xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n" +
            "      <wst:TokenType>http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/sct</wst:TokenType>\n" +
            "      <wst:RequestedSecurityToken>\n" +
            "        <wsc:SecurityContextToken xmlns:wsc=\"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512\">\n" +
            "          <wsc:Identifier>urn:tokenid</wsc:Identifier>\n" +
            "        </wsc:SecurityContextToken>\n" +
            "      </wst:RequestedSecurityToken>\n" +
            "      <wst:RequestedProofToken>\n" +
            "        <wst:BinarySecret>TWbYuUCjczs1n4hQEWENV8HkGMcHOgQlJJircNa1Cd4=</wst:BinarySecret>\n" +
            "      </wst:RequestedProofToken>\n" +
            "      <wst:Entropy>\n" +
            "        <wst:BinarySecret>BF9gTwzpFq81zxDC4GLOt3R4a6CGXzfTx2OR1fW9K68=</wst:BinarySecret>\n" +
            "      </wst:Entropy>\n" +
            "      <wst:Lifetime xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
            "        <wsu:Created>2011-01-12T12:00:00.000Z</wsu:Created>\n" +
            "        <wsu:Expires>2011-01-12T14:00:00.000Z</wsu:Expires>\n" +
            "      </wst:Lifetime>\n" +
            "    </wst:RequestSecurityTokenResponse>\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";

    private static final String RSTR_COLLECTION_MESSAGE =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "  <soap:Body>\n" +
            "    <wst:RequestSecurityTokenResponseCollection xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n" +
            "     <wst:RequestSecurityTokenResponse>\n" +
            "      <wst:TokenType>http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/sct</wst:TokenType>\n" +
            "      <wst:RequestedSecurityToken>\n" +
            "        <wsc:SecurityContextToken xmlns:wsc=\"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512\">\n" +
            "          <wsc:Identifier>urn:tokenid</wsc:Identifier>\n" +
            "        </wsc:SecurityContextToken>\n" +
            "      </wst:RequestedSecurityToken>\n" +
            "      <wst:RequestedProofToken>\n" +
            "        <wst:BinarySecret>TWbYuUCjczs1n4hQEWENV8HkGMcHOgQlJJircNa1Cd4=</wst:BinarySecret>\n" +
            "      </wst:RequestedProofToken>\n" +
            "      <wst:Entropy>\n" +
            "        <wst:BinarySecret>BF9gTwzpFq81zxDC4GLOt3R4a6CGXzfTx2OR1fW9K68=</wst:BinarySecret>\n" +
            "      </wst:Entropy>\n" +
            "      <wst:Lifetime xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
            "        <wsu:Created>2011-01-12T12:00:00.000Z</wsu:Created>\n" +
            "        <wsu:Expires>2011-01-12T14:00:00.000Z</wsu:Expires>\n" +
            "      </wst:Lifetime>\n" +
            "     </wst:RequestSecurityTokenResponse>\n" +
            "    </wst:RequestSecurityTokenResponseCollection>\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";

    private static final String RSTR_MESSAGE_ENC_KEY =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">\n" +
            "  <soap:Body>\n" +
            "    <wst:RequestSecurityTokenResponse xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n" +
            "      <wst:TokenType>http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512/sct</wst:TokenType>\n" +
            "      <wst:RequestedSecurityToken>\n" +
            "        <wsc:SecurityContextToken xmlns:wsc=\"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512\">\n" +
            "          <wsc:Identifier>urn:tokenid</wsc:Identifier>\n" +
            "        </wsc:SecurityContextToken>\n" +
            "      </wst:RequestedSecurityToken>\n" +
            "      <wst:RequestedProofToken>\n" +
            "        <xenc:EncryptedKey\n" +
            "                Id=\"EncryptedKey-0-7af04984d19bf1f6de7175308becabfd\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "                <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "                <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                    <wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                        <wsse:KeyIdentifier\n" +
            "                            EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\">Xeg55vRyK3ZhAEhEf+YT0z986L0=</wsse:KeyIdentifier>\n" +
            "                    </wsse:SecurityTokenReference>\n" +
            "                </dsig:KeyInfo>\n" +
            "                <xenc:CipherData>\n" +
            "                    <xenc:CipherValue>Hu3L2DgS0b1XytFGc59LKZKALtpMjeItMBlENjd69heRZDjZAlwCXrl9om+cceyuL+sUTVus620byulxgTtQC5US2xWnr1SpW9RneUgM7UT1MVwoGtWiFBQ4urtr6fVTsZai5ZWmIyVEYokodoHQHze0suGJc1eHtzFvlwTU8oA=</xenc:CipherValue>\n" +
            "                </xenc:CipherData>\n" +
            "            </xenc:EncryptedKey>\n" +
            "      </wst:RequestedProofToken>\n" +
            "      <wst:Entropy>\n" +
            "        <xenc:EncryptedKey\n" +
            "                Id=\"EncryptedKey-0-f5ef22e59934251867621aaac66791af\" xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "                <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "                <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                    <wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "                        <wsse:KeyIdentifier\n" +
            "                            EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509SubjectKeyIdentifier\">Xeg55vRyK3ZhAEhEf+YT0z986L0=</wsse:KeyIdentifier>\n" +
            "                    </wsse:SecurityTokenReference>\n" +
            "                </dsig:KeyInfo>\n" +
            "                <xenc:CipherData>\n" +
            "                    <xenc:CipherValue>J1IDtBZBlDsd6gqMEMkzzdO4nRtpvPz2jNseAghSfxdfgo+F1V9OygHDGblHbwLKueD5df/cufZxX21++k5TMC6E/Khf6fh3oXkEEG3Yni7jOb6I/UquGi7JucR6Xiu5RhwGnBbThEaOZiJrsFQWEMRH/yzTScIxLNEfSQ5K2m0=</xenc:CipherValue>\n" +
            "                </xenc:CipherData>\n" +
            "            </xenc:EncryptedKey>\n" +
            "      </wst:Entropy>\n" +
            "      <wst:Lifetime xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
            "        <wsu:Created>2011-01-12T12:00:00.000Z</wsu:Created>\n" +
            "        <wsu:Expires>2011-01-12T14:00:00.000Z</wsu:Expires>\n" +
            "      </wst:Lifetime>\n" +
            "    </wst:RequestSecurityTokenResponse>\n" +
            "  </soap:Body>\n" +
            "</soap:Envelope>";

    static {
        try {
            beanFactory.addBean( "securityTokenResolver", new SimpleSecurityTokenResolver(TestDocuments.getDotNetServerCertificate(), TestDocuments.getDotNetServerPrivateKey()) );
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEncrypted() throws Exception {
        final ProcessRstrSoapResponse processRstrSoapResponse = new ProcessRstrSoapResponse();
        processRstrSoapResponse.setTokenType( SecurityTokenType.WSSC_CONTEXT );

        final ServerProcessRstrSoapResponse serverProcessRstrSoapResponse =
                new ServerProcessRstrSoapResponse( processRstrSoapResponse, beanFactory );

        final Message request = new Message( XmlUtil.parse(RSTR_MESSAGE_ENC_KEY) );
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverProcessRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final String entropy = (String) context.getVariable( "rstrResponseProcessor.serverEntropy" );
        final String secret = (String) context.getVariable( "rstrResponseProcessor.fullKey" );

        assertEquals( "entropy variable", "TYUuKs5n/dfq74NtipoUCg==", entropy );
        assertEquals( "secret variable", "ChwPqefb6cI65KKemlraIw==", secret );
    }

    @Test
    public void testVariables() throws Exception {
        final ProcessRstrSoapResponse processRstrSoapResponse = new ProcessRstrSoapResponse();
        processRstrSoapResponse.setTokenType( SecurityTokenType.WSSC_CONTEXT );
        processRstrSoapResponse.setVariablePrefix( "a" );

        final ServerProcessRstrSoapResponse serverProcessRstrSoapResponse =
                new ServerProcessRstrSoapResponse( processRstrSoapResponse, beanFactory );

        final Message request = new Message( XmlUtil.parse(RSTR_MESSAGE) );
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverProcessRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final Element token = (Element) context.getVariable( "a.token" );
        final String created = (String) context.getVariable( "a.createTime" );
        final String expires = (String) context.getVariable( "a.expiryTime" );
        final String entropy = (String) context.getVariable( "a.serverEntropy" );
        final String secret = (String) context.getVariable( "a.fullKey" );

        assertNotNull( "token variable", token );
        assertEquals( "created variable", "2011-01-12T12:00:00.000Z", created );
        assertEquals( "expires variable", "2011-01-12T14:00:00.000Z", expires );
        assertEquals( "entropy variable", "BF9gTwzpFq81zxDC4GLOt3R4a6CGXzfTx2OR1fW9K68=", entropy );
        assertEquals( "secret variable", "TWbYuUCjczs1n4hQEWENV8HkGMcHOgQlJJircNa1Cd4=", secret );
    }

    @Test
    public void testResponseCollection() throws Exception {
        final ProcessRstrSoapResponse processRstrSoapResponse = new ProcessRstrSoapResponse();
        processRstrSoapResponse.setTokenType( SecurityTokenType.WSSC_CONTEXT );

        final ServerProcessRstrSoapResponse serverProcessRstrSoapResponse =
                new ServerProcessRstrSoapResponse( processRstrSoapResponse, beanFactory );

        final Message request = new Message( XmlUtil.parse(RSTR_COLLECTION_MESSAGE) );
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverProcessRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );
    }

    @Test
    public void testUnexpectedToken() throws Exception {
        final ProcessRstrSoapResponse processRstrSoapResponse = new ProcessRstrSoapResponse();
        processRstrSoapResponse.setTokenType( SecurityTokenType.SAML2_ASSERTION );

        final ServerProcessRstrSoapResponse serverProcessRstrSoapResponse =
                new ServerProcessRstrSoapResponse( processRstrSoapResponse, beanFactory );

        final Message request = new Message( XmlUtil.parse(RSTR_MESSAGE) );
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverProcessRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.FALSIFIED, status );
    }

    @Test
    public void testAnyToken() throws Exception {
        final ProcessRstrSoapResponse processRstrSoapResponse = new ProcessRstrSoapResponse();
        processRstrSoapResponse.setTokenType( SecurityTokenType.UNKNOWN );

        final ServerProcessRstrSoapResponse serverProcessRstrSoapResponse =
                new ServerProcessRstrSoapResponse( processRstrSoapResponse, beanFactory );

        final Message request = new Message( XmlUtil.parse(RSTR_MESSAGE) );
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverProcessRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );
    }
}
