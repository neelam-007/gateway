package com.l7tech.server.saml;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import x0Protocol.oasisNamesTcSAML1.RequestType;
import x0Protocol.oasisNamesTcSAML1.AuthenticationQueryType;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Assertion.oasisNamesTcSAML1.SubjectType;
import x0Assertion.oasisNamesTcSAML1.NameIdentifierType;
import x0Assertion.oasisNamesTcSAML1.SubjectConfirmationType;
import org.apache.xmlbeans.XmlOptions;

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
        return suite;
    }

    /**
     *
     * @throws Exception
     */
    public void testAuthenticationQueryHandler() throws Exception {
        RequestDocument rdoc = RequestDocument.Factory.newInstance();
        RequestType rt = rdoc.addNewRequest();
        AuthenticationQueryType at = rt.addNewAuthenticationQuery();
        at.setAuthenticationMethod(Constants.PASSWORD_AUTHENTICATION);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType nameIdentifier = subject.addNewNameIdentifier();
        nameIdentifier.setFormat(Constants.NAMEIDENTIFIER_X509_SUBJECT);
        nameIdentifier.setStringValue("joe@yellow.com");
        SubjectConfirmationType subjectConfirmation = subject.addNewSubjectConfirmation();
        subjectConfirmation.setConfirmationMethodArray(new String[]{Constants.CONFIRMATION_HOLDER_OF_KEY});

        XmlOptions options = new XmlOptions();
        options.setSavePrettyPrint();
        options.setSavePrettyPrintIndent(2);
        rdoc.save(System.out, options);
    }

    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
