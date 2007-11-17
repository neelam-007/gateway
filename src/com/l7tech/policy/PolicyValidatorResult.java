package com.l7tech.policy;

import com.l7tech.common.util.Pair;
import com.l7tech.policy.assertion.Assertion;

import java.util.*;
import java.io.Serializable;

/**
 * This class represents the result of the policy validation.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class PolicyValidatorResult implements Serializable {
    private List<Error> errors = new ArrayList<Error>();
    private List<Warning> warnings = new ArrayList<Warning>();

    
    private Map<Pair<Long, Integer>,List<Message>> assertionMessages = new HashMap<Pair<Long, Integer>,List<Message>>();

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
        return warnings.size();
    }


    /**
     * Returns the unmodifiable collection of errors collected.
     *
     * @return the <code>List</code> of errors
     */
    public List<Error> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns the unmodifiable collection of warnings collected.
     *
     * @return the <code>List</code> of warninigs
     */
    public List<Warning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    /**
     * Returns the unmodifiable collection of all the messages collected.
     *
     * @return the <code>List</code> of all the messages
     */
    public List getMessages() {
        List<Message> all = new ArrayList<Message>();
        all.addAll(getErrors());
        all.addAll(getWarnings());
        return all;
    }
    /**
     * Add the error to this validator result.
     *
     * @param err the error to add
     */
    public void addError(Error err) {
        errors.add(err);
        final Pair<Long, Integer> assertion = new Pair<Long, Integer>(err.getPolicyOid(), err.getAssertionOrdinal());
        List<Message> list = assertionMessages.get(assertion);
        if (list == null) {
            list = new ArrayList<Message>();
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
        final Pair<Long, Integer> assertion = new Pair<Long, Integer>(w.getPolicyOid(), w.getAssertionOrdinal());
        List<Message> list = assertionMessages.get(assertion);
        if (list == null) {
            list = new ArrayList<Message>();
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
    public List<Message> messages(Assertion a) {
        List<Message> messages = assertionMessages.get(new Pair<Long, Integer>(a.getOwnerPolicyOid(), a.getOrdinal()));
        if (messages !=null) {
            return messages;
        }
        return Collections.emptyList();
    }
    /**
     * The class represents the policy validation result
     */
    public static class Message implements Serializable {
        private final Long policyOid;
        private final int assertionOrdinal;
        private final String message;
        private final Throwable throwable;
        private final int assertionPathOrder;

        Message(Long policyOid, int errorAssertionOrdinal, int apOrder, String message, Throwable throwable) {
            if (message == null) throw new IllegalArgumentException();
            this.policyOid = policyOid;
            this.assertionOrdinal = errorAssertionOrdinal;
            this.message = message;
            this.throwable = throwable;
            this.assertionPathOrder = apOrder;
        }

        public Long getPolicyOid() {
            return policyOid;
        }

        public int getAssertionOrdinal() {
            return assertionOrdinal;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public int getAssertionPathOrder() {
            return assertionPathOrder;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Message message1 = (Message) o;

            if (assertionOrdinal != message1.assertionOrdinal) return false;
            if (message != null ? !message.equals(message1.message) : message1.message != null) return false;
            if (policyOid != null ? !policyOid.equals(message1.policyOid) : message1.policyOid != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = (policyOid != null ? policyOid.hashCode() : 0);
            result = 31 * result + assertionOrdinal;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            result = 31 * result + (throwable != null ? throwable.hashCode() : 0);
            result = 31 * result + assertionPathOrder;
            return result;
        }
    }

    public static class Error extends Message implements Serializable {
        public Error(Long policyOid, int errorAssertionOrdinal, int apOrder, String message, Throwable throwable) {
            super(policyOid, errorAssertionOrdinal, apOrder, message, throwable);
        }

        public Error(Assertion error, AssertionPath ap, String message, Throwable throwable) {
            super(error.getOwnerPolicyOid(), error.getOrdinal(), ap.getPathOrder(), message, throwable);
        }
    }

    public static class Warning extends Message implements Serializable {
        public Warning(Long policyOid, int warningAssertionOrdinal, int apOrder, String message, Throwable throwable) {
            super(policyOid, warningAssertionOrdinal, apOrder, message, throwable);
        }

        public Warning(Assertion warning, AssertionPath ap, String message, Throwable throwable) {
            super(warning.getOwnerPolicyOid(), warning.getOrdinal(), ap.getPathOrder(), message, throwable);
        }
    }
}
