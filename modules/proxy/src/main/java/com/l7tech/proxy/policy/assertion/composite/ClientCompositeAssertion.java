/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.composite;

import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.l7tech.proxy.policy.ClientPolicyFactory;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.ClientAssertionWithMetaSupport;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ClientCompositeAssertion extends ClientAssertionWithMetaSupport {
    private CompositeAssertion bean;
    protected ClientAssertion[] children;

    public ClientCompositeAssertion( CompositeAssertion composite ) throws PolicyAssertionException {
        super(composite);
        Assertion child;
        List result = new ArrayList();
        for (Iterator i = composite.children(); i.hasNext();) {
            child = (Assertion)i.next();
            ClientAssertion cass = ClientPolicyFactory.getInstance().makeClientPolicy(child);
            if (cass != null)
                result.add(cass);
        }
        this.children = (ClientAssertion[]) result.toArray( new ClientAssertion[0] );
        this.bean = composite;
    }

    /**
     * Call decorateRequest() on the specified client assertion, handling any HttpHandshakeRequiredException
     * it may throw, turning it into AssertionStatus.FAILED with the "authentication missing" flag set.
     *
     * @param assertion  the assertion on which to call decorateRequest().   Must not be null.
     * @param context    the context to use for the call.  Must not be null.
     * @return the assertion status.
     */
    protected final AssertionStatus decorateRequest(ClientAssertion assertion, PolicyApplicationContext context)
            throws ConfigurationException, InvalidDocumentFormatException, OperationCanceledException, PolicyAssertionException, GeneralSecurityException, IOException, KeyStoreCorruptException, ClientCertificateException, PolicyRetryableException, BadCredentialsException, SAXException
    {
        try {
            return assertion.decorateRequest(context);
        } catch (HttpChallengeRequiredException e) {
            context.getDefaultAuthenticationContext().setAuthenticationMissing();
            return AssertionStatus.FAILED;
        }
    }

    /**
     * Call unDecorateReply() on the specified client assertion.  This method currently takes no additional action;
     * it is just here for symmetry with
     * {@link #decorateRequest(com.l7tech.proxy.policy.assertion.ClientAssertion,
     *                         com.l7tech.proxy.message.PolicyApplicationContext)}, and in case
     * customization is required in the future.
     *
     * @param assertion  the assertion on which to call unDecorateReply().   Must not be null.
     * @param context    the context to use for the call.  Must not be null.
     * @return the assertion status.
     */
    protected final AssertionStatus unDecorateReply(ClientAssertion assertion, PolicyApplicationContext context)
            throws InvalidDocumentFormatException, OperationCanceledException, PolicyAssertionException, GeneralSecurityException, IOException, ResponseValidationException, KeyStoreCorruptException, BadCredentialsException, SAXException
    {
        return assertion.unDecorateReply(context);
    }

    public ClientAssertion[] getChildren() {
        return children;
    }

    /** Remove all this composite assertion's children's pending decorations. */
    protected void rollbackPendingDecorations(PolicyApplicationContext context) {
        context.getPendingDecorations().remove(this);
        for (int i = 0; i < children.length; i++) {
            ClientAssertion child = children[i];
            if (child instanceof ClientCompositeAssertion)
                ((ClientCompositeAssertion)child).rollbackPendingDecorations(context);
            context.getPendingDecorations().remove(child);
        }
    }

    /**
     * Ensure that this CompositeAssertion has at least one child.
     * @throws com.l7tech.policy.assertion.PolicyAssertionException if the children list is empty
     */
    public void mustHaveChildren() throws PolicyAssertionException {
        if ( children.length == 0 )
            throw new PolicyAssertionException(bean, "CompositeAssertion has no children: " + this);
    }

    @Override
    public String toString() {
        return "<" + this.getClass().getName() + " children=" + children + ">";
    }

    protected void mustHaveChildren(CompositeAssertion data) throws PolicyAssertionException {
        if (data.getChildren().isEmpty())
            throw new PolicyAssertionException(bean, "CompositeAssertion has no children: " + this);
    }

    @Override
    public void visit(ClientAssertionVisitor visitor) {
        visitor.visit(this);
        for (ClientAssertion child : children) {
            child.visit(visitor);
        }        
    }
}
