package com.l7tech.policy.validator;

/**
 * Flags that can be set by an assertion to support validation.
 *
 * @author Steve Jones
 * @see com.l7tech.policy.assertion.AssertionMetadata#POLICY_VALIDATOR_FLAGS_FACTORY
 */
public enum ValidatorFlag {

    /**
     * Flagged by an assertion if, as currently configured, it must be preceeded by a signature source.
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

    /**
     * Flagged by an assertion if, as currently configured, it peforms validation of the target message.
     *
     * This is used to to display a validation warning message if the assertion targets the
     * request after it is routed.
     *
     * This flag is (probably) not applicable for assertions that modify the message.
     *
     * NOTE: The recipient for the assertion is also checked, so the assertion does not need to do this. 
     */
    PERFORMS_VALIDATION,

    /**
     * Flagged by an assertion if, as currently configured, it will gather X.509 credentials.
     */
    GATHERS_X509_CREDENTIALS,

}
