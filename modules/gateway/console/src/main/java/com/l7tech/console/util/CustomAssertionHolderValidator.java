package com.l7tech.console.util;

import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.validator.CustomPolicyValidator;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.validator.AssertionValidatorSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.console.api.CustomConsoleContext.addCustomExtensionInterfaceFinder;

/**
 * Validator for instances of Custom Assertion Holder.
 */
public class CustomAssertionHolderValidator extends AssertionValidatorSupport<CustomAssertionHolder> {

    public CustomAssertionHolderValidator (CustomAssertionHolder cah) {
        super(cah);

        CustomAssertion ca = cah.getCustomAssertion();
        if (ca instanceof CustomPolicyValidator) {
            CustomPolicyValidator customPolicyValidator = (CustomPolicyValidator) ca;

            // ***** NOTE *****
            // CommonUIServices is not added to Console Context because there is no need for it
            // during policy validation. CommonUIServices should only be used by custom assertion
            // UI code.
            //
            Map<String, Object> consoleContext = new HashMap<>(1);
            addCustomExtensionInterfaceFinder(consoleContext);

            // Add warning messages, if any.
            //
            List<String> warningMessages = customPolicyValidator.getWarningMessages(consoleContext);
            if (warningMessages != null) {
                for (String warningMessage : warningMessages) {
                    this.addWarningMessage(warningMessage);
                }
            }

            // Add error messages, if any.
            //
            List<String> errorMessages = customPolicyValidator.getErrorMessages(consoleContext);
            if (errorMessages != null) {
                for (String errorMessage : errorMessages) {
                    this.addMessage(errorMessage);
                }
            }
        }
    }
}