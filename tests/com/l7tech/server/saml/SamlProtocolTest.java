package com.l7tech.server.saml;

import com.l7tech.common.security.Keys;
import com.l7tech.common.security.saml.Constants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
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

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import java.util.HashMap;
import java.util.Map;

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
                Keys.createTestSsgKeystoreProperties();
            }
        };

        return wrapper;
    }

    /**
     *
     * @throws Exception
     */
    public void xtestAuthenticationQueryHandler() throws Exception {
        RequestDocument rdoc = RequestDocument.Factory.newInstance();
        RequestType rt = rdoc.addNewRequest();
        AuthenticationQueryType at = rt.addNewAuthenticationQuery();
        at.setAuthenticationMethod(Constants.PASSWORD_AUTHENTICATION);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType nameIdentifier = subject.addNewNameIdentifier();
        nameIdentifier.setFormat(Constants.NAMEIDENTIFIER_X509_SUBJECT);
        nameIdentifier.setStringValue("cn=joe, o=yellow.com");
        SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
        subjectConfirmation.setConfirmationMethodArray(new String[]{Constants.CONFIRMATION_HOLDER_OF_KEY});

        XmlOptions options = new XmlOptions();
        // options.setSavePrettyPrint();
        //options.setSavePrettyPrintIndent(2);
        Map prefixes = new HashMap();
        prefixes.put(Constants.NS_SAMLP, Constants.NS_SAMLP_PREFIX);
        prefixes.put(Constants.NS_SAML, Constants.NS_SAML_PREFIX);

        options.setSaveSuggestedPrefixes(prefixes);
        Document doc = (Document)rdoc.newDomNode(options);
        System.out.println(XmlUtil.nodeToString(doc));
        //rdoc.save(System.out, options);
    }


    /**
     *
     * @throws Exception
     */
    public void testAuthenticationQueryHandlerInSoap() throws Exception {
        XmlOptions options = new XmlOptions();
        Map prefixes = new HashMap();
        prefixes.put(Constants.NS_SAMLP, Constants.NS_SAMLP_PREFIX);
        prefixes.put(Constants.NS_SAML, Constants.NS_SAML_PREFIX);
        options.setSaveSuggestedPrefixes(prefixes);

        RequestDocument rdoc = RequestDocument.Factory.newInstance(options);
        RequestType rt = rdoc.addNewRequest();
        AuthenticationQueryType at = rt.addNewAuthenticationQuery();
        at.setAuthenticationMethod(Constants.PASSWORD_AUTHENTICATION);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType nameIdentifier = subject.addNewNameIdentifier();
        nameIdentifier.setFormat(Constants.NAMEIDENTIFIER_X509_SUBJECT);
        nameIdentifier.setStringValue("cn=joe, o=yellow.com");
        SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
        subjectConfirmation.setConfirmationMethodArray(new String[]{Constants.CONFIRMATION_HOLDER_OF_KEY});

        SOAPMessage sm = MessageFactory.newInstance().createMessage();
        SOAPBody body = sm.getSOAPPart().getEnvelope().getBody();
        //rdoc.save(System.out, options);
        final Element documentElement = ((Document)rdoc.newDomNode(options)).getDocumentElement();
        SoapUtil.domToSOAPElement(body, documentElement);
        sm.writeTo(System.out);

        ResponseDocument response = new AuthenticationQueryHandler(rdoc).getResponse();
        final SignerInfo signerInfo = KeystoreUtils.getInstance().getSignerInfo();
        Document msgOut = Responses.asSoapMessage(response, signerInfo);
        XmlUtil.nodeToOutputStream(msgOut, System.out);
    }


    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
