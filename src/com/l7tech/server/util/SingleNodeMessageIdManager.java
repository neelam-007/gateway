/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

/**
 * A fast MessageIdManager that works on a non-clustered (single-node-only) SSG.  Does not write to the database
 * or otherwise persistently store the IDs in any way.
 */
public class SingleNodeMessageIdManager implements MessageIdManager {
    private Set messageIdSet = new HashSet();
    private long lastExpiryScan = 0L;
    private static final long EXPIRY_SCAN_MILLIS = 1000 * 60 * 5; // scan every 5 minutes
    public static final long EXPIRY_GRACE_MILLIS = 1000 * 60 * 1; // remember for at least 1 minute longer than asked

    private static class Holder {
        private static SingleNodeMessageIdManager INSTANCE = new SingleNodeMessageIdManager();
    }

    public static SingleNodeMessageIdManager getInstance() {
        return Holder.INSTANCE;
    }

    private SingleNodeMessageIdManager() {}

    public void assertMessageIdIsUnique(MessageId prospect) throws MessageIdManager.DuplicateMessageIdException {
        assertMessageIdIsUnique(prospect, System.currentTimeMillis());
    }

    public synchronized void assertMessageIdIsUnique(MessageId prospect, long currentTime) throws DuplicateMessageIdException {
        if (messageIdSet.contains(prospect))
            throw new DuplicateMessageIdException();
        messageIdSet.add(prospect);
        if (currentTime - lastExpiryScan > EXPIRY_SCAN_MILLIS)
            doExpiryScan(currentTime);
    }

    private synchronized void doExpiryScan(long now) {
        for (Iterator i = messageIdSet.iterator(); i.hasNext();) {
            MessageId messageId = (MessageId)i.next();
            if (now >= messageId.getNotValidOnOrAfterDate() - EXPIRY_GRACE_MILLIS)
                i.remove();
        }
    }
}
