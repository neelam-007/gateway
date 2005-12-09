/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.common.message;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;

import java.util.List;

/**
 * Provides access to any {@link SecurityToken}s that currently accompany this {@link Message}.
 * <p>
 * This includes both transport-level (e.g. {@link com.l7tech.common.security.token.http.HttpBasicToken}
 * and message-level (e.g. {@link com.l7tech.common.xml.saml.SamlAssertion}) tokens.
 */
public interface SecurityKnob extends MessageKnob {
    /**
     * @return the array of security tokens that accompany this message.  Never null, but often empty.
     */
    List getAllSecurityTokens();

    /**
     * Adds a {@link SecurityToken} to the current message.
     * Will not replace any token of the same type that might already be present.
     * @param token the token to be added. Must not be null.
     */
    void addSecurityToken(SecurityToken token);

    /**
     * Obtain the undecoration results for this Message, if it was undecorated.
     *
     * @return the ProcessorResult, or null if this Message has not been undecorated.
     */
    ProcessorResult getProcessorResult();

    /**
     * Store the undecoration results for this Message, if it has been undecorated.
     *
     * @param pr the results of undecorating this message, or null to remove any existing results.
     */
    void setProcessorResult(ProcessorResult pr);
}
