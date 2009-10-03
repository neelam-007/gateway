package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.Wsdl;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple utility class for validators.  This class makes it easy to accumulate a collection
 * of simple validator warnings and add them all at once each time validate() is called on an assertion class.
 */
public class AssertionValidatorSupport<AT extends Assertion> implements AssertionValidator {
    private final AT assertion;
    private Collection<Pair<String, Throwable>> errs = new ArrayList<Pair<String, Throwable>>();

    public AssertionValidatorSupport(AT assertion) {
        this.assertion = assertion;
    }

    public AT getAssertion() {
        return assertion;
    }

    public void addMessage(String message) {
        errs.add(new Pair<String, Throwable>(message, null));
    }

    public void addMessageAndException(String message, Throwable exception) {
        errs.add(new Pair<String, Throwable>(message, exception));
    }

    public boolean requireNonEmpty(String str, String message) {
        if (str == null || str.trim().length() < 1) {
            addMessage(message);
            return false;
        }
        return true;
    }

    public void requireNonEmptyArray(String[] array, String messageIfEmpty) {
        if (array == null || array.length < 1 || array[0] == null)
            addMessage(messageIfEmpty);
    }

    public void requireValidInteger(String intStr, String messageIfMissing, String messageIfInvalid) {
        requireNonEmpty(intStr, messageIfMissing);
        if (intStr != null && !isValidInt(intStr))
            addMessage(messageIfInvalid);
    }

    public void requireValidIntegerOrContextVariable(String val, String messageIfMissing, String messageIfInvalid) {
        if (!requireNonEmpty(val, messageIfMissing))
            return;
        if (Syntax.getReferencedNames(val).length <= 0 && !isValidInt(val))
            addMessage(messageIfInvalid);
    }

    @Override
    public void validate(AssertionPath path, Wsdl wsdl, boolean soap, PolicyValidatorResult result) {
        for (Pair<String, Throwable> err : errs) {
            result.addError(new PolicyValidatorResult.Error(assertion, path, err.left, err.right));
        }
    }

    private boolean isValidInt(String intStr) {
        try {
            Integer.parseInt(intStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
