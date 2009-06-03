package com.l7tech.proxy.policy;

import com.l7tech.policy.AllAssertions;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientUnknownAssertion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;

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
        AssertionRegistry.installEnhancedMetadataDefaults();
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
            if (!(everything[i] instanceof UnknownAssertion)) {
                assertFalse("Unknown assertion : " + foo.getName(), foo instanceof ClientUnknownAssertion);
            }
        }
    }

    public void testComposite() throws Exception {
        AllAssertion all = new AllAssertion(Arrays.asList(AllAssertions.BRIDGE_EVERYTHING));
        ClientPolicyFactory pfac = ClientPolicyFactory.getInstance();
        pfac.makeClientPolicy(all);
    }

    /**
     * Test <code>ClientPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}