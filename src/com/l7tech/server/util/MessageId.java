/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

/**
 * Holds an opaque message identifier, which must be encoded as a string, and an expiry time.  The expiry time
 * is not considered signifiant for purposes of equals() and hashCode().
 */
public final class MessageId {
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
        return opaqueIdentifier.equals(obj);
    }

    public int hashCode() {
        return opaqueIdentifier.hashCode();
    }
}
