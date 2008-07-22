/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 * Encapsulates the result of an Assertion's execution.  Immutable.
 *
 * @author alex
 * @version $Revision$
 */
public class AssertionResult {
    public AssertionResult( Assertion assertion, AssertionStatus status, String message, Object[] params, Throwable cause ) {
        // TODO: Defensively copy anything?
        _assertion = assertion;
        _status = status;
        _message = message;
        _params = params;
        _cause = cause;
    }

    public AssertionResult( Assertion assertion, AssertionStatus status ) {
        this( assertion, status, null, null, null );
    }

    public AssertionResult( Assertion assertion, AssertionStatus status, String message ) {
        this( assertion, status, message, null, null );
    }

    public AssertionResult( Assertion assertion, AssertionStatus status, String message, Throwable cause ) {
        this( assertion, status, message, null, cause );
    }

    public AssertionResult( Assertion assertion, AssertionStatus status, Throwable cause ) {
        this( assertion, status, null, null, cause );
    }

    public AssertionResult( Assertion assertion, AssertionStatus status, String message, Object[] params ) {
        this( assertion, status, message, params, null );
    }

    public Assertion getAssertion() {
        return _assertion;
    }

    public AssertionStatus getStatus() {
        return _status;
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
    private final AssertionStatus _status;
    private final String _message;
    private final Object[] _params;
    private final Throwable _cause;
}
