package com.l7tech.external.assertions.validatenonsoapsaml;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.wsdl.Wsdl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValidateNonSoapSamlTokenAssertionValidatorTest {
    private ValidateNonSoapSamlTokenAssertionValidator validator;
    private ValidateNonSoapSamlTokenAssertion assertion;
    private AssertionPath path;
    private PolicyValidationContext validationContext;
    private PolicyValidatorResult validatorResult;

    @Before
    public void setup() {
        assertion = new ValidateNonSoapSamlTokenAssertion();
        validator = new ValidateNonSoapSamlTokenAssertionValidator(assertion);
        path = new AssertionPath(new Assertion[]{assertion});
        validationContext = new PolicyValidationContext(PolicyType.PRIVATE_SERVICE, null, (Wsdl) null, false, null);
        validatorResult = new PolicyValidatorResult();
    }

    @Test
    public void signatureNotRequired() {
        assertion.setRequireDigitalSignature(false);

        validator.validate(path, validationContext, validatorResult);

        assertEquals(1, validatorResult.getWarningCount());
        assertEquals(ValidateNonSoapSamlTokenAssertionValidator.NO_SIGNATURE, validatorResult.getWarnings().get(0).getMessage());
    }

    @Test
    public void signatureRequired() {
        assertion.setRequireDigitalSignature(true);

        validator.validate(path, validationContext, validatorResult);

        assertEquals(0, validatorResult.getWarningCount());
    }
}
