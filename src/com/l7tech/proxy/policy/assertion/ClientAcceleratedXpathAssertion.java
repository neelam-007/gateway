/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.policy.assertion.*;
import com.l7tech.proxy.ConfigurationException;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.message.PolicyApplicationContext;
import com.tarari.xml.xpath.RAXContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Superclass for hardware-accelerated XPaths in the policy application code.
 */
public class ClientAcceleratedXpathAssertion extends ClientXpathAssertion {
    private static final Logger logger = Logger.getLogger(ClientAcceleratedXpathAssertion.class.getName());
    private static final Auditor auditor = new Auditor(null, logger);
    protected final String expr;
    protected final ClientAssertion softwareDelegate;

    /**
     * Prepare a hardware accelerated xpath assertion.
     *
     * @param assertion   the Request or Response xpath assertion containing the xpath expression to use.  Mustn't be null.
     * @param isRequest  true if this applies to the Request message; false if it applies to the response.
     * @param softwareDelegate a ServerAssertion to which checkRequest() should be delegated if hardware acceleration can't be performed.
     */
    protected ClientAcceleratedXpathAssertion(XpathBasedAssertion assertion, boolean isRequest, ClientAssertion softwareDelegate) {
        super(assertion, isRequest);
        if (!(assertion instanceof RequestAcceleratedXpathAssertion) &&
            !(assertion instanceof ResponseAcceleratedXpathAssertion))
                throw new IllegalArgumentException(); // can't happen
        this.softwareDelegate = softwareDelegate;
        String expr = assertion.getXpathExpression().getExpression();
        try {
            // Register this Xpath with the tarari hardware
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.add(expr);
        } catch (InvalidXpathException e) {
            logger.log(Level.WARNING, "Assertion will always fail: Invalid Xpath expression: " + expr, e);
            expr = null;
        }
        this.expr = expr;
    }

    public AssertionStatus decorateRequest(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, ClientCertificateException, IOException, SAXException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, PolicyAssertionException, InvalidDocumentFormatException, ConfigurationException
    {
        if (!isRequest)
            return AssertionStatus.NONE; // No action required during request decoration stage

        final AssertionStatus result = checkMatch(context.getRequest());
        if (result == null)
            return softwareDelegate.decorateRequest(context);
        return result;
    }

    public AssertionStatus unDecorateReply(PolicyApplicationContext context) throws BadCredentialsException, OperationCanceledException, GeneralSecurityException, IOException, SAXException, ResponseValidationException, KeyStoreCorruptException, PolicyAssertionException, InvalidDocumentFormatException
    {
        final AssertionStatus result;
        if (isRequest)
            result = checkMatch(context.getRequest()); // take same policy branches during undecorate stage as we took during decorate stage
        else
            result = checkMatch(context.getResponse());
        if (result == null)
            return softwareDelegate.unDecorateReply(context);
        return result;
    }

    private AssertionStatus checkMatch(Message mess) throws IOException
    {
        if (expr == null ) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.SERVER_ERROR;
        }

        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
        if (tarariContext == null) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_HARDWARE);
            return null;
        }

        int index = tarariContext.getIndex(expr);
        if (index < 1) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_UNSUPPORTED_PATTERN);
            return null;
        }

        TarariKnob tknob = null;
        try {
            // Ensure Tarari context is attached, if possible
            // TODO need a better way to attach this
            mess.isSoap();
            tknob = (TarariKnob) mess.getKnob(TarariKnob.class);
            if (tknob == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return null;
            }

            TarariMessageContext tmc = tknob.getContext();
            TarariMessageContextImpl tmContext = (TarariMessageContextImpl)tmc;
            if (tmContext == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return null;
            }

            RAXContext raxContext = tmContext.getRaxContext();
            int numMatches = raxContext.getCount(index);
            if (numMatches > 0) {
                auditor.logAndAudit(isRequest ? AssertionMessages.XPATH_SUCCEED_REQUEST : AssertionMessages.XPATH_SUCCEED_RESPONSE);
                return AssertionStatus.NONE;
            } else {
                return AssertionStatus.FALSIFIED;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(isRequest ? AssertionMessages.XPATH_REQUEST_NOT_XML : AssertionMessages.XPATH_RESPONSE_NOT_XML);
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"The required attachment " + e.getWhatWasMissing() + "was not found in the request"}, e);
            return AssertionStatus.FAILED;
        }
    }

    protected void finalize() throws Throwable {
        if (expr != null) {
            // Decrement the reference count for this Xpath with the Tarari hardware
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.remove(expr);
        }
        super.finalize();
    }
}
