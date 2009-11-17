package com.l7tech.server.policy;

/**
 * Metadata for a policy. 
 *
 * @author steve
 */
public interface PolicyMetadata {

    /**
     * Does at least one assertion in the policy strongly prefer to use Tarari?
     *
     * <p>As opposed to use of a pre-parsed DOM tree. </p>
     *
     * @return true if tarari is desired.
     */
    boolean isTarariWanted();

    /**
     * Does this policy contain a WSS assertion?
     *
     * <p>If this is the case, then DOM would likely be better than using Tarari.</p>
     *
     * @return true if there is WSS in the policy.
     */
    boolean isWssInPolicy();

    /**
     * Does at least one assertion in the policy permit multipart data?
     *
     * <p>This can be used to determine if a request should be processed by a
     * particular policy.</p>
     *
     * @return true if the policy processes (request) multipart.
     */
    boolean isMultipart();
}
