package com.l7tech.policy.assertion;

import java.util.logging.Level;

import com.l7tech.common.util.SoapFaultUtils;

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

    public static final AssertionStatus UNDEFINED         = make( -1, Level.INFO, "Undefined" );

    /** Assertion finished successfully */
    public static final AssertionStatus NONE              = make(          0, Level.FINE, "No Error" );
    public static final AssertionStatus BAD_REQUEST       = make( CLIENT + 0, Level.INFO, "Bad Request" );
    /** Credentials required but missing */
    public static final AssertionStatus AUTH_REQUIRED     = make( CLIENT + 1, Level.INFO, "Authentication Required", true );
    /** Credentials present but erroneous */
    public static final AssertionStatus AUTH_FAILED       = make( CLIENT + 2, Level.WARNING, "Authentication Failed", true );
    public static final AssertionStatus UNAUTHORIZED      = make( CLIENT + 2, Level.INFO, "Unauthorized", true );
    public static final AssertionStatus SERVICE_DISABLED  = make( CLIENT + 3, Level.INFO, "Service Disabled" );
    /** Couldn't resolve a service for this request */
    public static final AssertionStatus SERVICE_NOT_FOUND = make( CLIENT + 4, Level.INFO, "Service Not Found" );

    /** Assertion not yet implemented. */
    public static final AssertionStatus NOT_YET_IMPLEMENTED = make( MISC + 0, Level.INFO, "Not yet implemented!" );
    public static final AssertionStatus NOT_APPLICABLE      = make( MISC + 1, Level.INFO, "Not applicable in this context" );

    /** Generic catch-all status */
    public static final AssertionStatus SERVER_ERROR        = make( SERVER + 0, Level.INFO, "Internal Server Error" );
    public static final AssertionStatus SERVER_AUTH_FAILED  = make( SERVER + 1, Level.INFO, "Access Denied by Protected Service" );

    /** A generic negative result */
    public static final AssertionStatus FALSIFIED = make( POLICY + 0, Level.INFO, "Assertion Falsified" );
    public static final AssertionStatus FAILED    = make( POLICY + 1, Level.INFO, "Error in Assertion Processing" );

    public int getNumeric() {
        return _numeric;
    }

    public Level getLevel() {
        return _level;
    }

    public String getMessage() {
        return _message;
    }
    
    public boolean isAuthProblem() {
        return _authProblem;
    }

    public String getSoapFaultCode() {
        if ( _numeric >= CLIENT && _numeric < SERVER )
            return SoapFaultUtils.FC_CLIENT;
        else
            return SoapFaultUtils.FC_SERVER;
    }

    private static AssertionStatus make( int numeric, Level level, String message, boolean auth ) {
        return new AssertionStatus( numeric, level, message, auth );
    }

    private static AssertionStatus make( int numeric, Level level, String message ) {
        return make( numeric, level, message, false );
    }

    private AssertionStatus( int numeric, Level level, String message, boolean auth ) {
        _numeric = numeric;
        _level = level;
        _message = message;
        _authProblem = auth;
    }

    public String toString() {
        return "<" + getClass() + ": " + getNumeric() + "=" + getLevel().getName() + ":"  + getMessage() + ">";
    }

    protected final int _numeric;
    protected final Level _level;
    protected final String _message;
    protected final boolean _authProblem;
}
