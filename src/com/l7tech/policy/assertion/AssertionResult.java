/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import com.l7tech.message.Request;

/**
 * Encapsulates the result of an Assertion's execution.  Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class AssertionResult {
    public AssertionResult( Assertion assertion, Request request, AssertionError error, String message, Object[] params, Throwable cause ) {
        _assertion = assertion;
        _request = request;
        _error = error;
        _message = message;
        _params = params;
        _cause = cause;
    }

    public AssertionResult( Assertion assertion, Request request, AssertionError error, String message ) {
        this( assertion, request, error, message, null, null );
    }

    public AssertionResult( Assertion assertion, Request request, AssertionError error, Throwable cause ) {
        this( assertion, request, error, null, null, cause );
    }

    public AssertionResult( Assertion assertion, Request request, AssertionError error, String message, Object[] params ) {
        this( assertion, request, error, message, params, null );
    }

    public Assertion getAssertion() {
        return _assertion;
    }

    public Request getRequest() {
        return _request;
    }

    public AssertionError getError() {
        return _error;
    }

    public String getMessage() {
        return _message;
    }

    public Object[] getParams() {
        return _params;
    }

    public Throwable getCause() {
        return _cause;
    }

    private final Assertion _assertion;
    private final Request _request;
    private final AssertionError _error;
    private final String _message;
    private final Object[] _params;
    private final Throwable _cause;
}
