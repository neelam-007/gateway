package com.l7tech.server.saml;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.MockServletApi;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xmlbeans.XmlOptions;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import x0Assertion.oasisNamesTcSAML1.NameIdentifierType;
import x0Assertion.oasisNamesTcSAML1.SubjectConfirmationType;
import x0Assertion.oasisNamesTcSAML1.SubjectType;
import x0Protocol.oasisNamesTcSAML1.AuthenticationQueryType;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.RequestType;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author emil
 * @version 28-Jul-2004
 */
public class SamlProtocolTest extends TestCase {
    private static MockServletApi servletApi;

    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlProtocolTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            protected void setUp() throws Exception {
                servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");

            }
        };

        return wrapper;
    }

    /**
     * @throws Exception
     */
    public void xtestAuthenticationQueryHandler() throws Exception {
        RequestDocument rdoc = RequestDocument.Factory.newInstance();
        RequestType rt = rdoc.addNewRequest();
        AuthenticationQueryType at = rt.addNewAuthenticationQuery();
        at.setAuthenticationMethod(SamlConstants.PASSWORD_AUTHENTICATION);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType nameIdentifier = subject.addNewNameIdentifier();
        nameIdentifier.setFormat(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT);
        nameIdentifier.setStringValue("cn=joe, o=yellow.com");

        XmlOptions options = new XmlOptions();
        // xmlOptions.setSavePrettyPrint();
        //xmlOptions.setSavePrettyPrintIndent(2);
        Map prefixes = new HashMap();
        prefixes.put(SamlConstants.NS_SAMLP, SamlConstants.NS_SAMLP_PREFIX);
        prefixes.put(SamlConstants.NS_SAML, SamlConstants.NS_SAML_PREFIX);

        options.setSaveSuggestedPrefixes(prefixes);
        //Document doc = (Document)rdoc.newDomNode(xmlOptions);
        //System.out.println(XmlUtil.nodeToString(doc));
        rdoc.save(System.out, options);
    }


    /**
     * @throws Exception
     */
    public void testAuthenticationQueryHandlerInSoap() throws Exception {
        RequestDocument rdoc = RequestDocument.Factory.newInstance();
        RequestType rt = rdoc.addNewRequest();
        AuthenticationQueryType at = rt.addNewAuthenticationQuery();
        at.setAuthenticationMethod(SamlConstants.PASSWORD_AUTHENTICATION);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType nameIdentifier = subject.addNewNameIdentifier();
        nameIdentifier.setFormat(SamlConstants.NAMEIDENTIFIER_X509_SUBJECT);
        nameIdentifier.setStringValue("cn=joe, o=yellow.com");
        SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
        subjectConfirmation.setConfirmationMethodArray(new String[]{SamlConstants.CONFIRMATION_HOLDER_OF_KEY});

        SOAPMessage sm = SoapUtil.getAxisMessageFactory().createMessage();
        SOAPBody body = sm.getSOAPPart().getEnvelope().getBody();
        // rdoc.save(System.out, xmlOptions);
        final Element documentElement = ((Document)rdoc.newDomNode(SamlUtilities.xmlOptions())).getDocumentElement();
        SoapUtil.domToSOAPElement(body, documentElement);
        //sm.writeTo(System.out);

        ResponseDocument response = new AuthenticationQueryHandler(rdoc).getResponse();
        final SignerInfo signerInfo = KeystoreUtils.getInstance().getSignerInfo();
        Document msgOut = Responses.asSoapMessage(response, signerInfo);
        XmlUtil.nodeToOutputStream(msgOut, System.out);
    }

    public void xtestBadRequestExpectedSoapFault() throws Exception {

        MockHttpServletRequest requestMock = servletApi.getServletRequest();
        MockHttpServletResponse responseMock = servletApi.getServletResponse();

        MessageFactory msgFactory = SoapUtil.getAxisMessageFactory();

        SamlProtocolServlet servlet = new SamlProtocolServlet();
        servlet.init(servletApi.getServletConfig());
        servlet.doPost(requestMock, responseMock);
        SOAPMessage msg = msgFactory.createMessage(new MimeHeaders(), new ByteArrayInputStream(responseMock.getContentAsByteArray()));
        SOAPPart soapPart = msg.getSOAPPart();
        SOAPFault fault = soapPart.getEnvelope().getBody().getFault();
        assertTrue(fault != null);
    }

    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
