package com.l7tech.policy.validator;

import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomCredentialSource;
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
        context = new PolicyValidationContext(PolicyType.INCLUDE_FRAGMENT, null, null, (Wsdl) null, false, null);
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

    @Test
    public void validateAgainstNoAccessControlAssertions() throws Exception {
        final CustomAssertionHolder toValidate = new CustomAssertionHolder();
        //noinspection serial
        toValidate.setCustomAssertion(new CustomAssertion() {
            @Override
            public String getName() {
                return "Test CustomAssertion";
            }
        });
        toValidate.setCategories(Category.MESSAGE, Category.CUSTOM_ASSERTIONS);

        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);

        assertSame(0, validationResult.getErrorCount());
        assertSame(0, validationResult.getWarningCount());
    }

    @Test
    public void validateAgainstAccessControlAssertions() throws Exception {
        final CustomAssertionHolder toValidate = new CustomAssertionHolder();
        //noinspection serial
        toValidate.setCustomAssertion(new CustomAssertion() {
            @Override
            public String getName() {
                return "Test CustomAssertion";
            }
        });
        toValidate.setCategories(Category.MESSAGE, Category.ACCESS_CONTROL, Category.CUSTOM_ASSERTIONS);

        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);

        assertSame(1, validationResult.getErrorCount());
        assertSame(0, validationResult.getWarningCount());
    }

    @Test
    public void validateAgainstCustomCredentialSourceAssertionsReturningFalse() throws Exception {
        final CustomAssertionHolder toValidate = new CustomAssertionHolder();

        //noinspection serial
        class TestCustomCredentialSourceAssertion implements CustomAssertion, CustomCredentialSource {
            @Override public String getName() { return "TestCustomCredentialSourceAssertion"; }
            @Override public boolean isCredentialSource() { return false; }
        }

        //noinspection serial
        toValidate.setCustomAssertion(new TestCustomCredentialSourceAssertion());
        toValidate.setCategories(Category.MESSAGE, Category.CUSTOM_ASSERTIONS);

        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);

        assertSame(0, validationResult.getErrorCount());
        assertSame(0, validationResult.getWarningCount());
    }

    @Test
    public void validateAgainstCustomCredentialSourceAssertionsReturningTrue() throws Exception {
        final CustomAssertionHolder toValidate = new CustomAssertionHolder();

        //noinspection serial
        class TestCustomCredentialSourceAssertion implements CustomAssertion, CustomCredentialSource {
            @Override public String getName() { return "TestCustomCredentialSourceAssertion"; }
            @Override public boolean isCredentialSource() { return true; }
        }

        //noinspection serial
        toValidate.setCustomAssertion(new TestCustomCredentialSourceAssertion());
        toValidate.setCategories(Category.MESSAGE, Category.CUSTOM_ASSERTIONS);

        final PathValidator validator = new PathValidator(new AssertionPath(toValidate), context, license, validationResult);
        validator.validate(toValidate);

        assertSame(1, validationResult.getErrorCount());
        assertSame(0, validationResult.getWarningCount());
    }
}
