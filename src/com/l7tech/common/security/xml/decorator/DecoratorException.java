/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml.decorator;

/**
 * @author mike
 */
public class DecoratorException extends Exception {
    public DecoratorException() {}
    public DecoratorException(String message) {super(message);}
    public DecoratorException(Throwable cause) {super(cause);}
    public DecoratorException(String message, Throwable cause) {super(message, cause);}
}
