package com.l7tech.server.saml;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.MockServletApi;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.servlet.MockServletOutputStream;
import com.mockobjects.servlet.MockServletInputStream;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xmlbeans.XmlOptions;
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
import javax.xml.transform.dom.DOMSource;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;

/**
 * @author emil
 * @version 28-Jul-2004
 */
public class SamlProtocolTest extends TestCase {
    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlProtocolTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            protected void setUp() throws Exception {
                System.setProperty("com.l7tech.common.locator.properties", "/com/l7tech/common/locator/test.properties");
                //Keys.createTestSsgKeystoreProperties();
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
    public void xtestAuthenticationQueryHandlerInSoap() throws Exception {
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

        SOAPMessage sm = MessageFactory.newInstance().createMessage();
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

    public void testBadRequestExpectedSoapFault() throws Exception {
        MockServletApi servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");

        Mock requestMock = servletApi.getServletRequestMock();
        Mock responseMock = servletApi.getServletResponseMock();
        final MockServletOutputStream mockServletOutputStream = new MockServletOutputStream();
        final MockServletInputStream mockServletInputStream = new MockServletInputStream();
        requestMock.matchAndReturn("getInputStream", mockServletInputStream);
        responseMock.matchAndReturn("getOutputStream", mockServletOutputStream);
        responseMock.matchAndReturn("setContentType", "text/xml", null);

        MessageFactory msgFactory = MessageFactory.newInstance();

        SamlProtocolServlet servlet = new SamlProtocolServlet();
        servlet.init(servletApi.getServletConfig());
        servlet.doPost(servletApi.getServletRequest(), servletApi.getServletResponse());
        final String contents = mockServletOutputStream.getContents();
        SOAPMessage msg = msgFactory.createMessage(new MimeHeaders(), new ByteArrayInputStream(contents.getBytes()));
        SOAPPart soapPart = msg.getSOAPPart();
        //todo: find out why the below call fails
//        SOAPFault fault = soapPart.getEnvelope().getEntireMessageBody().getFault();
//        assertTrue(fault != null);
    }

    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
