package com.l7tech.external.assertions.retrieveservicewsdl;

import static org.junit.Assert.*;

import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableMetadata;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test the RetrieveServiceWsdlAssertion.
 */
public class RetrieveServiceWsdlAssertionTest {

    @Test
    public void testGetVariablesUsedDefault() {
        List<String> variablesUsed = Arrays.asList(new RetrieveServiceWsdlAssertion().getVariablesUsed());

        assertEquals(4, variablesUsed.size());

        assertTrue(variablesUsed.contains("service.oid"));
        assertTrue(variablesUsed.contains("gateway.cluster.hostname"));
        assertTrue(variablesUsed.contains("request.url.protocol"));
        assertTrue(variablesUsed.contains("request.tcp.localPort"));
    }

    @Test
    public void testGetVariablesUsed_WithServiceIdButNotHostnameOrPortVariables() {
        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setHost("localhost");
        assertion.setPort("8080");
        assertion.setServiceId("${foo}");

        List<String> variablesUsed = Arrays.asList(assertion.getVariablesUsed());

        assertEquals(2, variablesUsed.size());
        assertTrue(variablesUsed.contains("foo"));
        assertTrue(variablesUsed.contains("request.url.protocol"));
    }

    @Test
    public void testGetVariablesSetDefault() {
        VariableMetadata[] variablesSet = new RetrieveServiceWsdlAssertion().getVariablesSet();
        assertEquals(0, variablesSet.length);
    }

    @Test
    public void testGetVariablesSetWithMessageVariableTarget() {
        String messageTargetVariableName = "msgTargetVar";

        MessageTargetableSupport messageTarget = new MessageTargetableSupport(TargetMessageType.OTHER);
        messageTarget.setOtherTargetMessageVariable(messageTargetVariableName);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setMessageTarget(messageTarget);

        VariableMetadata[] variablesSet = assertion.getVariablesSet();

        assertEquals(1, variablesSet.length);
        assertEquals(messageTargetVariableName, variablesSet[0].getName());
    }

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new RetrieveServiceWsdlAssertion());
    }

}
