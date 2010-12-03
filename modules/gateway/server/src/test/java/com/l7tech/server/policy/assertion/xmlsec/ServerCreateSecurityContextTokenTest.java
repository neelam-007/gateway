package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.xmlsec.CreateSecurityContextToken;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.SecureConversationContextManager;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MockConfig;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import org.junit.Ignore;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * 
 */
public class ServerCreateSecurityContextTokenTest {

    private static final Logger logger = Logger.getLogger( ServerCreateSecurityContextTokenTest.class.getName() );

    private static final SecureConversationContextManager contextManager = new SecureConversationContextManager( new MockConfig( new Properties() ) );
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    static {
        beanFactory.addBean( "secureConversationContextManager", contextManager );
    }

    @Test
    public void testCreateContext() throws Exception {
        doCreateContext( false, 0 );
    }

    @Test
    public void testCreateContextLifetime() throws Exception {
        doCreateContext( true, 0, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                final Element identifierElement = getSingleChildElement( document.getDocumentElement() );
                final String contextId = XmlUtil.getTextValue( identifierElement );
                final SecureConversationSession session = contextManager.getSession( contextId );
                assertNotNull( "Secure conversation context", session );
                assertEquals( "Context lifetime", 1000, session.getExpiration()-session.getCreation() );
            }
        }  );
    }

    @Test
    public void testCreateContextWithEntropy() throws Exception {
        doCreateContext( true, 0, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                final Element identifierElement = getSingleChildElement( document.getDocumentElement() );
                final String contextId = XmlUtil.getTextValue( identifierElement );
                final SecureConversationSession session = contextManager.getSession( contextId );
                assertNotNull( "Secure conversation context", session );
                assertArrayEquals( "Context client entropy", HexUtils.decodeBase64( "BXD+ruMMCmBeSkCshKtsFMtk1wbWwSVCWNW7FPJ+SyU=" ), session.getClientEntropy() );
            }
        }  );
    }

    @BugNumber(9548)
    @Ignore
    @Test
    public void testCreateContextWithKeySize() throws Exception {
        doCreateContext( false, 512, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                final Element identifierElement = getSingleChildElement( document.getDocumentElement() );
                final String contextId = XmlUtil.getTextValue( identifierElement );
                final SecureConversationSession session = contextManager.getSession( contextId );
                assertNotNull( "Secure conversation context", session );
                assertEquals( "Context key size", 512, session.getKeySize() );
                assertEquals( "Context actual key size", 512, session.getSharedSecret().length * 8 );
            }
        } );

    }

    private void doCreateContext( final boolean entropy,
                                  final int keySize ) throws Exception {
        doCreateContext( entropy, keySize, null );
    }

    private void doCreateContext( final boolean entropy,
                                  final int keySize,
                                  final Functions.UnaryVoid<Document> validationCallback ) throws Exception {
        final CreateSecurityContextToken createSecurityContextToken = new CreateSecurityContextToken();
        createSecurityContextToken.setUseSystemDefaultSessionDuration(false);
        createSecurityContextToken.setLifetime( 1000 );
        createSecurityContextToken.setVariablePrefix( "create" );

        final ServerCreateSecurityContextToken serverCreateSecurityContextToken = new ServerCreateSecurityContextToken( createSecurityContextToken, beanFactory );

        final String entropyText =
                "            <wst:Entropy>\n" +
                "                <wst:BinarySecret Type=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512/Nonce\">BXD+ruMMCmBeSkCshKtsFMtk1wbWwSVCWNW7FPJ+SyU=</wst:BinarySecret>\n" +
                "            </wst:Entropy>\n";

        final String keySizeText =
                "            <wst:KeySize>"+keySize+"</wst:KeySize>\n";

        final Message request = new Message( XmlUtil.parse(
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <s:Header xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\">\n" +
                "        <wsa:MessageID>message1</wsa:MessageID>\n" +
                "        <wsa:Action>http://schemas.xmlsoap.org/ws/2005/02/trust/RST/SCT</wsa:Action>\n" +
                "    </s:Header>\n" +
                "    <s:Body>\n" +
                "        <wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n" +
                "            <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n" +
                        ( entropy ? entropyText : "" ) +
                        ( keySize > 0 ? keySizeText : "" ) +
                "        </wst:RequestSecurityToken>\n" +
                "    </s:Body>\n" +
                "</s:Envelope>" ));
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final LoginCredentials loginCredentials = LoginCredentials.makeLoginCredentials( new HttpBasicToken("Alice", "password".toCharArray()), HttpBasic.class );
        context.getAuthenticationContext( request ).addCredentials( loginCredentials );
        context.getAuthenticationContext( request ).addAuthenticationResult( new AuthenticationResult( new UserBean("Alice"), loginCredentials.getSecurityToken(), null, false ) );

        final AssertionStatus status = serverCreateSecurityContextToken.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final String token = (String) context.getVariable( "create.issuedSCT" );
        final Document tokenDoc = XmlUtil.parse( token );

        if ( validationCallback != null ) validationCallback.call( tokenDoc );

        ensureValidToken( tokenDoc );
    }

    private void ensureValidToken( final Document document ) throws InvalidDocumentFormatException, IOException {
        logger.info( XmlUtil.nodeToFormattedString( document ));

        XmlUtil.visitNodes( document.getDocumentElement(), new Functions.UnaryVoid<Node>(){
            @Override
            public void call( final Node node ) {
                if ( node.getNodeType() == Node.ATTRIBUTE_NODE ) {
                    Attr attributeNode = (Attr) node;

                    // Check wsu:Id validity
                    if ( SoapConstants.WSU_URIS.contains( attributeNode.getNamespaceURI() ) &&
                         "Id".equals(attributeNode.getLocalName()) ) {
                        assertTrue( "Valid wsu:Id - " + attributeNode.getValue(), XmlUtil.isValidXmlNcName( attributeNode.getValue() ) );
                    }
                }
            }
        } );
    }

    private Element getSingleChildElement( final Element parent ) {
        try {
            return XmlUtil.findOnlyOneChildElement( parent );
        } catch ( TooManyChildElementsException e ) {
            throw new RuntimeException(e);
        }
    }
}
