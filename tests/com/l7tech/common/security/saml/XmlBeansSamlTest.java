package com.l7tech.common.security.saml;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Calendar;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;

import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlError;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.AssertionDocument;
import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML1.ConditionsType;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilderFactory;


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
        System.out.println("The document is "+(doc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError) iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = doc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore  = type.getNotBefore();
        Calendar notAfter   = type.getNotOnOrAfter();
        System.out.println("The not before is: "+notBefore);
        System.out.println("The not after is: "+notAfter);
    }

    public void testParseAssertionFromNode() throws Exception {
        Document doc = getDocument("com/l7tech/common/security/saml/saml1.xml");
        XmlOptions xo = new XmlOptions();
        xo.setLoadLineNumbers();
        AssertionDocument assertionDoc = AssertionDocument.Factory.parse(doc.getDocumentElement(), xo);
        xo = new XmlOptions();
        Collection errors = new ArrayList();
        xo.setErrorListener(errors);
        System.out.println("The document is "+(assertionDoc.validate(xo) ? "valid" : "invalid"));
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            XmlError xerr = (XmlError) iterator.next();
            System.out.println(xerr);
        }

        AssertionType at = assertionDoc.getAssertion();
        ConditionsType type = at.getConditions();
        Calendar notBefore  = type.getNotBefore();
        Calendar notAfter   = type.getNotOnOrAfter();
        System.out.println("The not before is: "+notBefore);
        System.out.println("The not after is: "+notAfter);
    }

    private Document getDocument(String resourceName)
         throws IOException, ParserConfigurationException, SAXException {
           ClassLoader cl = getClass().getClassLoader();
           InputStream is = cl.getResourceAsStream(resourceName);
           if (is == null) {
               throw new FileNotFoundException(resourceName);
           }
           DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
           df.setNamespaceAware(true);
           return df.newDocumentBuilder().parse(is);
       }

    /**
     * Test <code>KeysTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
