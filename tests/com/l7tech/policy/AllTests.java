package com.l7tech.policy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.l7tech.policy.validator.DefaultPolicyValidatorTest;
import com.l7tech.policy.assertion.AssertionTest;

/**
 * Class <code>AllTests</code> defines all tests to be run for the policy
 * package.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class AllTests extends TestCase {
    /**
     * test <code>AllTests</code> constructor
     */
    public AllTests(String name) {
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
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>AllTests</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
