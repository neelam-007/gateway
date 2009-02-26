package com.l7tech.policy.validator;

/**
 * Flags that can be set by an assertion to support validation.
 *
 * @author Steve Jones
 * @see com.l7tech.policy.assertion.AssertionMetadata#POLICY_VALIDATOR_FLAGS_FACTORY
 */
public enum ValidatorFlag {

    /**
     * Flagged by an assertion if, as currently configured, it must be preceeded by a request signatuare source.
     */
    REQUIRE_SIGNATURE,

    /**
     * Flagged by a SecurityHeaderAddressable assertion if, as currently configured,
     * the Gateway server assertion's checkRequest() method does NOT bypass request enforcement
     * when configured with a non-local WSS recipient.
     * <p/>
     * This is used to suppress the validator warning
     * "A WSSRecipient other than Default will not be enforced by the gateway.  This assertion will always succeed."
     * that would otherwise be issued if the assertion was targeted at the Request.
     */
    PROCESSES_NON_LOCAL_WSS_RECIPIENT,    


}
