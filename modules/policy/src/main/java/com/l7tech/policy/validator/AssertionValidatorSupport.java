package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.Syntax;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Simple utility class for validators.  This class makes it easy to accumulate a collection
 * of simple validator warnings and add them all at once each time validate() is called on an assertion class.
 */
public class AssertionValidatorSupport<AT extends Assertion> implements AssertionValidator {
    private final AT assertion;
    private Collection<PendingMessage> errs = new ArrayList<PendingMessage>();
    private Collection<PendingMessage> warns = new ArrayList<PendingMessage>();

    public AssertionValidatorSupport(AT assertion) {
        this.assertion = assertion;
    }

    public AT getAssertion() {
        return assertion;
    }

    public void addMessage(String message) {
        errs.add(new PendingMessage(assertion, message));
    }

    public void addMessageAndException(String message, Throwable exception) {
        errs.add(new PendingMessage(assertion, message, exception));
    }

    protected void addMessage(PendingMessage message) {
        errs.add(message);
    }

    public void addWarningMessage(String message) {
        warns.add(new PendingMessage(assertion, message, null));
    }

    protected void addWarningMessage(PendingMessage message) {
        warns.add(message);
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
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        for (PendingMessage err : errs) {
            //noinspection ThrowableResultOfMethodCallIgnored
            result.addError(new PolicyValidatorResult.Error(findTarget(err), path, err.getMessage(), err.getThrowable()));
        }
        for (PendingMessage warn : warns) {
            result.addWarning(new PolicyValidatorResult.Warning(findTarget(warn), path, warn.getMessage(), null));
        }
    }

    private Assertion findTarget(PendingMessage warn) {
        Assertion target = warn.getTargetAssertion();
        return target != null ? target : assertion;
    }

    private boolean isValidInt(String intStr) {
        try {
            Integer.parseInt(intStr);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected static class PendingMessage {
        private final Assertion targetAssertion;
        private final String message;
        private final Throwable throwable;

        public PendingMessage(Assertion targetAssertion, String message) {
            this(targetAssertion, message, null);
        }

        public PendingMessage(Assertion targetAssertion, String message, Throwable throwable) {
            this.targetAssertion = targetAssertion;
            this.message = message;
            this.throwable = throwable;
        }

        public Assertion getTargetAssertion() {
            return targetAssertion;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }
}
