package com.l7tech.policy;

import com.l7tech.policy.assertion.AssertionMetadataTest;
import com.l7tech.policy.assertion.AssertionTest;
import com.l7tech.server.policy.validator.DefaultPolicyValidatorTest;
import com.l7tech.policy.wsp.WspConstantsTest;
import com.l7tech.server.policy.CompositeAssertionTest;
import com.l7tech.server.policy.DefaultPolicyPathBuilderTest;
import com.l7tech.server.policy.variable.ExpandVariablesTest;
import com.l7tech.server.policy.assertion.ServerSslAssertionTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import static org.junit.Assert.*;


/**
 * Class <code>AllPolicyTests</code> defines all tests to be run for the policy
 * package.
 * <p>
 * @author Emil Marceta
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    AssertionTest.class,
    WspConstantsTest.class,
    SamplePolicyTest.class,
    WspReaderTest.class,
    WspWriterTest.class,
    CompositeAssertionTest.class,
    AssertionTraversalTest.class,
    DefaultPolicyPathBuilderTest.class,
    DefaultPolicyValidatorTest.class,
    PolicyCloneTest.class,
    ExpandVariablesTest.class,
    AssertionMetadataTest.class,
    ServerSslAssertionTest.class

})
public class AllPolicyTests {
}
