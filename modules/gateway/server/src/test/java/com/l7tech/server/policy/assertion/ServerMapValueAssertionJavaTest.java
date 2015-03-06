package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MapValueAssertion;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.NameValuePair;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ServerMapValueAssertionJavaTest {

    private static NameValuePair[] mappings = new NameValuePair[]{
            new NameValuePair("cat", "/catthing"),
            new NameValuePair("dog", "/${dog}thing"),
            new NameValuePair("fanc(y|ie)", "/${0}thing/${1}"),
            new NameValuePair("pat${patvar}withvar", "/patvar")
    };

    private TestAudit audit;
    private MapValueAssertion assertion;
    private ServerMapValueAssertion serverAssertion;
    private PolicyEnforcementContext pec;

    @Before
    public void setUp() throws Exception {
        audit = new TestAudit();

        assertion = new MapValueAssertion();
        assertion.setInputExpr("${in}");
        assertion.setOutputVar("out");
        assertion.setMappings(mappings);

        serverAssertion = new ServerMapValueAssertion(assertion);

        ApplicationContexts.inject(serverAssertion,
                Collections.<String, Object>singletonMap("auditFactory", audit.factory()));

        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());

        pec.setVariable("in", "cat");
        pec.setVariable("dog", "dawg");
    }

    /**
     * succeed if input value matched by a mapping expression
     */
    @Test
    public void testCheckRequest_InputMatchesMapping_MatchAuditedAndValueSetToOutputVariable() throws Exception {
        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals("/catthing", pec.getVariable("out"));

        // PATTERN_NOT_MATCHED won't be audited because the very first pattern in the list gets matched
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
    }

    /**
     * fail if input value not matched by a mapping expression
     */
    @Test
    public void testCheckRequest_InputNotMatchedToMapping_NoMatchAuditedAndAssertionFails() throws Exception {
        pec.setVariable("in", "nothingWillMatchThis");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, status);

        try {
            pec.getVariable("out");
            fail("Expected a NoSuchVariableException.");
        } catch (NoSuchVariableException e) {
            // expected
        }

        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
    }

    /**
     * always fail cleanly if mappings is null
     */
    @Test
    public void testCheckRequest_MappingsNull_NoMatchAuditedAndAssertionFails() throws Exception {
        assertion.setMappings(null);
        serverAssertion = new ServerMapValueAssertion(assertion, audit.factory());

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, status);

        try {
            pec.getVariable("out");
            fail("Expected a NoSuchVariableException.");
        } catch (NoSuchVariableException e) {
            // expected
        }

        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
    }

    /**
     * support context variable in the mapping output value
     */
    @Test
    public void testCheckRequest_InputMatchesMappingWithContextVariable_MatchAuditedAndValueSetToOutputVariable() throws Exception {
        pec.setVariable("in", "dog");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals("/dawgthing", pec.getVariable("out"));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
    }

    /**
     * support regex capture group pseudo-variables in the mapping output value
     */
    @Test
    public void testCheckRequest_InputMatchesMappingWithRegexPseudoVars_MatchAuditedAndValueSetToOutputVariable() throws Exception {
        pec.setVariable("in", "fancie");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals("/fanciething/ie", pec.getVariable("out"));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
    }

    /**
     * support context variable in the mapping pattern
     */
    @Test
    public void testCheckRequest_InputMatchesMappingWithContextVar_MatchAuditedAndValueSetToOutputVariable() throws Exception {
        pec.setVariable("in", "patzarfwithvar");
        pec.setVariable("patvar", "zarf");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals("/patvar", pec.getVariable("out"));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
    }

    /**
     * support context variable in the mapping pattern (negative test)
     */
    @Test
    public void testCheckRequest_InputDoesNotMatchMappingWithContextVar_NoMatchAuditedAndAssertionFails() throws Exception {
        pec.setVariable("in", "patzarffwithvar");
        pec.setVariable("patvar", "zarf");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, status);

        try {
            pec.getVariable("out");
            fail("Expected a NoSuchVariableException.");
        } catch (NoSuchVariableException e) {
            // expected
        }

        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
    }

    /**
     * regex metacharacters in mapping pattern context variable must be treated as literals
     */
    @Test
    public void testCheckRequest_InputMatchesMappingWithRegexMetachars_MatchAuditedAndValueSetToOutputVariable() throws Exception {
        pec.setVariable("in", "pata|bwithvar");
        pec.setVariable("patvar", "a|b");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals("/patvar", pec.getVariable("out"));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
    }

    /**
     * regex metacharacters in mapping pattern context variable must be treated as literals
     */
    @Test
    public void testCheckRequest_InputDoesNotMatchMappingWithRegexMetachars_NoMatchAuditedAndAssertionFails() throws Exception {
        pec.setVariable("in", "patawithvar");
        pec.setVariable("patvar", "a|b");

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.FAILED, status);

        try {
            pec.getVariable("out");
            fail("Expected a NoSuchVariableException.");
        } catch (NoSuchVariableException e) {
            // expected
        }

        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
        assertTrue(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
    }

    /**
     * fail cleanly if the input expression is null
     */
    @Test
    @BugNumber(12178)
    public void testCheckRequest_InputExpressionNull_MisconfigurationAuditedAndAssertionFails() throws Exception {
        assertion.setInputExpr(null);

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.SERVER_ERROR, status);
        assertTrue(audit.isAuditPresent(AssertionMessages.ASSERTION_MISCONFIGURED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
    }

    /**
     * fail cleanly if the target variable cannot be set
     */
    @Test
    @BugNumber(12188)
    public void testCheckRequest_TargetOutputVariableNotSettable_VariableNotSetAuditedAndAssertionFails() throws Exception {
        PolicyEnforcementContext pec = mock(PolicyEnforcementContext.class);

        when(pec.getVariableMap(any(String[].class), any(Audit.class)))
                .thenReturn(CollectionUtils.<String, Object>mapBuilder().put("in", "cat").map());

        doThrow(new VariableNotSettableException("unsettable")).when(pec).setVariable(anyString(), any());

        assertion.setOutputVar("unsettable");

        try {
            serverAssertion.checkRequest(pec);
            fail("Expected AssertionStatusException.");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
        }

        assertTrue(audit.isAuditPresent(AssertionMessages.VARIABLE_NOTSET));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_NO_PATTERNS_MATCHED));
        assertFalse(audit.isAuditPresent(AssertionMessages.MAP_VALUE_PATTERN_NOT_MATCHED));
    }
}