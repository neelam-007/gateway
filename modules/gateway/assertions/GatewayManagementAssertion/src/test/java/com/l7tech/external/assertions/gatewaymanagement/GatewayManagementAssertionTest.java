package com.l7tech.external.assertions.gatewaymanagement;

import com.l7tech.policy.PolicyValidatorResult;
import static org.junit.Assert.*;

import com.l7tech.policy.assertion.AssertionMetadata;
import org.junit.Test;

/**
 * Tests for Gateway Management Assertion
 */
public class GatewayManagementAssertionTest {

    @Test
    public void testValidationWarning() {
        final GatewayManagementAssertion gatewayManagementAssertion = new GatewayManagementAssertion();
        final GatewayManagementAssertion.Validator validator =
                new GatewayManagementAssertion.Validator( gatewayManagementAssertion );

        final PolicyValidatorResult result = new PolicyValidatorResult();
        validator.validate( null, null, true, result );

        assertEquals( "Validation warning present", 1, result.getWarningCount() );
        assertEquals( "Validation warning message", "Assertion is for use only with a Gateway Management Service", result.getWarnings().get(0).getMessage());
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
