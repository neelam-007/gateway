/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

import java.io.Serializable;

/**
 * Holds an opaque message identifier, which must be encoded as a string, and an expiry time.  The expiry time
 * is not considered signifiant for purposes of equals() and hashCode().
 */
public final class MessageId implements Serializable {
    private final String opaqueIdentifier;
    private final long notValidOnOrAfterDate;

    public MessageId(String opaqueIdentifier, long notValidOnOrAfterDate) {
        this.opaqueIdentifier = opaqueIdentifier;
        this.notValidOnOrAfterDate = notValidOnOrAfterDate;
    }

    public String getOpaqueIdentifier() {
        return opaqueIdentifier;
    }

    public long getNotValidOnOrAfterDate() {
        return notValidOnOrAfterDate;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MessageId)) return false;
        MessageId other = (MessageId)obj;
        return opaqueIdentifier.equals(other.opaqueIdentifier);
    }

    public int hashCode() {
        return opaqueIdentifier.hashCode();
    }
}
