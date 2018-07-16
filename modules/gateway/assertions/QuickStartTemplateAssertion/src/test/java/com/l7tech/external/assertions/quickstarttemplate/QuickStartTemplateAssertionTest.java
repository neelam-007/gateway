package com.l7tech.external.assertions.quickstarttemplate;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Assert;
import org.junit.Test;

import javax.validation.constraints.AssertTrue;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Test the QuickStartTemplateAssertion.
 */
public class QuickStartTemplateAssertionTest {

    private static final Logger log = Logger.getLogger(QuickStartTemplateAssertionTest.class.getName());

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy( new QuickStartTemplateAssertion() );
    }

    public void testVersionExistence() {
        QuickStartTemplateAssertion assertion = new QuickStartTemplateAssertion();
        VariableMetadata[] data = assertion.getVariablesSet();
        Assert.assertTrue(Arrays.asList(data).stream().filter(x->x.getName().equals(QuickStartTemplateAssertion.QS_VERSION)).count() > 0);
    }

}
