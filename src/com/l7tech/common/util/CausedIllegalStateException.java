/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

/**
 * Just like IllegalStateException but now with causes
 */
public class CausedIllegalStateException extends IllegalStateException {
    public CausedIllegalStateException() {
    }

    public CausedIllegalStateException(String s) {
        super(s);
    }

    public CausedIllegalStateException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }

    public CausedIllegalStateException(Throwable cause) {
        super();
        initCause(cause);
    }

}
