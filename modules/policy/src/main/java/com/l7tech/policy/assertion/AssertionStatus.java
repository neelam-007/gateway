package com.l7tech.policy.assertion;

import com.l7tech.util.SoapConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Typesafe enum with values for lots of conceivable error conditions (including NONE)
 * that could result from processing an assertion.
 *
 * @author alex
 */
public class AssertionStatus {
    static final int CLIENT = 400;
    static final int SERVER = 500;
    static final int POLICY = 600;
    static final int MISC   = 1000;

    private static final Map statuses = new HashMap();

    public static final AssertionStatus UNDEFINED         = make( statuses, -1, Level.INFO, "Undefined" );

    /** Assertion finished successfully */
    public static final AssertionStatus NONE              = make( statuses,          0, Level.FINE, "No Error" );

    /** The message is not syntactically or semantically valid */
    public static final AssertionStatus BAD_REQUEST       = make( statuses, CLIENT + 0, Level.INFO, "Bad Request" );
    /** Credentials required but missing */
    public static final AssertionStatus AUTH_REQUIRED     = make( statuses, CLIENT + 1, Level.INFO, "Authentication Required", true );
    /** Credentials present but erroneous */
    public static final AssertionStatus AUTH_FAILED       = make( statuses, CLIENT + 2, Level.WARNING, "Authentication Failed", true );
    public static final AssertionStatus UNAUTHORIZED      = make( statuses, CLIENT + 2, Level.INFO, "Unauthorized", true );
    public static final AssertionStatus SERVICE_DISABLED  = make( statuses, CLIENT + 3, Level.INFO, "Service Disabled" );
    /** Couldn't resolve a service for this request */
    public static final AssertionStatus SERVICE_NOT_FOUND = make( statuses, CLIENT + 4, Level.INFO, "Service Not Found.  The request may have been sent to an invalid URL, or intended for an unsupported operation." );

    /** Assertion not yet implemented. */
    public static final AssertionStatus NOT_YET_IMPLEMENTED = make( statuses, MISC + 0, Level.INFO, "Not yet implemented!" );
    public static final AssertionStatus NOT_APPLICABLE      = make( statuses, MISC + 1, Level.INFO, "Not applicable in this context" );

    /** Generic catch-all status */
    public static final AssertionStatus SERVER_ERROR        = make( statuses, SERVER + 0, Level.INFO, "Internal Server Error" );
    public static final AssertionStatus SERVER_AUTH_FAILED  = make( statuses, SERVER + 1, Level.INFO, "Access Denied by Protected Service" );
    public static final AssertionStatus BAD_RESPONSE        = make( statuses, SERVER + 2, Level.INFO, "Bad Response from Protected Service" );
    public static final AssertionStatus SERVICE_UNAVAILABLE = make( statuses, SERVER + 3, Level.INFO, "Service Temporarily Unavailable" );

    /** The message may be valid, but does not satisfy a logical predicate */
    public static final AssertionStatus FALSIFIED = make( statuses, POLICY + 0, Level.INFO, "Assertion Falsified" );
    /** The assertion is unable to determine whether the message is acceptable; this does not automatically imply that the message is valid or invalid */
    public static final AssertionStatus FAILED    = make( statuses, POLICY + 1, Level.INFO, "Error in Assertion Processing" );

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
            return SoapConstants.FC_CLIENT;
        else
            return SoapConstants.FC_SERVER;
    }

    private static AssertionStatus make( Map registry, int numeric, Level level, String message, boolean auth ) {
        return new AssertionStatus( registry, numeric, level, message, auth );
    }

    private static AssertionStatus make( Map registry, int numeric, Level level, String message ) {
        return make( registry, numeric, level, message, false );
    }

    private AssertionStatus( Map registry, int numeric, Level level, String message, boolean auth ) {
        _numeric = numeric;
        _level = level;
        _message = message;
        _authProblem = auth;
        registry.put(new Integer(numeric), this);
    }

    /** @return assertion status for this integer, or null. */
    public static AssertionStatus fromInt(int status) {
        return (AssertionStatus)statuses.get(new Integer(status));
    }

    public String toString() {
        return "<" + getClass() + ": " + getNumeric() + "=" + getLevel().getName() + ":"  + getMessage() + ">";
    }

    protected final int _numeric;
    protected final Level _level;
    protected final String _message;
    protected final boolean _authProblem;
}
