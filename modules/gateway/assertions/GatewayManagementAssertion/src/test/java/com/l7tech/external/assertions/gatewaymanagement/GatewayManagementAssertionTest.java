package com.l7tech.external.assertions.gatewaymanagement;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.validator.PolicyValidationContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Gateway Management Assertion
 */
public class GatewayManagementAssertionTest {

    @Test
    public void testValidationWarningsAuthenticated() {
        final GatewayManagementAssertion gatewayManagementAssertion = new GatewayManagementAssertion();
        final GatewayManagementAssertion.Validator validator =
                new GatewayManagementAssertion.Validator( gatewayManagementAssertion );

        final PolicyValidatorResult result = new PolicyValidatorResult();
        validator.validate( new AssertionPath( new Assertion[]{ new AuthenticationAssertion(), gatewayManagementAssertion } ), new PolicyValidationContext(null, null, null, true, null), result );

        assertEquals( "Validation warning present", 1, result.getWarningCount() );
        assertEquals( "Validation warning message", "Assertion is for use only with a Gateway Management Service", result.getWarnings().get(0).getMessage());
    }

    @Test
    public void testValidationWarningsAnonymous() {
        final GatewayManagementAssertion gatewayManagementAssertion = new GatewayManagementAssertion();
        final GatewayManagementAssertion.Validator validator =
                new GatewayManagementAssertion.Validator( gatewayManagementAssertion );

        final PolicyValidatorResult result = new PolicyValidatorResult();
        validator.validate( new AssertionPath( new Assertion[]{ gatewayManagementAssertion } ), new PolicyValidationContext(null, null, null, true, null), result );

        assertEquals( "Validation warning present", 2, result.getWarningCount() );
        assertEquals( "Validation warning message", "Assertion is for use only with a Gateway Management Service", result.getWarnings().get(0).getMessage());
        assertEquals( "Validation warning message", "An authentication assertion should precede this assertion, anonymous users have no access permissions.", result.getWarnings().get(1).getMessage());
    }

    @Test
    public void testMetadata() {
        final GatewayManagementAssertion gatewayManagementAssertion = new GatewayManagementAssertion();
        AssertionMetadata metadata = gatewayManagementAssertion.meta();
        assertEquals( "Assertion policy name", "GatewayManagement", metadata.get( AssertionMetadata.WSP_EXTERNAL_NAME ) );
    }

    @Test
    public void testSetsVariables() {
        final GatewayManagementAssertion gatewayManagementAssertion = new GatewayManagementAssertion();
        assertEquals("no variables", 0, gatewayManagementAssertion.getVariablesSet().length );
        
        gatewayManagementAssertion.setVariablePrefix( "prefix" );
        assertEquals("variables", 4, gatewayManagementAssertion.getVariablesSet().length);
    }
}
