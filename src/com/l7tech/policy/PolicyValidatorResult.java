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

    /**
     * Returns the number of the errors that were collected
     * in this policy validator result.
     *
     * @return the number of errors
     */
    public int getErroCount() {
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
     * Add the error to this validator result.
     *
     * @param err the error to add
     */
    public void addError(Error err) {
        errors.add(err);
    }

    /**
     * The class represents the policy validation error
     */
    public static class Error {
        private Assertion assertion;
        private String message;
        private Throwable throwable;

        public Error(Assertion erroAssertion, String message, Throwable throwable) {
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
}
