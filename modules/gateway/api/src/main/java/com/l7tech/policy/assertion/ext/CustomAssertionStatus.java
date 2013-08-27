package com.l7tech.policy.assertion.ext;

/**
 * Error conditions that could result from processing a Custom Assertion.
 */
public enum CustomAssertionStatus {

    /** Assertion finished successfully */
    NONE,

    /** Credentials present but erroneous */
    AUTH_FAILED,
    UNAUTHORIZED,

    /** The message may be valid, but does not satisfy a logical predicate */
    FALSIFIED,

    /** The assertion is unable to determine whether the message is acceptable; this does not automatically imply that the message is valid or invalid */
    FAILED
}
