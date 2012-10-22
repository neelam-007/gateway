package com.l7tech.policy.validator;

import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.ValidationUtils;

/**
 * Policy validator for Add Header assertion.
 */
public class AddHeaderAssertionValidator extends AssertionValidatorSupport<AddHeaderAssertion> {
    public AddHeaderAssertionValidator(AddHeaderAssertion assertion) {
        super(assertion);

        final String name = assertion.getHeaderName();
        final String value = assertion.getHeaderValue();

        if (name == null || name.trim().length() < 1) {
            addMessage("Header name is empty.");
        } else if (!name.trim().equals(name)) {
            addMessage("Header name has leading or trailing whitespace.");
        } else {
            try {
                if (Syntax.getReferencedNames(name).length < 1 && !ValidationUtils.isValidMimeHeaderName(name)) {
                    addMessage("Header name is not valid: " + ValidationUtils.getMimeHeaderNameMessage(name));
                }
            } catch (VariableNameSyntaxException e) {
                // invalid variable syntax should not cause an exception
            }
        }

        if (value == null) {
            addMessage("Value is null.");
        } else if (!value.trim().equals(value)) {
            addWarningMessage("Value has leading or trailing whitespace.");
        } else {
            try {
                if (Syntax.getReferencedNames(value).length < 1 && !ValidationUtils.isValidMimeHeaderValue(value)) {
                    addWarningMessage("Value is not a valid MIME header value: " + ValidationUtils.getMimeHeaderValueMessage(name));
                }
            } catch (VariableNameSyntaxException e) {
                // invalid variable syntax should not cause an exception
            }
        }
    }
}
