/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.util;

import java.io.IOException;

/**
 * @author mike
 * @version $Revision$
 */
public class CausedIOException extends IOException {
    public CausedIOException(Throwable cause) {
        super();
        initCause(cause);
    }
}
