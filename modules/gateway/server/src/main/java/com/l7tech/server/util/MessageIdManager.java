/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.server.util;

/**
 * @author mike
 */
public interface MessageIdManager {

    /**
     * Represents a failure during message id check.
     */
    class MessageIdCheckException extends Exception {
        public MessageIdCheckException( final String message ) {
            super( message );
        }

        public MessageIdCheckException( final String message, final Throwable cause ) {
            super( message, cause );
        }

        MessageIdCheckException() {
            super();
        }
    }

    /**
     * Represents that message id being checked is already stored.
     */
    class DuplicateMessageIdException extends MessageIdCheckException {}

    /**
     * Atomically check for the presence of the specified message Id, and remember it either way.  If the specified
     * prospect MessageId is already known, throws DuplicateMessageIdException.  Otherwise,
     * stores the prospect MessageId and then returns.
     *
     * @param prospect The message Id to test and set.
     * @throws DuplicateMessageIdException if this message Id is already known.
     * @throws MessageIdCheckException if the message Id check fails.
     */
    void assertMessageIdIsUnique(MessageId prospect) throws MessageIdCheckException;

}
