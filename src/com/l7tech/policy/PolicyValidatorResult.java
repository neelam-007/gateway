package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class represents the result of the policy validation.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyValidatorResult {
    private List errors = new ArrayList();
    private List warnings = new ArrayList();

    /**
     * Returns the number of the errors that were collected
     * in this policy validator result.
     *
     * @return the number of errors
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * Returns the number of the warnings that were collected
     * in this policy validator result.
     *
     * @return the number of warning
     */
    public int getWarningCount() {
        return errors.size();
    }


    /**
     * Returns the unmodifiable collection of errors collected.
     *
     * @return the <code>List</code> of errors
     */
    public List getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns the unmodifiable collection of warnings collected.
     *
     * @return the <code>List</code> of warninigs
     */
    public List getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Add the error to this validator result.
     *
     * @param err the error to add
     */
    public void addError(Error err) {
        errors.add(err);
    }

    /**
     * Add the warning to this validator result.
     *
     * @param w the warning to add
     */
    public void addWarning(Warning w) {
        warnings.add(w);
    }


    /**
     * The class represents the policy validation error
     * todo: add warning and info levels
     */
    private static class Message {
        private Assertion assertion;
        private String message;
        private Throwable throwable;

        public Message(Assertion erroAssertion, String message, Throwable throwable) {
            this.assertion = erroAssertion;
            this.message = message;
            this.throwable = throwable;
        }

        public Assertion getAssertion() {
            return assertion;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getThrowable() {
            return throwable;
        }

    }

    public static class Error extends Message {
        public Error(Assertion erroAssertion, String message, Throwable throwable) {
            super(erroAssertion, message, throwable);
        }
    }

    public static class Warning extends Message {
        public Warning(Assertion erroAssertion, String message, Throwable throwable) {
            super(erroAssertion, message, throwable);
        }
    }
}
