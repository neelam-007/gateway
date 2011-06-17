package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.BuildRstrSoapResponse;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.xml.processor.MockProcessorResult;
import com.l7tech.security.xml.processor.X509BinarySecurityTokenImpl;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.server.secureconversation.SessionCreationException;
import com.l7tech.server.secureconversation.StoredSecureConversationSessionManagerStub;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.MockConfig;

import static org.junit.Assert.*;

import com.l7tech.util.SoapConstants;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 *
 */
public class ServerBuildRstrSoapResponseTest {

    private static final Logger logger = Logger.getLogger( ServerBuildRstrSoapResponseTest.class.getName() );

    private static final InboundSecureConversationContextManager contextManager = new InboundSecureConversationContextManager( new MockConfig( new Properties() ), new StoredSecureConversationSessionManagerStub() );
    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

    static {
        beanFactory.addBean( "inboundSecureConversationContextManager", contextManager );
    }

    @Test
    public void testCancellationResponse() throws Exception {
        final BuildRstrSoapResponse buildRstrSoapResponse = new BuildRstrSoapResponse();
        buildRstrSoapResponse.setResponseForIssuance( false );

        final ServerBuildRstrSoapResponse serverBuildRstrSoapResponse =
                new ServerBuildRstrSoapResponse( buildRstrSoapResponse, beanFactory );

        final Message request = new Message( XmlUtil.parse(
                "<s:Envelope \n" +
                "        xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "        xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\"\n" +
                "        xmlns:wss=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"\n" +
                "        xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\"\n" +
                "    >\n" +
                "    <s:Header>\n" +
                "        <wsa:MessageID>message1</wsa:MessageID>\n" +
                "        <wsa:Action>http://schemas.xmlsoap.org/ws/2005/02/trust/RST/SCT/Cancel</wsa:Action>\n" +
                "    </s:Header>\n" +
                "    <s:Body>\n" +
                "        <wst:RequestSecurityToken>\n" +
                "            <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Cancel</wst:RequestType>\n" +
                "            <wst:CancelTarget><wss:SecurityTokenReference><wss:Reference URI=\"token1\"/></wss:SecurityTokenReference></wst:CancelTarget>\n"+
                "        </wst:RequestSecurityToken>\n" +
                "    </s:Body>\n" +
                "</s:Envelope>" ));
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverBuildRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final Message rstrResponse = (Message) context.getVariable( "responseBuilder.rstrResponse" );
        final Document rstrDoc = rstrResponse.getXmlKnob().getDocumentReadOnly();

        ensureValidMessage( rstrDoc );
    }

    @Test
    public void testSAML11Response() throws Exception {
        doResponse( false, false, getSaml11Token() );
    }

    @Test
    public void testSAML11ResponseWithClientCert() throws Exception {
        doResponse( false, true, getSaml11Token() );
    }

    @Test
    public void testSAML20Response() throws Exception {
        doResponse( false, false, getSaml20Token() );
    }

    @Test
    public void testSAML20ResponseWithClientCert() throws Exception {
        doResponse( false, true, getSaml20Token() );
    }

    @Test
    public void testSecureConversationResponse() throws Exception {
        doResponse( false, false, generateContextToken(false) );
    }

    @Test
    public void testSecureConversationResponseWithEntropy() throws Exception {
        doResponse( true, false, generateContextToken(true)  );
    }

    @Test
    public void testSecureConversationResponseWithClientCert() throws Exception {
        doSecureConversationResponse( true, false, true, generateContextToken(false), null, null, getEncryptedKeyValidationCallback() );
    }

    @Test
    public void testSecureConversationResponseWithClientCertAndEntropy() throws Exception {
        doSecureConversationResponse( true, true, true, generateContextToken(true), null, null, getEncryptedKeyValidationCallback() );
    }

