package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.BuildRstSoapRequest;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.MockConfig;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.TimeUnit;
import com.l7tech.xml.WsTrustRequestType;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * 
 */
public class ServerBuildRstSoapRequestTest {

    private static final StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
    static {
        beanFactory.addBean( "serverConfig", new MockConfig(new Properties()) );
    }

    private static final String[] testWsTrustNamespaces = new String[]{
        "http://schemas.xmlsoap.org/ws/2004/04/trust",
        "http://schemas.xmlsoap.org/ws/2005/02/trust",
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512"
    };
    
    @Test
    public void testBasic() throws Exception {
        for ( final String trustNs : testWsTrustNamespaces ) {
            doTest( trustNs,
                    getPolicyNs(trustNs) );
        }
    }

    @Test
    public void testBasicCancel() throws Exception {
        for ( final String trustNs : testWsTrustNamespaces ) {
            doTestWithToken( WsTrustRequestType.CANCEL,
                             "CancelTarget",
                             trustNs,
                             getPolicyNs(trustNs) );
        }
    }

    @Test
    public void testBasicValidate() throws Exception {
        for ( final String trustNs : testWsTrustNamespaces ) {
            doTestWithToken( WsTrustRequestType.VALIDATE,
                             "ValidateTarget",
                             trustNs,
                             getPolicyNs(trustNs) );
        }
    }

    @Test
    public void testCancelWithSecureConversationToken() throws Exception {
        final String trustNs = "http://docs.oasis-open.org/ws-sx/ws-trust/200512";
        final SecureConversationSession session = new SecureConversationSession(
                "http://docs.oasis-open.org/ws-sx/ws-secureconversation/200512",
                "urn:token:token-0123456789",
                new byte[32],
                System.currentTimeMillis(),
                System.currentTimeMillis() + TimeUnit.HOURS.toMillis( 1 ),
                new UserBean("TestUser"),
                null );
        doTestWithToken( WsTrustRequestType.CANCEL,
                         "CancelTarget",
                         trustNs,
                         getPolicyNs(trustNs),
                         session );
    }

