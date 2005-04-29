package com.l7tech.common.security.saml;

import com.l7tech.common.util.XmlUtil;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;


/**
 */
public class XmlBeansSamlTest extends TestCase {
    public XmlBeansSamlTest(String name) throws Exception {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> containing the XmlBeansSamlTest
     * <p/>
     * Add new tests at the bottom of the list.
     */
    public static Test suite() {
        return new TestSuite(XmlBeansSamlTest.class);

    }


    public void testParseAssertionFromFile() throws Exception {
        ClassLoader cl = getClass().getClassLoader();
        URL assertionUrl = cl.getResource("com/l7tech/common/security/saml/saml1.xml");
        assertTrue("Could not locate the resource", assertionUrl != null);

        XmlOptions xo = new XmlOptions();
        xo.setLoadLineNumbers();
        AssertionDocument doc = AssertionDocument.Factory.parse(assertionUrl, xo);
        //System.out.println(doc.getAssertion());
        xo = new XmlOptions();
        Collection errors = new ArrayList();
        xo.setErrorListener(errors);
        System.out.println("The document is " + (doc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError)iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = doc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore = type.getNotBefore();
        Calendar notAfter = type.getNotOnOrAfter();
        System.out.println("The not before is: " + notBefore);
        System.out.println("The not after is: " + notAfter);
    }

    public void testParseAssertionFromNode() throws Exception {
        Document doc = getDocument("com/l7tech/common/security/saml/saml1.xml");
        XmlOptions xo = new XmlOptions();
        xo.setLoadLineNumbers();
        AssertionDocument assertionDoc = AssertionDocument.Factory.parse(doc.getDocumentElement(), xo);
        xo = new XmlOptions();
        Collection errors = new ArrayList();
        xo.setErrorListener(errors);
        System.out.println("The document is " + (assertionDoc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError)iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = assertionDoc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore = type.getNotBefore();
        Calendar notAfter = type.getNotOnOrAfter();
        System.out.println("The not before is: " + notBefore);
        System.out.println("The not after is: " + notAfter);
    }

    public void testCreateEmptyAssertion() throws Exception {
        AssertionDocument adoc = AssertionDocument.Factory.newInstance();
        AssertionType assertion = AssertionType.Factory.newInstance();

        assertion.setMinorVersion(new BigInteger("0"));
        assertion.setMajorVersion(new BigInteger("1"));
        assertion.setAssertionID(Long.toHexString(System.currentTimeMillis()));
        assertion.setIssuer("ssg");
        assertion.setIssueInstant(Calendar.getInstance());
        ConditionsType ct = ConditionsType.Factory.newInstance();
        Calendar now = Calendar.getInstance();
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);
        ct.setNotBefore(now);
        Calendar c2 = (Calendar)now.clone();
        c2.roll(Calendar.MINUTE, 5);
        ct.setNotOnOrAfter(c2);
        assertion.setConditions(ct);

        AuthenticationStatementType at = assertion.addNewAuthenticationStatement();
        at.setAuthenticationMethod("urn:oasis:names:tc:SAML:1.0:am:password");
        at.setAuthenticationInstant(now);
        SubjectType subject = at.addNewSubject();
        NameIdentifierType ni = subject.addNewNameIdentifier();
        ni.setStringValue("fred");
        SubjectConfirmationType st = subject.addNewSubjectConfirmation();
        st.addConfirmationMethod("urn:oasis:names:tc:SAML:1.0:cm:sender-vouches");

        adoc.setAssertion(assertion);
        XmlOptions xo = new XmlOptions();
        Map namespaces = new HashMap();
        namespaces.put("saml", "urn:oasis:names:tc:SAML:1.0:assertion");
        xo.setSaveImplicitNamespaces(namespaces);
        xo.setSavePrettyPrint();
        xo.setSavePrettyPrintIndent(2);
        xo.setLoadLineNumbers();

        StringWriter sw = new StringWriter();
        adoc.save(sw, xo);
        System.out.println("The document is: " + sw.toString());
    }

    private Document getDocument(String resourceName)
      throws IOException, SAXException {
        ClassLoader cl = getClass().getClassLoader();
        InputStream is = cl.getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException(resourceName);
        }
        return XmlUtil.parse(is);
    }

    /**
     * Test <code>KeysTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
