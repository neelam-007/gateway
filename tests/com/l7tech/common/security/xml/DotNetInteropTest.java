package com.l7tech.common.security.xml;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;

/**
 * Test xml digital signature and encryption interoperability with messages
 * generated by a .net client using WSE 2.0
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 15, 2004<br/>
 * $Id$<br/>
 */
public class DotNetInteropTest extends TestCase {

    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(DotNetInteropTest.class);
        return suite;
    }

    public void testValidateSignatureFromdotNetRequest() throws Exception {
        Document signedDoc = getSignedRequest();
        Element bodyEl = SoapUtil.getBody(signedDoc);
        X509Certificate[] clientCert = SoapMsgSigner.validateSignature(signedDoc, bodyEl);
        assertTrue(clientCert.length > 0);
        assertTrue(clientCert[0].getSubjectDN().toString().equals("CN=WSE2QuickStartClient"));
        System.out.println("Signature verified successfully for subject: " + clientCert[0].getSubjectDN() + ".");
    }

    private Document getSignedRequest() throws Exception {
        return TestDocuments.getTestDocument(TestDocuments.DOTNET_SIGNED_REQUEST);
    }
}
