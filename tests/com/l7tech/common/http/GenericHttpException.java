/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import java.io.IOException;

/**
 * Catch-all exception for problems during a generic HTTP request.
 */
public class GenericHttpException extends IOException {
    public GenericHttpException() {
    }

    public GenericHttpException(Throwable cause) {
        super();
        initCause(cause);
    }

    public GenericHttpException(String s) {
        super(s);
    }

    public GenericHttpException(String s, Throwable cause) {
        super(s);
        initCause(cause);
    }
}
