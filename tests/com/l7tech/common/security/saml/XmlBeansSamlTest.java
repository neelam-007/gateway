package com.l7tech.common.security.saml;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;
import oasisNamesTcSAML10Assertion.AssertionDocument;
import oasisNamesTcSAML10Assertion.AssertionType;
import oasisNamesTcSAML10Assertion.ConditionsType;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Calendar;

import com.bea.xml.XmlOptions;
import com.bea.xml.XmlError;

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
        URL assertionUrl = cl.getResource("com/l7tech/saml/saml1.xml");
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


    /**
     * Test <code>KeysTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
