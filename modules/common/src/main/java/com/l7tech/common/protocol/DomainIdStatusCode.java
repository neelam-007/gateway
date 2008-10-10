package com.l7tech.common.protocol;

/**
 * Defines possible values for the {status} field in the L7-Domain-ID-Status header defined
 * by {@link com.l7tech.common.protocol.SecureSpanConstants.HttpHeaders#HEADER_DOMAINIDSTATUS},
 * and returned by {@link DomainIdStatusHeader}.
 */
public enum DomainIdStatusCode {
    /**
     * Status code indicating that the client did not attempt to gather identifier information.
     * This is the default status to be assumed by a recipient if no L7-Domain-ID-Status header is present
     * in a request.
     */
    NOTATTEMPTED,

    /**
     * Status code indicating that the client is aware that a policy calls for inclusion of identifier
     * information but declines to include any due to client-side policy.
     */
    DECLINED,

    /**
     * Status code indicating that the client gathered at least some identifier information.
     * The client may include identifier names with the names of additional headers that contain the
     * corresponding values.
     * <p/>
     * The client may choose to withhold some or all of the gathered information, depending on its
     * local policy, and it is legal to send a status of INCLUDED while not actually listing any
     * identifiers.
     */
    INCLUDED,

    /**
     * Status code indicating that the client attempted to gather identifier information but was unable
     * to obtain any.  No identifier values should be included with this status.  If any are included,
     * the recipient should ignore them.
     */
    FAILED,
}
