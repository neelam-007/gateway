package com.l7tech.policy.assertion;

import com.l7tech.common.util.SoapUtil;

/**
 * Typesafe enum with values for lots of conceivable error conditions (including NONE)
 * that could result from processing an assertion.
 *
 * @author alex
 * @version $Revision$
 */
public class AssertionStatus {
    static final int CLIENT = 400;
    static final int SERVER = 500;
    static final int POLICY = 600;
    static final int MISC   = 1000;

    public static final AssertionStatus UNDEFINED        = make( -1, "Undefined" );

    /** Assertion finished successfully */
    public static final AssertionStatus NONE              = make(           0, "No Error" );
    public static final AssertionStatus BAD_REQUEST       = make( CLIENT +  0, "Bad Request" );
    /** Credentials required but missing */
    public static final AssertionStatus AUTH_REQUIRED     = make( CLIENT +  1, "Authentication Required" );
    /** Credentials present but erroneous */
    public static final AssertionStatus AUTH_FAILED       = make( CLIENT +  2, "Authentication Failed" );
    public static final AssertionStatus FORBIDDEN         = make( CLIENT +  3, "Forbidden" );
    public static final AssertionStatus SERVICE_DISABLED  = make( CLIENT +  3, "Service Disabled" );
    /** Couldn't resolve a service for this request */
    public static final AssertionStatus SERVICE_NOT_FOUND = make( CLIENT +  4, "Service Not Found" );
    public static final AssertionStatus UNAUTHORIZED      = make( CLIENT +  2, "Unauthorized" );

    /** Assertion not yet implemented. */
    public static final AssertionStatus NOT_YET_IMPLEMENTED = make( MISC + 0, "Not yet implemented!" );
    public static final AssertionStatus NOT_APPLICABLE      = make( MISC + 1, "Not applicable in this context" );

    /** Generic catch-all status */
    public static final AssertionStatus SERVER_ERROR = make( SERVER + 0, "Internal Server Error" );

    /** A generic negative result */
    public static final AssertionStatus FALSIFIED = make( POLICY + 0, "Assertion Falsified" );
    public static final AssertionStatus FAILED    = make( POLICY + 1, "Error in Assertion Processing" );

    public int getNumeric() {
        return _numeric;
    }

    public String getMessage() {
        return _message;
    }

    public String getSoapFaultCode() {
        if ( _numeric >= CLIENT && _numeric < SERVER )
            return SoapUtil.FC_CLIENT;
        else
            return SoapUtil.FC_SERVER;
    }

    private static AssertionStatus make( int numeric, String message ) {
        return new AssertionStatus( numeric, message );
    }

    private AssertionStatus( int numeric, String message ) {
        _numeric = numeric;
        _message = message;
    }

    public String toString() {
        return "<" + getClass() + ": " + getNumeric() + "=" + getMessage() + ">";
    }

    protected final int _numeric;
    protected final String _message;

}
