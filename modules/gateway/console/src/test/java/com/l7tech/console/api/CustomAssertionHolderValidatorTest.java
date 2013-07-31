package com.l7tech.console.api;

import com.l7tech.console.util.Registry;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.cei.CustomExtensionInterfaceFinder;
import com.l7tech.policy.assertion.ext.validator.CustomPolicyValidator;
import com.l7tech.policy.validator.DefaultPolicyValidator;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.wsdl.Wsdl;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CustomAssertionHolderValidatorTest {

    private final String WARNING_MSG_1 = "CustomAssertionHolderValidatorTest Warning #1";
    private final String WARNING_MSG_2 = "CustomAssertionHolderValidatorTest Warning #2";
    private final String ERROR_MSG_1 = "CustomAssertionHolderValidatorTest Error #1";
    private final String ERROR_MSG_2 = "CustomAssertionHolderValidatorTest Error #2";

    private PolicyValidator policyValidator;
    private PolicyValidationContext pvc;
    private AssertionLicense assertionLicense;

    @Mock
    private Registry mockRegistry;

    @Mock
    private PolicyPathBuilderFactory pathBuilderFactory;

    private class CustomAssertionWithValidation implements CustomAssertion, CustomPolicyValidator {

        private final boolean raiseWarnings;
        private final boolean raiseErrors;

        public CustomAssertionWithValidation (boolean raiseWarnings, boolean raiseErrors) {
            super();
            this.raiseWarnings = raiseWarnings;
            this.raiseErrors = raiseErrors;
        }

        @Override
        public String getName () {
            return "Test Custom Assertion Name";
        }

        @Override
        public List<String> getWarningMessages (Map<String, Object> consoleContext) {
            // Test the CustomExtensionInterfaceFinder exists in the console context.
            //
            CustomExtensionInterfaceFinder extensionInterfaceFinder =
                (CustomExtensionInterfaceFinder) consoleContext.get(CustomExtensionInterfaceFinder.CONSOLE_CONTEXT_KEY);
            assertNotNull(extensionInterfaceFinder);

            if (raiseWarnings) {
                LinkedList<String> result = new LinkedList<String>();
                result.add(WARNING_MSG_1);
                result.add(WARNING_MSG_2);
                return result;
            } else {
                return null;
            }
        }

        @Override
        public List<String> getErrorMessages (Map<String, Object> consoleContext) {
            // Test the CustomExtensionInterfaceFinder exists in the console context.
            //
            CustomExtensionInterfaceFinder extensionInterfaceFinder =
                (CustomExtensionInterfaceFinder) consoleContext.get(CustomExtensionInterfaceFinder.CONSOLE_CONTEXT_KEY);
            assertNotNull(extensionInterfaceFinder);

            if (raiseErrors) {
                LinkedList<String> result = new LinkedList<String>();
                result.add(ERROR_MSG_1);
                result.add(ERROR_MSG_2);
                return result;
            } else {
                return null;
            }
        }
    }

    @Before
    public void setup() {
        when(pathBuilderFactory.makePathBuilder()).then(new Returns(new PolicyPathBuilder() {
            @Override
            public Assertion inlineIncludes(Assertion assertion, @Nullable Set<String> includedGuids, boolean includeDisabled) throws InterruptedException, PolicyAssertionException {
                return assertion;
            }

            @Override
            public List<PolicyAssertionException> preProcessIncludeFragments(@Nullable Assertion assertion) {
                return new ArrayList<PolicyAssertionException>(0);
            }

            @Override
            public PolicyPathResult generate(final Assertion assertion, boolean processIncludes, int maxPaths) throws InterruptedException, PolicyAssertionException {

                return new PolicyPathResult() {
                    @Override
                    public int getPathCount() {
                        return 1;
                    }

                    @Override
                    public Set<AssertionPath> paths() {
                        Set<AssertionPath> result = new HashSet<AssertionPath>();
                        result.add(new AssertionPath(assertion));
                        return result;
                    }
                };
            }
        }));

        when(mockRegistry.getPolicyValidator()).then(new Returns(new DefaultPolicyValidator(null, pathBuilderFactory)));

        Registry.setDefault(mockRegistry);

        policyValidator = Registry.getDefault().getPolicyValidator();
        pvc = new PolicyValidationContext(PolicyType.PRIVATE_SERVICE, null, (Wsdl) null, false, null);
        assertionLicense = new AssertionLicense() {
            @Override
            public boolean isAssertionEnabled(Assertion assertion) {
                return true;
            }
        };
    }

    @Test
    public void testNoWarningsNoErrors() throws InterruptedException {
        boolean raiseWarnings = false;
        boolean raiseErrors = false;
        CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCustomAssertion(new CustomAssertionWithValidation(raiseWarnings, raiseErrors));

        PolicyValidatorResult result = policyValidator.validate(assertion, pvc, assertionLicense);
        this.verifyValidationResult(raiseWarnings, raiseErrors, result);
    }

    @Test
    public void testWarningsOnly() throws InterruptedException {
        boolean raiseWarnings = true;
        boolean raiseErrors = false;
        CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCustomAssertion(new CustomAssertionWithValidation(raiseWarnings, raiseErrors));

        PolicyValidatorResult result = policyValidator.validate(assertion, pvc, assertionLicense);
        this.verifyValidationResult(raiseWarnings, raiseErrors, result);
    }

    @Test
    public void testErrorsOnly() throws InterruptedException {
        boolean raiseWarnings = false;
        boolean raiseErrors = true;
        CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCustomAssertion(new CustomAssertionWithValidation(raiseWarnings, raiseErrors));

        PolicyValidatorResult result = policyValidator.validate(assertion, pvc, assertionLicense);
        this.verifyValidationResult(raiseWarnings, raiseErrors, result);
    }

    @Test
    public void testBothWarningsAndErrors() throws InterruptedException {
        boolean raiseWarnings = true;
        boolean raiseErrors = true;
        CustomAssertionHolder assertion = new CustomAssertionHolder();
        assertion.setCustomAssertion(new CustomAssertionWithValidation(raiseWarnings, raiseErrors));

        PolicyValidatorResult result = policyValidator.validate(assertion, pvc, assertionLicense);
        this.verifyValidationResult(raiseWarnings, raiseErrors, result);
    }

    private void verifyValidationResult (boolean raiseWarnings, boolean raiseErrors, PolicyValidatorResult policyValidatorResult) {

        // Need to loop through warnings and errors b/c policy validation result may contain other messages
        // (ie. "No route assertion.", "This path potentially allows non-xml content through.")

        // Verify warning messages.
        //
        List<PolicyValidatorResult.Warning> actualWarnings = policyValidatorResult.getWarnings();
        boolean warningMsg1Found = false;
        boolean warningMsg2Found = false;
        for (PolicyValidatorResult.Warning actualWarning : actualWarnings) {
            String msg = actualWarning.getMessage();
            if (msg.equals(WARNING_MSG_1)) {
                warningMsg1Found = true;
            } else if (msg.equals(WARNING_MSG_2)) {
                warningMsg2Found = true;
            }
        }

        assertEquals(raiseWarnings, warningMsg1Found);
        assertEquals(raiseWarnings, warningMsg2Found);

        // Verify error messages.
        //
        List<PolicyValidatorResult.Error> actualErrors = policyValidatorResult.getErrors();
        boolean errorMsg1Found = false;
        boolean errorMsg2Found = false;
        for (PolicyValidatorResult.Error actualError : actualErrors) {
            String msg = actualError.getMessage();
            if (msg.equals(ERROR_MSG_1)) {
                errorMsg1Found = true;
            } else if (msg.equals(ERROR_MSG_2)) {
                errorMsg2Found = true;
            }
        }

        assertEquals(raiseErrors, errorMsg1Found);
        assertEquals(raiseErrors, errorMsg2Found);
    }
}