package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.BuildRstSoapRequest;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.MockConfig;
import com.l7tech.util.TimeUnit;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

    @Test
    public void testBasic() throws Exception {
        final SoapVersion soapVersion = SoapVersion.SOAP_1_2;
        final BuildRstSoapRequest buildRstSoapRequest = new BuildRstSoapRequest();
        buildRstSoapRequest.setTokenType( SecurityTokenType.SAML2_ASSERTION );
        buildRstSoapRequest.setSoapVersion( soapVersion );
        buildRstSoapRequest.setWsTrustNamespace( SoapUtil.WST_NAMESPACE3 );
        buildRstSoapRequest.setIssuerAddress( "http://issuer/" );
        buildRstSoapRequest.setAppliesToAddress( "http://appliesto/" );
        buildRstSoapRequest.setIncludeClientEntropy( true );
        buildRstSoapRequest.setKeySize( 256 );
        buildRstSoapRequest.setLifetime( 60000L );
        final ServerBuildRstSoapRequest serverBuildRstSoapRequest =
                new ServerBuildRstSoapRequest( buildRstSoapRequest, beanFactory );

        final Message request = new Message();
        final Message response = new Message();
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );

        final AssertionStatus status = serverBuildRstSoapRequest.checkRequest( context );
        assertEquals( "AssertionStatus", AssertionStatus.NONE, status );

        final Message rstRequest = (Message) context.getVariable( "requestBuilder.rstRequest" );
        final Document rstDoc = rstRequest.getXmlKnob().getDocumentReadOnly();

        System.out.println( XmlUtil.nodeToFormattedString( rstDoc ) );

        final Element soapBody = SoapUtil.getBodyElement( rstDoc );
        final Element rstElement = XmlUtil.findOnlyOneChildElement( soapBody );
        final Element appliesToElement = XmlUtil.findExactlyOneChildElementByName( rstElement, "http://schemas.xmlsoap.org/ws/2004/09/policy", "AppliesTo" );
        final Element issuerElement = XmlUtil.findExactlyOneChildElementByName( rstElement, "http://docs.oasis-open.org/ws-sx/ws-trust/200512", "Issuer" );
        final Element entropyElement = XmlUtil.findExactlyOneChildElementByName( rstElement, "http://docs.oasis-open.org/ws-sx/ws-trust/200512", "Entropy" );
        final Element keySizeElement = XmlUtil.findExactlyOneChildElementByName( rstElement, "http://docs.oasis-open.org/ws-sx/ws-trust/200512", "KeySize" );
        final Element lifetimeElement = XmlUtil.findExactlyOneChildElementByName( rstElement, "http://docs.oasis-open.org/ws-sx/ws-trust/200512", "Lifetime" );
        
        assertEquals( "Soap version", soapVersion.getNamespaceUri(), rstDoc.getDocumentElement().getNamespaceURI() );
        assertEquals( "Applies-to address", "http://appliesto/", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(XmlUtil.findOnlyOneChildElement( appliesToElement ))));
        assertEquals( "Issuer address", "http://issuer/", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement( issuerElement )));
        assertFalse( "Empty entropy", XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement( entropyElement )).isEmpty() );
        assertEquals( "Key size", "256", XmlUtil.getTextValue(keySizeElement));
        assertTrue( "Lifetime future", System.currentTimeMillis() < ISO8601Date.parse(XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(lifetimeElement))).getTime() );
        assertTrue( "Lifetime duration", System.currentTimeMillis()+ TimeUnit.MINUTES.toMillis(5) > ISO8601Date.parse(XmlUtil.getTextValue(XmlUtil.findOnlyOneChildElement(lifetimeElement))).getTime() );
    }

}
