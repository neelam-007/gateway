package com.l7tech.server.policy.filter;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.test.BugNumber;
import static junit.framework.Assert.*;
import org.junit.*;

import java.util.Arrays;

/**
 *
 */
public class HideUnsupportedClientAssertionsTest {
    @BeforeClass
    public static void initAssertions() throws Exception {
        new AssertionRegistry().afterPropertiesSet();
    }

    @Test
    public void testHideUntargetedAssertion() throws Exception {
        WssEncryptElement ass = new WssEncryptElement();
        ass.setTargetMessage(new MessageTargetableSupport("somecontextvariable"));
        Filter hider = new HideUnsupportedClientAssertions();
        Assertion result = hider.filter(null, new AllAssertion(Arrays.asList(ass)));
        final CompositeAssertion compResult = (CompositeAssertion) result;
        assertTrue("WssEncryptElement targeted at context variable should have been filtered", compResult.getChildren().size() == 0);
    }

    @Test
    @BugNumber(8305)
    public void testDontHideNonTargetableAssertion() throws Exception {
        Assertion ass = new TrueAssertion();
        Filter hider = new HideUnsupportedClientAssertions();
        Assertion result = hider.filter(null, new AllAssertion(Arrays.asList(ass)));
        assertNotNull("TrueAssertion should not have been filtered", result);
        final CompositeAssertion compResult = (CompositeAssertion) result;
        assertTrue("TrueAssertion should not have been filtered", compResult.getChildren().size() > 0 && compResult.getChildren().get(0) == ass);
    }
}
