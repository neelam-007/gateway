package com.l7tech.proxy.policy;

import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author alex
 * @version $Revision$
 */
public class ClientPolicyFactoryTest extends TestCase {
    /**
     * test <code>ClientPolicyFactoryTest</code> constructor
     */
    public ClientPolicyFactoryTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ClientPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ClientPolicyFactoryTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testCompleteness() throws Exception {
        ClientPolicyFactory pfac = ClientPolicyFactory.getInstance();

        ClientAssertion foo;
        Assertion[] everything = AllAssertions.BRIDGE_EVERYTHING;
        for (int i = 0; i < everything.length; i++) {
            foo = pfac.makeClientPolicy(everything[i]);
        }
    }

    /**
     * Test <code>ClientPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}