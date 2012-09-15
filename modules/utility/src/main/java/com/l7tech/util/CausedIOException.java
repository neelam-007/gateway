/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.util;

import java.io.IOException;

/**
 * Required pre 1.6 to enable the 'initCause' hack as there was no constructor available before 1.6 to take a cause.
 *
 * @author mike
 * @version $Revision$
 */
public class CausedIOException extends IOException {
    public CausedIOException(Throwable cause) {
        super();
        initCause(cause);
    }

    public CausedIOException() {        
    }

    public CausedIOException(String s) {
        super(s);
    }

    public CausedIOException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
