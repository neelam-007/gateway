package com.l7tech.server.saml;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author emil
 * @version 22-Jul-2004
 */
public class SamlTokenGeneratorTest extends TestCase {

    /**
     * create the <code>TestSuite</code> for the
     * SamlTokenGeneratorTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlTokenGeneratorTest.class);
        return suite;
    }


    /**
     * Test <code>TrustedCertAdminTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

}
