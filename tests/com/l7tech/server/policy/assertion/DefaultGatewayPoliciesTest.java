package com.l7tech.server.policy.assertion;

import com.l7tech.objectmodel.HibernatePersistenceManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.DefaultGatewayPolicies;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author alex
 * @version $Revision$
 */
public class DefaultGatewayPoliciesTest extends TestCase {
    /**
     * test <code>DefaultGatewayPoliciesTest</code> constructor
     */
    public DefaultGatewayPoliciesTest( String name ) throws Exception {
        super( name );
        HibernatePersistenceManager.initialize(null);
    }

    /**
     * create the <code>TestSuite</code> for the DefaultGatewayPoliciesTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( DefaultGatewayPoliciesTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testStuff() {
        DefaultGatewayPolicies dgp = DefaultGatewayPolicies.getInstance();
        Assertion certPolicy = dgp.getCertPolicy();
        System.out.println("Cert-based authentication policy:\n" + certPolicy);
        Assertion defaultPolicy = dgp.getDefaultPolicy();
        System.out.println("Default policy:\n" + defaultPolicy);
    }

    /**
     * Test <code>DefaultGatewayPoliciesTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }
}