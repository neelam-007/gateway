/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server;

import com.l7tech.server.util.MessageIdManager;
import com.l7tech.server.util.MessageId;

/** @author alex */
public class StubMessageIdManager implements MessageIdManager {
    public void assertMessageIdIsUnique(MessageId prospect) throws DuplicateMessageIdException {
    }
}
