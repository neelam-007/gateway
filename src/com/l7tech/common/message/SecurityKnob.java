/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.common.message;

import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;

import java.util.List;

/**
 * Provides access to any {@link SecurityToken}s that currently accompany this {@link Message}, and holds
 * any new decorations that are pending to be applied to this message.
 * <p>
 * The security tokens include both transport-level (e.g. {@link com.l7tech.common.security.token.http.HttpBasicToken}
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

    /**
     * Get the decorations that should be applied to this Message some time in the future. One DecorationRequirements
     * per recipient, the default recipient having its requirements at the end of the array. Can return an empty array
     * but never null.
     */
    DecorationRequirements[] getDecorationRequirements();

    /**
     * Get the decorations that should be applied to this Message some time in the future,
     * creating a new default set of decorations if there are no decorations pending.
     *
     * @return the current DecorationRequirements for this message, possibly newly created.  Never null.
     */
    DecorationRequirements getOrMakeDecorationRequirements();

    /**
     * Get the decoration requirements that should be applied to this Message in a security header addressed
     * to the specified recipient (role or actor).
     *
     * @param recipient   the recipient info whose decoration requirements are to be adjusted.  If null, this
     *                    method behaves identically to #getOrMakeDecorationRequirements().
     * @return the DecorationRequirements for this recipient, possibly newly created.  Never null.
     */
    DecorationRequirements getAlternateDecorationRequirements(XmlSecurityRecipientContext recipient);
}
