/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

/**
 * @author mike
 */
public interface MessageIdManager {

    public static class DuplicateMessageIdException extends Exception {}

    /**
     * Atomically check for the presence of the specified message Id, and remember it either way.  If the specified
     * prospect MessageId is already known, throws IllegalStateException.  Otherwise,
     * stores the prospect MessageId and then returns.
     *
     * @param prospect The message Id to test and set.
     * @throws DuplicateMessageIdException if this message Id is already known.
     */
    void assertMessageIdIsUnique(MessageId prospect) throws DuplicateMessageIdException;

}
