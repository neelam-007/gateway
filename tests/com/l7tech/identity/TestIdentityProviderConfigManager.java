package com.l7tech.identity;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.l7tech.util.Locator;

/**
 * @author alex
 */
public class TestIdentityProviderConfigManager extends TestCase {
    /**
     * test <code>TestIdentityProviderConfigManager</code> constructor
     */
    public TestIdentityProviderConfigManager(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * TestIdentityProviderConfigManager <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(TestIdentityProviderConfigManager.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * basic teast retrieving the identoty provider config manager
     * @throws Exception
     */
    public void testLocateIdentityProviderConfigManager()
      throws Exception {
        IdentityProviderConfigManager icm =
                (IdentityProviderConfigManager)Locator.
                getDefault().lookup(com.l7tech.identity.IdentityProviderConfigManager.class);

    }
    /**
     * Test <code>TestIdentityProviderConfigManager</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }
}