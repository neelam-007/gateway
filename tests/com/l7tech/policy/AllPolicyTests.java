package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionTest;
import com.l7tech.policy.validator.DefaultPolicyValidatorTest;
import com.l7tech.policy.wsp.WspTranslator21to30Test;
import com.l7tech.common.security.Keys;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;

/**
 * Class <code>AllPolicyTests</code> defines all tests to be run for the policy
 * package.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AllPolicyTests extends TestCase {
    /**
     * test <code>AllPolicyTests</code> constructor
     */
    public AllPolicyTests(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the containing the tests for
     * the policy package.
     * <p>
     * Add new tests at the bottom of the list.
     *
     */
    public static Test suite() {
        // Set up test keystores for fake-Tomcat
        try {
            Keys.createTestSsgKeystoreProperties();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        TestSuite suite = new TestSuite();
        suite.addTest(new TestSuite(AssertionTest.class));
    	suite.addTest(new TestSuite(SamplePolicyTest.class));
        suite.addTest(new TestSuite(WspReaderTest.class));
        suite.addTest(new TestSuite(WspWriterTest.class));
        suite.addTest(new TestSuite(CompositeAssertionTest.class));
        suite.addTest(new TestSuite(AssertionTraversalTest.class));
        suite.addTest(new TestSuite(DefaultPolicyPathBuilderTest.class));
        suite.addTest(new TestSuite(DefaultPolicyValidatorTest.class));
        suite.addTest(new TestSuite(PolicyCloneTest.class));
        suite.addTest(new TestSuite(WspTranslator21to30Test.class));
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>AllPolicyTests</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