    private void doTest( final String trustNs,
                         final String policyNs ) throws Exception {
        final SoapVersion soapVersion = SoapVersion.SOAP_1_2;
        final BuildRstSoapRequest buildRstSoapRequest = buildRstSoapRequest( soapVersion, true, 256, 60000L );
        buildRstSoapRequest.setWsTrustNamespace( trustNs );
        final Document rstDoc = evaluateAssertion( buildRstSoapRequest, null );

        final Element soapBody = SoapUtil.getBodyElement( rstDoc );
        final Element rstElement = XmlUtil.findOnlyOneChildElement( soapBody );
        final Element appliesToElement = XmlUtil.findExactlyOneChildElementByName( rstElement, policyNs, "AppliesTo" );
        final Element issuerElement = XmlUtil.findExactlyOneChildElementByName( rstElement, trustNs, "Issuer" );
        final Element entropyElement = XmlUtil.findExactlyOneChildElementByName( rstElement, trustNs, "Entropy" );
        final Element keySizeElement = XmlUtil.findExactlyOneChildElementByName( rstElement, trustNs, "KeySize" );
        final Element lifetimeElement = XmlUtil.findExactlyOneChildElementByName( rstElement, trustNs, "Lifetime" );

        assertEquals( "Soap version", soapVersion.getNamespaceUri(), rstDoc.getDocumentElement().getNamespaceURI() );
        assertEquals( "Applies-to address", "http://appliesto/", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(XmlUtil.findOnlyOneChildElement( appliesToElement ))));
        assertEquals( "Issuer address", "http://issuer/", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement( issuerElement )));
        assertFalse( "Empty entropy", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement( entropyElement )).isEmpty() );
        assertEquals( "Key size", "256", XmlUtil.getTextValue(keySizeElement));
        assertTrue( "Lifetime future", System.currentTimeMillis() < ISO8601Date.parse(XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(lifetimeElement))).getTime() );
        assertTrue( "Lifetime duration", System.currentTimeMillis()+ TimeUnit.MINUTES.toMillis(5) > ISO8601Date.parse(XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(lifetimeElement))).getTime() );

    }

    private void doTestWithToken( final WsTrustRequestType requestType,
                                  final String targetElementName,
                                  final String trustNs,
                                  final String policyNs ) throws Exception {
        final Document str = XmlUtil.parse( "<SecurityTokenReference xmlns=\""+SoapConstants.SECURITY_NAMESPACE+"\">reference here</SecurityTokenReference>" );
        doTestWithToken( requestType, targetElementName, trustNs, policyNs, str.getDocumentElement() );
    }
    
    private void doTestWithToken( final WsTrustRequestType requestType,
                                  final String targetElementName,
                                  final String trustNs,
                                  final String policyNs,
                                  final Object tokenObject ) throws Exception {
        final SoapVersion soapVersion = SoapVersion.SOAP_1_1;
        final BuildRstSoapRequest buildRstSoapRequest = buildRstSoapRequest( soapVersion, false, null, null );
        buildRstSoapRequest.setWsTrustNamespace( trustNs );
        buildRstSoapRequest.setTargetTokenVariable( "token" );
        buildRstSoapRequest.setRequestType( requestType );

        final Document rstDoc = evaluateAssertion( buildRstSoapRequest, tokenObject );

        final Element soapBody = SoapUtil.getBodyElement( rstDoc );
        final Element rstElement = XmlUtil.findOnlyOneChildElement( soapBody );
        final Element appliesToElement = XmlUtil.findExactlyOneChildElementByName( rstElement, policyNs, "AppliesTo" );
        final Element issuerElement = XmlUtil.findExactlyOneChildElementByName( rstElement, trustNs, "Issuer" );
        final Element entropyElement = XmlUtil.findFirstChildElementByName( rstElement, trustNs, "Entropy" );
        final Element keySizeElement = XmlUtil.findFirstChildElementByName( rstElement, trustNs, "KeySize" );
        final Element lifetimeElement = XmlUtil.findFirstChildElementByName( rstElement, trustNs, "Lifetime" );
        final Element tokenElement = XmlUtil.findFirstChildElementByName( rstElement, trustNs, targetElementName );
        final Element baseElement = XmlUtil.findFirstChildElementByName( rstElement, trustNs, "Base" );

        assertEquals( "Soap version", soapVersion.getNamespaceUri(), rstDoc.getDocumentElement().getNamespaceURI() );
        assertEquals( "Applies-to address", "http://appliesto/", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(XmlUtil.findOnlyOneChildElement( appliesToElement ))));
        assertEquals( "Issuer address", "http://issuer/", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement( issuerElement )));
        assertNull( "Entropy", entropyElement );
        assertNull( "KeySize", keySizeElement );
        assertNull( "Lifetime", lifetimeElement );
        if ( "http://docs.oasis-open.org/ws-sx/ws-trust/200512".equals( trustNs ) ) {
            assertNotNull( "Target Token", tokenElement );
            assertNotNull( "STR", XmlUtil.findFirstChildElementByName( tokenElement, SoapConstants.SECURITY_NAMESPACE, SoapConstants.SECURITYTOKENREFERENCE_EL_NAME ) );
        } else {
            assertNotNull("Base", baseElement);
            assertNotNull( "STR", XmlUtil.findFirstChildElementByName( baseElement, SoapConstants.SECURITY_NAMESPACE, SoapConstants.SECURITYTOKENREFERENCE_EL_NAME ) );
        }
    }

    private Document evaluateAssertion( final BuildRstSoapRequest buildRstSoapRequest,
                                        final Object tokenObject ) throws IOException, PolicyAssertionException, NoSuchVariableException, SAXException {
        final ServerBuildRstSoapRequest serverBuildRstSoapRequest =
                new ServerBuildRstSoapRequest( buildRstSoapRequest, beanFactory );

        final Message request = new Message();
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        if ( tokenObject != null ) {
            context.setVariable( "token", tokenObject );
        }

        final AssertionStatus status = serverBuildRstSoapRequest.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final Message rstRequest = (Message) context.getVariable( "requestBuilder.rstRequest" );
        final Document rstDoc = rstRequest.getXmlKnob().getDocumentReadOnly();

        System.out.println( XmlUtil.nodeToFormattedString( rstDoc ) );

        return rstDoc;
    }

    private BuildRstSoapRequest buildRstSoapRequest( final SoapVersion soapVersion,
                                                     final boolean entropy,
                                                     final Integer keySize,
                                                     final Long lifetime ) {
        final BuildRstSoapRequest buildRstSoapRequest = new BuildRstSoapRequest();
        buildRstSoapRequest.setTokenType( SecurityTokenType.SAML2_ASSERTION );
        buildRstSoapRequest.setSoapVersion( soapVersion );
        buildRstSoapRequest.setWsTrustNamespace( SoapUtil.WST_NAMESPACE3 );
        buildRstSoapRequest.setRequestType( WsTrustRequestType.ISSUE );
        buildRstSoapRequest.setIssuerAddress( "http://issuer/" );
        buildRstSoapRequest.setAppliesToAddress( "http://appliesto/" );
        buildRstSoapRequest.setIncludeClientEntropy( entropy );
        buildRstSoapRequest.setKeySize( keySize );
        buildRstSoapRequest.setLifetime( lifetime );
        return buildRstSoapRequest;
    }

    private String getPolicyNs( final String trustNs ) {
        if ( "http://schemas.xmlsoap.org/ws/2004/04/trust".equals( trustNs ) ) {
            return "http://schemas.xmlsoap.org/ws/2002/12/policy";
        }
        return "http://schemas.xmlsoap.org/ws/2004/09/policy";
    }

}
