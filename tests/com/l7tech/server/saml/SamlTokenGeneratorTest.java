package com.l7tech.server.saml;

import com.l7tech.common.security.Keys;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author emil
 * @version 22-Jul-2004
 */
public class SamlTokenGeneratorTest extends TestCase {

    /**
     * create the <code>TestSuite</code> for the
     * SamlPolicyTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlTokenGeneratorTest.class);
        TestSetup wrapper = new TestSetup(suite) {

            /**
             * sets the test environment
             *
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                Keys.createTestSsgKeystoreProperties();
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }


    /**
     * Test <code>TrustedCertAdminTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }

}