    @Test
    public void testResponseAppliesTo() throws Exception {
        // test no AppliesTo if not enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeAppliesTo( false );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "AppliesTo element count", 0L, (long) document.getElementsByTagNameNS( "*", "AppliesTo" ).getLength() );
            }
        } );

        // test value used when enabled.
        final String appliesToAddress = "http://layer7tech.com/nowhere";
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeAppliesTo( true );
                buildRstrSoapResponse.setAddressOfEPR( appliesToAddress );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "AppliesTo element count", 1L, (long) document.getElementsByTagNameNS( "*", "AppliesTo" ).getLength() );
                final Element appliesToElement = (Element) document.getElementsByTagNameNS( "*", "AppliesTo" ).item( 0 );
                final Element eprElement = getSingleChildElement( appliesToElement );
                final Element addressElement = getSingleChildElement( eprElement );
                assertEquals( "AppliesTo address", appliesToAddress, XmlUtil.getTextValue( addressElement ));
            }
        } );

    }

    @Test
    public void testResponseRequestedAttachedReference() throws Exception {
        // test no RequestedAttachedReference if not enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeAttachedRef( false );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "RequestedAttachedReference element count", 0L, (long) document.getElementsByTagNameNS( "*", "RequestedAttachedReference" ).getLength() );
            }
        } );

        // test RequestedAttachedReference present when enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeAttachedRef( true );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "RequestedAttachedReference element count", 1L, (long) document.getElementsByTagNameNS( "*", "RequestedAttachedReference" ).getLength() );
            }
        } );
    }

    @Test
    public void testResponseRequestedUnattachedReference() throws Exception {
        // test no RequestedUnattachedReference if not enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeUnattachedRef( false );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "RequestedUnattachedReference element count", 0L, (long) document.getElementsByTagNameNS( "*", "RequestedUnattachedReference" ).getLength() );
            }
        } );

        // test RequestedUnattachedReference present when enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeUnattachedRef( true );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "RequestedUnattachedReference element count", 1L, (long) document.getElementsByTagNameNS( "*", "RequestedUnattachedReference" ).getLength() );
            }
        } );
    }

    @Test
    public void testResponseLifetime() throws Exception {
        // test no lifetime if not enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeLifetime( false );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "Lifetime element count", 0L, (long) document.getElementsByTagNameNS( "*", "Lifetime" ).getLength() );
            }
        } );

        // test no lifetime present when enabled
        doSecureConversationResponse( true, false, true, getSaml11Token(), new Functions.UnaryVoid<BuildRstrSoapResponse>(){
            @Override
            public void call( final BuildRstrSoapResponse buildRstrSoapResponse ) {
                buildRstrSoapResponse.setIncludeLifetime( true );
                buildRstrSoapResponse.setLifetime( 1000L );
            }
        }, null, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                assertEquals( "Lifetime element count", 1L, (long) document.getElementsByTagNameNS( "*", "Lifetime" ).getLength() );
                final Element lifetimeElement = (Element) document.getElementsByTagNameNS( "*", "Lifetime" ).item( 0 );
                final Element createdElement = getChildElement( lifetimeElement, SoapConstants.WSU_NAMESPACE, "Created" );
                final Element expiresElement = getChildElement( lifetimeElement, SoapConstants.WSU_NAMESPACE, "Expires" );

                try {
                    final Date created = ISO8601Date.parse( XmlUtil.getTextValue(createdElement) );
                    final Date expires = ISO8601Date.parse( XmlUtil.getTextValue(expiresElement) );

                    assertEquals( "Response lifetime", 1000L, expires.getTime()-created.getTime() );
                } catch ( ParseException e ) {
                    throw new RuntimeException( e );
                }
            }
        } );
    }

    @Test
    public void testNamespaceConsistency() throws Exception {
        final String[] tokens = new String[]{ generateContextToken(false), getSaml11Token(), getSaml20Token() };
        
        for ( final String token : tokens ) {
            logger.info( "Testing for token with NS: " + XmlUtil.parse( token ).getDocumentElement().getNamespaceURI() );

            // Test SOAP NS matches the request message
            doNamespaceTest( true, token, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE );
            doNamespaceTest( true, token, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE );

            // Test supported ws-addressing versions
            for ( final String addressingUri : SoapConstants.WSA_NAMESPACE_ARRAY ) {
                doNamespaceTest( true, token, "http://www.w3.org/2005/08/addressing", addressingUri );
            }

            // Test supported ws-trust versions and that they default to the correct WSP / WSA versions
            final Map<String,String> trustToPolicyNsMap = new HashMap<String,String>(){{
                put( SoapConstants.WST_NAMESPACE, SoapConstants.WSP_NAMESPACE );
                put( SoapConstants.WST_NAMESPACE2, SoapConstants.WSP_NAMESPACE2 );
                put( SoapConstants.WST_NAMESPACE3, SoapConstants.WSP_NAMESPACE2 );
            }};
            final Map<String,String> trustToAddressingNsMap = new HashMap<String,String>(){{
                put( SoapConstants.WST_NAMESPACE, SoapConstants.WSA_NAMESPACE );
                put( SoapConstants.WST_NAMESPACE2, SoapConstants.WSA_NAMESPACE2 );
                put( SoapConstants.WST_NAMESPACE3, SoapConstants.WSA_NAMESPACE_10 );
            }};
            for ( final String trustUri : Arrays.asList( SoapConstants.WST_NAMESPACE, SoapConstants.WST_NAMESPACE2, SoapConstants.WST_NAMESPACE3 ) ) {
                doNamespaceTest( false, token, "http://docs.oasis-open.org/ws-sx/ws-trust/200512", trustUri, trustToPolicyNsMap.get(trustUri), trustToAddressingNsMap.get(trustUri) );
            }
        }
    }

    private void doNamespaceTest( final boolean addressing, final String token, final String oldNamespace, final String newNamespace, final String... otherNamespaces ) throws Exception {
        doSecureConversationResponse( addressing, false, true, token, null, new Functions.Unary<String,String>(){
            @Override
            public String call( final String s ) {
                String request = s.replace( oldNamespace, newNamespace );
                // fix inconsistent action for old ws-trust version
                request = request.replace( "http://schemas.xmlsoap.org/ws/2004/04/trust/Issue", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue" );
                request = request.replace( "http://schemas.xmlsoap.org/ws/2004/04/trust/RST/Issue", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/RST/Issue" );
                request = request.replace( "http://schemas.xmlsoap.org/ws/2004/04/trust/RST/SCT", "http://schemas.xmlsoap.org/ws/2004/04/security/trust/RST/SCT" );
                assertTrue( "Message updated", oldNamespace.equals( newNamespace ) || !s.equals(request) );
                return request;
            }
        }, new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                final boolean[] sawNewNs = new boolean[]{ false };
                final Set<String> seenOtherNamespaces = new HashSet<String>();
                XmlUtil.visitNodes( document.getDocumentElement(), new Functions.UnaryVoid<Node>(){
                    @Override
                    public void call( final Node node ) {
                        assertFalse( "Old namespace used : " + node, !oldNamespace.equals( newNamespace ) && oldNamespace.equals( node.getNamespaceURI() ));
                        if ( newNamespace.equals( node.getNamespaceURI() ) ) {
                            sawNewNs[0] =  true;
                        } else {
                            if ( node.getNamespaceURI() != null ) seenOtherNamespaces.add( node.getNamespaceURI() );
                        }
                    }
                } );
                Set<String> missingNs = new TreeSet<String>( Arrays.asList( otherNamespaces ));
                missingNs.removeAll( seenOtherNamespaces );
                assertTrue( "New namespace used - " + newNamespace, sawNewNs[0] );
                assertEquals( "Missing namespaces", Collections.<String>emptySet(), missingNs );
            }
        } );
    }

    private void doResponse( final boolean entropy,
                             final boolean clientCert,
                             final String token ) throws Exception {
        doSecureConversationResponse( true, entropy, clientCert, token, null, null, null );
    }

    private void doSecureConversationResponse( final boolean addressing,
                                               final boolean entropy,
                                               final boolean clientCert,
                                               final String token,
                                               final Functions.UnaryVoid<BuildRstrSoapResponse> configCallback,
                                               final Functions.Unary<String,String> requestCallback,
                                               final Functions.UnaryVoid<Document> validationCallback ) throws Exception {
        final BuildRstrSoapResponse buildRstrSoapResponse = new BuildRstrSoapResponse();

        buildRstrSoapResponse.setVariablePrefix( "rstr" );
        buildRstrSoapResponse.setResponseForIssuance( true );
        buildRstrSoapResponse.setIncludeKeySize( true );
        buildRstrSoapResponse.setIncludeAppliesTo( true );
        buildRstrSoapResponse.setAddressOfEPR( "http://gateway.l7tech.com/service" );
        buildRstrSoapResponse.setIncludeLifetime( true );
        buildRstrSoapResponse.setLifetime( 300000L );
        buildRstrSoapResponse.setIncludeAttachedRef( true );
        buildRstrSoapResponse.setIncludeUnattachedRef( true );
        buildRstrSoapResponse.setTokenIssued( token );

        if ( configCallback != null ) configCallback.call( buildRstrSoapResponse );

        final ServerBuildRstrSoapResponse serverBuildRstrSoapResponse =
                new ServerBuildRstrSoapResponse( buildRstrSoapResponse, beanFactory );

        final String entropyText =
                "            <wst:Entropy>\n" +
                "                <wst:BinarySecret Type=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512/Nonce\">BXD+ruMMCmBeSkCshKtsFMtk1wbWwSVCWNW7FPJ+SyU=</wst:BinarySecret>\n" +
                "            </wst:Entropy>\n";

        final String action = token.startsWith( "<saml" ) ?
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue" :
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/SCT";

        String requestText =
                "<s:Envelope\n" +
                "    xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                (addressing ? "    xmlns:wsa=\"http://www.w3.org/2005/08/addressing\"\n" : "") +
                "    xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\"\n" +
                "    >\n" +
                ( addressing ?
                "    <s:Header>\n" +
                "        <wsa:MessageID>message1</wsa:MessageID>\n" +
                "        <wsa:Action>"+action+"</wsa:Action>\n" +
                "    </s:Header>\n" : "" )+
                "    <s:Body>\n" +
                "        <wst:RequestSecurityToken>\n" +
                "            <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n" +
                ( entropy ? entropyText : "" ) +
                "        </wst:RequestSecurityToken>\n" +
                "    </s:Body>\n" +
                "</s:Envelope>" ;
        
        if ( requestCallback != null ) requestText = requestCallback.call( requestText );

        final Message request = new Message( XmlUtil.parse( requestText ) );
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        if ( clientCert ) {
            final MockProcessorResult mockResult = new MockProcessorResult(){
                final X509BinarySecurityTokenImpl token = new X509BinarySecurityTokenImpl( TestDocuments.getWssInteropAliceCert(), null);
                {
                   token.onPossessionProved();
                }

                @Override
                public XmlSecurityToken[] getXmlSecurityTokens() {
                    try {
                        return new  XmlSecurityToken[]{ token };
                    } catch ( Exception e ) {
                        throw new RuntimeException(e);
                    }
                }
            };
            request.getSecurityKnob().setProcessorResult( mockResult );
            context.getAuthenticationContext( request ).addCredentials( LoginCredentials.makeLoginCredentials( mockResult.getXmlSecurityTokens()[0], null ));
        }

        final AssertionStatus status = serverBuildRstrSoapResponse.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final Message rstrResponse = (Message) context.getVariable( "rstr.rstrResponse" );
        final Document rstrDoc = rstrResponse.getXmlKnob().getDocumentReadOnly();

        if ( validationCallback != null ) validationCallback.call( rstrDoc );

        ensureValidMessage( rstrDoc );
    }

    private Functions.UnaryVoid<Document> getEncryptedKeyValidationCallback() {
        return new Functions.UnaryVoid<Document>(){
            @Override
            public void call( final Document document ) {
                final NodeList encryptedKeyList = document.getElementsByTagNameNS( "http://www.w3.org/2001/04/xmlenc#", "EncryptedKey" );
                assertTrue( "Encrypted key(s) in response", encryptedKeyList.getLength() > 0 );

                final NodeList binarySecretList = document.getElementsByTagNameNS( "*", "BinarySecret" );
                assertTrue( "No binary secrets in response", binarySecretList.getLength() == 0 );
            }
        };
    }

    private String getSaml11Token() {
        return
                "<saml:Assertion xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" MinorVersion=\"1\" MajorVersion=\"1\" AssertionID=\"saml-1291246709466\" Issuer=\"OASIS Interop Test CA\" IssueInstant=\"2010-12-01T23:38:29.466Z\">\n" +
                "  <saml:Conditions NotBefore=\"2010-01-01T00:00:00.000Z\" NotOnOrAfter=\"2100-01-01T00:00:00.000Z\"/>\n" +
                "  <saml:AuthenticationStatement AuthenticationMethod=\"urn:oasis:names:tc:SAML:1.0:am:X509-PKI\" AuthenticationInstant=\"2010-12-01T23:38:29.466Z\">\n" +
                "    <saml:Subject>\n" +
                "      <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=Alice, OU=OASIS Interop Test Cert, O=OASIS</saml:NameIdentifier>\n" +
                "    </saml:Subject>\n" +
                "    <saml:SubjectLocality IPAddress=\"10.20.10.44\"/>\n" +
                "  </saml:AuthenticationStatement>\n" +
                "<ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#saml-1291246709466\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>zULzkEcouM2E7B54U1bDNBHqHXQ=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>XGkvZrqXuKHR/0k3NK6giKwW8B6tgS+aS1xjl2DNqoRiBGeLmptIdJi8kNC+UB6ZYwJhUCiYFyOB+ppCFJefTUll4vGJedu40K5VU/YZKqMFINtIO4xni2OalLOJj1B/zhQbfcoHkc/H2LqlEcMxjTa1qS9Ma6LVihgGIJhnc9WB9cL1NM+DdAhW6uf2gbyfwjM5Aql8ovnzEimT3sekKJq+cgGjNjV61xUt+FP+1jl8Xr/X88f/R6UREPPxk21GhciUnA9mAePepfKUeQYl2A9mafyLOzXMvGC6zTOCNzANky9k0hnyYYUjVQ+tUU19vqH2ITdyIOwYxHYXxtI0Bg==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:KeyIdentifier ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">Ej2H+Yjjzeek30ZkPazyS7CHVzc=</wsse:KeyIdentifier></wsse:SecurityTokenReference></KeyInfo></ds:Signature></saml:Assertion>";
    }

    private String getSaml20Token() {
        return
                "<saml2:Assertion Version=\"2.0\" ID=\"saml-1291246744225\" IssueInstant=\"2010-12-01T23:39:04.225Z\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">\n" +
                "  <saml2:Issuer>OASIS Interop Test CA</saml2:Issuer>\n" +
                "  <ds:Signature xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:SignedInfo><ds:CanonicalizationMethod Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/><ds:SignatureMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#rsa-sha1\"/><ds:Reference URI=\"#saml-1291246744225\"><ds:Transforms><ds:Transform Algorithm=\"http://www.w3.org/2000/09/xmldsig#enveloped-signature\"/><ds:Transform Algorithm=\"http://www.w3.org/2001/10/xml-exc-c14n#\"/></ds:Transforms><ds:DigestMethod Algorithm=\"http://www.w3.org/2000/09/xmldsig#sha1\"/><ds:DigestValue>I/0EbZJV/CvbZ2n0Oqg0D+rx+NU=</ds:DigestValue></ds:Reference></ds:SignedInfo><ds:SignatureValue>exfe1AmsEMGQCArataqRyUpJ4IarH4qEwslAYb5SUttlMyNQ5r/yIM1mtv1jBgrTz+NXjKpz6w6N0VN/i0lzRlp4P0qTjU3zKkmZ7QBF01RoQMDaOUT0hXQJgBrsUduGundxjXpDOEnvc0I9GepKi+h89Bdq7c+E6NxaBW932nGXvGKUtaeAB03FEAWrNEw7fb++VOGWdC8N+NXbuu4Z4iCYD913eOwXf904vSjkLWeqnAZz6hPTEnExOfyNii3qslAfILaFeFBGtkcHhItE6u5fRclXSXfoTClriIA4XxUIaIC+aZ3/I+hW+UmN1ECaa09n7yw6uAr1WxYMbt7KLQ==</ds:SignatureValue><KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><wsse:SecurityTokenReference xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:KeyIdentifier ValueType=\"http://docs.oasis-open.org/wss/oasis-wss-soap-message-security-1.1#ThumbprintSHA1\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">Ej2H+Yjjzeek30ZkPazyS7CHVzc=</wsse:KeyIdentifier></wsse:SecurityTokenReference></KeyInfo></ds:Signature><saml2:Subject>\n" +
                "    <saml2:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=Alice, OU=OASIS Interop Test Cert, O=OASIS</saml2:NameID>\n" +
                "  </saml2:Subject>\n" +
                "  <saml2:Conditions NotBefore=\"2010-01-01T00:00:00.000Z\" NotOnOrAfter=\"2100-01-01T00:00:00.000Z\"/>\n" +
                "  <saml2:AuthnStatement AuthnInstant=\"2010-12-01T23:39:04.225Z\">\n" +
                "    <saml2:SubjectLocality Address=\"10.20.10.44\"/>\n" +
                "    <saml2:AuthnContext>\n" +
                "      <saml2:AuthnContextClassRef>urn:oasis:names:tc:SAML:1.0:am:X509-PKI</saml2:AuthnContextClassRef>\n" +
                "    </saml2:AuthnContext>\n" +
                "  </saml2:AuthnStatement>\n" +
                "</saml2:Assertion>";
    }

    private String generateContextToken(final boolean entropy) throws SessionCreationException {
        final String token;
        final String id = contextManager.createContextForUser(
                new UserBean( "Alice" ),
                "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512",
                300000L,
                entropy ? HexUtils.randomBytes(32) : null,
                256 ).getIdentifier();

        token =
                "<wsc:SecurityContextToken\n" +
                "  wsu:Id=\"SecurityContextToken-1-597f0c3a-5d58-4a75-be02-0f8c7a7626ad\"\n" +
                "  xmlns:wsc=\"http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n" +
                "    <wsc:Identifier>"+id+"</wsc:Identifier>\n" +
                "</wsc:SecurityContextToken>";
        return token;
    }

    private void ensureValidMessage( final Document document ) throws InvalidDocumentFormatException, IOException {
        // logger.info( XmlUtil.nodeToFormattedString( document ));

        assertNotNull( "SOAP body present", SoapUtil.getBodyElement( document ) );

        XmlUtil.visitNodes( document.getDocumentElement(), new Functions.UnaryVoid<Node>(){
            @Override
            public void call( final Node node ) {
                if ( (int) node.getNodeType() == (int) Node.ATTRIBUTE_NODE ) {
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

    private Element getChildElement( final Element parent,
                                     final String namespace,
                                     final String localName ) {
        try {
            return XmlUtil.findExactlyOneChildElementByName( parent, namespace, localName );
        } catch ( TooManyChildElementsException e ) {
            throw new RuntimeException(e);
        } catch ( MissingRequiredElementException e ) {
            throw new RuntimeException(e);
        }
    }

    private Element getSingleChildElement( final Element parent ) {
        try {
            return XmlUtil.findOnlyOneChildElement( parent );
        } catch ( TooManyChildElementsException e ) {
            throw new RuntimeException(e);
        }
    }

}
