package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;

import java.util.*;

/**
 * This class represents the result of the policy validation.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyValidatorResult {
    private List errors = new ArrayList();
    private List warnings = new ArrayList();
    private Map assertionMessages = new HashMap();

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
        final Assertion assertion = err.getAssertion();
        List list = (List)assertionMessages.get(assertion);
        if (list == null) {
            list = new ArrayList();
            assertionMessages.put(assertion, list);
        }
        list.add(err);
    }

    /**
     * Add the warning to this validator result.
     *
     * @param w the warning to add
     */
    public void addWarning(Warning w) {
        warnings.add(w);
        final Assertion assertion = w.getAssertion();
        List list = (List)assertionMessages.get(assertion);
        if (list == null) {
            list = new ArrayList();
            assertionMessages.put(assertion, list);
        }
        list.add(w);
    }

    /**
     * Retrieve the list of messages for a given assertion.
     * @param a the assertion or <b>null</b> for messages that do not belong
     *          to a particular assertion
     * @return the list of assertion messages
     */
    public List messages(Assertion a) {
        List messages = (List)assertionMessages.get(a);
        if (messages !=null) {
            return messages;
        }
        return Collections.EMPTY_LIST;
    }
    /**
     * The class represents the policy validation error
     * todo: add warning and info levels
     */
    public static class Message {
        private Assertion assertion;
        private String message;
        private Throwable throwable;

        Message(Assertion erroAssertion, String message, Throwable throwable) {
            if (message == null) {
                throw new IllegalArgumentException();
            }
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

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Message)) return false;

            final Message message1 = (Message)o;

            if (assertion != null ? !assertion.equals(message1.assertion) : message1.assertion != null) return false;
            if (!message.equals(message1.message)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (assertion != null ? assertion.hashCode() : 0);
            result = 29 * result + message.hashCode();
            return result;
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
