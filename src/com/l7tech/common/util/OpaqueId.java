/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Holds a unique ID consisting of a non-null byte array.
 */
public final class OpaqueId implements Serializable {
    private final byte[] opaque;
    private transient int hash = 0;

    /** Create a new OpaqueId with a random byte array. */
    public OpaqueId() {
        opaque = new byte[20];
        new SecureRandom().nextBytes(opaque);
    }

    /** Create a new OpaqueId with the specified random byte array. */
    public OpaqueId(byte[] opaque) {
        if (opaque == null)
            throw new IllegalArgumentException("opaque ID must be a non-null byte array");
        this.opaque = opaque;
    }

    public byte[] getOpaque() {
        return opaque;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpaqueId)) return false;

        final OpaqueId opaqueId = (OpaqueId)o;

        if (!Arrays.equals(opaque, opaqueId.opaque)) return false;

        return true;
    }

    public int hashCode() {
        if (hash == 0 && opaque != null)
            hash = HashCode.compute(opaque);
        return hash;
    }

    public String toString() {
        return HexUtils.hexDump(opaque);
    }
}
