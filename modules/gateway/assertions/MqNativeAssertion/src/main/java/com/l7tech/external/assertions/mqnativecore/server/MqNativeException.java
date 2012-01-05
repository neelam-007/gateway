/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id: MqRuntimeException.java 27203 2010-11-20 01:00:55Z vchan $
 */

package com.l7tech.external.assertions.mqnativecore.server;

/**
 * This exception is thrown when there's a MQ native problem.
 */
public class MqNativeException extends Exception {
    public MqNativeException(String message) {
        super( message );
    }

    public MqNativeException(Throwable e) {
        super(e);
    }

    public MqNativeException(String message, Throwable cause) {
        super(message, cause);
    }
}