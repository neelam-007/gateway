/**
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */
package com.l7tech.message;

import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.security.xml.WsSecurityVersion;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.ProcessorException;
import com.l7tech.security.xml.processor.ProcessorResult;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

/**
 * Provides access to any {@link SecurityToken}s that currently accompany this {@link Message}, and holds
 * any new decorations that are pending to be applied to this message.
 * <p>
 * The security tokens include both transport-level (e.g. {@link com.l7tech.security.token.http.HttpBasicToken}
 * and message-level (e.g. {@link com.l7tech.xml.saml.SamlAssertion}) tokens.
 */
public interface SecurityKnob extends MessageKnob {
    /**
     * @return the array of security tokens that accompany this message.  Never null, but often empty.
     */
    List<SecurityToken> getAllSecurityTokens();

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
     * Obtain the undecoration results for this Message, running the undecorator lazily if it needs
     * to be done, and if an undecorator is available with the current implementation.
     * <p/>
     * Currently this method works in Client assertions but not Server assertions.
     *
     * @return the ProcessorResult, or null if this Message has not been undecorated and no
     *         lazy undecoration service is available.
     * @throws ProcessorException if processing fails
     * @throws java.io.IOException if there is a problem reading a message
     * @throws org.xml.sax.SAXException if there is a problem parsing the XML
     */
    ProcessorResult getOrCreateProcessorResult() throws ProcessorException, SAXException, IOException;

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
     * @return Current decoration requirements.  May be empty but never null.
     */
    DecorationRequirements[] getDecorationRequirements();

    /**
     * Tests whether there already exist decoration requirements for this message.
     *
     * @return true if decoration requirements have already been added.
     */
    boolean hasDecorationRequirements();

    /**
     * Get the decorations that should be applied to this Message some time in the future,
     * creating a new default set of decorations if there are no decorations pending.
     *
     * @return the current DecorationRequirements for this message, possibly newly created.  Never null.
     */
    DecorationRequirements getOrMakeDecorationRequirements();

    /**
     * Removes all decoration requirements. Should be called after decorations are applied to a message,
     * in order to prevent them from being applied more than once.
     */
    void removeAllDecorationRequirements();

    /**
     * Get the decoration requirements that should be applied to this Message in a security header addressed
     * to the specified recipient (role or actor).
     *
     * @param recipient   the recipient info whose decoration requirements are to be adjusted.  If null, this
     *                    method behaves identically to #getOrMakeDecorationRequirements().
     * @return the DecorationRequirements for this recipient, possibly newly created.  Never null.
     */
    DecorationRequirements getAlternateDecorationRequirements(XmlSecurityRecipientContext recipient);

    /**
     * Tests whether there are decoration requirements for the specified recipient for this message. If the recipient
     * is null or is local, then this is the same as calling #hasDecorationRequirements().
     *
     * @param recipient the recipient whose decoration requirements we want to check exist or not.
     * @return true if the decoration requirements for the recipient are found.
     */
    boolean hasAlternateDecorationRequirements(XmlSecurityRecipientContext recipient);

    /**
     * Gets the WSS version that the policy associates with a message target.
     */
    WsSecurityVersion getPolicyWssVersion();

    /**
     * Sets the WSS version that the policy associates with a message target.
     */
    void setPolicyWssVersion(WsSecurityVersion version);

    /**
     * Store the decoration results for this Message, if it has been decorated.
     */
    void addDecorationResult(WssDecorator.DecorationResult dr);

    /**
     * Obtain the decoration results for this Message, for the specified actor, if it was undecorated.
     *
     * @return a list of DecorationResult's if decorations were added to the Message for the specified actor, or null otherwise
     */
    List<WssDecorator.DecorationResult> getDecorationResults(String actor);

    /**
     * @return all decoration results applied to the message, for all actors.  May be empty but never null.
     */
    List<WssDecorator.DecorationResult> getAllDecorationResults();

    /**
     * Removes the decoration results recorded for the specified actor. Should be called when security headers are removed and/or their actor modified.
     */
    void removeDecorationResults(String actor);

    /**
     * @return true if signature confirmations were validated for this message,
     *         false if validation was not (yet) performed
     */
    boolean isSignatureConfirmationValidated();

    /**
     * Sets the status for signature confirmation validation.
     * Should be called by assertions that process and validate signature confirmations.
     */
    void setSignatureConfirmationValidated(boolean validated);

    /**
     * @return true if the signatures present in a (request) message need to be confirmed in the related response message
     */
    public boolean isNeedsSignatureConfirmations();

    /**
     * Marks a (request) message as requiring signature confirmations in the related response.
     */
    public void setNeedsSignatureConfirmations(boolean needsConfirmations);

    /**
     * @return true if there are any (pending) WSS 11 decorations, or false otherwise.
     */
    boolean hasWss11Decorations();
}
