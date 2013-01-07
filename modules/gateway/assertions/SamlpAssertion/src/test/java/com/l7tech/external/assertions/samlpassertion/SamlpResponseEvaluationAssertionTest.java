package com.l7tech.external.assertions.samlpassertion;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

/**
 * User: vchan
 */
public class SamlpResponseEvaluationAssertionTest extends BaseAssertionTestCase<SamlpResponseEvaluationAssertion> {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SamlpResponseEvaluationAssertion());
    }

    @Test
    public void testParseAssertion() {

        final String policyXml =
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "      <L7p:SamlpResponseEvaluation>\n" +
                "        <L7p:AuthenticationStatement samlAuthenticationInfo=\"included\">\n" +
                "          <L7p:AuthenticationMethods stringArrayValue=\"included\"/>\n" +
                "        </L7p:AuthenticationStatement>\n" +
                "        <L7p:NameQualifier stringValue=\"\"/>\n" +
                "        <L7p:Version boxedIntegerValue=\"1\"/>\n" +
                "      </L7p:SamlpResponseEvaluation>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

        try {
            Object obj = parseAssertionFromXml(policyXml);
            assertNotNull(obj);
            assertTrue(isAssertionClass(obj));

        } catch (Exception ex) {
            fail("Unexpected error encountered -- " + ex);
        }
    }


    protected boolean isAssertionClass(Object obj) {
        return (obj instanceof SamlpResponseEvaluationAssertion);
    }

    protected SamlpResponseEvaluationAssertion castAssertionClass(Object obj) {
        return SamlpResponseEvaluationAssertion.class.cast(obj);
    }

    @Test
    public void testFeatureNames() throws Exception {
        assertEquals("assertion:SamlpResponseEvaluation", new SamlpResponseEvaluationAssertion().getFeatureSetName());
    }

}