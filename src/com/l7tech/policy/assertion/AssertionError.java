package com.l7tech.policy.assertion;

/**
 * @author alex
 * @version $Revision$
 */
public class AssertionError {
    public static final int CLIENT = 400;
    public static final int SERVER = 500;
    public static final int POLICY = 600;
    public static final int MISC   = 1000;

    public static final AssertionError NONE          = make(          0, "No Error" );
    public static final AssertionError BAD_REQUEST   = make( CLIENT | 0, "Bad Request" );
    public static final AssertionError AUTH_REQUIRED = make( CLIENT | 1, "Authentication Required" );
    public static final AssertionError AUTH_FAILED   = make( CLIENT | 2, "Authentication Failed" );
    public static final AssertionError FORBIDDEN     = make( CLIENT | 3, "Forbidden" );
    public static final AssertionError NOT_FOUND     = make( CLIENT | 4, "Service Not Found" );

    public static final AssertionError NOT_YET_IMPLEMENTED = make( MISC | 0, "Not yet implemented!" );
    public static final AssertionError NOT_APPLICABLE      = make( MISC | 1, "Not applicable in this context" );

    public static final AssertionError SERVER_ERROR = make( SERVER | 0, "Internal Server Error" );

    public static final AssertionError FALSIFIED = make( POLICY | 0, "Assertion Falsified" );
    public static final AssertionError FAILED    = make( POLICY | 1, "Error in Assertion Processing" );

    public int getNumeric() {
        return _numeric;
    }

    public String getMessage() {
        return _message;
    }

    private static AssertionError make( int numeric, String message ) {
        return new AssertionError( numeric, message );
    }

    private AssertionError( int numeric, String message ) {
        _numeric = numeric;
        _message = message;
    }

    protected final int _numeric;
    protected final String _message;

}
