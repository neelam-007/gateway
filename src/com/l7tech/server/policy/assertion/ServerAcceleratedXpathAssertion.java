/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TarariKnob;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.xml.InvalidXpathException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.common.xml.tarari.GlobalTarariContext;
import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.tarari.TarariMessageContextImpl;
import com.l7tech.common.xml.tarari.util.TarariXpathConverter;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.tarari.xml.xpath.RAXContext;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common code used by both {@link ServerRequestAcceleratedXpathAssertion} and {@link ServerResponseAcceleratedXpathAssertion}.
 * The "Accelerated" request and response xpath assertions are not "real" policy assertions; they are just
 * alternate implementations of the request and response xpath assertions that use the hardware instead.  The
 * ServerPolicyFactory instantiates the hardware-assisted versions if hardware support seems to be available.
 */
public abstract class ServerAcceleratedXpathAssertion implements ServerAssertion {
    protected static final Logger logger = Logger.getLogger(ServerAcceleratedXpathAssertion.class.getName());

    protected final ApplicationContext applicationContext;
    protected final String expr;
    protected final boolean isReq;
    protected final ServerAssertion softwareDelegate;
    private final Auditor auditor;

    /**
     * Prepare a hardware accelerated xpath assertion.
     *
     * @param assertion   the Request or Response xpath assertion containing the xpath expression to use.  Mustn't be null.
     * @param applicationContext  the application context from which to get the Tarari server context.  Mustn't be null.
     * @param softwareDelegate a ServerAssertion to which checkRequest() should be delegated if hardware acceleration can't be performed.
     */
    protected ServerAcceleratedXpathAssertion(XpathBasedAssertion assertion, ApplicationContext applicationContext, ServerAssertion softwareDelegate) {
        if (!(assertion instanceof RequestXpathAssertion) &&
            !(assertion instanceof ResponseXpathAssertion))
                throw new IllegalArgumentException(); // can't happen
        this.applicationContext = applicationContext;
        this.softwareDelegate = softwareDelegate;
        final XpathExpression xpathExpression = assertion.getXpathExpression();
        Map nsmap = xpathExpression.getNamespaces();
        String expr = xpathExpression.getExpression();
        isReq = assertion instanceof RequestXpathAssertion;
        try {
            // Convert this Xpath into tarari format
            expr = TarariXpathConverter.convertToTarariXpath(nsmap, expr);

            // Register this Xpath with the tarari hardware
            GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
            if (tarariContext != null)
                tarariContext.add(expr);
        } catch (InvalidXpathException e) {
            logger.log(Level.WARNING, "Assertion will always fail: Invalid Xpath expression: " + expr, e);
            expr = null;
        } catch (ParseException e) {
            logger.log(Level.WARNING, "Assertion will always fail: Invalid Xpath expression: " + expr, e);
            expr = null;
        }
        this.expr = expr;
        auditor = new Auditor(this, applicationContext, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (expr == null ) {
            auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
            return AssertionStatus.SERVER_ERROR;
        }

        GlobalTarariContext tarariContext = TarariLoader.getGlobalContext();
        if (tarariContext == null) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_HARDWARE);
            return softwareDelegate.checkRequest(context);
        }

        int index = tarariContext.getIndex(expr);
        if (index < 1) {
            auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_UNSUPPORTED_PATTERN);
            return softwareDelegate.checkRequest(context);
        }

        TarariKnob tknob = null;
        Message mess = isReq ? context.getRequest() : context.getResponse();
        try {
            // Ensure Tarari context is attached, if possible
            // TODO need a better way to attach this
            mess.isSoap();
            tknob = (TarariKnob) mess.getKnob(TarariKnob.class);
            if (tknob == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return softwareDelegate.checkRequest(context);
            }

            TarariMessageContext tmc = tknob.getContext();
            TarariMessageContextImpl tmContext = (TarariMessageContextImpl)tmc;
            if (tmContext == null) {
                auditor.logAndAudit(AssertionMessages.ACCEL_XPATH_NO_CONTEXT);
                return softwareDelegate.checkRequest(context);
            }

            RAXContext raxContext = tmContext.getRaxContext();
            int numMatches = raxContext.getCount(index);
            if (numMatches > 0) {
                auditor.logAndAudit(isReq ? AssertionMessages.XPATH_SUCCEED_REQUEST : AssertionMessages.XPATH_SUCCEED_RESPONSE);
                return AssertionStatus.NONE;
            } else {
                return AssertionStatus.FALSIFIED;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(isReq ? AssertionMessages.XPATH_REQUEST_NOT_XML : AssertionMessages.XPATH_RESPONSE_NOT_XML);
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
