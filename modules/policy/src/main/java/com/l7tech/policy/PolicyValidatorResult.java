package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

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

    
    private Map<Collection<Integer>,List<Message>> assertionMessages = new HashMap<Collection<Integer>,List<Message>>();

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
    public List<Message> getMessages() {
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
        List<Message> list = assertionMessages.get(err.getAssertionIndexPath());
        if (list == null) {
            list = new ArrayList<Message>();
            assertionMessages.put(err.getAssertionIndexPath(), list);
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
        List<Message> list = assertionMessages.get(w.getAssertionIndexPath());
        if (list == null) {
            list = new ArrayList<Message>();
            assertionMessages.put(w.getAssertionIndexPath(), list);
        }
        list.add(w);
    }

    /**
     * Retrieve the list of messages for a given assertion.
     *
     * @param a the assertion or <b>null</b> for messages that do not belong
     *          to a particular assertion
     * @return the list of assertion messages
     */
    public List<Message> messages(Assertion a) {
        return messages( buildIndexPath(a));
    }

    /**
     * Retrieve the list of messages for a given assertion by ordinal.
     *
     * @param assertionOrdinal the assertion or <b>null</b> for messages that do not belong
     *          to a particular assertion
     * @return the list of assertion messages
     */
    public List<Message> messages(int assertionOrdinal) {
        List<Message> messages = null;

        for ( List<Message> messageList : assertionMessages.values() ) {
            if ( !messageList.isEmpty() ) {
                Message first = messageList.get( 0 );
                if ( first.getAssertionOrdinal() == assertionOrdinal ) {
                    messages = messageList;
                    break;
                }
            }
        }

        if ( messages != null ) {
            return messages;
        }

        return Collections.emptyList();
    }

    /**
     * Retrieve the list of messages for a given assertion.
     *
     * @param assertionIndexPath the assertion index path
     * @return the list of assertion messages
     */
    public List<Message> messages(List<Integer> assertionIndexPath) {
        List<Message> messages = assertionMessages.get(assertionIndexPath);
        if (messages !=null) {
            return messages;
        }
        return Collections.emptyList();
    }

    private static List<Integer> buildIndexPath(final Assertion assertion) {
        List<Integer> ords = new ArrayList<Integer>();

        Assertion current = assertion;
        while( current != null ) {
            Assertion parent = current.getParent();
            if (parent != null) {
                ords.add( ((CompositeAssertion)parent).getChildren().indexOf( current ) );
            }
            current = parent;
        }

        Collections.reverse(ords);

        return ords;
    }
    
    /**
     * The class represents the policy validation result
     */
    public static class Message implements Serializable {
        private final int ordinal;
        private final List<Integer> assertionIndexPath;
        private final String message;
        private final Throwable throwable;
        private final String remedialActionClassname;

        Message(Collection<Integer> assertionIndexPath, int ordinal, String message, Throwable throwable, String remedialActionClassname) {
            if (message == null) throw new IllegalArgumentException();
            this.ordinal = ordinal;
            this.assertionIndexPath = new ArrayList<Integer>( assertionIndexPath );
            this.message = message;
            this.throwable = throwable;
            this.remedialActionClassname = remedialActionClassname;
        }

        Message(Collection<Integer> assertionIndexPath, int ordinal, String message, Throwable throwable) {
            this( assertionIndexPath, ordinal, message, throwable, null );
        }

        Message(Assertion assertion, String message, Throwable throwable) {
            this( buildIndexPath(assertion), assertion.getOrdinal(), message, throwable);
        }

        public Message(Assertion assertion, String message, Throwable throwable, String remedialActionClassname) {
            this( buildIndexPath(assertion), assertion.getOrdinal(), message, throwable, remedialActionClassname);
        }

        public List<Integer> getAssertionIndexPath() {
            return Collections.unmodifiableList( assertionIndexPath );
        }

        public int getAssertionOrdinal() {
            return ordinal;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public String getRemedialActionClassname() {
            return remedialActionClassname;
        }

        @SuppressWarnings({"RedundantIfStatement"})
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Message message1 = (Message) o;

            if (ordinal != message1.ordinal) return false;
            if (!assertionIndexPath.equals( message1.assertionIndexPath )) return false;
            if (message != null ? !message.equals(message1.message) : message1.message != null) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = ordinal * 17;
            result = 31 * result + assertionIndexPath.hashCode();
            result = 31 * result + (message != null ? message.hashCode() : 0);
            return result;
        }
    }

    public static class Error extends Message implements Serializable {
        public Error(List<Integer> errorAssertionIndexPath, int ordinal, String message, Throwable throwable) {
            super(errorAssertionIndexPath, ordinal, message, throwable);
        }

        public Error(Assertion error, String message, Throwable throwable) {
            super(error, message, throwable);
        }

        public Error(Assertion error, String message, Throwable throwable, String remedialActionClassname) {
            super(error, message, throwable, remedialActionClassname);
        }
    }

    public static class Warning extends Message implements Serializable {
        public Warning(List<Integer> warningAssertionIndexPath, int ordinal, String message, Throwable throwable) {
            super(warningAssertionIndexPath, ordinal, message, throwable);
        }

        public Warning(Assertion warning, String message, Throwable throwable) {
            super(warning, message, throwable);
        }

        public Warning(Assertion warning, String message, Throwable throwable, String remedialActionClassname) {
            super(warning, message, throwable, remedialActionClassname);
        }
    }
}
