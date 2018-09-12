package com.l7tech.server.policy;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.SetsVariables;
import com.l7tech.policy.assertion.UsesVariables;

/**
 * Metadata for a policy. 
 *
 * @author steve
 */
public interface PolicyMetadata extends UsesVariables, SetsVariables {

    /**
     * Does this policy contain a WSS assertion?
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

    /**
     * Get the PolicyHeader for the Policy described by this metadata.
     *
     * @return the header information, or null if this is somehow not available.
     */
    PolicyHeader getPolicyHeader();
}
