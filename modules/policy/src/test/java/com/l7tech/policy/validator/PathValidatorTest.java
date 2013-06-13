package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.wsdl.Wsdl;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class PathValidatorTest {
    private PolicyValidationContext context;
    private PolicyValidatorResult validationResult;
    private AssertionLicense license;

    @Before
    public void setup() {
        context = new PolicyValidationContext(PolicyType.INCLUDE_FRAGMENT, null, (Wsdl) null, false, null);
        validationResult = new PolicyValidatorResult();
        license = new AssertionLicense() {
            @Override
            public boolean isAssertionEnabled(Assertion assertion) {
                return true;
            }
        };
    }

    @Test
    public void validateAgainstPermittedAssertionsPermitted() throws Exception {
        final AllAssertion toValidate = new AllAssertion();
        context.setPermittedAssertionClasses(Collections.singleton(toValidate.getClass().getName()));
        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);
        assertNoWarningsOrErrors();
    }

    @Test
    public void validateAgainstPermittedAssertionsDenied() throws Exception {
        final AllAssertion toValidate = new AllAssertion();
        // no assertions are permitted
        context.setPermittedAssertionClasses(Collections.<String>emptySet());
        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);

        assertEquals(1, validationResult.getWarningCount());
        assertEquals(0, validationResult.getErrorCount());
        final PolicyValidatorResult.Warning warning = validationResult.getWarnings().get(0);
        assertEquals("Permission is denied for this assertion. The policy cannot be saved.", warning.getMessage());
        assertEquals(1, warning.getAssertionOrdinal());
    }

    @Test
    public void doNotValidateAgainstPermittedAssertions() throws Exception {
        final AllAssertion toValidate = new AllAssertion();
        // don't validate against white list
        context.setPermittedAssertionClasses(null);
        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);
        assertNoWarningsOrErrors();
    }

    @Test
    public void doNotValidateUnknownAssertionAgainstPermittedAssertions() throws Exception {
        final UnknownAssertion toValidate = new UnknownAssertion();
        // no assertions permitted
        context.setPermittedAssertionClasses(Collections.<String>emptySet());
        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);

        // the only warning should be the 'unrecognized assertion' warning
        assertEquals(1, validationResult.getWarningCount());
        assertEquals(0, validationResult.getErrorCount());
        final PolicyValidatorResult.Warning warning = validationResult.getWarnings().get(0);
        assertEquals("This assertion is unrecognized and may cause all requests to this service to fail.", warning.getMessage());
    }

    private void assertNoWarningsOrErrors() {
        assertEquals(0, validationResult.getWarningCount());
        assertEquals(0, validationResult.getErrorCount());
    }
}
