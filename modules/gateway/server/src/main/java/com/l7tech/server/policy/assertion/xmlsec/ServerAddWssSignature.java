/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditHaver;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssDecorationConfig;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server assertion for Signing.
 *
 * This abstract class accommodates subclasses which may require signing sometimes, but not all times based on the
 * subclasses target assertions configuration.
 *
 * @author alex
 */
public abstract class ServerAddWssSignature<AT extends Assertion> extends AbstractMessageTargetableServerAssertion<AT> {
    protected final AddWssSignatureSupport addWssSignatureSupport;
    protected final Audit auditor;

    protected ServerAddWssSignature( final AT assertion,
                                     final WssDecorationConfig wssConfig,
                                     final MessageTargetable messageTargetable,
                                     final ApplicationContext spring,
                                     final Logger logger,
                                     final boolean failIfNotSigning ) {
        super(assertion, messageTargetable);
        this.auditor = new Auditor(this, spring, logger);
        AuditHaver auditHaver = new AuditHaver() {
            @Override
            public Audit getAuditor() {
                return auditor;
            }
        };
        this.addWssSignatureSupport = new AddWssSignatureSupport(auditHaver, wssConfig, spring, failIfNotSigning, Assertion.isResponse(assertion));
    }

    /**
     * This assertion is message targetable. By default it will be applied to the response if there are any
     * decoration requirements added. Otherwise an assertion like 'Apply WS-Security' is required for any
     * decoration requirements added to be applied.
     */
    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        return addWssSignatureSupport.applySignatureDecorationRequirements(context, message, messageDescription, authContext, hasDecorationRequirements(), new AddWssSignatureSupport.SignedElementSelector() {
            @Override
            public int selectElementsToSign(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage) throws PolicyAssertionException {
                return addDecorationRequirements(context, authContext, soapmsg, wssReq, targetMessage);
            }
        });
    }

    /**
     * If this method returns true, then any decoration requirements added via addDecorationRequirements will be
     * added to the target message. Override if a subclass wants to control when it needs to decorate based on it's
     * configuration.
     *
     * @return true when decoration requirements should be incorporated with the target message.
     */
    public boolean hasDecorationRequirements(){
        return true;
    }

    /** @see AddWssSignatureSupport.SignedElementSelector#selectElementsToSign */
    @SuppressWarnings({"JavaDoc"})
    protected abstract int addDecorationRequirements(PolicyEnforcementContext context, AuthenticationContext authContext, Document soapmsg, DecorationRequirements wssReq, Message targetMessage)
        throws PolicyAssertionException;

    @Override
    public Audit getAuditor() {
        return addWssSignatureSupport.getAuditor();
    }
}
